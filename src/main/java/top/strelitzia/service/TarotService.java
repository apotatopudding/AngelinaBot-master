package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.dao.TarotMapper;
import top.strelitzia.model.TarotInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TarotService {
    @Autowired
    private TarotMapper tarotMapper;

//    private String oriFileOfFileName;

    @AngelinaFriend(keyWords = {"请远山小姐为我占卜吧"}, description = "远山小姐的神奇占卜术，很灵的哦")
    public ReplayInfo FriendTakeTarot(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("您今日已经占卜过了呢，您是想看看看今日的占卜结果吗");
        if (this.takeTarotPass(messageInfo.getQq() ,messageInfo.getName())){
            String folderPath = "runFile/TarotPicture";
            //D:/新建文件夹/angelina/runFile/wallPaper
            // F:/new folder/AngelinaBot-master/AngelinaBot-master/runFile/wallPaper
            File folder = new File(folderPath);
            File[] files = folder.listFiles();
            if(folder.isDirectory()) {
                int picNum = files.length;
                //创建图片编号数组用于筛选
                List<Integer> picList = new ArrayList();
                int i,j,selectPicIndex = 0;
                for (i=1;i<=picNum;i++){
                    picList.add(i);
                }
                //筛选出需要的三个不重复图片文件编号
                List<Integer> newList = createRandoms(picList,3);
                String tarotCard1 = null,tarotCard2 = null,tarotCard3 = null;
                //创建循环以取出值对应到文件编号
                for(j=0;j<newList.size();j++){
                    selectPicIndex = newList.get(j);
                    File selectFile = files[selectPicIndex];
                    //oriFileName = selectFile.getAbsolutePath();
                    log.info("file:"+selectFile.getName());
                    switch (j){
                        case 0:
                            tarotCard1 = selectFile.getName();
                            break;
                        case 1:
                            tarotCard2 = selectFile.getName();
                            break;
                        default:
                            tarotCard3 = selectFile.getName();
                            break;
                    }
                }
                this.tarotMapper.updateTarotCardByQQ(messageInfo.getQq(),tarotCard1,tarotCard2,tarotCard3);
            }
            replayInfo.setReplayMessage("您的三张牌已经抽取完毕，请您翻开第一张预言牌吧");
        }

        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第一张预言"}, description = "第一份预言即将揭示")
    public ReplayInfo TarotFirstCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard1ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard1();
        replayInfo.setReplayImg(new File(filePath));
        replayInfo.setReplayMessage(this.takeTarotName(filePath));
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第二张预言"}, description = "第二份预言即将揭示")
    public ReplayInfo TarotSecondCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard2ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard2();
        replayInfo.setReplayImg(new File(filePath));
        replayInfo.setReplayMessage(this.takeTarotName(filePath));
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"翻开第三张预言"}, description = "第三份预言即将揭示")
    public ReplayInfo TaroThirdCard(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TarotInfo tarotInfo = this.tarotMapper.selectCard3ByQQ(messageInfo.getQq());
        String folderPath = "runFile/TarotPicture/";
        String filePath = folderPath + tarotInfo.getTarotCard3();
        replayInfo.setReplayImg(new File(filePath));
        replayInfo.setReplayMessage(this.takeTarotName(filePath));
        return replayInfo;
    }

    public String takeTarotName(String filePath) {
        double r = Math.random();
        int point = filePath.lastIndexOf(".");
        int point1 = filePath.lastIndexOf("T");
        String s = filePath.substring(point1+1,point);
        if (r <= 0.5D) {
            s = ("当前塔罗牌为"+s+"，牌位为正位\n如果需要我为您解牌的话请说远山小姐解牌 XX（牌名）哦");
        }else {
            s = ("当前塔罗牌为"+s+"，牌位为反位\n如果需要我为您解牌的话请说远山小姐解牌 XX（牌名）哦");
        }
        return s;
    }




    public boolean takeTarotPass(Long qq ,String name){
        //判断是否到达每天三次条件
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


    private List<Integer> createRandoms(List<Integer> list, int n) {
        Map<Integer,String> map = new HashMap();
        List<Integer> news = new ArrayList();
        if (list.size() <= n) {
            return list;
        } else {
            while (map.size() < n) {
                int random = (int)(Math.random() * list.size());
                if (!map.containsKey(random)) {
                    map.put(random, "");
                    news.add(list.get(random));
                }
            }
            return news;
        }
    }


}


