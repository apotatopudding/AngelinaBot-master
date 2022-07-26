package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaEvent;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.EventEnum;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.IntegralInfo;
import top.strelitzia.util.PetPetUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PetPetService {

    @Autowired
    private PetPetUtil petPetUtil;

    @Autowired
    private IntegralMapper integralMapper;

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
        replayInfo.setReplayMessage("这一次换我守在这里狞笑，您快带着大家分崩离析");
        return replayInfo;
    }

    @AngelinaGroup(keyWords =
            {"口我",
            "透透",
            "透我",
            "cao你妈",
            "艹你妈",
            "草你妈",
            "我可以cao你吗",
            "可以cao你吗",
            "我可以操你吗",
            "可以操你吗",
            "我可以草你吗",
            "可以草你吗",
            "我可以艹你吗",
            "可以艹你吗",
            "cao我",
            "草我",
            "艹我"}, description = "禁言功能")
    public ReplayInfo MuteSomeOne(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setMuted((new Random().nextInt(30) + 1) * 60);
        return replayInfo;
    }

    @AngelinaEvent(event = EventEnum.MemberJoinEvent, description = "入群欢迎")
    public ReplayInfo memberJoin(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("欢迎" + messageInfo.getName()
                                    + "，在这段同行路上，我想成为您可以依赖的伙伴。"
                                    + "您可以随时通过【琴柳】呼唤我"
                                    + "\n洁哥源码：https://github.com/Strelizia02/AngelinaBot");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"贴贴","亲亲","抱抱","么么"}, description = "发送琴柳感语音")
    public ReplayInfo newAudio(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/Audio/qinLiuGan";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if(folder.isDirectory() && files.length != 0) {
            int picNum = files.length;
            int selectPicIndex = (int) (Math.random()*picNum);
            File selectFile = files[selectPicIndex];
            String oriFileName = selectFile.getAbsolutePath();
            replayInfo.setReplayAudio(new File(oriFileName));
        }else {log.info("引用了一个空文件或空文件夹");}
        return replayInfo;
    }

    @AngelinaEvent(event = EventEnum.MemberLeaveEvent, description = "退群提醒")
    public ReplayInfo memberLeaven(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("一路上摆脱深池的追杀很不容易。后来我和"+ messageInfo.getName() +"说过话，"+ messageInfo.getName() +"好像不记得我了。对"+ messageInfo.getName() +"来说.......是好事吧?"
                + "\n有机会的话，我还想找"+ messageInfo.getName() +"聊聊诗歌和小说。啊，能交个朋友就更好啦。");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"？"}, description = "发送？图")
    public ReplayInfo GroupQuestion(MessageInfo messageInfo) {
        String path = "runFile/questionPicture/question.jpg";
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayImg(new File(path));
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"壁纸"}, description = "发送一张壁纸库的图片")
    public ReplayInfo GroupWallPaper(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            //如果输入了版本名字，则根据版本名字随机抽取图片
            String path = messageInfo.getArgs().get(1);
            String folderPath = "runFile/wallPaper/"+path;
            File folder = new File(folderPath);
            if (folder.isDirectory()){
                log.info("folder:"+folder.getName());
                File[] files = folder.listFiles();
                if (files==null){
                    replayInfo.setReplayMessage("读取出错，请联系琴柳机器人的运行者");
                    log.error( folder.getName()+"文件夹内不存在文件" );
                }else {
                    int selectPicIndex =new Random().nextInt (files.length);
                    File selectFile = files[selectPicIndex];
                    String oriFileName = selectFile.getAbsolutePath();
                    replayInfo.setReplayImg(new File(oriFileName));
                }
            }else {
                replayInfo.setReplayMessage("您选择的壁纸版本不存在");
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
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"好图"}, description = "发送一张图库图片")
    public ReplayInfo GroupPicture(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/picture";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if(folder.isDirectory()) {
            int picNum = files.length;
            int selectPicIndex = (int) (Math.random()*picNum);
            File selectFile = files[selectPicIndex];
            String oriFileName = selectFile.getAbsolutePath();
            replayInfo.setReplayImg(new File(oriFileName));
        }
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"签到"}, description = "每日签到")
    public ReplayInfo checkIn(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        int special =this.integralMapper.selectBySwitch();//特殊奖励查询
        //查询当天是否已签到
        Integer today = this.integralMapper.selectDayCountByQQ(messageInfo.getQq());
        if(today == null) today = 0 ;
        if (today < 1) {
            Integer integral = this.integralMapper.selectByQQ(messageInfo.getQq());
            if(integral == null) integral = 0 ;
            int add = new Random().nextInt(3) + 1;//随机加1~3分
            if(special!=0) add = special;//有特殊奖励时，加分改为特殊分
            integral = integral + add;
            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),messageInfo.getName(),messageInfo.getQq(),integral);
            replayInfo.setReplayMessage("每日签到成功，获得积分："+add+"分");
            this.integralMapper.updateByQQ(messageInfo.getQq(), messageInfo.getName());//签到状态更新
        }else {
            replayInfo.setReplayMessage("您今日已签到，不可重复签到");
        }
        return replayInfo;
    }



    @AngelinaGroup(keyWords = {"查询积分榜"}, description = "积分榜查询")
    public ReplayInfo inquireIntegral(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            //以结果判定是不是数字，如果是数字，以QQ查询，如果不是，以名字查询
            boolean result = messageInfo.getArgs().get(1).matches("[0-9]+");
            if (!result){
                String name = messageInfo.getArgs().get(1);
                List<Integer> Integral = this.integralMapper.selectByName(name);
                //判定信息是否为空
                if(Integral.size()==0){
                    replayInfo.setReplayMessage("积分榜里没有您要查询的信息呢，您看看您是不是写错了");
                }else {
                    String remind = "";
                    if(Integral.size()>1){
                        remind = "\n由于名字出现重复，可能存在查询不准确情况，请使用QQ查询";
                    }
                    replayInfo.setReplayMessage(name + "的积分为" + Integral + remind);
                }
            }else {
                Long QQ = Long.valueOf(messageInfo.getArgs().get(1));
                Integer Integral = this.integralMapper.selectByQQ(QQ);
                //判定信息是否为空，空信息可能是因为名字为数字
                if(Integral==null){
                    //将QQ转换为名字进行二次查询，仍然查不到即没有
                    String name = String.valueOf(QQ);
                    List<Integer> nameIntegral = this.integralMapper.selectByName(name);
                    if(nameIntegral.size()==0){
                        replayInfo.setReplayMessage("积分榜里没有您要查询的信息呢，您看看您是不是写错了");
                    }else{
                        String remind = "";
                        if(nameIntegral.size()>1){
                            remind = "\n由于名字出现重复，可能存在查询不准确情况，请使用QQ查询";
                        }
                        replayInfo.setReplayMessage(name + "的积分为" + nameIntegral + remind);
                    }
                }else {
                    replayInfo.setReplayMessage(QQ+"的积分为"+Integral);
                }
            }
        }else {
            StringBuilder s = new StringBuilder();
            List<IntegralInfo> integralInfoList = this.integralMapper.selectFiveByName();
            int i = 0;
            for(IntegralInfo integralInfo : integralInfoList){
                i = i + 1;
                s.append("第").append(i).append("名为：").append(integralInfo.getName());
                s.append("   他的积分为").append(integralInfo.getIntegral()).append("\n");
            }
            replayInfo.setReplayMessage(s.toString());
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
        textLine.addString("#抢答");
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
        textLine.addString("#下注 (选手) (编号)");
        textLine.addSpace(1);
        textLine.addString("*远山小姐解牌 x(牌名)");
        textLine.nextLine();
        textLine.addString("(下注命令与猜干员时回答干员名都无需bot名称)");
        replayInfo.setReplayImg(textLine.drawImage(50,true));
        return replayInfo;
    }
}
