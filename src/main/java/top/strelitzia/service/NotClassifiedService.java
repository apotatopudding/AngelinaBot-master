package top.strelitzia.service;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaEvent;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.EventEnum;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.angelinaBot.util.AdminUtil;
import top.strelitzia.dao.AdminUserMapper;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class NotClassifiedService {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private static boolean close = true;

    public static volatile String QQInstance;

    private static volatile List<String> GroupInstance = null;

    @Value("${userConfig.ownerQQ}")
    private String ownerQQ;

    @Value("${userConfig.testGroup}")
    private List<String> testGroup;

    public static String QQSetInstance(String ownerQQ){
        if (QQInstance == null) {
            synchronized (NotClassifiedService.class) {
                if (QQInstance == null) {
                    QQInstance = ownerQQ;
                }
            }
        }
        return QQInstance;
    }
    public List<String> GroupSetInstance(){
        if (GroupInstance == null) {
            synchronized (NotClassifiedService.class) {
                if (GroupInstance == null) {
                    GroupInstance = this.testGroup;
                }
            }
        }
        return GroupInstance;
    }

    @AngelinaEvent(event = EventEnum.BotJoinGroupEvent, description = "bot进群")
    public ReplayInfo BotJoin(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getMemberList().size()<20 && close){
            if (!GroupSetInstance().contains(String.valueOf(messageInfo.getGroupId()))) {
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
                    + "\n如果你也希望拥有一个自己的机器人，可以加入安洁莉娜克隆中心获取"
                    + "\n克隆中心群号：235917683");
        }
        return replayInfo;
    }

    @AngelinaEvent( event = EventEnum.GroupRecall, description = "撤回事件回复" )
    public ReplayInfo GroupRecall(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayImg(new File("runFile/petpet/saiLeach.gif"));
        replayInfo.setRecallTime(30);
        replayInfo.setReplayMessage("这一次换我守在这里狞笑，您快带着大家分崩离析");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"口我","透透","透我","cao我","草我","艹我"}, description = "嘿嘿嘿")
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

    @AngelinaGroup(keyWords = {"贴贴","亲亲","抱抱","么么"}, description = "发送琴柳感语音", sort = "娱乐功能",funcClass = "大宝贝")
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

    @AngelinaGroup(keyWords = {"我爱你","爱你"}, description = "发送琴柳感语音", sort = "娱乐功能",funcClass = "大宝贝")
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

    @AngelinaGroup(keyWords = {"？"}, description = "发送？图", sort = "娱乐功能")
    public ReplayInfo GroupQuestion(MessageInfo messageInfo) {
        String path = "runFile/questionPicture/question.jpg";
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setRecallTime(30);
        replayInfo.setReplayImg(new File(path));
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"壁纸"}, description = "发送一张壁纸库的图片", sort = "娱乐功能",funcClass = "好图分享")
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

    @Autowired
    private AdminUserMapper adminUserMapper;

    //@AngelinaFriend(keyWords = {"群组清查"})
    public ReplayInfo find(MessageInfo messageInfo) throws InterruptedException {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (AdminUtil.getAdmin(messageInfo.getQq())) {
            if (messageInfo.getArgs().size()>1){
                if (messageInfo.getArgs().get(1).equals("暂时关闭")) {
                    close = false;
                    replayInfo.setReplayMessage("已关闭群组人数清查，持续时间五分钟，五分钟后将会自动再次开启群组人数清查");
                    sendMessageUtil.sendFriendMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    new Thread(()->{
                        try{Thread.sleep(300000);}catch (InterruptedException e){log.error(e.toString());}
                        close = true;
                        replayInfo.setReplayMessage("已重新开启");
                    }).start();
                }
            }else {
                replayInfo.setReplayMessage("开始清理");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                Bot bot = Bot.getInstance(replayInfo.getLoginQQ());
                ContactList<Group> groups = bot.getGroups();
                for(Group group : groups){
                    if (group.getMembers().size()<20 && !testGroup.contains(String.valueOf(group.getId()))){
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

    @AngelinaGroup(keyWords = {"禁言"},description = "嘿嘿嘿")
    public ReplayInfo spoof(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getBotPermission().getLevel()>messageInfo.getUserAdmin().getLevel()) replayInfo.setMuted(10);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"反馈"},description = "可以用于给运营管理发送反馈的消息，只限单条文字内容消息")
    public ReplayInfo feedback(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo();
        replayInfo.setGroupId(messageInfo.getGroupId());
        replayInfo.setQq(Long.valueOf(QQSetInstance(this.ownerQQ)));
        replayInfo.setLoginQQ(MiraiFrameUtil.messageIdMap.get(messageInfo.getGroupId()));
        if(messageInfo.getArgs().size()>1){
            String s = "琴柳收到一条反馈消息:\n" + messageInfo.getArgs().get(1) +
                    "\n发送方为" + messageInfo.getGroupName() + "群成员" + messageInfo.getName() +
                    "\n群号：" + messageInfo.getGroupId();
            replayInfo.setReplayMessage(s);
        }
        sendMessageUtil.sendFriendMsg(replayInfo);
        replayInfo =new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("反馈已发送");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"文字转语音"}, sort = "娱乐功能")
    public ReplayInfo word(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);

        String path = "runFile/Audio/text.mp3";
        String word;
        if(messageInfo.getArgs().size()>1) {
            word = messageInfo.getArgs().get(1);
            textToSpeech(word);
            replayInfo.setReplayAudio(new File(path));
        }else {
            replayInfo.setReplayMessage("您还没说要转换的文字是什么呢");
        }
        return replayInfo;
    }

    public static void textToSpeech(String text) {
        try {
            //构建音频格式 调用注册表应用
            Dispatch spVoice = new ActiveXComponent("Sapi.SpVoice").getObject();
            //音频文件输出流
            Dispatch spFileStream = new ActiveXComponent("Sapi.SpFileStream").getObject();
            //构建音频格式 调用注册表应用
            Dispatch spAudioFormat = new ActiveXComponent("Sapi.SpAudioFormat").getObject();

            //设置spAudioFormat音频流格式类型22
            Dispatch.put(spAudioFormat, "Type", new Variant(22));
            //设置spFileStream文件输出流的音频格式
            Dispatch.putRef(spFileStream, "Format", spAudioFormat);
            //设置spFileStream文件输出流参数地址等
            Dispatch.call(spFileStream, "Open", new Variant("./runFile/Audio/text.mp3"), new Variant(3), new Variant(true));
            // 设置声音对象的音频输出流为输出文件对象
            Dispatch.putRef(spVoice, "AudioOutputStream", spFileStream);
            //设置spVoice声音对象的音量大小100(0-100)
            Dispatch.put(spVoice, "Volume", new Variant(100));
            //设置spVoice声音对象的速度 0为正常速度(-10 ~ +10)
            Dispatch.put(spVoice, "Rate", new Variant(0));
            //设置spVoice声音对象中的文本内容进行朗读
            log.info(Dispatch.call(spVoice, "Speak", new Variant(text)).toString());
            // 关闭输出文件
            Dispatch.call(spFileStream, "Close");
            Dispatch.putRef(spVoice, "AudioOutputStream", null);
            spAudioFormat.safeRelease();
            spFileStream.safeRelease();
            spVoice.safeRelease();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
