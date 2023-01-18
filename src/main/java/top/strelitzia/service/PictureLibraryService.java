package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
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
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.PictureLibraryMapper;
import top.strelitzia.model.PictureLibraryInfo;
import top.strelitzia.util.BaiduAPIUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 此服务包含存图，读图，人工审核
 */
@Service
@Slf4j
public class PictureLibraryService {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private PictureLibraryMapper pictureLibraryMapper;

    @Value("${baiduAuditConfig.APP_ID}")
    private String APP_ID;

    @Value("${baiduAuditConfig.API_KEY}")
    private String API_KEY;

    @Value("${baiduAuditConfig.SECRET_KEY}")
    private String SECRET_KEY;

    private final static Set<Long> saveGroupList = new HashSet<>();
    private final static Set<Long> checkGroupList = new HashSet<>();

    @AngelinaGroup(keyWords = {"存图"}, description = "给图库存入图片，提供给好图功能使用", sort = "娱乐功能",funcClass = "好图分享")
    public ReplayInfo savePic(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (pictureLibraryMapper.selectBlack(messageInfo.getQq()).equals(1)){
            replayInfo.setReplayMessage("您没有权限");
            return replayInfo;
        }
        if (saveGroupList.contains(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("群组内已有群友正在存图，请等待结束后方可存入新的图片");
            return replayInfo;
        }else {
            saveGroupList.add(messageInfo.getGroupId());
        }
        PictureLibraryInfo picInfo = new PictureLibraryInfo();
        if(messageInfo.getArgs().size()>1){
            picInfo.setFolder(messageInfo.getArgs().get(1));
            replayInfo.setReplayMessage("文件夹名收录成功，请发送要存的图，注意一次只支持一张图");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }else {
            picInfo.setFolder("未分类");
            replayInfo.setReplayMessage("请发送要存的图，注意一次只支持一张图");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }
        replayInfo.setReplayMessage(null);
        boolean right = false;
        while (!right){
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    if (message.getImgUrlList().size() > 0){
                        return message.getGroupId().equals(messageInfo.getGroupId()) &&
                                message.getQq().equals(messageInfo.getQq());
                    }else {
                        try {
                            return message.getGroupId().equals(messageInfo.getGroupId()) &&
                                    message.getQq().equals(messageInfo.getQq()) &&
                                    message.getText().equals("存图结束");
                        }catch (NullPointerException e){
                            saveGroupList.remove(messageInfo.getGroupId());
                            return false;
                        }
                    }
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setQq(messageInfo.getQq());
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null){
                replayInfo.setReplayMessage("写入超时，写入已关闭");
                saveGroupList.remove(messageInfo.getGroupId());
                return replayInfo;
            }
            if (recall.getImgUrlList().size() > 0) {
                if(recall.getImgUrlList().size()>1){
                    replayInfo.setReplayMessage("只支持依次存图，请重新发送");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    continue;
                }else {
                    String url = recall.getImgUrlList().get(0);
                    replayInfo.setReplayMessage("图片已接收，正在调用百度图像审核");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    String filePath;

                    //单例模式的百度API实例
                    BaiduAPIUtil baiduAPIUtil = BaiduAPIUtil.getInstance(2);
                    if(baiduAPIUtil == null){
                        baiduAPIUtil = BaiduAPIUtil.setInstance(2,APP_ID, API_KEY, SECRET_KEY);
                    }
                    //调用百度api图片审核
                    String s = baiduAPIUtil.BaiduCheck(url);
                    String format = recall.getImgTypeList().get(0).toString();
                    if(Objects.equals(format, "UNKNOWN")){
                        format = "jpg";
                    }
                    if (s.equals("不合规")){
                        replayInfo.setReplayMessage("审核不通过，请重新选图发送");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        continue;
                    }else if (s.equals("疑似")) {
                        picInfo.setType(0);
                        picInfo.setFormat(format);
                        picInfo.setUploadQQ(recall.getQq());
                        pictureLibraryMapper.insectPicture(picInfo);
                        Integer id = pictureLibraryMapper.selectId();
                        String doubtPath = "runFile/doubt/"+picInfo.getFolder();
                        File doubtFolder = new File(doubtPath);
                        if (!doubtFolder.exists()) doubtFolder.mkdirs();
                        filePath = doubtPath + "/"+ picInfo.getFolder() +"-"+ id + "." +format;
                        log.info("进入疑似文件夹");
                    }else {
                        picInfo.setType(1);
                        picInfo.setFormat(format);
                        picInfo.setUploadQQ(recall.getQq());
                        pictureLibraryMapper.insectPicture(picInfo);
                        Integer id = pictureLibraryMapper.selectId();
                        String passPath = "runFile/picture/"+picInfo.getFolder();
                        File passFolder = new File(passPath);
                        if (!passFolder.exists()) passFolder.mkdirs();
                        filePath = passFolder + "/"+ picInfo.getFolder() +"-"+ id + "." + format;
                        log.info("正式收录");
                    }
                    try {
                        download(url, filePath);
                    } catch (IOException e) {
                        log.error(e.toString());
                    }
                    replayInfo.setReplayMessage("添加成功，如果不需要发图了，回复存图结束即可");
                }
            }else {
                replayInfo.setReplayMessage("结束成功，正在写入图库");
                right = true;
            }
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
        }
        saveGroupList.remove(messageInfo.getGroupId());
        replayInfo.setReplayMessage("存图完成");
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"批量存图"},description = "对本地现有的文件直接快速存图")
    public ReplayInfo quantitySavePic(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        if (admin) {
            File folder = new File("runFile/localPic");
            if (!folder.exists()) folder.mkdirs();
            File[] folderList = folder.listFiles();
            if (folderList == null) {
                replayInfo.setReplayMessage("文件夹为空，存图失败");
                return replayInfo;
            }
            for (File folder1 : folderList) {
                if (folder1.isDirectory()) {
                    PictureLibraryInfo picInfo = new PictureLibraryInfo();
                    picInfo.setFolder(folder1.getName());
                    File[] folder1List = folder1.listFiles();
                    if (folder1List == null) continue;
                    for (File file : folder1List) {
                        picInfo.setType(1);
                        int point = file.getName().lastIndexOf(".");
                        picInfo.setFormat(file.getName().substring(point+1));
                        pictureLibraryMapper.insectPicture(picInfo);
                        Integer id = pictureLibraryMapper.selectId();
                        Path source = Paths.get(file.getAbsolutePath());
                        Path targetFolder = Paths.get("runFile/picture/" +picInfo.getFolder());
                        Path target = Paths.get("runFile/picture/" +picInfo.getFolder()+ "/" +picInfo.getFolder()+ "-" +id+ "." +picInfo.getFormat());
                        try {
                            if (!Files.exists(targetFolder)) Files.createDirectory(targetFolder);
                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            log.error(e.toString());
                        }
                        log.info("图片{}已写入", picInfo.getFolder() + "-" + id);
                    }
                    folder1.delete();
                }
            }
            replayInfo.setReplayMessage("导入完成");
        }else {
            replayInfo.setReplayMessage("权限不足");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"好图"}, description = "发送一张图库图片", sort = "娱乐功能",funcClass = "好图分享")
    public ReplayInfo pictureLibrary(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            if (messageInfo.getArgs().get(1).equals("列表")) {
                List<String> folderList= pictureLibraryMapper.selectFolderList();
                if (folderList.size() == 0) {
                    replayInfo.setReplayMessage("图库暂未收录图片");
                    log.error("尚未收录任何文件夹");
                } else {
                    TextLine textLine = new TextLine();
                    textLine.addCenterStringLine("图片可选版本有：");
                    textLine.nextLine();
                    for (String folder : folderList) {
                        textLine.addCenterStringLine(folder);
                        textLine.nextLine();
                    }
                    replayInfo.setReplayImg(textLine.drawImage());
                }
            } else {
                //如果输入了版本名字，则根据文件夹名字随机抽取图片
                String folder = messageInfo.getArgs().get(1);
                List<PictureLibraryInfo> picInfolist = pictureLibraryMapper.selectAllPictureByFolder(folder);
                if (picInfolist.size() == 0) {
                    replayInfo.setReplayMessage("您选择的图片版本不存在");
                }else {
                    PictureLibraryInfo picInfo = picInfolist.get(new Random().nextInt(picInfolist.size()));
                    String filePath = "runFile/picture/" +folder+ "/" +folder+ "-" +picInfo.getPictureId()+ "." +picInfo.getFormat();
                    File file = new File(filePath);
                    try {
                        BufferedImage image = ImageIO.read(file);
                        replayInfo.setReplayImg(image);
                    }catch (IOException e){
                        log.error(e.toString());
                    }
                }
            }
        }else {
            //如果没输入，直接随机抽取一张已审核图片
            PictureLibraryInfo picInfo = pictureLibraryMapper.selectAllPictureByType();
            if (picInfo.getPictureId()==null){
                replayInfo.setReplayMessage("图库暂未收录图片");
                log.error("尚未收录任何文件夹");
            }else {
                String filePath = "runFile/picture/"+picInfo.getFolder()+"/"+picInfo.getFolder()+ "-" + picInfo.getPictureId() + "." +picInfo.getFormat();
                replayInfo.setReplayImg(new File(filePath));
            }
        }
        replayInfo.setRecallTime(100);
        return replayInfo;
    }



    @AngelinaGroup(keyWords = {"审核"},description = "对图库图片人工审核", sort = "娱乐功能",funcClass = "好图分享")
    public ReplayInfo checkPicture(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (checkGroupList.contains(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("群组内已有审核正在进行，请等待结束后方可开始新的审核");
            return replayInfo;
        }else {
            checkGroupList.add(messageInfo.getGroupId());
        }
        boolean audit = pictureLibraryMapper.selectAudit(messageInfo.getQq())>0;//审核组的成员
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        if(audit||admin){
            replayInfo.setReplayMessage("审核开始，一个群组限制只能同时存在一个审核行为。请输入“通过”，“不通过”或“标记”，如需停止输入“关闭审核”即可");
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            while (true) {
                PictureLibraryInfo picInfo = pictureLibraryMapper.selectPictureWithoutCheckByType(1);
                if (picInfo == null){
                    replayInfo.setReplayMessage("已不存在未审核图片，审核结束");
                    checkGroupList.remove(messageInfo.getGroupId());
                    return replayInfo;
                }
                File file = new File("runFile/picture/" +picInfo.getFolder()+ "/" +picInfo.getFolder()+ "-" +picInfo.getPictureId()+ "." +picInfo.getFormat());
                replayInfo.setReplayImg(file);
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.getReplayImg().clear();
                AngelinaListener angelinaListener = new AngelinaListener() {
                    @Override
                    public boolean callback(MessageInfo message) {
                        return message.getGroupId().equals(messageInfo.getGroupId()) &&
                                message.getQq().equals(messageInfo.getQq()) &&
                                (message.getText().equals("通过") || message.getText().equals("不通过") || message.getText().equals("标记") || message.getText().equals("关闭审核"));
                    }
                };
                angelinaListener.setGroupId(messageInfo.getGroupId());
                angelinaListener.setFunctionId("check");
                angelinaListener.setSecond(120);
                MessageInfo callBack = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
                if (callBack == null){
                    replayInfo.setReplayMessage("等待超时，审核结束");
                    checkGroupList.remove(messageInfo.getGroupId());
                    return replayInfo;
                }
                if(callBack.getText().equals("通过")){
                    pictureLibraryMapper.updateAuditAndType(picInfo.getPictureId(),1);
                    replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"，置入常规图库成功");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else if (callBack.getText().equals("不通过")){
                    if (file.delete()){
                        pictureLibraryMapper.deletePictureByPictureId(picInfo.getPictureId());
                        replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"，图片已被删除");
                    }else {
                        pictureLibraryMapper.updateAuditAndType(picInfo.getPictureId(),2);
                        replayInfo.setReplayMessage("图片操作异常，已备注");
                    }
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else if(callBack.getText().equals("标记")){
                    pictureLibraryMapper.updateAuditAndType(picInfo.getPictureId(),999);
                    replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"的图片已被标注");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else {
                    replayInfo.setReplayMessage("审核已结束");
                    checkGroupList.remove(messageInfo.getGroupId());
                    return replayInfo;
                }
            }
        }else{
            replayInfo.setReplayMessage("权限不足");
            checkGroupList.remove(messageInfo.getGroupId());
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"疑似"})
    public ReplayInfo checkDoubtPicture(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        replayInfo.setReplayMessage("审核开始。请输入“通过”，“不通过”或“标记”，如需停止输入“关闭审核”即可");
        sendMessageUtil.sendFriendMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        if(admin){
            while (true) {
                PictureLibraryInfo picInfo = pictureLibraryMapper.selectPictureWithoutCheckByType(0);
                if (picInfo == null){
                    replayInfo.setReplayMessage("已不存在未审核图片，审核结束");
                    return replayInfo;
                }
                String path = "runFile/doubt/" +picInfo.getFolder()+ "/" + picInfo.getFolder() + "-" + picInfo.getPictureId() + "." +picInfo.getFormat();
                try {
                    BufferedImage image = ImageIO.read(new File(path));
                    replayInfo.setReplayImg(image,picInfo.getFormat());
                }catch (IOException e){
                    log.error(e.toString());
                }
                sendMessageUtil.sendFriendMsg(replayInfo);
                replayInfo.getReplayImg().clear();
                AngelinaListener angelinaListener = new AngelinaListener() {
                    @Override
                    public boolean callback(MessageInfo message) {
                        return message.getQq().equals(messageInfo.getQq()) &&
                                (message.getText().equals("通过") || message.getText().equals("不通过") || message.getText().equals("关闭审核") || message.getText().equals("标记") );
                    }
                };
                angelinaListener.setFunctionId("checkDoubt");
                angelinaListener.setSecond(120);
                MessageInfo callBack = AngelinaEventSource.waiter2(angelinaListener).getMessageInfo();
                if (callBack == null){
                    replayInfo.setReplayMessage("等待超时，审核结束");
                    return replayInfo;
                }
                if(callBack.getText().equals("通过")){
                    File targetFolder = new File("runFile/picture/"+picInfo.getFolder());
                    if (!targetFolder.exists()) targetFolder.mkdirs();
                    Path source = Paths.get(path);
                    Path target = Paths.get("runFile/picture/" +picInfo.getFolder()+ "/" + picInfo.getFolder() + "-" + picInfo.getPictureId() + "." +picInfo.getFormat());
                    try{
                        Files.move(source,target, StandardCopyOption.REPLACE_EXISTING);
                    }catch (IOException e){
                        log.error(e.toString());
                    }
                    pictureLibraryMapper.updateAuditAndType(picInfo.getPictureId(),1);
                    replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"，置入常规图库成功");
                    sendMessageUtil.sendFriendMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else if (callBack.getText().equals("不通过")){
                    Path source = Paths.get(path);
                    try {
                        Files.delete(source);
                        pictureLibraryMapper.deletePictureByPictureId(picInfo.getPictureId());
                        replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"，图片已被删除");
                    }catch (IOException e){
                        log.error(e.toString());
                    }
                    sendMessageUtil.sendFriendMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else if(callBack.getText().equals("标记")){
                    pictureLibraryMapper.updateAuditAndType(picInfo.getPictureId(),999);
                    replayInfo.setReplayMessage("审核结果已收录，编号"+picInfo.getPictureId()+"的图片已被标注");
                    sendMessageUtil.sendFriendMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }else {
                    replayInfo.setReplayMessage("审核已结束");
                    return replayInfo;
                }
            }
        }else{
            replayInfo.setReplayMessage("权限不足");
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"删除图片"})
    public ReplayInfo deletePicture(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean audit = pictureLibraryMapper.selectAudit(messageInfo.getQq())>0;//审核组的成员
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        if(audit||admin){
            if(messageInfo.getArgs().size()>1){
                if (messageInfo.getArgs().get(1).matches("^[1-9]\\d*$")){
                    Integer pictureId = Integer.valueOf(messageInfo.getArgs().get(1));
                    PictureLibraryInfo picInfo = pictureLibraryMapper.selectPictureById(pictureId);
                    String type;
                    if(picInfo.getType().equals(0)){
                        type = "doubt";
                    }else {
                        type = "picture";
                    }
                    pictureLibraryMapper.deletePictureByPictureId(picInfo.getPictureId());
                    String path = "runFile/" +type+ "/" +picInfo.getFolder()+ "/" +picInfo.getFolder()+ "-" +picInfo.getPictureId()+ "." +picInfo.getFormat();
                    try {
                        Files.delete(Paths.get(path));
                        replayInfo.setReplayMessage("编号"+picInfo.getPictureId()+"图片已被删除");
                    } catch (IOException e) {
                        log.error(e.toString());
                        replayInfo.setReplayMessage("编号"+picInfo.getPictureId()+"数据已被删除，但文件删除失败");
                    }
                }else {
                    replayInfo.setReplayMessage("输入不符");
                }
            }else {
                replayInfo.setReplayMessage("请输入查询的图片ID");
            }
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"查询上传者"})
    public ReplayInfo inquireUploader(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean audit = pictureLibraryMapper.selectAudit(messageInfo.getQq())>0;//审核组的成员
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        if(audit||admin){
            if(messageInfo.getArgs().size()>1){
                if (messageInfo.getArgs().get(1).matches("^[1-9]\\d*$")){
                    Integer pictureId = Integer.valueOf(messageInfo.getArgs().get(1));
                    Long qq = pictureLibraryMapper.selectUploadQQByPictureId(pictureId);
                    replayInfo.setReplayMessage("该图片ID对应的上传者QQ为："+qq);
                }else {
                    replayInfo.setReplayMessage("输入不符");
                }
            }else {
                replayInfo.setReplayMessage("请输入查询的图片ID");
            }
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"图库黑名单"})
    public ReplayInfo blacklistUploader(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean audit = pictureLibraryMapper.selectAudit(messageInfo.getQq()) > 0;//审核组的成员
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());//顶级权限
        if (audit || admin) {
            if(messageInfo.getArgs().size()>2) {
                if (messageInfo.getArgs().get(1).equals("添加")) {
                    if (messageInfo.getArgs().get(2).matches("^[1-9]\\d*$")) {
                        Long qq = Long.valueOf(messageInfo.getArgs().get(2));
                        pictureLibraryMapper.insertBlack(qq);
                        replayInfo.setReplayMessage("该人员已被加入黑名单");
                    }else {
                        replayInfo.setReplayMessage("输入不符");
                    }
                }else if(messageInfo.getArgs().get(1).equals("移除")){
                    if (messageInfo.getArgs().get(2).matches("^[1-9]\\d*$")) {
                        Long qq = Long.valueOf(messageInfo.getArgs().get(2));
                        pictureLibraryMapper.deleteBlack(qq);
                        replayInfo.setReplayMessage("该人员已被移除黑名单");
                    }else {
                        replayInfo.setReplayMessage("输入不符");
                    }
                }else if(messageInfo.getArgs().get(1).equals("查询")){
                    if (messageInfo.getArgs().get(2).matches("^[1-9]\\d*$")) {
                        Long qq = Long.valueOf(messageInfo.getArgs().get(2));
                        Integer num = pictureLibraryMapper.selectBlack(qq);
                        if(num>0){
                            replayInfo.setReplayMessage("黑名单中存在该人员");
                        }else {
                            replayInfo.setReplayMessage("黑名单中不存在该人员");
                        }
                    }else {
                        replayInfo.setReplayMessage("输入不符");
                    }
                }else {
                    replayInfo.setReplayMessage("不存在该指令");
                }
            }else{
                replayInfo.setReplayMessage("需输入操作和QQ号");
            }
        }
        return replayInfo;
    }

    private void download(String url, String path) throws IOException {
        URL u = new URL(url);
        HttpURLConnection httpUrl = (HttpURLConnection) u.openConnection();
        httpUrl.connect();
        InputStream is = httpUrl.getInputStream();
        FileOutputStream fs = new FileOutputStream(path);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            fs.write(buffer, 0, len);
        }
    }
}
