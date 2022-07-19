package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaEvent;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.EventEnum;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
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
    private IntegralMapper IntegralMapper;

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
                                    + "\n洁哥源码：https://github.com/Strelizia02/AngelinaBot"
                                    + "\n详细菜单请阅：https://github.com/Strelizia02/AngelinaBot/wiki");
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
        String folderPath = "runFile/wallPaper";
        //D:/新建文件夹/angelina/runFile/wallPaper
        //F:/new folder/AngelinaBot-master/AngelinaBot-master/runFile/wallPaper
        File folder1 = new File(folderPath);
        File[] files1 = folder1.listFiles();
        for(File indexFile:files1) {
            if(indexFile.isDirectory()) {
                log.info("folder:"+indexFile.getName());
                File folder2 = new File(indexFile.getAbsolutePath());
                File[] files2 = folder2.listFiles();
                int picNum = files2.length;
                int selectPicIndex = (int) (Math.random()*picNum);
                File selectFile = files2[selectPicIndex];
                String oriFileName = selectFile.getAbsolutePath();
                replayInfo.setReplayImg(new File(oriFileName));
            }
        }
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"好图"}, description = "发送一张图库图片")
    public ReplayInfo GroupPicture(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String folderPath = "runFile/picture";
        //D:/新建文件夹/angelina/runFile/wallPaper
        //F:/new folder/AngelinaBot-master/AngelinaBot-master/runFile/wallPaper
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
    @AngelinaGroup(keyWords = {"查询积分榜"}, description = "积分榜查询")
    public ReplayInfo inquireIntegral(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            String name = messageInfo.getArgs().get(1);
            replayInfo.setReplayMessage(name+"的积分为"+this.IntegralMapper.selectByName(name));
        }else {
            StringBuilder s = new StringBuilder();
            List<IntegralInfo> integralInfoList = this.IntegralMapper.selectFiveByName();
            int i = 0;
            for(IntegralInfo integralInfo : integralInfoList){
                i = i + 1;
                s.append("第").append(i).append("名为：").append(integralInfo.getName());
                s.append("   他的积分为").append(integralInfo.getIntegral()+"\n");
            }
            replayInfo.setReplayMessage(s.toString());
        }
        return replayInfo;
    }

}
