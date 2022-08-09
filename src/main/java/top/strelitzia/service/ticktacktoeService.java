package top.strelitzia.service;

import net.mamoe.mirai.message.data.Dice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

@Service
public class TicktacktoeService {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private final Set<Long> groupList =new HashSet<>();

    private final Map<Integer,List<Integer>> map =new HashMap<>(){
        {
            put(1,new ArrayList<>(Arrays.asList(0,0)));
            put(2,new ArrayList<>(Arrays.asList(0,1)));
            put(3,new ArrayList<>(Arrays.asList(0,2)));
            put(4,new ArrayList<>(Arrays.asList(1,0)));
            put(5,new ArrayList<>(Arrays.asList(1,1)));
            put(6,new ArrayList<>(Arrays.asList(1,2)));
            put(7,new ArrayList<>(Arrays.asList(2,0)));
            put(8,new ArrayList<>(Arrays.asList(2,1)));
            put(9,new ArrayList<>(Arrays.asList(2,2)));
        }
    };

    @AngelinaGroup(keyWords = {"井字棋"},description = "井字棋游戏")
    public ReplayInfo ticktacktoeBegin(MessageInfo messageInfo){
        ReplayInfo replayInfo =new ReplayInfo(messageInfo);
        if (groupList.contains(messageInfo.getGroupId())) {
            replayInfo.setReplayMessage("本群井字棋还没有结束，结束了再试吧");
            return replayInfo;
        }
        replayInfo.setReplayMessage("创建比赛成功，正在等待第二位选手加入" +
                "\n请发送（加入）来加入井字棋比赛");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        groupList.add(messageInfo.getGroupId());
        boolean join = true;
        MessageInfo recallOfJoin = new MessageInfo();
        while (join) {
            //等待二号玩家加入
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    String s = message.getText();
                    if(message.getText() == null) s ="  ";
                    return message.getGroupId().equals(messageInfo.getGroupId()) &&
                            s.equals("加入");
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            recallOfJoin = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recallOfJoin == null) {
                replayInfo.setReplayMessage("操作超时，对局取消");
                return replayInfo;
            }
            if (recallOfJoin.getQq().equals(messageInfo.getQq())) {
                replayInfo.setReplayMessage("不能和自己对战哦");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
            else join = false;
        }
        replayInfo.setReplayMessage(recallOfJoin.getName() + "加入成功\n" + "请两位选手发送“扔骰子”来获取先后手吧");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        int first = 0, second = 0;
        Long offensive = messageInfo.getQq(), defensive = recallOfJoin.getQq();
        String offensiveName = messageInfo.getName(), defensiveName = recallOfJoin.getName();
        while(second == 0) {
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    String s = message.getText();
                    return message.getGroupId().equals(messageInfo.getGroupId()) &&
                            s.equals("扔骰子");
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            MessageInfo recallOfSelect = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recallOfSelect == null) {
                replayInfo.setReplayMessage("操作超时，对局取消");
                return replayInfo;
            }else {
                //发送扔骰子时确定先后顺序
                if (first == 0) {
                    first = new Random().nextInt(6) + 1;
                    replayInfo.setDice(new Dice(first));
                    offensive = recallOfSelect.getQq();
                    offensiveName = recallOfSelect.getName();
                    replayInfo.setReplayMessage( recallOfSelect.getName()+"的点数是"+first +
                            "\n现在，请第二位选手"+ defensiveName +"丢出您的骰子吧");
                }else {
                    if (offensive.equals(recallOfSelect.getQq())) replayInfo.setReplayMessage("您已经丢过骰子了呢");
                    else {
                        second = new Random().nextInt(6) + 1;
                        replayInfo.setDice(new Dice(second));
                        if (first > second) {
                            defensive = recallOfSelect.getQq();
                            defensiveName = recallOfSelect.getName();
                            replayInfo.setReplayMessage(recallOfSelect.getName() + "的点数是" + second +
                                    "\n" + offensiveName + "获得先手，" + defensiveName + "获得后手");
                        } else if (first < second) {
                            Long temporaryQQ = offensive;
                            String temporaryName = offensiveName;
                            offensive = recallOfSelect.getQq();
                            offensiveName = recallOfSelect.getName();
                            defensive = temporaryQQ;
                            defensiveName = temporaryName;
                            replayInfo.setReplayMessage(recallOfSelect.getName() + "的点数是" + second +
                                    "\n" + offensiveName + "获得先手，" + defensiveName + "获得后手");
                        } else {
                            first = 0;
                            second = 0;
                            replayInfo.setReplayMessage("哎呀，丢到相同的点数了，没办法了，只能再丢一次了");
                        }
                    }
                }
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setDice(null);
                replayInfo.setReplayMessage(null);
            }
        }
        //生成棋盘底图
        BufferedImage baseImg = generateBase(offensiveName,defensiveName);
        replayInfo.setReplayImg(baseImg);
        replayInfo.setRecallTime(60);
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.getReplayImg().clear();
        //开始进行下棋
        //输入下棋数据，生成坐标
        List<Long> QQList = new ArrayList<>();
        QQList.add(defensive);
        QQList.add(offensive);
        Long now = defensive;
        int[][] form = new int[3][3];
        boolean win = false;
        int number = 0;
        while (!win) {
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    return message.getGroupId().equals(messageInfo.getGroupId()) &&
                            QQList.contains(message.getQq()) &&
                            message.getText().matches("[0-9]+");
                    //message.getText().matches("^[0-9]+(.[0-9]+)?$");
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null) {
                replayInfo.setReplayMessage("操作超时，对局取消");
                return replayInfo;
            }
            if (now.equals(recall.getQq())) {
                replayInfo.setReplayMessage("别着急，还不是你的回合哦");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            //原数组输入方法，未启用
            //int point = recall.getText().lastIndexOf(".");
            //int a = Integer.parseInt(recall.getText().substring(0, point));
            //int b = Integer.parseInt(recall.getText().substring(point+1));
            int recallNum = Integer.parseInt(recall.getText());
            if (recallNum > 9) {
                replayInfo.setReplayMessage("？？？？？");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            List<Integer> list = map.get(recallNum);
            int a = list.get(0);
            int b = list.get(1);
            if (form[a][b] != 0) {
                replayInfo.setReplayMessage("位置重复了，请您检查后重试");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }

            String name;
            //写入数组
            if (recall.getQq().equals(offensive)) {
                form[a][b] = 1;
                name = defensiveName;
            } else {
                form[a][b] = 2;
                name = offensiveName;
            }
            win = judge(form);//获取判断结果
            number += 1;
            now = recall.getQq();
            //如果有结果，发送通知
            if (win) {
                if (recall.getQq().equals(offensive)) replayInfo.setReplayMessage("恭喜" + offensiveName + "获得胜利");
                else replayInfo.setReplayMessage("恭喜" + defensiveName + "获得胜利");
            } else if (number >= 9) {
                win = true;
                replayInfo.setReplayMessage("平局，还想再玩一次吗，想再玩一次的话，就回复”是“就可以了。如果不想继续，就回复”否“就可以退出了");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                AngelinaListener toAgain = new AngelinaListener() {
                    @Override
                    public boolean callback(MessageInfo message) {
                        return message.getGroupId().equals(messageInfo.getGroupId()) &&
                                QQList.contains(message.getQq()) &&
                                (message.getText().equals("是")||message.getText().equals("否"));
                    }
                };
                toAgain.setGroupId(messageInfo.getGroupId());
                MessageInfo recall2 = AngelinaEventSource.waiter(toAgain).getMessageInfo();
                if (recall2 == null) {
                    replayInfo.setReplayMessage("您太长时间没回答我，对局已经关闭了");
                    return replayInfo;
                }
                if(recall2.getText().equals("是")) {
                    now = defensive;
                    form = new int[3][3];
                    win = false;
                    number = 0;
                    replayInfo.setReplayMessage("重置棋局成功，请"+offensiveName+"先手");
                    replayInfo.setReplayImg(baseImg);
                    replayInfo.setRecallTime(60);
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.getReplayImg().clear();
                    replayInfo.setReplayMessage(null);
                    continue;
                }else{
                    replayInfo.setReplayMessage("琴柳收到！对局已关闭");
                }
            }
            //生成棋盘图
            BufferedImage img = generateWrite(name, baseImg, form, win);
            replayInfo.setReplayImg(img);
            replayInfo.setRecallTime(60);
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.getReplayImg().clear();
            replayInfo.setReplayMessage(null);
        }
        groupList.remove(messageInfo.getGroupId());
        return replayInfo;
    }

    private BufferedImage generateBase( String offensiveName, String defensiveName ){
        TextLine textLine =new TextLine(20);
        int i;
        textLine.addSpace(3);
        textLine.addString(offensiveName);
        textLine.nextLine();
        textLine.nextLine();
        textLine.addSpace(3);
        textLine.addString(defensiveName);
        textLine.nextLine();
        textLine.nextLine();
        textLine.nextLine();
        textLine.nextLine();
        textLine.addSpace(10);
        try {
            File file1 = new File("runFile/ticktacktoe/new.png");
            Image img1 = ImageIO.read(file1).getScaledInstance(56, 56, Image.SCALE_DEFAULT);
            textLine.addImage(img1,100,60,2,2);
            File file2 = new File("runFile/ticktacktoe/new2.png");
            Image img2 = ImageIO.read(file2).getScaledInstance(56, 56, Image.SCALE_DEFAULT);
            textLine.addImage(img2,100,160,2,2);
            //InputStream pic = new ClassPathResource("/runFile/newPic.jpg").getInputStream();
            File file = new File("runFile/ticktacktoe/basePic.jpg");
            Image img = ImageIO.read(file).getScaledInstance(750, 750, Image.SCALE_DEFAULT);
            textLine.addImage(img,100,350,10,10);
        }catch (IOException e){
            e.printStackTrace();
        }
        for(i=0;i<10;i++){
            textLine.nextLine();
        }
        return textLine.drawImage(50,true);
    }

    private BufferedImage generateWrite( String name,BufferedImage baseImg,int [][] form,boolean win){
        int width = baseImg.getWidth();
        int height = baseImg.getHeight();
        String str = "选手" + name + "正在思考";
        BufferedImage image = new BufferedImage(width, height, 1);
        try {
            File file = new File("runFile/ticktacktoe/red.png");
            Image circle = ImageIO.read(file).getScaledInstance(750, 750, Image.SCALE_DEFAULT);
            file = new File("runFile/ticktacktoe/green.png");
            Image fork = ImageIO.read(file).getScaledInstance(750, 750, Image.SCALE_DEFAULT);

            Graphics graphics = image.getGraphics();
            graphics.drawImage(baseImg, 0, 0, null);

            if(!win){
                graphics.setFont(new Font("新宋体", Font.BOLD, 40));
                graphics.setColor(new Color(249, 243, 227));
                graphics.drawString(str, 150, 320);
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (form[i][j] == 1) graphics.drawImage(circle, 135+j*160, 380+i*165, 112,112,null);
                    else if(form[i][j] == 2) graphics.drawImage(fork, 135+j*160, 380+i*165, 112,112,null);

                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return image;
    }

    private boolean judge(int [][] form){
        int i,j;
        List<Integer> list = new ArrayList<>();
        //横向比照
        for( i=0;i<3;i++){
            for (j=0;j<3;j++){
                list.add(form[i][j]);
            }
            if(list.get(0).equals(list.get(1)) && list.get(0).equals(list.get(2)) && list.get(0)!=0) return true;
            list.clear();
        }

        //纵向比照
        for( j=0;j<3;j++){
            for (i=0;i<3;i++){
                list.add(form[i][j]);
            }
            if(list.get(0).equals(list.get(1)) && list.get(0).equals(list.get(2)) && list.get(0)!=0) return true;
            list.clear();
        }

        //右斜比照
        for (i=0;i<3;i++){
            list.add(form[i][i]);
        }
        if(list.get(0).equals(list.get(1)) && list.get(0).equals(list.get(2)) && list.get(0)!=0) return true;
        list.clear();

        //左斜比照
        for (i=0;i<3;i++){
            list.add(form[i][2-i]);
        }
        if(list.get(0).equals(list.get(1)) && list.get(0).equals(list.get(2)) && list.get(0)!=0) return true;
        list.clear();

        return false;
    }

}
