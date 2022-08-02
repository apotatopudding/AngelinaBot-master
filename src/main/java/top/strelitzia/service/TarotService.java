package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.dao.TarotMapper;
import top.strelitzia.model.TarotInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
public class TarotService {
    @Autowired
    private TarotMapper tarotMapper;

//    private String oriFileOfFileName;

    @AngelinaFriend(keyWords = {"请远山小姐为我占卜吧"}, description = "远山小姐的神奇占卜术，很灵的哦")
    public ReplayInfo FriendTakeTarot(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo =new TarotInfo();
        replayInfo.setReplayMessage("您今日已经占卜过了呢，您是想看看看今日的占卜结果吗");
        if (this.takeTarotPass(messageInfo.getQq(),messageInfo.getName())){
            tarotInfo.setQq(messageInfo.getQq());
            String folderPath = "runFile/TarotPicture";
            File folder = new File(folderPath);
            File[] files = folder.listFiles();
            if(folder.isDirectory() && files!=null) {
                //筛选出需要的三个不重复图片文件编号
                List<String> newList = createRandoms(files);
                tarotInfo.setTarotCard1Position(new SecureRandom().nextInt(2));
                tarotInfo.setTarotCard1(newList.get(0));
                tarotInfo.setTarotCard2Position(new SecureRandom().nextInt(2));
                tarotInfo.setTarotCard2(newList.get(1));
                tarotInfo.setTarotCard3Position(new SecureRandom().nextInt(2));
                tarotInfo.setTarotCard3(newList.get(2));
                this.tarotMapper.updateTarotCardByQQ(tarotInfo);
                replayInfo.setReplayMessage("您的三张牌已经抽取完毕，请您翻开第一张预言牌吧");
            }else {
                replayInfo.setReplayMessage("文件读取失败，请通知琴柳机器人的运行者");
            }
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第一张预言"}, description = "第一份预言即将揭示")
    public ReplayInfo TarotFirstCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard1ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard1();
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            replayInfo.setReplayImg(img);
        }catch (IOException e){
            e.printStackTrace();
        }
        int point = tarotInfo.getTarotCard1().lastIndexOf(".");
        String s = tarotInfo.getTarotCard1().substring(0,point);
        if (tarotInfo.getTarotCard1Position()==0){
            s = ("当前塔罗牌为"+s+"，牌位为正位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }else {
            s = ("当前塔罗牌为"+s+"，牌位为反位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }
        replayInfo.setReplayMessage(s);
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第二张预言"}, description = "第二份预言即将揭示")
    public ReplayInfo TarotSecondCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard2ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard2();
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            replayInfo.setReplayImg(img);
        }catch (IOException e){
            e.printStackTrace();
        }
        int point = tarotInfo.getTarotCard2().lastIndexOf(".");
        String s = tarotInfo.getTarotCard2().substring(0,point);
        if (tarotInfo.getTarotCard2Position()==0){
            s = ("当前塔罗牌为"+s+"，牌位为正位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }else {
            s = ("当前塔罗牌为"+s+"，牌位为反位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }
        replayInfo.setReplayMessage(s);
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第三张预言"}, description = "第三份预言即将揭示")
    public ReplayInfo TaroThirdCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard3ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard3();
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            replayInfo.setReplayImg(img);
        }catch (IOException e){
            e.printStackTrace();
        }
        int point = tarotInfo.getTarotCard3().lastIndexOf(".");
        String s = tarotInfo.getTarotCard3().substring(0,point);
        if (tarotInfo.getTarotCard3Position()==0){
            s = ("当前塔罗牌为"+s+"，牌位为正位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }else {
            s = ("当前塔罗牌为"+s+"，牌位为反位\n如果需要我为您解牌的话请说远山小姐解牌  XX（牌名）哦");
        }
        replayInfo.setReplayMessage(s);
        return replayInfo;
    }

    public boolean takeTarotPass(Long qq ,String name){
        //判断是否到达每天一次条件
        boolean pass = false;
        TarotInfo tarotInfo =this.tarotMapper.selectTarotByQQ(qq);
        if (tarotInfo == null) {
            tarotInfo = new TarotInfo();
            tarotInfo.setQq(qq);
            tarotInfo.setTarotCount(0);
        }
        Integer today = tarotInfo.getTarotCount();
        if (today < 1) {
            pass = true;
            today = today + 1;
            this.tarotMapper.updateTarotByQQ(qq,name,today);
        }
        return pass;
    }

    private List<String> createRandoms(File[] files) {
        List<String> list =new ArrayList<>();
        for(File file : files){ list.add(file.getName()); }
        List<String> news = new ArrayList<>();
        for(int i=0;i<3;i++){
            String s = list.get(new Random().nextInt(list.size()));
            news.add(s);
            list.remove(s);
        }
        return news;
    }
}


