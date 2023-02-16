package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.AdminUtil;
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.LookWorldMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 此功能全部来自各个API提供，兴趣添加，不保证使用情况
 */
@Service
@Slf4j
public class APIService {

    @Autowired
    private LookWorldMapper lookWorldMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private NotClassifiedService not;

    @Value("${APIConfig.token}")
    private String token;

    private Map<Long,Integer> lookWorldMap = new HashMap<>();

    private static volatile String instance;

    public String tokenInstance(){
        if (instance == null) {
            synchronized (APIService.class) {
                if (instance == null) {
                    instance = this.token;
                }
            }
        }
        return instance;
    }

    //默认发图功能关闭
    private static boolean sendPic = false;

    //默认国外节点
    private static final boolean node = false;

    /*@AngelinaGroup(keyWords = {"切换节点"}, sort = "API功能")
    public ReplayInfo change(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())){
            replayInfo.setReplayMessage("您没有更改权限");
        }else {
            if (node) {
                node = false;
                replayInfo.setReplayMessage("已切换为国外节点");
            }else {
                node = true;
                replayInfo.setReplayMessage("已切换为国内节点");
            }
        }
        return replayInfo;
    }*/
    private static String nodeUrl(){
        if (node){
            return "https://api.63ik.cn/";
        }else {
            return "https://www.sybapi.cc/";
        }
    }

    @AngelinaGroup(keyWords = {"动漫图片"}, sort = "API功能")
    public ReplayInfo pic(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            if (messageInfo.getArgs().get(1).equals("图片模式")){
                if (!AdminUtil.getAdmin(messageInfo.getQq())){
                    replayInfo.setReplayMessage("您没有更改权限");
                }else {
                    if (sendPic){
                        replayInfo.setReplayMessage("您已经处于图片模式了");
                    }else {
                        sendPic = true;
                        replayInfo.setReplayMessage("切换成功");
                    }
                }
            }else if(messageInfo.getArgs().get(1).equals("文字模式")) {
                if (!AdminUtil.getAdmin(messageInfo.getQq())){
                    replayInfo.setReplayMessage("您没有更改权限");
                }else {
                    if (sendPic){
                        sendPic = false;
                        replayInfo.setReplayMessage("切换成功");
                    }else {
                        replayInfo.setReplayMessage("您已经处于文字模式了");
                    }
                }
            }else {
                replayInfo = content(messageInfo);
            }
        }else {
            replayInfo = content(messageInfo);
        }
        return replayInfo;
    }

    private ReplayInfo content(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String url = nodeUrl()+"api/acg?key=" + tokenInstance();
        try {
            JSONObject json = readJsonFromUrl(url);
            JSONObject result = json.getJSONObject("result");
            String s = result.getString("url");
            if (sendPic){
                replayInfo.setReplayImg(s);
            }else {
                replayInfo.setReplayMessage(s);
            }
        } catch (JSONException e) {
            log.error(e.toString());
        }
         return replayInfo;
    }

    /**
     * 每日看世界的图片方式API解析
     */
    public boolean lookWorldPic(){
        /*
        String jsonUrl = "http://bjb.yunwj.top/php/tp/lj.php";
        String picUrl;
        try {
            JSONObject json = readJsonFromUrl(jsonUrl);
            picUrl = json.getString("tp1");
        }catch (JSONException | NullPointerException e){
            e.printStackTrace();
            autoPrint();
            return true;
        }
        */
        String picUrl = "https://api.03c3.cn/zb/";
        try {
            CloseableHttpClient httpClient  = HttpClients.createDefault();
            CloseableHttpResponse response;
            HttpGet httpGet = new HttpGet(picUrl);
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity httpEntity = response.getEntity();
                if(httpEntity.getContentLength() == 0){
                    autoPrint();
                    return true;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String path = "runFile/news";
                File folder = new File(path);
                if (!folder.exists()) folder.mkdirs();
                FileOutputStream out = new FileOutputStream("runFile/news/" + sdf.format(new Date()) + ".jpg");
                out.write(EntityUtils.toByteArray(httpEntity));
            }
        }catch (IOException e){
            log.error(e.toString());
            return false;
        }
        return true;
        //URL url = new URL("http://bjb.yunwj.top/php/tp/60.jpg");

    }

    public void autoPrint(){
        ReplayInfo replayInfo = new ReplayInfo();
        replayInfo.setQq(Long.valueOf(not.QQSetInstance()));
        Iterator<Long> it = MiraiFrameUtil.messageIdMap.values().iterator();
        replayInfo.setLoginQQ(it.next());
        replayInfo.setReplayMessage("自动获取失败，请手动存入当日的六十秒图片");
        sendMessageUtil.sendFriendMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        replayInfo = print(replayInfo);
        sendMessageUtil.sendFriendMsg(replayInfo);
    }

    @AngelinaFriend(keyWords = {"看世界存图"})
    public ReplayInfo manualPrint(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您没有权限");
        }else {
            replayInfo.setReplayMessage("请输入当日的六十秒图片");
            sendMessageUtil.sendFriendMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            replayInfo = print(replayInfo);
        }
        return replayInfo;
    }

    private ReplayInfo print(ReplayInfo replayInfo) {
        AngelinaListener angelinaListener = new AngelinaListener() {
            @Override
            public boolean callback(MessageInfo message) {
                return message.getQq().equals(515133662L) &&
                        message.getImgUrlList()!=null;
            }
        };
        angelinaListener.setSecond(300);
        MessageInfo recall = AngelinaEventSource.waiter2(angelinaListener).getMessageInfo();
        if (recall == null){
            replayInfo.setReplayMessage("等待超时，您可以后续通过私聊指令看世界存图再次输入");
        }else {
            try {
                URL u = new URL(recall.getImgUrlList().get(0));
                HttpURLConnection httpUrl;
                httpUrl = (HttpURLConnection) u.openConnection();
                httpUrl.connect();
                InputStream in = httpUrl.getInputStream();
                String path = "runFile/news";
                File folder = new File(path);
                if (!folder.exists()) folder.mkdirs();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                FileOutputStream out = new FileOutputStream(path+"/"+sdf.format(new Date())+".jpg");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                replayInfo.setReplayMessage("收录成功");
            }catch (IOException e){
                log.error(e.toString());
                replayInfo.setReplayMessage("收录失败");
            }
        }
        return replayInfo;
    }

    /**
     * 每日看世界的文字方式API解析
     */
    public boolean lookWorldWord() {
        String url = "http://bjb.yunwj.top/php/qq.php";
        try {
            JSONObject json = readJsonFromUrl(url);
            if (json == null){
                return false;
            }
            String word = json.getString("wb");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String path = "runFile/news";
            File folder = new File(path);
            if (!folder.exists()) folder.mkdirs();
            FileOutputStream out = new FileOutputStream("runFile/news/" + sdf.format(new Date()) + ".txt");
            out.write(word.getBytes(UTF_8));
        } catch (IOException | JSONException e) {
            log.error(e.toString());
            return false;
        }
        return true;
    }

    @AngelinaFriend(keyWords = {"强制更新每日看世界"})
    @AngelinaGroup(keyWords = {"强制更新每日看世界"}, sort = "订阅功能",funcClass = "每日看世界")
    public ReplayInfo manualUpdate(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您没有权限");
        }else {
            boolean word = lookWorldWord();
            boolean pic = lookWorldPic();
            replayInfo.setReplayMessage("强制更新完成\n文字版获取结果：" + word + "\n图片版获取结果：" + pic);
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"每日看世界"}, sort = "订阅功能",funcClass = "每日看世界")
    public ReplayInfo lookWorldWord(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //注意，这里每天三次查询次数设置在了内存，如果重启bot，查询次数会被清空
        Integer time = lookWorldMap.get(messageInfo.getGroupId());
        if (time == null) time = 0;
        if (time < 3) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            try (FileReader reader = new FileReader("runFile/news/" + sdf.format(new Date()) + ".txt");BufferedReader buffReader = new BufferedReader(reader)){
                String line = buffReader.readLine();
                if (line != null) {
                    String[] split = line.split("【换行】");
                    TextLine textLine = new TextLine(30);
                    textLine.addCenterStringLine("《每天60秒读懂世界》");
                    for (String s : split) {
                        textLine.addString(s.replace("【换行】", "\n"));
                    }
                    replayInfo.setReplayImg(textLine.drawImage(50, false, true));
                }else {
                    replayInfo.setReplayMessage("文字版获取失败，您可以自行点击查看专栏"+zhiHu());
                }
            }catch (IOException e){
                log.error(e.toString());
                try {
                    replayInfo.setReplayMessage("文字版获取失败，您可以自行点击查看专栏"+zhiHu());
                }catch (IOException exception){
                    log.error(e.toString());
                }
            }
            lookWorldMap.put(messageInfo.getGroupId(),time+1);
        }else {
            replayInfo.setReplayMessage("当前群组今日查阅次数已经超过上限");
        }
        return replayInfo;
    }

    public void cleanLookWorldTime(){
        lookWorldMap = new HashMap<>();
    }

    @AngelinaGroup(keyWords = {"历史上的今天"}, sort = "API功能")
    public ReplayInfo history(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String url =  nodeUrl() + "api/history-today?key="+tokenInstance();
        try {
            JSONObject json = readJsonFromUrl(url);
            JSONArray result = json.getJSONArray("result");
            StringBuilder s = new StringBuilder();
            s.append("历史上的今天\n");
            for(Object object:result){
                JSONObject a = (JSONObject) object;
                s.append("●").append(a.getInt("year")).append("年，");
                s.append(a.getString("title")).append("\n");
            }
            replayInfo.setReplayMessage(s.toString());
        }catch (JSONException e){
            log.error(e.toString());
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"每日一句"}, sort = "API功能")
    public ReplayInfo oneWord(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String url =  nodeUrl() + "api/word?key="+tokenInstance();
        try {
            JSONObject json = readJsonFromUrl(url);
            JSONObject result = json.getJSONObject("result");
            String s = result.getString("text");
            replayInfo.setReplayMessage(s);
        }catch (JSONException e){
            log.error(e.toString());
        }
        return replayInfo;
    }


    @AngelinaGroup(keyWords = {"舔狗日记"},sort = "API功能")
    public ReplayInfo lickDog(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String url =  nodeUrl() + "api/tgrj-word?key="+tokenInstance();
        try{
            JSONObject json = readJsonFromUrl(url);
            JSONObject result = json.getJSONObject("result");
            String s = result.getString("text");
            replayInfo.setReplayMessage(s);
        }catch (JSONException e){
            log.error(e.toString());
        }
        return replayInfo;
    }

    public static JSONObject readJsonFromUrl(String url){
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader out = new BufferedReader(new InputStreamReader(is,UTF_8));
            StringBuilder s = new StringBuilder();
            int cp;
            while ((cp = out.read()) != -1) {
                s.append((char) cp);
            }
            String sb = s.toString();
            if (sb.equals("")) return null;
            int i = sb.indexOf("{");
            sb = sb.substring(i);
            int p = sb.lastIndexOf("}");
            sb = sb.substring(0,p+1);
            return new JSONObject(sb.trim());
        }catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String zhiHu() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpget = new HttpGet("https://www.zhihu.com/api/v4/columns/c_1261258401923026944/items");
        response = httpclient.execute(httpget);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            //5.获取响应内容
            HttpEntity httpEntity = response.getEntity();
            JSONObject json = new JSONObject(EntityUtils.toString(httpEntity, "utf-8"));
            JSONArray data = json.getJSONArray("data");
            String zhiHuUrl = data.getJSONObject(0).getString("url");
            return zhiHuUrl;
        } else {
            //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
            System.out.println("返回状态不是200");
            System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
            return null;
        }
    }

}
