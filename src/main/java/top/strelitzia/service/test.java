package top.strelitzia.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.util.AudioUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

//@Service
@Slf4j
public class test {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @AngelinaGroup(keyWords = {"戳他"}, description = "测试")
    public ReplayInfo chuoMe(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Long a = null;
        if (messageInfo.getArgs().size() > 1) {
            a = Long.valueOf(messageInfo.getArgs().get(1));
        }
        replayInfo.setNudged(a);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"禁言自己"}, description = "测试")
    public ReplayInfo nudeSelf(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("臣妾做不到啊");
        return replayInfo;
    }

    //@AngelinaGroup(keyWords = {"呼叫"}, description = "测试")
    public ReplayInfo hujiao(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("杰哥今天寄了，琴柳在执勤，可以呼叫琴柳和稀音哦");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"转换"}, description = "测试")
    public ReplayInfo changeFormat(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        File sourceFile = new File("runFile/Audio/source.mp3");//输入
        File targetFile = new File("runFile/Audio/source.amr");//输出
        AudioUtil.mp3ToAmr(sourceFile, targetFile);//转换
        replayInfo.setReplayAudio(targetFile);
        //replayInfo.setReplayMessage("转换完成");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"全体禁言"}, description = "测试")
    public ReplayInfo muteAll(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setMutedAll(true);
        replayInfo.setReplayMessage("禁言开启");
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"权限查询"}, description = "测试")
    public ReplayInfo botPermission(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setPermission(true);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"时间转换"}, description = "测试")
    public ReplayInfo timeChangeFormit(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = new SimpleDateFormat("ss").format(currentTimeMillis);
        replayInfo.setReplayMessage(currentTime);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"底图"}, description = "测试")
    public ReplayInfo generateBase(MessageInfo messageInfo) throws IOException {
        ReplayInfo replayInfo =new ReplayInfo(messageInfo);
        TextLine textLine =new TextLine(20);
        int i;
        for(i=0;i<4;i++){
            textLine.nextLine();
        }
        textLine.addCenterStringLine("当前为：");
        textLine.addSpace(10);
        File file1 = new File("runFile/ticktacktoe/new.png");
        Image img1 = ImageIO.read(file1).getScaledInstance(56, 56, Image.SCALE_DEFAULT);
        textLine.addImage(img1,100,70,2,2);
        File file2 = new File("runFile/ticktacktoe/new2.png");
        Image img2 = ImageIO.read(file2).getScaledInstance(56, 56, Image.SCALE_DEFAULT);
        textLine.addImage(img2,100,170,2,2);
        try {
            //InputStream pic = new ClassPathResource("/runFile/newPic.jpg").getInputStream();
            File file = new File("runFile/ticktacktoe/newPic.jpg");
            Image img = ImageIO.read(file).getScaledInstance(750, 750, Image.SCALE_DEFAULT);
            textLine.addImage(img,100,350,10,10);
        }catch (IOException e){
            e.printStackTrace();
        }
        for(i=0;i<10;i++){
            textLine.nextLine();
        }
        BufferedImage img = textLine.drawImage(50,true);
        replayInfo.setReplayImg(img);
        return replayInfo;
    }



}


