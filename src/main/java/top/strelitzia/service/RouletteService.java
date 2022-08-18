package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.SendMessageUtil;

import java.util.*;

@Service
public class RouletteService {

    @Autowired
    SendMessageUtil sendMessageUtil;

    //轮盘赌map
    private static final Map<Long, List<Integer>> rouletteInfo = new HashMap<>();

    //轮盘赌对决map
    private static final Map<Long,List<Long>> rouletteDuel = new HashMap<>();

    @AngelinaGroup(keyWords = {"上子弹","上膛"}, description = "守护铳轮盘赌，看看谁是天命之子(多颗子弹直接在后面输入数字）")
    public ReplayInfo Roulette(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        int bulletNum ;
        //判断数字
        if (messageInfo.getArgs().size()>1){
            boolean result = messageInfo.getArgs().get(1).matches("[0-9]+");
            if (!result){
                StringBuilder s = new StringBuilder();
                char[] arr=messageInfo.getArgs().get(1).toCharArray();
                for(char c :arr){
                    if (c>=48&&c<=57){
                        s.append(c - '0');
                    }
                }
                if (s.toString().equals("")){
                    replayInfo.setReplayMessage("对不起啊博士，没能理解您的意思，请务必告诉我数字呢");
                    return replayInfo;
                }
                bulletNum = Integer.parseInt(s.toString());
            }else {
                bulletNum = Integer.parseInt(messageInfo.getArgs().get(1));
            }
            if (bulletNum > 6){
                replayInfo.setReplayMessage("博士，您装入的子弹数量太多了");
                return replayInfo;
            }else if(bulletNum == 6) {
                replayInfo.setReplayMessage("博士...您是要自杀吗");
                return replayInfo;
            }
        }else {
            bulletNum = 1;
        }
        int bullet = 0;
        if (bulletNum == 1){
            //只加一个子弹
            for (int j=0;j<6;j++){
                bullet=bullet+new Random().nextInt(2);
            }
            replayInfo.setReplayMessage("（放入了 1 颗子弹）");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }else {
            //加N个子弹,随机选弹仓加入子弹，则触发位置是最小的弹仓号
            LinkedList<Integer> list = new LinkedList<>();
            List<Integer> situList = new ArrayList<>(Arrays.asList(0,1,2,3,4,5));
            for(int i=0;i<bulletNum;i++){
                int situ = new Random().nextInt(situList.size());
                bullet = situList.get(situ);
                situList.remove(situ);
                list.add(bullet);
            }
            bullet=Collections.min(list);
            replayInfo.setReplayMessage("（放入了 "+ bulletNum +" 颗子弹）");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }
        List<Integer> rouletteInitial = new ArrayList<>(Arrays.asList(bullet,0));
        rouletteInfo.put(messageInfo.getGroupId(),rouletteInitial);
        replayInfo.setReplayMessage("这是一把充满荣耀与死亡的守护铳，不幸者将再也发不出声音。勇士们啊，扣动你们的扳机！感谢Outcast提供的守护铳！");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"开枪"}, description = "进入生死的轮回")
    public ReplayInfo openGun(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<Integer> rouletteNum = rouletteInfo.get(messageInfo.getGroupId());
        //判断是否已经上膛
        if (rouletteNum == null){
            replayInfo.setReplayMessage("您还没上子弹呢");
            return replayInfo;
        }
        //取出子弹位置和开枪次数进行对比
        Integer bullet = rouletteNum.get(0);
        Integer trigger = rouletteNum.get(1);
        if(bullet.equals(trigger)){
            if(messageInfo.getBotPermission().getLevel() > messageInfo.getUserAdmin().getLevel()){
                replayInfo.setMuted((new Random().nextInt(5) + 1) * 60);//轮盘赌禁言时间
                replayInfo.setReplayMessage("对不起，"+replayInfo.getName()+"，我也不想这样的......");
            }else {
                replayInfo.setReplayMessage("我的手中的这把守护铳，找了无数工匠都难以修缮如新。不......不该如此......");
            }
            //清空这一次的轮盘赌
            rouletteInfo.remove(messageInfo.getGroupId());
        }else {
            switch ( trigger) {
                case 0 -> replayInfo.setReplayMessage("无需退路。( 1 / 6 )");
                case 1 -> replayInfo.setReplayMessage("英雄们，为这最强大的信念，请站在我们这边。( 2 / 6 )");
                case 2 -> replayInfo.setReplayMessage("颤抖吧，在真正的勇敢面前。( 3 / 6 )");
                case 3 -> replayInfo.setReplayMessage("哭嚎吧，为你们不堪一击的信念。( 4 / 6 ) ");
                case 4 -> replayInfo.setReplayMessage("现在可没有后悔的余地了。( 5 / 6 )");
            }
            rouletteNum.remove(1);
            rouletteNum.add(trigger+1);
            rouletteInfo.put(messageInfo.getGroupId(),rouletteNum);
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"轮盘对决"}, description = "六人参赛，一人丧命")
    public ReplayInfo RouletteDuel(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (rouletteDuel.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("当前群组的轮盘对决还没有结束，您可以继续报名加入哦");
            return replayInfo;
        }
        List<Long> QQList = new ArrayList<>();
        replayInfo.setReplayMessage("这是一把充满荣耀与死亡的守护铳，六个弹槽只有一颗子弹，六位参赛者也将会有一位不幸者将再也发不出声音。" +
                "\n有胆量的，就发送加入来加入决斗吧");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        int i=1;
        while (i<=6){
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    boolean reply;
                    try {
                        reply = message.getGroupId().equals(messageInfo.getGroupId()) &&
                                message.getText().equals("加入");
                    }catch (NullPointerException e){
                        reply = false;
                    }
                    return reply;
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(30);
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null) {
                replayInfo.setReplayMessage("抱歉，太长时间没有人报名，轮盘对决已经取消了，您可以再发起一场新的轮盘对决呢");
                return replayInfo;
            }
            if (QQList.contains(recall.getQq())){
                replayInfo.setReplayMessage("您已经参加了轮盘对决，不要重复参加哦");
                break;
            }else {
                replayInfo.setReplayMessage("欢迎第"+ i +"位挑战者" + recall.getName() + "\n愿主保佑你，我的勇士。");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                QQList.add(recall.getQq());
                i++;
            }
        }
        rouletteDuel.put(messageInfo.getGroupId(),QQList);
        replayInfo.setReplayMessage("报名完成，可以开始对决了");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"对决开始"}, description = "轮盘对决的生死抉择开始了")
    public ReplayInfo RouletteDuelBegging(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<Long> QQList = rouletteDuel.get(messageInfo.getGroupId());
        //查询次数决定能不能开始
        if(QQList.size() < 6){
            replayInfo.setReplayMessage("参赛人数还不足六人，还不能开始对决呢。");
        }else {
            //计算子弹位置设立随机数
            int bullet = 0;
            for (int i=0;i<6;i++){
                bullet=bullet+new Random().nextInt(2);
            }
            switch (bullet) {
                case 0 -> replayInfo.setQq(QQList.get(0));
                case 1 -> replayInfo.setQq(QQList.get(1));
                case 2 -> replayInfo.setQq(QQList.get(2));
                case 3 -> replayInfo.setQq(QQList.get(3));
                case 4 -> replayInfo.setQq(QQList.get(4));
                default -> replayInfo.setQq(QQList.get(5));
            }
            //把获取到的禁言QQ带入禁言功能并且实现禁言
            replayInfo.setMuted(5 * 60);
            if(new Random().nextInt(100)>98){
                int muted = 60;
                for(int i=0;i<6;i++){
                    replayInfo.setQq(QQList.get(i));
                    replayInfo.setMuted(muted);
                    sendMessageUtil.sendGroupMsg(replayInfo);
                }
                replayInfo.setReplayMessage("子弹炸膛了！博士！您还好吧？");
            }else {
                replayInfo.setAT(replayInfo.getQq());
                replayInfo.setReplayMessage("，永别了，安息吧......");
            }
            rouletteDuel.remove(messageInfo.getGroupId());
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"轮盘对决结束"}, description = "结束轮盘对决")
    public ReplayInfo closeRoulette(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        rouletteDuel.remove(messageInfo.getGroupId());
        replayInfo.setReplayMessage("轮盘对决已结束");
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"轮盘赌结束"}, description = "结束轮盘赌")
    public ReplayInfo closeRouletteDuel(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        rouletteInfo.remove(messageInfo.getGroupId());
        replayInfo.setReplayMessage("轮盘赌已结束");
        return replayInfo;
    }

}
