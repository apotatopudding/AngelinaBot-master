package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaEvent;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.EventEnum;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.AdminUserMapper;
import top.strelitzia.model.AdminUserInfo;
import top.strelitzia.util.AdminUtil;
import top.strelitzia.util.PetPetUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PetPetService {

    @Autowired
    private PetPetUtil petPetUtil;

    private static boolean close = true;

    private final List<Long> groupList = new ArrayList<>(
            Arrays.asList(740369727L)
    );

    @AngelinaEvent(event = EventEnum.BotJoinGroupEvent, description = "bot进群")
    public ReplayInfo BotJoin(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getMemberList().size()<20 && close){
            if (!groupList.contains(messageInfo.getGroupId())) {
                replayInfo.setReplayMessage("琴柳不支持测试群使用，即将自动退群" +
                        "\n如果想要拥有一个自己的机器人，可以加入安洁莉娜克隆中心" +
                        "\n克隆中心群号：235917683");
                replayInfo.setQuitTime(3);
            }
        }else {
            replayInfo.setReplayMessage("大家好！新人琴柳，向您报道！"
                    + "您可以随时通过【琴柳】呼唤我"
                    + "\n洁哥源码：https://github.com/Strelizia02/AngelinaBot"
                    + "\n琴柳版源码：https://github.com/apotatopudding/AngelinaBot-master"
                    + "\n欢迎加入琴柳主群：679030636"
                    + "\n如果你也希望拥有一个自己的机器人的话，可以加入安洁莉娜克隆中心获取"
                    + "\n克隆中心群号：235917683");
        }
        return replayInfo;
    }

    @AngelinaEvent(event = EventEnum.NudgeEvent, description = "发送头像的摸头动图")
    @AngelinaGroup(keyWords = {"摸头", "摸我", "摸摸"}, description = "发送头像的摸头动图")
    public ReplayInfo PetPet(MessageInfo messageInfo) {

        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //BufferedImage userImage = ImageUtil.Base64ToImageBuffer(ImageUtil.getImageBase64ByUrl("http://q.qlogo.cn/headimg_dl?dst_uin=" + messageInfo.getQq() + "&spec=100"));
        //String path = "runFile/petpet/frame.gif";
        //petPetUtil.getGif(path, userImage);
        //replayInfo.setReplayImg(new File(path));
        //return replayInfo;
        BufferedImage userImage;
        try {
            userImage = ImageIO.read(new URL("http://q.qlogo.cn/headimg_dl?dst_uin=" + messageInfo.getQq() + "&spec=100"));
            String path = "runFile/petpet/frame.gif";
            petPetUtil.getGif(path, userImage);
            replayInfo.setRecallTime(60);
            replayInfo.setReplayImg(new File(path));
            return replayInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @AngelinaEvent( event = EventEnum.GroupRecall, description = "撤回事件回复" )
    public ReplayInfo GroupRecall(MessageInfo messageInfo) {
        String path = "runFile/petpet/saiLeach.gif";
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayImg(new File(path));
        replayInfo.setRecallTime(30);
        replayInfo.setReplayMessage("这一次换我守在这里狞笑，您快带着大家分崩离析");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"口我","透透","透我","cao我","草我","艹我"}, description = "禁言功能")
    public ReplayInfo MuteSomeOne(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getBotPermission().getLevel()>messageInfo.getUserAdmin().getLevel()) {
            replayInfo.setMuted((new Random().nextInt(5) + 1) * 60);
        }else {
            replayInfo.setReplayMessage("但愿你已认清自己的不义");
        }
        return replayInfo;
    }

    @AngelinaEvent(event = EventEnum.MemberJoinEvent, description = "入群欢迎")
    public ReplayInfo memberJoin(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("欢迎" + messageInfo.getName()
                                    + "，在这段同行路上，我想成为您可以依赖的伙伴。"
                                    + "您可以随时通过【琴柳】呼唤我");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"贴贴","亲亲","抱抱","么么"}, description = "发送琴柳感语音")
    public ReplayInfo newAudio(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/Audio/qinLiuGan";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if(files == null){
            replayInfo.setReplayMessage("文件夹不存在，请检查文件夹");
            return replayInfo;
        }
        if(folder.isDirectory() && files.length != 0) {
            int selectAudioIndex = new SecureRandom().nextInt(files.length);
            File selectFile = files[selectAudioIndex];
            String oriFileName = selectFile.getAbsolutePath();
            replayInfo.setReplayAudio(new File(oriFileName));
        }else {log.info("引用了一个空文件或空文件夹");}
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"我爱你","爱你"}, description = "发送琴柳感语音")
    public ReplayInfo loveAudio(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/Audio/cao";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if(files == null){
            replayInfo.setReplayMessage("文件夹不存在，请检查文件夹");
            return replayInfo;
        }
        if(folder.isDirectory() && files.length != 0) {
            int selectAudioIndex = new SecureRandom().nextInt(files.length);
            File selectFile = files[selectAudioIndex];
            String oriFileName = selectFile.getAbsolutePath();
            replayInfo.setReplayAudio(new File(oriFileName));
        }else {log.info("引用了一个空文件或空文件夹");}
        return replayInfo;
    }

    @AngelinaEvent(event = EventEnum.MemberLeaveEvent, description = "退群提醒")
    public ReplayInfo memberLeaven(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage(messageInfo.getName()+"离开了我们！" +
                "\n你有没有听见群友的悲鸣！" +
                "\n你有没有感受到"+messageInfo.getGroupName()+"在分崩离析！" +
                "\n你不曾注意"+messageInfo.getGroupOwnerName()+"在狞笑" +
                "\n你有没有想过，"+messageInfo.getName()+"不再是朋友！"+messageInfo.getGroupName()+"不再是家园！");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"？"}, description = "发送？图")
    public ReplayInfo GroupQuestion(MessageInfo messageInfo) {
        String path = "runFile/questionPicture/question.jpg";
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setRecallTime(30);
        replayInfo.setReplayImg(new File(path));
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"壁纸"}, description = "发送一张壁纸库的图片")
    public ReplayInfo GroupWallPaper(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            if(messageInfo.getArgs().get(1).equals("列表")){
                String folderPath = "runFile/wallPaper";
                File folder = new File(folderPath);
                File[] files = folder.listFiles();
                TextLine textLine = new TextLine();
                textLine.addCenterStringLine("壁纸可选版本有：");
                textLine.nextLine();
                if (files == null){
                    replayInfo.setReplayMessage("读取出错，请联系琴柳机器人的运行者");
                    log.error("目录下找不到任何文件夹");
                }else {
                    for (File selectFolder : files) {
                        if(selectFolder.isDirectory()) {
                            textLine.addCenterStringLine(selectFolder.getName());
                            textLine.nextLine();
                        }
                    }
                    replayInfo.setReplayImg(textLine.drawImage(50,true));
                }
            }else {
                //如果输入了版本名字，则根据版本名字随机抽取图片
                String path = messageInfo.getArgs().get(1);
                String folderPath = "runFile/wallPaper/" + path;
                File folder = new File(folderPath);
                if (folder.isDirectory()) {
                    log.info("folder:" + folder.getName());
                    File[] files = folder.listFiles();
                    if (files == null) {
                        replayInfo.setReplayMessage("读取出错，请联系琴柳机器人的运行者");
                        log.error(folder.getName() + "文件夹内不存在文件");
                    } else {
                        int selectPicIndex = new Random().nextInt(files.length);
                        File selectFile = files[selectPicIndex];
                        String oriFileName = selectFile.getAbsolutePath();
                        replayInfo.setReplayImg(new File(oriFileName));
                    }
                } else {
                    replayInfo.setReplayMessage("您选择的壁纸版本不存在");
                }
            }
        }else {
            //如果没输入，直接随机抽取文件夹下抽取一张图片
            String folderPath = "runFile/wallPaper";
            File folder1 = new File(folderPath);
            File[] files1 = folder1.listFiles();
            if (files1 == null){
                replayInfo.setReplayMessage("读取出错，请联系琴柳机器人的运行者");
                log.error("目录下找不到任何文件夹");
            }else {
                int selectFolderIndex =new Random().nextInt (files1.length);
                File selectFolder =files1[selectFolderIndex];
                if(selectFolder.isDirectory()) {
                    //当抽取的文件夹是文件夹时，从中随机抽出一张图
                    log.info("folder:"+selectFolder.getName());
                    File folder2 = new File(selectFolder.getAbsolutePath());
                    File[] files2 = folder2.listFiles();
                    if (files2==null){
                        replayInfo.setReplayMessage("读取出错，请联系琴柳机器人的运行者");
                        log.error( selectFolder.getName()+"文件夹内不存在文件" );
                    }else {
                        int selectPicIndex =new Random().nextInt (files2.length);
                        File selectFile = files2[selectPicIndex];
                        String oriFileName = selectFile.getAbsolutePath();
                        replayInfo.setReplayImg(new File(oriFileName));
                    }
                }else {
                    //当抽出的不是文件夹即源目录下带有某个图像文件时，直接发送该图片
                    log.info("file:"+selectFolder.getName());
                    String oriFileName = selectFolder.getAbsolutePath();
                    replayInfo.setReplayImg(new File(oriFileName));
                }
            }
        }
        replayInfo.setRecallTime(60);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"好图"}, description = "发送一张图库图片")
    public ReplayInfo GroupPicture(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/picture";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if(files == null){
            replayInfo.setReplayMessage("文件夹不存在，请检查文件夹");
            return replayInfo;
        }
        if(folder.isDirectory() && files.length != 0) {
            int picNum = files.length;
            int selectPicIndex = (int) (Math.random()*picNum);
            File selectFile = files[selectPicIndex];
            String oriFileName = selectFile.getAbsolutePath();
            replayInfo.setReplayImg(new File(oriFileName));
        }
        else {log.info("引用了一个空文件或空文件夹");}
        replayInfo.setRecallTime(60);
        return replayInfo;
    }

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;


    @AngelinaFriend(keyWords = {"群组清查"})
    public ReplayInfo find(MessageInfo messageInfo) throws InterruptedException {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<AdminUserInfo> admins = adminUserMapper.selectAllAdmin();
        if (AdminUtil.getSqlAdmin(messageInfo.getQq(), admins)) {
            if (messageInfo.getArgs().size()>1){
                if (messageInfo.getArgs().get(1).equals("暂时关闭")) {
                    close = false;
                    new Thread(()->{
                        try{Thread.sleep(300000);}catch (InterruptedException e){log.error(e.toString());}
                        close = true;
                        replayInfo.setReplayMessage("已重新开启");
                        sendMessageUtil.sendFriendMsg(replayInfo);
                    }).start();
                    replayInfo.setReplayMessage("已关闭群组人数清查，持续时间五分钟，五分钟后将会自动再次开启群组人数清查");
                }
            }else {
                replayInfo.setReplayMessage("开始清理");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                Bot bot = Bot.getInstance(replayInfo.getLoginQQ());
                ContactList<Group> groups = bot.getGroups();
                for(Group group : groups){
                    if (group.getMembers().size()<20 && !groupList.contains(group.getId())){
                        group.sendMessage("琴柳不支持测试群使用，即将自动退群" +
                                "\n如果想要拥有一个自己的机器人，可以加入安洁莉娜克隆中心" +
                                "\n克隆中心群号：235917683");
                        Thread.sleep(3000);
                        group.quit();
                    }
                }
                replayInfo.setReplayMessage("清理完成");
            }
        }else {
            replayInfo.setReplayMessage("您没有权限");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"功能列表"}, description = "琴柳的功能指令对照表")
    public ReplayInfo testIma(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TextLine textLine = new TextLine(35);
        textLine.addCenterStringLine("特殊功能指令对照表");
        textLine.addCenterStringLine("（带#号的指令意为群聊命令，前面需加bot名称）");
        textLine.addCenterStringLine("（带*号的指令意为私聊命令）");
        textLine.addCenterStringLine("（带$号的指令意为带权限命令）");
        textLine.addString("#轮盘对决");
        textLine.addSpace(7);
        textLine.addString("#给轮盘上子弹 #上膛 #拔枪吧");
        textLine.nextLine();
        textLine.addString("#对决开始");
        textLine.addSpace(7);
        textLine.addString("#开枪");
        textLine.nextLine();
        textLine.addString("#轮盘对决结束");
        textLine.addSpace(5);
        textLine.addString("#轮盘赌结束");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("#创建卡池");
        textLine.addSpace(7);
        textLine.addString("#开始绝地作战");
        textLine.nextLine();
        textLine.addString("#添加卡片");
        textLine.addSpace(7);
        textLine.addString("*搜索");
        textLine.nextLine();
        textLine.addString("#删除卡片");
        textLine.addSpace(7);
        textLine.addString("*前往");
        textLine.nextLine();
        textLine.addString("#抽卡");
        textLine.addSpace(9);
        textLine.addString("*出击");
        textLine.nextLine();
        textLine.addString("#销毁卡池");
        textLine.addSpace(7);
        textLine.addString("#*绝地查询 属性");
        textLine.nextLine();
        textLine.addSpace(12);
        textLine.addString("#*绝地查询 巡逻范围");
        textLine.nextLine();
        textLine.addString("#档案补全");
        textLine.addSpace(7);
        textLine.addString("#绝地游戏描述");
        textLine.nextLine();
        textLine.addString("#我知道了");
        textLine.nextLine();
        textLine.addSpace(12);
        textLine.addString("#的小小茶话会");
        textLine.nextLine();
        textLine.addString("#猜干员");
        textLine.addSpace(8);
        textLine.addString("#(干员名字)!");
        textLine.nextLine();
        textLine.addString("#重启猜干员");
        textLine.addSpace(6);
        textLine.addString("#$退出茶话会");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("#$开启破站解析");
        textLine.addSpace(4);
        textLine.addString("#$群组开启");
        textLine.nextLine();
        textLine.addString("#$关闭破站解析");
        textLine.addSpace(4);
        textLine.addString("（此命令无需bot名）");
        textLine.nextLine();
        textLine.addSpace(12);
        textLine.addString("#$群组关闭");
        textLine.nextLine();
        textLine.addString("#查询积分榜");
        textLine.nextLine();
        textLine.addString("#查询积分榜 X(名字)");
        textLine.addString("*请远山小姐为我占卜吧");
        textLine.nextLine();
        textLine.addString("#查询积分榜 X(QQ)");
        textLine.addString("*翻开第一张预言");
        textLine.nextLine();
        textLine.addSpace(12);
        textLine.addString("*翻开第二张预言");
        textLine.nextLine();
        textLine.addString("#赛马娘");
        textLine.addSpace(8);
        textLine.addString("*翻开第三张预言");
        textLine.nextLine();
        textLine.addString("#下注 (选手 编号)");
        textLine.addSpace(1);
        textLine.addString("*远山小姐解牌 X(牌名)");
        textLine.nextLine();
        textLine.addString("(下注命令与猜干员时回答干员名都无需bot名称)");
        replayInfo.setReplayImg(textLine.drawImage(50,true));
        return replayInfo;
    }
}
