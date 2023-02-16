package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.AdminUtil;
import top.strelitzia.arknightsDao.*;
import top.strelitzia.dao.AdminUserMapper;
import top.strelitzia.model.*;
import top.strelitzia.util.FormatStringUtil;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/
@Service
@Slf4j
public class UpdateDataService {

    @Autowired
    private UpdateMapper updateMapper;

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    private BuildingSkillMapper buildingSkillMapper;

    @Autowired
    private SkinInfoMapper skinInfoMapper;

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private MaterialMadeMapper materialMadeMapper;

    @Autowired
    private EnemyMapper enemyMapper;

    @Autowired
    private EquipMapper equipMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private SkillDescMapper skillDescMapper;

    @Autowired
    private AgentTagsMapper agentTagsMapper;

    //    private String url = "https://cdn.jsdelivr.net/gh/Kengxxiao/ArknightsGameData@master/zh_CN/gamedata/";
//    private String url = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/";
//    private String url = "http://vivien8261.gitee.io/arknights-bot-resource/gamedata/";
    private final String url = "https://raw.fastgit.org/yuanyan3060/Arknights-Bot-Resource/master/";
//    private final String url = "https://raw.githubusercontent.com/yuanyan3060/Arknights-Bot-Resource/main/";

    /**
     * 先判断版本是否相同→如果版本不同，开始更新→置位1→下载数据文件→置位0→下载完成→置位2→写入数据→置位0→写入完成
     */
    private static int updateStatus = 0;

    @AngelinaGroup(keyWords = {"更新"}, description = "尝试更新数据", sort = "权限功能")
    @AngelinaFriend(keyWords = {"更新"}, description = "尝试更新数据")
    public ReplayInfo downloadDataFile(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您无更新权限");
        } else {
            if (updateStatus == 0) {
                DownloadOneFileInfo downloadInfo = new DownloadOneFileInfo();

                if (messageInfo.getArgs().size() > 2) {
                    downloadInfo.setHostname(messageInfo.getArgs().get(1));
                    downloadInfo.setPort(Integer.parseInt(messageInfo.getArgs().get(2)));
                    downloadInfo.setUseHost(true);
                } else {
                    downloadInfo.setUseHost(false);
                }
                boolean finish = downloadDataFile(downloadInfo);
                if (finish) {
                    replayInfo.setReplayMessage("更新完成");
                } else {
                    replayInfo.setReplayMessage("更新失败，请从后台日志查看更新情况");
                }
            } else if (updateStatus == 1) {
                replayInfo.setReplayMessage("正在下载数据文件中，请稍后再试");
            } else {
                replayInfo.setReplayMessage("正在写入数据库中，请稍后再试");
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"全量更新"}, description = "强制全量更新数据", sort = "权限功能")
    @AngelinaFriend(keyWords = {"全量更新"}, description = "强制全量更新数据")
    public ReplayInfo downloadDataFileForce(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您无更新权限");
        } else {
            if (updateStatus == 0) {
                DownloadOneFileInfo downloadInfo = new DownloadOneFileInfo();
                rebuildDatabase();
                if (messageInfo.getArgs().size() > 2) {
                    downloadInfo.setHostname(messageInfo.getArgs().get(1));
                    downloadInfo.setPort(Integer.parseInt(messageInfo.getArgs().get(2)));
                    downloadInfo.setUseHost(true);
                } else {
                    downloadInfo.setUseHost(false);
                }
                deleteAllFile(Paths.get("runFile/download"));
                log.info("正在执行全量更新，旧数据文件已完成清理");
                boolean finish = downloadDataFile(downloadInfo);
                if (finish) {
                    replayInfo.setReplayMessage("更新完成");
                } else {
                    replayInfo.setReplayMessage("更新失败，请从后台日志查看更新情况");
                }
            } else if (updateStatus == 1) {
                replayInfo.setReplayMessage("正在下载数据文件中");
            } else {
                replayInfo.setReplayMessage("正在写入数据库中");
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"更新语音"}, description = "更新语音数据", sort = "权限功能")
    @AngelinaFriend(keyWords = {"更新语音"}, description = "更新语音数据")
    public ReplayInfo updateVoiceFile(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您无更新权限");
        } else {
            DownloadOneFileInfo downloadInfo = new DownloadOneFileInfo();
            if (messageInfo.getArgs().size() > 2) {
                downloadInfo.setHostname(messageInfo.getArgs().get(1));
                downloadInfo.setPort(Integer.parseInt(messageInfo.getArgs().get(2)));
                downloadInfo.setUseHost(true);
            } else {
                downloadInfo.setUseHost(false);
            }
            updateOperatorVoice(downloadInfo);
            replayInfo.setReplayMessage("更新语音完成");
        }
        return replayInfo;
    }

//    @AngelinaGroup(keyWords = {"更新卡池"}, description = "更新卡池数据", sort = "权限功能")
//    public ReplayInfo update(MessageInfo messageInfo) {
//        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
//        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
//            replayInfo.setReplayMessage("您无更新权限");
//        } else {
//            updateGachaPoolInfo();
//            replayInfo.setReplayMessage("更新卡池完成");
//        }
//        return replayInfo;
//    }
//
//    private void updateGachaPoolInfo() {
//        //获取卡池数据json
//        JSONObject gachaPool = new JSONObject(getJsonStringFromFile("gacha_table.json"));
//        log.info("提取卡池列表");
//        JSONArray gachaPoolClient = gachaPool.getJSONArray("gachaPoolClient");
//        for (Object pool : gachaPoolClient) {
//            GachePoolInfo gachePoolInfo = new GachePoolInfo();
//            gachePoolInfo.setGachaPoolId(((JSONObject) pool).getString("gachaPoolId"));
//            gachePoolInfo.setGachaIndex(((JSONObject) pool).getInt("gachaIndex"));
//            gachePoolInfo.setGachaPoolName(((JSONObject) pool).getString("gachaPoolName"));
//            gachePoolInfo.setOpenTime(((JSONObject) pool).getInt("openTime"));
//            gachePoolInfo.setEndTime(((JSONObject) pool).getInt("endTime"));
//            gachePoolInfo.setGachaRuleType(((JSONObject) pool).getString("gachaRuleType"));
//            updateMapper.insertGachePool(gachePoolInfo);
//        }
//    }

    /**
     * 数据文件下载判断，以最新数据判定是否需要下载数据文件
     * 当需要下载时，如果存在符合条件的压缩包，则在后续更新中全部启动压缩包提取模式
     * @param downloadInfo 下载信息，主要用于带入代理信息
     * @return 更新结果
     */
    public boolean downloadDataFile(DownloadOneFileInfo downloadInfo) {
        downloadInfo.setUrl(url + "gamedata/excel/data_version.txt");
        String charKey;
        try {
            charKey = getJsonStringFromUrl(downloadInfo);
        } catch (IOException e) {
            log.error("在线dataVersion获取出现故障，问题原因为：" + e);
            return false;
        }

        //确保状态是未正在下载
        if (updateStatus == 0) {
            File dataVersionFile = new File("runFile/download/data_version.txt");
            boolean Download = true;
            //version文件不存在时，进行下载操作
            if (dataVersionFile.exists()) {
                try {
                    String dataVersion = getJsonStringFromFile("data_version.txt");
                    //version文件存在且和线上version不相等时，进行下载操作
                    if (dataVersion.replace("\n", "").equals(charKey.replace("\n", ""))) {
                        log.info("线上版本和当前数据文件相同，无需下载");
                        Download = false;
                    } else {
                        log.info("线上版本和当前数据文件不同，准备下载");
                    }
                } catch (NullPointerException e) {
                    log.info("数据文件不存在，准备下载");
                }
            } else {
                log.info("数据文件不存在，准备下载");
            }

            if (Download) {
                updateStatus = 1;
                downloadInfo.setPullZip(checkZipVersion(charKey));
                if (!updateDateFile(downloadInfo)) {
                    return false;
                }
            }
            return updateAllData(new DownloadOneFileInfo(downloadInfo));
        } else if (updateStatus == 1) {
            log.warn("数据文件正在下载中，无法重复下载，请等待文件下载完成");
            return false;
        } else {
            log.warn("数据库正在写入数据中，请等待更新完成");
            return false;
        }
    }

    /**
     * 直接获取固定位置压缩包内的dataVersion数据与输入的版本数据匹配情况
     * 压缩包内的数据不写为文件，直接以数据流形式在内存中对比
     * @param charKey 需要对比的版本数据
     * @return 是否匹配，true为匹配，false为不匹配
     */
    private boolean checkZipVersion(String charKey){
        try {
            String zipPath = "Arknights-Bot-Resource-main/gamedata/excel/data_version.txt";
            String zipFileName = "Arknights-Bot-Resource-main.zip";
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("runFile/zipDateFile/" + zipFileName), Charset.forName("GBK"));
            ZipEntry zipEntry;
            String dataVersion = "";
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(zipPath)){
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(zipInputStream))){
                        StringBuilder stringBuilder = new StringBuilder();
                        String content;
                        while ((content = br.readLine()) != null) {
                            stringBuilder.append(content);
                        }
                        dataVersion = stringBuilder.toString();
                    }
                    log.info("已获取压缩包数据，正在进行版本对比");
                    break;
                }
            }
            zipInputStream.close();
            if (dataVersion.replace("\n", "").equals(charKey.replace("\n", ""))) {
                log.info("压缩包版本匹配当前版本号");
                return true;
            } else {
                log.info("压缩包版本与当前版本不同，切换为云端下载");
                return false;
            }
        } catch (IOException | NullPointerException e) {
            log.info("未找到压缩包数据文件，尝试进行云端下载");
            return false;
        }
    }

    /**
     * 尝试寻找zip文件并提取所需文件
     * 提供两种提取方式，一种是文件夹方式提取，另一种是单独提取文件
     *
     * @param single  是否使用单独提取
     * @param takeMap 提取文件表
     *                当模式为单独提取时，key为zip文件目录，value为runFile文件目录\n
     *                当模式为文件夹提取时，key为zip文件夹目录，value为runFile文件夹目录
     * @throws IOException 抛出文件提取失败信息
     */
    private void pullFromZipFile(boolean single, Map<String, String> takeMap) throws IOException {
        String zipFileName = "Arknights-Bot-Resource-main.zip";
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("runFile/zipDateFile/" + zipFileName), Charset.forName("GBK"));
        ZipEntry zipEntry;
        if (single) {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (takeMap.containsKey(zipEntry.getName())) {
                    String filePath = takeMap.get(zipEntry.getName());
                    try (FileOutputStream f = new FileOutputStream(filePath)) {
                        // 提取该数据文件到指定目录
                        f.write(zipInputStream.readAllBytes());
                    }
                    takeMap.remove(zipEntry.getName());
                }
                if (takeMap.size() == 0) return;
            }
        } else {
            boolean download = false;
            //用于保存正在读取的zip文件夹目录
            String currentFolder = "";
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (download && !zipEntry.getName().contains(currentFolder)) {
                    download = false;
                    takeMap.remove(currentFolder);
                }
                if (download) {
                    //读取截断到/后的文件名字
                    String fileName = zipEntry.getName().substring(zipEntry.getName().lastIndexOf("/") + 1);
                    String filePath = takeMap.get(currentFolder) + fileName;
                    try (FileOutputStream f = new FileOutputStream(filePath)) {
                        // 提取该数据文件到指定目录
                        f.write(zipInputStream.readAllBytes());
                    }
                }
                if (!download && takeMap.containsKey(zipEntry.getName())) {
                    download = true;
                    currentFolder = zipEntry.getName();
                }
            }
        }
    }

    /**
     * 数据文件的清理和下载任务清单派发节点
     *
     * @param downloadInfo 获取是否启用代理的代理信息
     * @return 下载是否成功
     */
    private boolean updateDateFile(DownloadOneFileInfo downloadInfo) {
        //文件夹表
        List<String> directoryList = Arrays.asList(
                "download",
                "skin",
                "voice",
                "operatorPng",
                "itemIcon",
                "avatar",
                "skill");
        for (String directoryName : directoryList) {
            Path path = Paths.get("runFile/" + directoryName);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            if (directoryName.equals("download")) {
                deleteAllFile(path);
                log.info("旧数据文件已完成清理");
            }
        }

        //数据文件表
        Map<String, String> fileOfDirectory = new HashMap<>() {{
            put("character_table.json", "excel");
            put("gacha_table.json", "excel");
            put("skill_table.json", "excel");
            put("building_data.json", "excel");
            put("handbook_info_table.json", "excel");
            put("charword_table.json", "excel");
            put("char_patch_table.json", "excel");
            put("item_table.json", "excel");
            put("skin_table.json", "excel");
            put("battle_equip_table.json", "excel");
            put("uniequip_table.json", "excel");
            put("enemy_database.json", "levels/enemydata");
        }};

        if (downloadInfo.isPullZip()) {
            String zipHeader = "Arknights-Bot-Resource-main/gamedata/";
            String downloadHeader = "runFile/download/";
            Map<String,String> takeMap = new HashMap<>();
            for (String fileName : fileOfDirectory.keySet()){
                takeMap.put(zipHeader + fileOfDirectory.get(fileName) + "/" + fileName, downloadHeader + fileName);
            }
            takeMap.put(zipHeader + "excel/data_version.txt", downloadHeader + "data_version.txt");
            try {
                pullFromZipFile(true,takeMap);
                updateStatus = 0;
                log.error("压缩包数据文件提取完成");
                return true;
            } catch (IOException e) {
                log.error("拉取出现错误，失败原因："+ e +"\n重新尝试下载");
            }
        }
        boolean result = true;
        log.info("开始下载数据文件");
        String fileName = "";
        try {
            for (String s : fileOfDirectory.keySet()) {
                fileName = s;
                downloadInfo.setSecond(1200);
                downloadInfo.setFileName("runFile/download/" + fileName);
                downloadInfo.setUrl(url + "gamedata/" + fileOfDirectory.get(fileName) + "/" + fileName);
                downloadOneFile(downloadInfo);
            }
        } catch (IOException e) {
            result = false;
            log.error("下载数据文件{}失败，请单独手动替换文件，问题原因：\n" + e, fileName);
            Path path = Paths.get("runFile/download/" + fileName);
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (result) {
            Path path = Paths.get("runFile/download/data_version.txt");
            downloadInfo.setFileName(path.toString());
            downloadInfo.setUrl(url + "gamedata/excel/data_version.txt");
            try {
                downloadOneFile(downloadInfo);
                log.info("数据文件下载完成");
                updateStatus = 0;
                return true;
            } catch (IOException e) {
                log.error("下载data_version失败，请单独手动替换文件，问题原因：\n" + e);
                if (Files.exists(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        log.error(ex.toString());
                    }
                }
                updateStatus = 0;
                return false;
            }
        }else {
            updateStatus = 0;
            return false;
        }
    }

    /**
     * 删除目标path下所有文件
     * @param path 目标path
     */
    private void deleteAllFile(Path path){
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                // 遍历删除文件
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文件下载方法，先检索文件是否有存在，如果存在将跳过下载
     * @param downloadInfo 带入所有封装的下载信息进行下载
     * @throws IOException 下载IO错误报错
     */
    private void downloadOneFile(DownloadOneFileInfo downloadInfo) throws IOException {
        File file = new File(downloadInfo.getFileName());
        if (file.exists()) {
            log.info("{}文件已存在，无需下载", downloadInfo.getFileName());
            return;
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpGet = new HttpGet(downloadInfo.getUrl());
        if (downloadInfo.isUseHost()) {
            //如果要使用代理，加上代理服务器信息
            HttpHost httpHost = new HttpHost(downloadInfo.getHostname(), downloadInfo.getPort());
            RequestConfig config = RequestConfig.custom().setProxy(httpHost).build();
            httpGet.setConfig(config);
        }
        response = httpClient.execute(httpGet);
        //判断响应状态为200，进行处理
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();//获取响应文本体
            try (FileOutputStream fs = new FileOutputStream(downloadInfo.getFileName())) {
                fs.write(EntityUtils.toByteArray(httpEntity));
            }
            log.info("下载{}文件成功", downloadInfo.getFileName());
        } else {
            //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理
            log.error("下载{}文件失败"+response.getStatusLine().getStatusCode(), downloadInfo.getFileName());
        }
    }

    /**
     * 发送url的get请求获取结果json字符串
     * @param downloadInfo 相关信息
     * @return 返回结果String
     */
    public String getJsonStringFromUrl(DownloadOneFileInfo downloadInfo) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpGet = new HttpGet(downloadInfo.getUrl());
//        httpGet.setHeader("User-Agent", "PostmanRuntime/7.26.8");
//        httpGet.setHeader("Authorization", "2");
//        httpGet.setHeader("Host", "andata.somedata.top");
        if (downloadInfo.isUseHost()) {
            //如果要使用代理，加上代理服务器信息
            HttpHost httpHost = new HttpHost(downloadInfo.getHostname(), downloadInfo.getPort());
            RequestConfig config = RequestConfig.custom().setProxy(httpHost).build();
            httpGet.setConfig(config);
        }
        response = httpclient.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();//获取响应文本体
        return EntityUtils.toString(httpEntity, "utf-8");
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.set("User-Agent", "PostmanRuntime/7.26.8");
//        httpHeaders.set("Authorization", "2");
//        httpHeaders.set("Host", "andata.somedata.top");
//        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);
//        String s = null;
//        try {
//            s = restTemplate
//                    .exchange(url, HttpMethod.GET, httpEntity, String.class).getBody();
//        } catch (Exception ignored) {
//
//        }
//        return s;
    }

    /**
     * 读取文件的内容字符串
     * @param fileName url
     * @return 返回结果String
     */
    public String getJsonStringFromFile(String fileName) {
        Path path = Paths.get("runFile/download/" + fileName);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return null;
        }
//        File file = new File("runFile/download/" + fileName);
//        StringBuilder lastStr = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
//            // System.out.println("以行为单位读取文件内容，一次读一整行：");
//            String tempString;
//            // 一次读入一行，直到读入null为文件结束
//            while ((tempString = reader.readLine()) != null) {
//                lastStr.append(tempString);
//            }
//        } catch (IOException e) {
//            log.warn("读取错误：" + e);
//            return null;
//        }
//        return lastStr.toString();
    }

    /**
     * 更新总支，分别调用所有的子更新方法
     * @param downloadOneFileInfo 封装更新信息，传入代理部分参数
     * @return 更新是否成功
     */
    public boolean updateAllData(DownloadOneFileInfo downloadOneFileInfo) {
        String charKey = getJsonStringFromFile("data_version.txt");
        if (charKey == null) {
            return false;
        }
        String dataVersion = updateMapper.getVersion();
        if (dataVersion == null) {
            updateMapper.insertVersion();//如果不存在，手动更新一个0出来避免后续无法更新数据库的版本号
        }
        try {
            if (updateStatus == 0) {
                if (!charKey.equals(dataVersion)) {
                    log.info("数据库和数据文件版本不同，开始更新全部数据");
                    if (!downloadOneFileInfo.isPullZip() || checkZipVersion(charKey)){
                        downloadOneFileInfo.setPullZip(true);
                        log.info("压缩包与数据文件版本匹配，尝试从压缩包提取资料");
                    }
                    updateStatus = 2;
//                    //清理干员数据(因部分召唤物无char_id，不方便进行增量更新)
//                    log.info("清理干员数据");
//                    updateMapper.clearUnknownData();
                    updateAllOperator();
                    updateOperatorEquipByJson();
                    updateAllEnemy();
                    updateMapAndItem();
                    updateSkin(downloadOneFileInfo);
                    updateItemIcon(downloadOneFileInfo);
                    updateOperatorPng(downloadOneFileInfo);
                    updateOperatorSkillPng(downloadOneFileInfo);
//                    updateOperatorVoice(downloadOneFileInfo);
                    updateMapper.updateVersion(charKey);
                    updateStatus = 0;
                    //updateMapper.doneUpdateVersion();
                    log.info("游戏数据更新完成--");
                } else {
                    log.info("数据库和数据文件版本相同，无需更新");
                }
            } else if (updateStatus == 1) {
                log.info("数据文件正在下载中，无法重复下载，请等待文件下载完成");
            } else {
                log.warn("数据库正在写入数据中，请等待更新完成");
            }
        } catch (JSONException e) {
            updateStatus = 0;
            log.warn("json解析出现错误，请检查json格式与完整性");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private static List<String> gachaCharList;

    /**
     * 公招tag字段注入数组
     * 如已更新，则读取已更新的值，如未更新，更新tag数组
     * @return 已更新的tag数组
     */
    private List<String> getGachaCharList(){
        if (gachaCharList == null) {
            //获取游戏公招描述部分以得到一个含有所有公招干员的表
            String recruit = new JSONObject(getJsonStringFromFile("gacha_table.json")).getString("recruitDetail");
            Pattern pattern = Pattern.compile("<(.*?)>");//此正则匹配xml格式的value
            Matcher matcher = pattern.matcher(recruit);
            String replaceAll = matcher.replaceAll("").replace(" ", "");
            String[] split = replaceAll.split("\n");
            //解析出全部的公招干员
            gachaCharList = new ArrayList<>();
            for (String s : split) {
                if (s.startsWith("★")) {
                    String[] chars = s.replace("★", "").replace("\\n", "").split("/");
                    gachaCharList.addAll(Arrays.asList(chars));
                }
            }
        }
        return gachaCharList;
    }

    /**
     * 更新所有干员数据登记到数据库中
     * 由于阿米娅的干员数据位于另一个json中，利用写入与遍历单独更新
     * 先通过更新基础数据，得到一个自增id
     * 以此自增id，分别更新干员的档案数据与cv配音数据
     * 召唤物只更新基础数据，并另置表为其更新
     */
    private void updateAllOperator(){
        //获取全部干员档案数据
        JSONObject infoTableJson = new JSONObject(getJsonStringFromFile("handbook_info_table.json")).getJSONObject("handbookDict");
        //获取全部干员技能数据
        JSONObject skillJson = new JSONObject(getJsonStringFromFile("skill_table.json"));
        //获取全部基建技能数据
        JSONObject buildingJson = new JSONObject(getJsonStringFromFile("building_data.json"));
        //获取配音演员档案数据
        JSONObject CVNameJson = new JSONObject(getJsonStringFromFile("charword_table.json")).getJSONObject("voiceLangDict");

        //分别获取全部干员基础数据与异格阿米娅干员基础数据
        List<JSONObject> operatorJsonList = new LinkedList<>(){{
            add(new JSONObject(getJsonStringFromFile("character_table.json")));
            add(new JSONObject(getJsonStringFromFile("char_patch_table.json")).getJSONObject("patchChars"));
        }};

        for (JSONObject operatorJson : operatorJsonList) {
            List<String> list = getGachaCharList();
            //利用迭代器依次取出干员信息
            for (Iterator<String> keys = operatorJson.keys(); keys.hasNext(); ) {
                String charId = keys.next();
                JSONObject operatorInfo = operatorJson.getJSONObject(charId);
                //鹰角坏事做尽！这里的null是个object的null值，不可直接以"== NULL"判断
                boolean isOperator = operatorInfo.get("itemObtainApproach") instanceof String;
                Integer id = operatorInfoMapper.getOperatorIdByChar(charId);
                if (id == null) {
                    id = operatorInfoMapper.getSummonerIdByChar(charId);
                    if (id != null) {
                        log.info("特殊召唤物{}已存在", charId);
                        continue;
                    }
                }else {
                    log.info("干员{}已存在", charId);
                    continue;
                }

                String name;
                if (isOperator) {
//                  置入干员表
                    Integer operatorId = updateOperatorByJson(charId, operatorInfo, skillJson, buildingJson);
                    String tempCharId;//由于升变阿米娅没有专用档案，改用阿米娅的ID去获取升变阿米娅的档案
                    if (charId.equals("char_1001_amiya2")) {
                        name = "近卫阿米娅";
                        tempCharId = "char_002_amiya";
                    } else {
                        name = operatorInfo.getString("name").trim();
                        tempCharId = charId;
                    }
                    JSONObject jsonObject = infoTableJson.getJSONObject(tempCharId);
                    JSONObject jsonObject1 = CVNameJson.getJSONObject(charId);
                    updateOperatorInfoById(charId, operatorId, jsonObject);
                    updateDubberInfoById(operatorId, jsonObject1);
                    // 判断干员名是否存在公招描述中
                    if (list.contains(name)) {
                        updateOperatorTag(charId, operatorInfo);
                    }
                }else {
//                  置入召唤物列表
                    Integer operatorId = updateOperatorInfo(false, charId, operatorInfo);
                    updatePanelData(false, operatorId, operatorInfo);

                }
            }
        }
        log.info("干员数据更新完成");
    }

    /**
     * 插入一条干员基础信息（档案、画师）
     * @param charId  干员char_id
     * @param operatorId 数据库中的干员Id
     */
    private void updateOperatorInfoById(String charId, Integer operatorId, JSONObject infoJsonObj) {
        OperatorBasicInfo operatorBasicInfo = new OperatorBasicInfo();
        operatorBasicInfo.setOperatorId(operatorId);
        operatorBasicInfo.setDrawName(takeDrawName(charId));
        JSONArray storyTextAudio = infoJsonObj.getJSONArray("storyTextAudio");
        for (int i = 0; i < storyTextAudio.length(); i++) {
            JSONObject story = storyTextAudio.getJSONObject(i);
            String storyText = story.getJSONArray("stories").getJSONObject(0).getString("storyText");
            String storyTitle = story.getString("storyTitle");
            switch (storyTitle) {
                case "基础档案" -> {
                    String[] split = storyText.split("\n");
                    int point = storyText.lastIndexOf("【矿石病感染情况】");
                    if(point != -1){
                        String infection = storyText.substring(point+9);
                        operatorBasicInfo.setInfection(infection);
                    }else {
                        int platformPoint = storyText.lastIndexOf("【维护检测报告】");
                        String infection = storyText.substring(platformPoint+8);
                        operatorBasicInfo.setInfection(infection);
                    }
                    for (String s : split) {
                        if (s.length() < 1) {
                            break;
                        }
                        String[] basicText = s.substring(1).split("】");
                        switch (basicText[0]) {
                            case "代号","型号" -> operatorBasicInfo.setCodeName(basicText[1].trim());
                            case "性别","设定性别" -> operatorBasicInfo.setSex(basicText[1].trim());
                            case "出身地","产地" -> operatorBasicInfo.setComeFrom(basicText[1].trim());
                            case "生日", "出厂日" -> operatorBasicInfo.setBirthday(basicText[1].trim());
                            case "种族","制造商" -> operatorBasicInfo.setRace(basicText[1].trim());
                            case "身高","高度" -> {
                                String str = basicText[1];
                                StringBuilder str2 = new StringBuilder();
                                if (str != null && !"".equals(str)) {
                                    for (int j = 0; j < str.length(); j++) {
                                        if (str.charAt(j) >= 48 && str.charAt(j) <= 57) {
                                            str2.append(str.charAt(j));
                                        }
                                    }
                                }
                                try {
                                    operatorBasicInfo.setHeight(Integer.parseInt(str2.toString()));
                                } catch (NumberFormatException e) {
                                    log.error("缺少身高数据");
                                }
                            }
                        }
                    }
                }
                case "综合体检测试" -> operatorBasicInfo.setComprehensiveTest(storyText);
                case "客观履历" -> operatorBasicInfo.setObjectiveResume(storyText);
                case "临床诊断分析" -> operatorBasicInfo.setClinicalDiagnosis(storyText);
                case "档案资料一" -> operatorBasicInfo.setArchives1(storyText);
                case "档案资料二" -> operatorBasicInfo.setArchives2(storyText);
                case "档案资料三" -> operatorBasicInfo.setArchives3(storyText);
                case "档案资料四" -> operatorBasicInfo.setArchives4(storyText);
                case "晋升记录", "晋升资料" -> operatorBasicInfo.setPromotionInfo(storyText);
            }
        }
        updateMapper.updateOperatorInfo(operatorBasicInfo);
    }

    /**
     * 独立提取未精英化干员画师
     * @param charId 干员Id
     * @return 画师名字
     */
    private String takeDrawName(String charId){
        JSONObject skinJson = new JSONObject(getJsonStringFromFile("skin_table.json")).getJSONObject("charSkins");
        JSONObject operatorInfo;
        if (charId.equals("char_1001_amiya2")){
            operatorInfo= skinJson.getJSONObject(charId+"#2");
        }else {
            operatorInfo = skinJson.getJSONObject(charId+"#1");
        }
        JSONObject displaySkin = operatorInfo.getJSONObject("displaySkin");
        try {
            JSONArray drawerList = displaySkin.getJSONArray("drawerList");
            return (String) drawerList.get(0);
        }catch (JSONException e){
            log.info("charId为{}的干员未录入画师姓名，已跳过数据收录",charId);
            return "";
        }
    }

    /**
     *  插入干员配音信息
     * @param operatorId 数据库中的干员Id
     * @param CVNameJsonObj 配音部分的JSON
     */
    private void updateDubberInfoById(Integer operatorId, JSONObject CVNameJsonObj){
        OperatorBasicInfo operatorBasicInfo = new OperatorBasicInfo();
        operatorBasicInfo.setOperatorId(operatorId);
        JSONObject dict = CVNameJsonObj.getJSONObject("dict");
        for (String area : dict.keySet()){
            JSONObject voiceLangType = dict.getJSONObject(area);
            switch (area) {
                case "CN_MANDARIN" -> operatorBasicInfo.setCvNameOfCNMandarin((String) voiceLangType.getJSONArray("cvName").get(0));
                case "CN_TOPOLECT" -> operatorBasicInfo.setCvNameOfCNTopolect((String) voiceLangType.getJSONArray("cvName").get(0));
                case "JP" -> operatorBasicInfo.setCvNameOfJP((String) voiceLangType.getJSONArray("cvName").get(0));
                case "KR" -> operatorBasicInfo.setCvNameOfKR((String) voiceLangType.getJSONArray("cvName").get(0));
                case "EN" -> operatorBasicInfo.setCvNameOfEN((String) voiceLangType.getJSONArray("cvName").get(0));
            }
        }
        updateMapper.updateCVNameByOperatorId(operatorBasicInfo);
    }

    /**
     * 从干员信息中获取单个干员的标签tag并按名字更新到数据库中
     * @param charId 干员的charId编号
     * @param operator character_table中干员基础信息部分解析数据
     */
    private void updateOperatorTag(String charId,JSONObject operator) {
        String name = operator.getString("name");
        List<String> agentTagsInfos = agentTagsMapper.selectAgentNameAll();
        if (agentTagsInfos.contains(name)) {
            log.info("干员{}已有公招tag", name);
            return;
        }

        StringBuilder position = new StringBuilder();
        switch (operator.getString("position")){
            case "MELEE" -> position.append("近战位");
            case "RANGED" -> position.append("远程位");
        }

        JSONArray tags = operator.getJSONArray("tagList");
        for (int i = 0; i < tags.length(); i++) {
            position.append(",").append(tags.getString(i));
        }

        int rarity = operator.getInt("rarity") + 1;
        switch (rarity){
            case 1 -> position.append(",").append("支援机械");
            case 5 -> position.append(",").append("资深干员");
            case 6 -> position.append(",").append("高级资深干员");
        }

        Map<String, String> operatorClass = new HashMap<>(8);
        operatorClass.put("PIONEER", "先锋干员");
        operatorClass.put("WARRIOR", "近卫干员");
        operatorClass.put("TANK", "重装干员");
        operatorClass.put("SNIPER", "狙击干员");
        operatorClass.put("CASTER", "术师干员");
        operatorClass.put("SUPPORT", "辅助干员");
        operatorClass.put("MEDIC", "医疗干员");
        operatorClass.put("SPECIAL", "特种干员");

        String profession = operator.getString("profession");
        position.append(",").append(operatorClass.get(profession));

        updateMapper.insertTags(charId, name, rarity, position.toString());
        log.info("{}干员tag信息更新成功", name);
    }

    /**
     * 增量更新敌人面板信息
     */
    public void updateAllEnemy() {
        log.info("开始更新敌人信息");
        //获取全部敌人数据
        JSONArray enemyObj = new JSONObject(getJsonStringFromFile("enemy_database.json")).getJSONArray("enemies");

        int length = 0;
        List<String> allEnemyId = enemyMapper.selectAllEnemyId();
        for (int i = 0; i < enemyObj.length(); i++) {
            String enemyId = enemyObj.getJSONObject(i).getString("Key");
            if (!allEnemyId.contains(enemyId)) {
                JSONObject oneEnemy = enemyObj.getJSONObject(i);
                JSONArray enemyJsonObj = oneEnemy.getJSONArray("Value");
                String name = enemyJsonObj.getJSONObject(0).getJSONObject("enemyData").getJSONObject("name").getString("m_value");
                for (int j = 0; j < enemyJsonObj.length(); j++) {
                    //一个敌人可能有多个阶段，比如我老婆霜星
                    JSONObject enemyData = enemyJsonObj.getJSONObject(j).getJSONObject("enemyData");
                    JSONObject attributes = enemyData.getJSONObject("attributes");
                    Integer atk = attributes.getJSONObject("atk").getInt("m_value");
                    Double baseAttackTime = attributes.getJSONObject("baseAttackTime").getDouble("m_value");
                    Integer def = attributes.getJSONObject("def").getInt("m_value");
                    Integer hpRecoveryPerSec = attributes.getJSONObject("hpRecoveryPerSec").getInt("m_value");
                    Integer magicResistance = attributes.getJSONObject("magicResistance").getInt("m_value");
                    Integer massLevel = attributes.getJSONObject("massLevel").getInt("m_value");
                    Integer maxHp = attributes.getJSONObject("maxHp").getInt("m_value");
                    Double moveSpeed = attributes.getJSONObject("moveSpeed").getDouble("m_value");
                    Double rangeRadius = enemyData.getJSONObject("rangeRadius").getDouble("m_value");
                    Integer silenceImmune = attributes.getJSONObject("silenceImmune").getBoolean("m_value") ? 0 : 1;
                    Integer sleepImmune = attributes.getJSONObject("sleepImmune").getBoolean("m_value") ? 0 : 1;
                    Integer stunImmune = attributes.getJSONObject("stunImmune").getBoolean("m_value") ? 0 : 1;

                    EnemyInfo enemyInfo = new EnemyInfo(enemyId, name, atk, baseAttackTime,
                            def, hpRecoveryPerSec, magicResistance, massLevel, maxHp,
                            moveSpeed, rangeRadius, silenceImmune, sleepImmune, stunImmune, j);

                    updateMapper.updateEnemy(enemyInfo);
                    length++;
                }
            }
        }
        log.info("敌人信息更新完成，共更新了{}个敌人信息", length);
    }

    /**
     * 更新地图、材料基础信息
     */
    public void updateMapAndItem() {

        log.info("从企鹅物流中拉取地图、材料数据");
        //地图列表
        String mapListUrl = "https://penguin-stats.cn/PenguinStats/api/v2/stages?server=CN";

        MapJson[] maps = restTemplate
                .getForObject(mapListUrl, MapJson[].class);
        int newMap = 0;
        for (MapJson map : maps) {
            List<String> mapIds = materialMadeMapper.selectAllMapId();
            if (!mapIds.contains(map.getStageId())) {
                updateMapper.updateStageData(map);
                newMap++;
            }
        }

        log.info("新增地图{}个", newMap);

        //章节列表
        String zoneListUrl = "https://penguin-stats.cn/PenguinStats/api/v2/zones";

        int newZone = 0;
        ZoneJson[] zones = restTemplate.getForObject(zoneListUrl, ZoneJson[].class);
        for (ZoneJson zone : zones) {
            List<String> zoneIds = materialMadeMapper.selectAllZoneId();
            if (!zoneIds.contains(zone.getZoneId())) {
                updateMapper.updateZoneData(zone);
                newZone++;
            }
        }

        log.info("新增章节{}个", newZone);

        updateItemAndFormula();

        //地图掉落关联表
        String matrixListUrl = "https://penguin-stats.cn/PenguinStats/api/v2/_private/result/matrix/CN/global";

        //全量更新所有掉落信息
        clearMatrixData();
        String matrixJsonStr = restTemplate.getForObject(matrixListUrl, String.class);
        JSONArray matrixJsons = new JSONObject(matrixJsonStr).getJSONArray("matrix");
        int length = matrixJsons.length();
        for (int i = 0; i < length; i++) {
            JSONObject matrix = matrixJsons.getJSONObject(i);
            try {
                String stageId = matrix.getString("stageId");
                Integer itemId = Integer.parseInt(matrix.getString("itemId"));
                Integer quantity = matrix.getInt("quantity");
                Integer times = matrix.getInt("times");
                updateMapper.updateMatrixData(stageId, itemId, quantity, times);
            } catch (NumberFormatException e) {
                //忽略家具材料
            }
        }
        log.info("企鹅物流数据更新完成--");
    }

    /**
     * 增量更新材料以及合成公式
     */
    public void updateItemAndFormula() {
        //材料列表
        List<String> ids = materialMadeMapper.selectAllMaterId();
        String jsonStringFromUrl = getJsonStringFromFile("item_table.json");
        if (jsonStringFromUrl != null) {
            JSONObject items = new JSONObject(jsonStringFromUrl).getJSONObject("items");
            Iterator<String> keys = items.keys();
            int newItem = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject itemObj = items.getJSONObject(key);
                String id = itemObj.getString("itemId");
                //增量更新
                if (!ids.contains(id)) {
                    String name = itemObj.getString("name");
                    String icon = itemObj.getString("iconId");
                    updateMapper.updateItemData(id, name, icon);
                    //更新合成信息
                    updateItemFormula(id);
                    newItem++;
                }
            }
            log.info("材料合成数据更新完成--");
            log.info("新增材料{}个", newItem);
        }
    }

    /**
     * 根据材料Id获取合成公式
     *
     * @param itemId 材料Id
     */
    public void updateItemFormula(String itemId) {
        //根据材料id，更新材料合成公式
        JSONArray buildingProductList = new JSONObject(getJsonStringFromFile("item_table.json")).getJSONObject("items").getJSONObject(itemId).getJSONArray("buildingProductList");
        if (buildingProductList != null && buildingProductList.length() > 0) {
            String roomType = buildingProductList.getJSONObject(0).getString("roomType");
            String formulaId = buildingProductList.getJSONObject(0).getString("formulaId");

            JSONArray formulaObj;
            if (roomType.equals("WORKSHOP")) {
                formulaObj = new JSONObject(getJsonStringFromFile("building_data.json")).getJSONObject("workshopFormulas").getJSONObject(formulaId).getJSONArray("costs");
                for (int i = 0; i < formulaObj.length(); i++) {
                    updateMapper.insertMaterialMade(itemId
                            , Integer.parseInt(formulaObj.getJSONObject(i).getString("id"))
                            , formulaObj.getJSONObject(i).getInt("count"));
                }
            } else if (roomType.equals("MANUFACTURE")) {
                formulaObj = new JSONObject(getJsonStringFromFile("building_data.json")).getJSONObject("manufactFormulas").getJSONObject(formulaId).getJSONArray("costs");
                for (int i = 0; i < formulaObj.length(); i++) {
                    updateMapper.insertMaterialMade(itemId
                            , Integer.parseInt(formulaObj.getJSONObject(i).getString("id"))
                            , formulaObj.getJSONObject(i).getInt("count"));
                }
            }
            log.info("{}材料合成信息更新成功", itemId);
        }
    }

    /**
     * 增量获取立绘图
     * 当压缩包版本检查通过时直接从压缩包获取全部立绘
     * 当压缩包版本检查没通过时设置下载信息进行下载
     * @param downloadInfo 预封装下载信息
     */
    public void updateSkin(DownloadOneFileInfo downloadInfo) {
        log.info("开始拉取时装数据");
        if (downloadInfo.isPullZip()) {
            Map<String, String> map = new HashMap<>() {{
                put("Arknights-Bot-Resource-main/skin/", "runFile/skin/");
            }};
            try {
                log.info("正在尝试从压缩包中拉取");
                pullFromZipFile(false, map);
                log.info("时装立绘拉取完成，进行数据库写入");
            } catch (IOException e) {
                downloadInfo.setPullZip(false);
                log.error("拉取出现错误，失败原因："+ e + "\n尝试下载");
            }
        }
        JSONObject skinJson = new JSONObject(getJsonStringFromFile("skin_table.json"));

        JSONObject charSkins = skinJson.getJSONObject("charSkins");
        //立绘只需要增量更新
        List<String> skinIdList = skinInfoMapper.selectAllSkinId();
        Iterator<String> keys = charSkins.keys();
        while (keys.hasNext()) {
            String skinId = keys.next();
            if (skinIdList.contains(skinId)){
                log.info("id为{}的时装已存在，跳过拉取",skinId);
                continue;
            }
            JSONObject singleSkin = charSkins.getJSONObject(skinId);
            //获取json中定义的skinId，开头为char是干员，召唤物和活动立绘缺失太多，跳过获取
            if (skinId.startsWith("char")){
                //获取皮肤对应的干员charId
                String charId = singleSkin.getString("charId");

                //以charId返回查询自增Id和名字，并创建初始封装
                SkinInfo skinInfo = skinInfoMapper.getOperatorInfoByChar(charId);

                if (skinInfo == null){
                    //如果干员找不到，就去特招找
                    skinInfo = skinInfoMapper.getSummonerInfoByChar(charId);
                    skinInfo.setType(1);
                }

                //通过skinId处理获取到实际的文件名字
                String skinFile;
                if (skinId.contains("@")){
                    skinFile = skinId.replace("@","_") + "b.png";
                }else {
                    skinFile = skinId.replace("#","_") + "b.png";
                }

                JSONObject displaySkin = singleSkin.getJSONObject("displaySkin");
                //获取皮肤名字，防止皮肤名字为null情况做归属判断，此处出现的null为object格式，不可用"=="null判断
                String skinName;
                if(displaySkin.get("skinName") instanceof String) {
                    skinName = displaySkin.getString("skinName");
                }else {
                    int sortId = displaySkin.getInt("sortId");
                    switch (sortId){
                        case -3 -> skinName = "干员" + skinInfo.getOperatorName() + "的默认服装";
                        case -2 -> skinName = "干员" + skinInfo.getOperatorName() + "精英化一级时的默认服装";
                        case -1 -> skinName = "干员" + skinInfo.getOperatorName() + "精英化二级时的默认服装";
                        default -> {
                            skinName = "unknown";
                            log.error("干员服装类别获取错误，服装id为{}，请提供给开发者修复",charId);
                        }
                    }
                }
                //获取画师表，以"/"分割，结尾最后一个"/"记得删掉
                JSONArray drawerList;
                StringBuilder drawer = new StringBuilder();
                Object drawerObj = displaySkin.get("drawerList");
                if(drawerObj instanceof JSONArray){
                    drawerList = displaySkin.getJSONArray("drawerList");
                    for (Object s : drawerList) {
                        drawer.append(s).append("/");
                    }
                    drawer.deleteCharAt(drawer.length()-1);
                }else {
                    drawer.append("unknown");
                }
                //非默认服装content会出现颜色和格式内容
                String content = "";
                if (displaySkin.get("dialog") instanceof String){
                    content = displaySkin.getString("dialog");
                }else if (displaySkin.get("content") instanceof String){
                    content = displaySkin.getString("content");
                }

                log.info("新增立绘：" + skinName);
                skinInfo.setSkinId(skinId);
                skinInfo.setCharId(charId);
                skinInfo.setSkinPath("runFile/skin/" + skinFile);
                skinInfo.setSkinName(skinName);
                skinInfo.setContent(content);
                skinInfo.setSkinGroupId(displaySkin.getString("skinGroupId"));
                skinInfo.setSkinGroupName(displaySkin.getString("skinGroupName"));
                skinInfo.setDrawerName(drawer.toString());
                try {
                    //非压缩包获取时，进行下载
                    if (!downloadInfo.isPullZip()) {
                        downloadInfo.setSecond(300);
                        downloadInfo.setFileName("runFile/skin/" + skinFile + ".png");
                        downloadInfo.setUrl(url + "skin/" + skinFile + ".png");
                        downloadOneFile(downloadInfo);
                        downloadInfo.setFileName(null);
                        downloadInfo.setUrl(null);
                    }
                    skinInfoMapper.insertBySkinInfo(skinInfo);
                }catch (IOException e) {
                    log.error("下载{}时装失败", skinName);
                }

            }
        }
        updateSkinGroupInfo(skinJson);
        log.info("时装数据更新完成--");
    }

    /**
     * 拉取时装系列信息写入operator_skin_group表
     * @param skinJson 立绘信息json表
     */
    private void updateSkinGroupInfo(JSONObject skinJson){
        log.info("开始拉取时装系列信息");
        JSONObject brandList = skinJson.getJSONObject("brandList");
        List<String> brandIdList = skinInfoMapper.selectAllBrandId();
        for (String brandId : brandList.keySet()) {
            SkinGroupInfo skinGroupInfo = new SkinGroupInfo();
            JSONObject singleBrand = brandList.getJSONObject(brandId);
            //系列中包含的所有id编号组
            //把这奇葩的"™"给我删咯！
            String brandName = singleBrand.getString("brandName").replace("™","");
            if (brandIdList.contains(brandId)){
                log.info("{}系列的时装信息已存在",brandName);
            } else {
                skinGroupInfo.setBrandId(brandId);
                skinGroupInfo.setBrandName(brandName);
                skinGroupInfo.setBrandCapitalName(singleBrand.getString("brandCapitalName"));
                skinGroupInfo.setDescription(singleBrand.getString("description"));
                skinInfoMapper.insertSkinGroupInfo(skinGroupInfo);
                log.info("新增时装系列：{}系列",brandName);
            }
            //时装系列信息收录完成后，给每个时装注入系列信息
            JSONArray groupList = singleBrand.getJSONArray("groupList");
            for (Object object : groupList) {
                String skinGroupId = (String) object;
                skinInfoMapper.updateBrandIdBySkinGroupId(brandId, skinGroupId);
            }
        }
    }

    /**
     * 更新材料图标，以材料表为基础update，只更新非base64的字段
     */
    public void updateItemIcon(DownloadOneFileInfo downloadInfo) {
        log.info("开始拉取最新材料图标");
        List<String> maters = materialMadeMapper.selectAllMaterId();
        if (downloadInfo.isPullZip()){
            Map<String,String> map = new HashMap<>(){{
                put("Arknights-Bot-Resource-main/item/","runFile/itemIcon/");
            }};
            try {
                log.info("正在尝试从压缩包中拉取");
                pullFromZipFile(false,map);
                for (String id : maters) {
                    String picBase64 = materialMadeMapper.selectMaterialPicById(id);
                    if (picBase64 == null) {
                        String iconId = materialMadeMapper.selectAllMaterIconId(id);
                        String fileName = "runFile/itemIcon/" + iconId + ".png";
                        materialMadeMapper.updateBase64ById(fileName, id);
                    }
                }
                log.info("材料图标拉取完成--");
                return;
            } catch (IOException e) {
                log.error("拉取出现错误，失败原因："+ e + "\n重新尝试下载");
            }
        }

        for (String id : maters) {
            String picBase64 = materialMadeMapper.selectMaterialPicById(id);
            if (picBase64 == null) {
                String iconId = materialMadeMapper.selectAllMaterIconId(id);
                try {
                    String fileName = "runFile/itemIcon/" + iconId + ".png";
                    downloadInfo.setSecond(300);
                    downloadInfo.setFileName(fileName);
                    downloadInfo.setUrl(url + "item/" + iconId + ".png");
                    downloadOneFile(downloadInfo);
                    downloadInfo.setFileName(null);
                    downloadInfo.setUrl(null);
                    materialMadeMapper.updateBase64ById(fileName, id);
                } catch (IOException e) {
                    log.error("下载{}材料图标失败", id);
                }
            }
        }
        log.info("材料图标更新完成--");
    }

    /**
     * 更新干员半身照，增量更新
     */
    public void updateOperatorPng(DownloadOneFileInfo downloadInfo) {
        log.info("开始更新干员半身照与头像");
        List<String> allOperatorId = operatorInfoMapper.getAllOperatorId();
        if (downloadInfo.isPullZip()){
            Map<String,String> map = new HashMap<>(){{
                put("Arknights-Bot-Resource-main/portrait/","runFile/operatorPng/");
                put("Arknights-Bot-Resource-main/avatar/","runFile/avatar/");
            }};
            try {
                log.info("正在尝试从压缩包中拉取");
                pullFromZipFile(false,map);
                for (String id : allOperatorId) {
                    String base = operatorInfoMapper.selectOperatorPngById(id);
                    if (base == null) {
                        String fileName = "runFile/operatorPng/" + id + "_1.png";
                        operatorInfoMapper.insertOperatorPngById(id, fileName);
                    }
                    String avatar = operatorInfoMapper.selectOperatorAvatarPngById(id);
                    if (avatar == null) {
                        String avatarFile = "runFile/avatar/" + id + ".png";
                        operatorInfoMapper.insertOperatorAvatarPngById(id, avatarFile);
                    }
                }
                log.info("干员半身照拉取完成--");
                return;
            } catch (IOException e) {
                log.error("拉取出现错误，失败原因："+ e + "\n重新尝试下载");
            }
        }

        for (String id : allOperatorId) {
            String base = operatorInfoMapper.selectOperatorPngById(id);
            if (base == null) {
                log.info(id + "半身照正在更新");
                try {
                    String fileName = "runFile/operatorPng/" + id + "_1.png";
                    downloadInfo.setSecond(300);
                    downloadInfo.setFileName(fileName);
                    downloadInfo.setUrl(url + "portrait/" + id + "_1.png");
                    downloadOneFile(downloadInfo);
                    downloadInfo.setFileName(null);
                    downloadInfo.setUrl(null);
                    operatorInfoMapper.insertOperatorPngById(id, fileName);
                } catch (IOException e) {
                    log.error("下载{}干员半身照失败", id);
                }
            }
            String avatar = operatorInfoMapper.selectOperatorAvatarPngById(id);
            if (avatar == null) {
                log.info(id + "头像正在更新");
                try {
                    String avatarFile = "runFile/avatar/" + id + ".png";
                    downloadInfo.setSecond(300);
                    downloadInfo.setFileName(avatarFile);
                    downloadInfo.setUrl(url + "avatar/" + id + ".png");
                    downloadOneFile(downloadInfo);
                    downloadInfo.setFileName(null);
                    downloadInfo.setUrl(null);
                    operatorInfoMapper.insertOperatorAvatarPngById(id, avatarFile);
                } catch (IOException e) {
                    log.error("下载{}干员头像失败", id);
                }
            }
        }
        log.info("干员半身照更新完成--");
    }

    /**
     * 更新干员技能图标
     */
    public void updateOperatorSkillPng(DownloadOneFileInfo downloadInfo) {
        log.info("开始更新干员技能图标");
        List<SkillInfo> skillInfo = skillDescMapper.selectAllSkillPng();
        if (downloadInfo.isPullZip()){
            Map<String,String> map = new HashMap<>(){{
                put("Arknights-Bot-Resource-main/skill/","runFile/skill/");
            }};
            try {
                log.info("正在尝试从压缩包中拉取");
                pullFromZipFile(false,map);
                for (SkillInfo skill : skillInfo) {
                    String png = skill.getSkillPng();
                    if (png == null) {
                        String fileName = "runFile/skill/skill_icon_" + skill.getSkillIdYj() + ".png";
                        operatorInfoMapper.insertOperatorSkillPngById(skill.getSkillIdYj(), fileName);
                    }
                }
                log.info("干员技能图标拉取完成--");
                return;
            } catch (IOException e) {
                log.error("拉取出现错误，失败原因："+ e + "\n重新尝试下载");
            }
        }

        for (SkillInfo skill : skillInfo) {
            String png = skill.getSkillPng();
            if (png == null) {
                log.info(skill.getSkillName() + "技能图标正在更新");
                try {
                    String fileName = "runFile/skill/skill_icon_" + skill.getSkillIdYj() + ".png";
                    downloadInfo.setSecond(300);
                    downloadInfo.setFileName(fileName);
                    downloadInfo.setUrl(url + "skill/skill_icon_" + URLEncoder.encode(skill.getSkillIdYj(), StandardCharsets.UTF_8) + ".png");
                    downloadOneFile(downloadInfo);
                    downloadInfo.setFileName(null);
                    downloadInfo.setUrl(null);
                    operatorInfoMapper.insertOperatorSkillPngById(skill.getSkillIdYj(), fileName);
                } catch (IOException e) {
                    log.error("下载{}干员技能图标失败", skill.getSkillName());
                }
            }
        }
        log.info("干员技能图标更新完成--");
    }

    /**
     * 更新干员语音文件，增量更新
     * @param downloadInfo 置入下载的代理信息
     */
    public void updateOperatorVoice(DownloadOneFileInfo downloadInfo) {
        log.info("开始更新干员语音");

        //中配
        downloadVoiceByType("voice_cn", downloadInfo);
        //日配
        //downloadVoiceByType("voice", downloadInfo);
        //方言
        //downloadVoiceByType("voice_custom", downloadInfo);
        //英语
        //downloadVoiceByType("voice_en", downloadInfo);
        //傻逼棒子话
        //downloadVoiceByType("voice_kr", downloadInfo);

        log.info("更新干员语音完成--");
    }

    private void downloadVoiceByType(String type, DownloadOneFileInfo downloadInfo) {
        String area;
        switch (type){
            case "voice_cn" -> area = "CN_mandarin";
            case "voice_custom" -> area = "CN_topolect";
            case "voice_en" -> area = "EN";
            case "voice_kr" -> area = "KR";
            default ->  area = "JP";
        }
        List<OperatorName> allOperatorId = operatorInfoMapper.getAllIdAndNameAndCV(area);
        String url = "https://static.prts.wiki/" + type + "/";
        for (OperatorName name : allOperatorId) {
            String voiceCharId = name.getCharId();
            if (type.equals("voice_custom")) {
                voiceCharId = name.getCharId() + "_cn_topolect";
            }
            Path tempPath = Paths.get("runFile/" + type + "/" + voiceCharId);
            if (Files.exists(tempPath)){
                try {
                    Files.createDirectories(tempPath);
                } catch (IOException e) {
                    log.error(e.toString());
                }
            }
            for (String voiceName : VoiceService.voiceList) {
                //判断是否存在该语音
                if (operatorInfoMapper.selectOperatorVoiceByCharIdAndName(type, name.getCharId(), voiceName) == 0) {
                    String path = voiceCharId + "/" + name.getOperatorName() + "_" + voiceName + ".wav";
                    if(name.getOperatorName().equals("近卫阿米娅")){
                        path = voiceCharId + "/阿米娅_" + voiceName + ".wav";
                    }
                    try {
                        downloadInfo.setSecond(300);
                        String filePath = "runFile/" + type + "/" + path;
                        downloadInfo.setFileName(filePath);
                        downloadInfo.setUrl(url+path);
                        downloadOneFile(downloadInfo);
                        downloadInfo.setFileName(null);
                        downloadInfo.setUrl(null);
                        //写入数据库
                        operatorInfoMapper.insertOperatorVoice(name.getCharId(), type, voiceName, filePath);
                        Thread.sleep(new Random().nextInt(5) * 1000);
                    } catch (IOException e) {
                        log.error("下载{}类型{}语音失败", type, name.getCharId() + "/" + voiceName);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 干员基础信息更新总支，分别更新基础数据，技能和基建技能到表中
     * @param charId 干员的charId号码
     * @param operatorJson character_table中干员基础信息部分解析数据
     * @param skillJson skill_table中干员基础信息部分解析数据
     * @param buildingJson building_data中干员基础信息部分解析数据
     * @return 该干员在数据库中的自增ID
     */
    public Integer updateOperatorByJson(String charId, JSONObject operatorJson, JSONObject skillJson, JSONObject buildingJson) {
        Integer operatorId = updateOperatorInfo(charId,operatorJson);
        if (operatorId != null) {
            updatePanelData(operatorId, operatorJson);
            updateTalent(operatorId, operatorJson);
            updateSkill(operatorId, operatorJson, skillJson);
            updateBuilding(charId,operatorId, buildingJson);
        }
        return operatorId;
    }

    /**
     * 更新干员基础信息，写入operator表，返回表中的自增ID号
     * 召唤物则写入operator_summoner表
     * @param isOperator 更新的基础信息是否为干员，是干员为true，是召唤物为false
     * @param charId 干员的charId号码
     * @param operatorJson character_table中干员基础信息部分解析数据
     * @return 数据库中的自增干员ID
     */
    private Integer updateOperatorInfo(boolean isOperator, String charId, JSONObject operatorJson){
        //职业名与数字标注转换
        Map<String, Integer> operatorClass = new HashMap<>(){{
            put("PIONEER", 1);
            put("WARRIOR", 2);
            put("TANK", 3);
            put("SNIPER", 4);
            put("CASTER", 5);
            put("SUPPORT", 6);
            put("MEDIC", 7);
            put("SPECIAL", 8);
        }};

        int rarity = operatorJson.getInt("rarity") + 1;
        boolean isNotObtainable = operatorJson.getBoolean("isNotObtainable");

        String name = getName(operatorJson);

        //封装干员信息
        OperatorInfo operatorInfo = new OperatorInfo();
        operatorInfo.setCharId(charId);
        operatorInfo.setOperatorName(name.trim());
        operatorInfo.setOperatorRarity(rarity);
        if (isNotObtainable) {
            operatorInfo.setAvailable(0);
        } else {
            operatorInfo.setAvailable(1);
        }
        operatorInfo.setInLimit(0);
        operatorInfo.setOperatorClass(operatorClass.get(operatorJson.getString("profession")));

        if (isOperator){
            updateMapper.insertOperator(operatorInfo);
            log.info("更新干员{}基本信息成功", name);
            return updateMapper.selectOperatorIdByCharId(charId);
        }else {
            updateMapper.insertSummoner(operatorInfo);
            log.info("更新特殊召唤物{}基本信息成功", name);
            return updateMapper.selectSummonerIdByCharId(charId);
        }
    }

    private Integer updateOperatorInfo(String charId, JSONObject operatorJson){
        return updateOperatorInfo(true,charId,operatorJson);
    }

    /**
     * 更新干员面板信息，写入operator表
     * 召唤物则写入operator_summoner表
     * 更新精英化信息，写入operator_evolve_costs表
     * 如果为召唤物，跳过精英化信息写入
     * @param isOperator 更新的基础信息是否为干员，是干员为true，是召唤物为false
     * @param operatorId 数据库中的自增干员ID
     * @param operatorJson character_table中干员基础信息部分解析数据
     */
    private void updatePanelData(boolean isOperator, Integer operatorId, JSONObject operatorJson){
        JSONArray phases = operatorJson.getJSONArray("phases");
        String name = getName(operatorJson);
        int length = phases.length();
        //封装干员面板信息（满级无潜能无信赖）
        JSONArray operatorPanel = phases.getJSONObject(length - 1).getJSONArray("attributesKeyFrames");
        JSONObject panelMax = operatorPanel.getJSONObject(operatorPanel.length() - 1).getJSONObject("data");
        OperatorData operatorData = new OperatorData();
        operatorData.setId(operatorId);
        operatorData.setAtk(panelMax.getInt("atk"));
        operatorData.setDef(panelMax.getInt("def"));
        operatorData.setMagicResistance(panelMax.getInt("magicResistance"));
        operatorData.setMaxHp(panelMax.getInt("maxHp"));
        operatorData.setBlockCnt(panelMax.getInt("blockCnt"));
        operatorData.setCost(panelMax.getInt("cost"));
        operatorData.setBaseAttackTime(panelMax.getDouble("baseAttackTime"));
        operatorData.setRespawnTime(panelMax.getInt("respawnTime"));
        if(isOperator) {
            updateMapper.updateOperatorData(operatorData);
            log.info("更新干员{}面板信息成功", name);
        }else {
            updateMapper.updateSummonerData(operatorData);
            log.info("更新特殊召唤物{}面板信息成功", name);
            return;
        }

        //封装干员精英化花费
        for (int i = 1; i < length; i++) {
            JSONObject array = phases.getJSONObject(i);
            if (array.get("evolveCost") instanceof JSONArray) {
                JSONArray evolveJson = array.getJSONArray("evolveCost");
                for (int j = 0; j < evolveJson.length(); j++) {
                    JSONObject evolve = evolveJson.getJSONObject(j);
                    //精英i花费
                    OperatorEvolveInfo operatorEvolveInfo = new OperatorEvolveInfo();
                    operatorEvolveInfo.setOperatorId(operatorId);
                    operatorEvolveInfo.setEvolveLevel(i);
                    operatorEvolveInfo.setUseMaterialId(evolve.getInt("id"));
                    operatorEvolveInfo.setUseNumber(evolve.getInt("count"));
                    updateMapper.insertOperatorEvolve(operatorEvolveInfo);
                }
            }
        }
        log.info("更新{}干员精英化材料成功", name);
    }

    private void updatePanelData(Integer operatorId, JSONObject operatorJson){
        updatePanelData(true,operatorId,operatorJson);
    }

    /**
     * 更新干员天赋信息，写入operator_talent表
     * @param operatorId 数据库中的自增干员ID
     * @param operatorJson character_table中干员基础信息部分解析数据
     */
    private void updateTalent(Integer operatorId, JSONObject operatorJson){
        String name = getName(operatorJson);
        //封装干员天赋
        if (operatorJson.get("talents") instanceof JSONArray) {
            JSONArray talents = operatorJson.getJSONArray("talents");
            for (int i = 0; i < talents.length(); i++) {
                JSONArray candidates = talents.getJSONObject(i).getJSONArray("candidates");
                for (int j = 0; j < candidates.length(); j++) {
                    TalentInfo talentInfo = new TalentInfo();
                    JSONObject candidate = candidates.getJSONObject(j);
                    if (candidate.get("name") instanceof String) {
                        talentInfo.setTalentName(candidate.getString("name"));
                    }
                    Pattern pattern = Pattern.compile("<(.*?)>");
                    if (candidate.get("description") instanceof String) {
                        Matcher matcher = pattern.matcher(candidate.getString("description"));
                        talentInfo.setDescription(matcher.replaceAll(""));
                    }
                    talentInfo.setLevel(candidate.getJSONObject("unlockCondition").getInt("level"));
                    talentInfo.setPhase(candidate.getJSONObject("unlockCondition").getInt("phase"));
                    talentInfo.setPotential(candidate.getInt("requiredPotentialRank"));
                    talentInfo.setOperatorId(operatorId);
                    updateMapper.insertOperatorTalent(talentInfo);
                }
            }
            log.info("更新{}干员天赋成功", name);
        }
    }

    /**
     * 更新干员技能，写入operator_skill表
     * 更新干员技能详细描述，写入operator_skill_desc表
     * 更新干员技能升级所需材料信息，写入operator_skill_mastery_costs
     * @param operatorId 数据库中的自增干员ID
     * @param operatorJson character_table中干员基础信息部分解析数据
     * @param skillJson skill_table中干员基础信息部分解析数据
     */
    private void updateSkill(Integer operatorId, JSONObject operatorJson, JSONObject skillJson){
        String name = getName(operatorJson);
        //封装干员技能
        JSONArray skills = operatorJson.getJSONArray("skills");
        for (int i = 0; i < skills.length(); i++) {
            OperatorSkillInfo operatorSkillInfo = new OperatorSkillInfo();
            operatorSkillInfo.setOperatorId(operatorId);
            operatorSkillInfo.setSkillIndex(i + 1);
            if (skills.getJSONObject(i).get("skillId") instanceof String) {
                JSONObject skillInfo = skillJson.getJSONObject(skills.getJSONObject(i).getString("skillId"));
                String skillName = skillInfo.getJSONArray("levels").getJSONObject(0).getString("name");
                String skillIdYj = skills.getJSONObject(i).getString("skillId");
                operatorSkillInfo.setSkillName(skillName);
                if (skillInfo.get("iconId") instanceof String) {
                    operatorSkillInfo.setSkillIdYj(skillInfo.getString("iconId"));
                } else {
                    Pattern skillIdPattern = Pattern.compile("\\[(0-9)\\]");
                    Matcher skillIdMatcher = skillIdPattern.matcher(skillIdYj);
                    operatorSkillInfo.setSkillIdYj(skillIdMatcher.replaceAll(""));
                }
                updateMapper.insertOperatorSkill(operatorSkillInfo);
                Integer skillId = updateMapper.selectSkillIdByName(skillName);

                JSONArray levels = skillInfo.getJSONArray("levels");

                for (int level = 0; level < levels.length(); level++) {
                    JSONObject skillDescJson = levels.getJSONObject(level);
                    SkillDesc skillDesc = new SkillDesc();
                    skillDesc.setSkillId(skillId);
                    skillDesc.setSkillLevel(level + 1);
                    skillDesc.setSkillType(skillDescJson.getInt("skillType"));

                    //获取技能面板数据的key-value列表
                    Map<String, Double> parameters = new HashMap<>();
                    JSONArray mapList = skillDescJson.getJSONArray("blackboard");
                    for (Object map : mapList){
                        parameters.put(((JSONObject) map).getString("key").toLowerCase(),((JSONObject) map).getDouble("value"));
                    }

                    skillDesc.setDescription(getValueByKeysFormatString(skillDescJson.getString("description"), parameters));
                    skillDesc.setSpType(skillDescJson.getJSONObject("spData").getInt("spType"));
                    skillDesc.setMaxCharge(skillDescJson.getJSONObject("spData").getInt("maxChargeTime"));
                    skillDesc.setSpCost(skillDescJson.getJSONObject("spData").getInt("spCost"));
                    skillDesc.setSpInit(skillDescJson.getJSONObject("spData").getInt("initSp"));
                    skillDesc.setDuration(skillDescJson.getInt("duration"));

                    updateMapper.updateSkillDesc(skillDesc);
                }
                log.info("更新{}干员技能{}信息成功", name, skillName);

                //获取技能等级列表(专一专二专三)
                JSONArray levelUpCostCond = skills.getJSONObject(i).getJSONArray("levelUpCostCond");
                //该技能专j+1的花费
                for (int j = 0; j < levelUpCostCond.length(); j++) {
                    JSONObject skillCostObj = levelUpCostCond.getJSONObject(j);
                    if (skillCostObj.get("levelUpCost") instanceof JSONArray) {
                        JSONArray levelUpCost = skillCostObj.getJSONArray("levelUpCost");
                        for (int k = 0; k < levelUpCost.length(); k++) {
                            SkillMaterInfo skillMaterInfo = new SkillMaterInfo();
                            skillMaterInfo.setSkillId(skillId);
                            skillMaterInfo.setMaterLevel(j + 1);
                            skillMaterInfo.setUseMaterialId(levelUpCost.getJSONObject(k).getInt("id"));
                            skillMaterInfo.setUseNumber(levelUpCost.getJSONObject(k).getInt("count"));
                            updateMapper.insertSkillMater(skillMaterInfo);
                        }
                    }
                }
                log.info("更新{}干员技能{}专精材料成功", name, skillName);
            }
        }
    }

    /**
     * 更新干员基建技能信息，写入operator_building_skill表
     * @param charId 干员的charId号码
     * @param operatorId 数据库中的自增干员ID
     * @param buildingJson building_data中干员基础信息部分解析数据
     */
    private void updateBuilding(String charId,Integer operatorId, JSONObject buildingJson){
        String name = operatorInfoMapper.getOperatorNameById(operatorId);
        if (name == null){
            name = operatorInfoMapper.getSummonerNameById(operatorId);
        }
        //封装干员基建技能
        if (buildingJson.getJSONObject("chars").has(charId)) {
            JSONObject chars = buildingJson.getJSONObject("chars").getJSONObject(charId);
            JSONObject buffs = buildingJson.getJSONObject("buffs");
            if (chars.get("buffChar") instanceof JSONArray) {
                JSONArray buildingData = chars.getJSONArray("buffChar");
                for (int i = 0; i < buildingData.length(); i++) {
                    if (buildingData.getJSONObject(i).get("buffData") instanceof JSONArray) {
                        JSONArray build1 = buildingData.getJSONObject(i).getJSONArray("buffData");
                        for (int j = 0; j < build1.length(); j++) {
                            BuildingSkill buildingSkill = new BuildingSkill();
                            JSONObject buildObj = build1.getJSONObject(j);
                            String buffId = buildObj.getString("buffId");
                            buildingSkill.setOperatorId(operatorId);
                            buildingSkill.setPhase(buildObj.getJSONObject("cond").getInt("phase"));
                            buildingSkill.setLevel(buildObj.getJSONObject("cond").getInt("level"));
                            buildingSkill.setBuffName(buffs.getJSONObject(buffId).getString("buffName"));
                            buildingSkill.setRoomType(buffs.getJSONObject(buffId).getString("roomType"));
                            //正则表达式去除标签
                            Pattern pattern = Pattern.compile("<(.*?)>");
                            Matcher matcher = pattern.matcher(buffs.getJSONObject(buffId).getString("description"));
                            buildingSkill.setDescription(matcher.replaceAll(""));
                            buildingSkillMapper.insertBuildingSkill(buildingSkill);
                        }
                    }
                }
            }
            log.info("更新干员{}基建技能成功", name);
        }
    }

    /**
     * 获取来自character_table中干员的名字
     * @param operatorJson character_table中干员基础信息部分解析数据
     * @return 干员名字
     */
    private String getName(JSONObject operatorJson){
        String name;
        //近卫兔兔改个名
        if (operatorJson.getJSONArray("phases").getJSONObject(0).getString("characterPrefabKey").equals("char_1001_amiya2")){
            name = "近卫阿米娅";
        }else {
            name = operatorJson.getString("name");
        }
        return name;
    }

    public void updateOperatorEquipByJson(){
        log.info("开始更新模组数据");
        JSONObject equip = new JSONObject(getJsonStringFromFile("battle_equip_table.json"));
        JSONObject equipUnlock = new JSONObject(getJsonStringFromFile("uniequip_table.json"));

        List<String> equipId = equipMapper.selectAllEquipId();
        Iterator<String> keys = equip.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!equipId.contains(key)) {
                EquipInfo equipInfo = new EquipInfo();

                JSONObject equipDict = equipUnlock.getJSONObject("equipDict").getJSONObject(key);
                equipInfo.setEquipId(equipDict.getString("uniEquipId"));
                equipInfo.setEquipName(equipDict.getString("uniEquipName"));
                equipInfo.setCharId(equipDict.getString("charId"));

                JSONArray phases = equip.getJSONObject(key).getJSONArray("phases");


                for (int i = 0; i < phases.length(); i++) {
                    equipInfo.setEquipLevel(i + 1);
                    JSONArray parts = phases.getJSONObject(i).getJSONArray("parts");

                    //天赋变化
                    StringBuilder additionalDescription = new StringBuilder();
                    StringBuilder overrideDescripton = new StringBuilder();

                    for (int j = 0; j < parts.length(); j++) {
                        JSONObject part = parts.getJSONObject(j);
                        JSONArray candidates;
                        switch (part.getString("target")) {
                            case "DISPLAY" -> {
                                candidates = part.getJSONObject("overrideTraitDataBundle").getJSONArray("candidates");
                                for (int k = 0; k < candidates.length(); k++) {
                                    JSONObject candidate = candidates.getJSONObject(k);
                                    //获取key-value列表
                                    Map<String, Double> parameters = new HashMap<>();
                                    JSONArray mapList = candidate.getJSONArray("blackboard");
                                    for (int keyId = 0; keyId < mapList.length(); keyId++) {
                                        parameters.put(mapList.getJSONObject(keyId).getString("key").toLowerCase(),
                                                mapList.getJSONObject(keyId).getDouble("value"));
                                    }
                                    if (candidate.get("additionalDescription") instanceof String) {
                                        String additional = candidate.getString("additionalDescription");
                                        additionalDescription.append(getValueByKeysFormatString(additional, parameters));
                                    }
                                    if (candidate.get("overrideDescripton") instanceof String) {
                                        String override = candidate.getString("overrideDescripton");
                                        overrideDescripton.append(getValueByKeysFormatString(override, parameters));
                                    }
                                }
                            }
                            case "TALENT_DATA_ONLY", "TALENT" -> {
                                candidates = part.getJSONObject("addOrOverrideTalentDataBundle").getJSONArray("candidates");
                                for (int k = 0; k < candidates.length(); k++) {
                                    JSONObject candidate = candidates.getJSONObject(k);
                                    //获取key-value列表
                                    Map<String, Double> parameters = new HashMap<>();
                                    JSONArray mapList = candidate.getJSONArray("blackboard");
                                    for (int keyId = 0; keyId < mapList.length(); keyId++) {
                                        parameters.put(mapList.getJSONObject(keyId).getString("key").toLowerCase(),
                                                mapList.getJSONObject(keyId).getDouble("value"));
                                    }
                                    if (candidate.get("upgradeDescription") instanceof String) {
                                        String additional = candidate.getString("upgradeDescription");
                                        additionalDescription.append(getValueByKeysFormatString(additional, parameters));
                                    }
                                }
                            }
                            case "TRAIT_DATA_ONLY", "TRAIT" -> {
                                candidates = part.getJSONObject("overrideTraitDataBundle").getJSONArray("candidates");
                                for (int k = 0; k < candidates.length(); k++) {
                                    JSONObject candidate = candidates.getJSONObject(k);
                                    //获取key-value列表
                                    Map<String, Double> parameters = new HashMap<>();
                                    JSONArray mapList = candidate.getJSONArray("blackboard");
                                    for (int keyId = 0; keyId < mapList.length(); keyId++) {
                                        parameters.put(mapList.getJSONObject(keyId).getString("key").toLowerCase(),
                                                mapList.getJSONObject(keyId).getDouble("value"));
                                    }
                                    if (candidate.get("overrideDescripton") instanceof String) {
                                        String override = candidate.getString("overrideDescripton");
                                        overrideDescripton.append(getValueByKeysFormatString(override, parameters));
                                    }
                                }
                            }
                        }
                    }

                    String addStr = additionalDescription.toString();
                    String overStr = overrideDescripton.toString();

                    if (addStr.equals("")) {
                        addStr = "无";
                    }
                    if (overStr.equals("")) {
                        overStr = "无";
                    }
                    String talentDesc = addStr + "|||" + overStr;
                    equipInfo.setDesc(talentDesc);
                    equipInfo.setLevel(parts.getJSONObject(0).
                            getJSONObject("overrideTraitDataBundle").getJSONArray("candidates").getJSONObject(0).getJSONObject("unlockCondition").getInt("level"));
                    equipInfo.setPhase(parts.getJSONObject(0).
                            getJSONObject("overrideTraitDataBundle").getJSONArray("candidates").getJSONObject(0).getJSONObject("unlockCondition").getInt("phase"));
                    equipMapper.insertEquipInfo(equipInfo);


                    JSONArray buffs = phases.getJSONObject(i).getJSONArray("attributeBlackboard");
                    for (int j = 0; j < buffs.length(); j++) {
                        String buffKey = buffs.getJSONObject(j).getString("key");
                        Double value = buffs.getJSONObject(j).getDouble("value");
                        equipMapper.insertEquipBuff(key, buffKey, value, i + 1);
                    }
                }

                JSONObject itemCost = equipDict.getJSONObject("itemCost");
                Iterator<String> keys1 = itemCost.keys();
                while (keys1.hasNext()) {
                    String level = keys1.next();
                    JSONArray cost = itemCost.getJSONArray(level);
                    for (int i = 0; i < cost.length(); i++) {
                        String materialId = cost.getJSONObject(i).getString("id");
                        Integer useNumber = cost.getJSONObject(i).getInt("count");
                        equipMapper.insertEquipCost(key, materialId, useNumber, Integer.parseInt(level));
                    }
                }

                JSONArray missionList = equipDict.getJSONArray("missionList");
                for (int i = 0; i < missionList.length(); i++) {
                    String missionId = missionList.getString(i);
                    String desc = equipUnlock.getJSONObject("missionList").getJSONObject(missionId).getString("desc");
                    equipMapper.insertEquipMission(key, missionId, desc);
                }
                log.info("{}模组信息更新成功", key);
            } else {
                log.info("已有{}模组信息", key);
            }
        }
        log.info("模组数据更新完毕");
    }

    public String getValueByKeysFormatString(String s, Map<String,Double> parameters){
        //使用正则表达式替换参数
        //代码可以运行不要乱改.jpg
        //这个正则已经不断进化成我看不懂的形式了
        Pattern pattern = Pattern.compile("<(.*?)>");
        Matcher matcher = pattern.matcher(s);
        String aaa = matcher.replaceAll("");
        Pattern p = Pattern.compile("(\\{-?([a-zA-Z/.\\]\\[0-9_@]+):?([0-9.]*)(%?)\\})");
        Matcher m = p.matcher(aaa);

        StringBuilder stringBuilder = new StringBuilder();
        while (m.find()) {
            String buffKey = m.group(2).toLowerCase();
            String percent = m.group(4);
            Double val = parameters.get(buffKey);
            String value;
            if (!percent.equals("")) {
                value = BigDecimal.valueOf(val).multiply(new BigDecimal(100)) + "%";
            } else {
                value = FormatStringUtil.FormatDouble2String(val) + percent;
            }
            m.appendReplacement(stringBuilder, value);
        }
        return m.appendTail(stringBuilder).toString().replace("--", "-");
    }

    private void rebuildDatabase() {
        Path destinationPath = Paths.get("runFile/arknights.db");
        try {
            Files.delete(destinationPath);
        } catch (IOException e) {
            log.error(e.toString());
        }
        try (InputStream is = new ClassPathResource("/database/arknights.db").getInputStream(); FileOutputStream fs = new FileOutputStream(destinationPath.toFile())) {
            fs.write(is.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearMatrixData() {
        Integer integer = updateMapper.selectMatrixCount();
        for (int i = 0; i <= integer / 100; i++) {
            updateMapper.clearMatrixData();
        }
    }
}
