package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.MemberPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.AdminUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.TalkTallInfo;

import java.util.*;

@Service
@Slf4j
public class TalkTallService {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private IntegralMapper integralMapper;

    private final Set<Long> groupList = new HashSet<>();

    @AngelinaGroup(keyWords = {"吹牛"},description = "吹牛游戏", sort = "娱乐功能",funcClass = "吹牛")
    public ReplayInfo talkTall(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (groupList.contains(messageInfo.getGroupId())) {
            replayInfo.setReplayMessage("游戏还未结束，暂不可开启新局");
            return replayInfo;
        }
        //检查积分是否足够
        int result = this.inquireIntegral(messageInfo.getQq());
        if (result != 0){
            if (result == 1) replayInfo.setReplayMessage("您还没有积分呢，试着参与活动以获取积分吧");
            else replayInfo.setReplayMessage("您的积分不足十分，多多参与活动获取更多积分吧");
            return replayInfo;
        }
        //添加群组防止重复开启
        groupList.add(messageInfo.getGroupId());
        //检查群临时会话是否开启
        MemberPermission permission = messageInfo.getBotPermission();
        if(permission.getLevel()<0){
            replayInfo.setReplayMessage("测试消息");
            try {
                sendMessageUtil.sendFriendMsg(replayInfo);
            }catch (IllegalStateException e){
                log.error(e.toString());
                groupList.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("群临时会话已关闭，无法开始游戏，请开启后再试");
                return replayInfo;
            }
            replayInfo.setReplayMessage("临时会话关系已确认，游戏即将开始，发起人无需再次报名");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }else {
            replayInfo.setReplayMessage("游戏即将开始，发起人无需再次报名");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }
        replayInfo.setReplayMessage("请输入【加入】加入比赛,了解玩法可以使用（吹牛描述）指令");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        Map<Long,String> participantQQMap = new HashMap<>();//参与QQ的列表
        participantQQMap.put(messageInfo.getQq(),messageInfo.getName());
        int i=1;
        //监听“报名”作为报名条件
        while (i<=5){
            AngelinaListener angelinaListener = new AngelinaListener() {
                //只有返回为加入时才生效
                @Override
                public boolean callback(MessageInfo message) {
                    boolean reply;
                    try {
                        reply = message.getGroupId().equals(messageInfo.getGroupId()) && message.getText().equals("加入");
                    }catch (NullPointerException e){
                        reply = false;
                    }
                    return reply;
                }
            };
            //监听等待
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(30);
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null){
                //检查是否足够两个人，如不足则关闭
                if(participantQQMap.size()<2){
                    replayInfo.setReplayMessage("人数尚不足二人，无法开启游戏，游戏关闭");
                    groupList.remove(messageInfo.getGroupId());
                    return replayInfo;
                }else {
                    replayInfo.setReplayMessage("等待时间超时，已加入"+participantQQMap.size()+"人，暂无人继续加入，游戏开始");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    break;
                }
            }
            result = this.inquireIntegral(recall.getQq());
            if (result != 0){
                if (result == 1) replayInfo.setReplayMessage("您还没有积分呢，试着参与活动以获取积分吧");
                else replayInfo.setReplayMessage("您的积分不足十分，多多参与活动获取更多积分吧");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            //列出已报名人员，如果重复报名直接跳过录入
            if(participantQQMap.containsKey(recall.getQq())){
                replayInfo.setReplayMessage("请勿重复报名");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }else {
                participantQQMap.put(recall.getQq(), recall.getName());
                StringBuilder s = new StringBuilder();
                s.append(recall.getName()).append("加入成功");
                if (i == 5) {
                    s.append("\n");
                    s.append("五人已满，所有人已经就绪，游戏开始");
                }
                replayInfo.setReplayMessage(s.toString());
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                i++;
            }
        }
        TalkTallInfo talkTallInfo = new TalkTallInfo();
        //一轮换一个人
        for(Long QQ : participantQQMap.keySet()){
            List<Integer> list = new ArrayList<>();//只用于私聊通知
            integralMapper.reduceIntegralByGroupId(messageInfo.getGroupId(),QQ,10);
            boolean repetition = true;//顺子检测开关
            while (repetition) {
                //一轮加一个骰子量
                for (i = 0; i < 6; i++) {
                    int random = new Random().nextInt(6) + 1;
                    list.add(random);
                }
                //对结果去重，如果去重没有减少代表是顺子，继续循环扔，如果不是就关闭退出循环
                Set<Integer> set = new HashSet<>(list);
                if (set.size() == 6){
                    list = new ArrayList<>();
                }else {
                    repetition = false;
                }
            }
            for(int point : list) {
                //每多一个，总数量加一
                switch (point) {
                    case 1 -> talkTallInfo.setNumOfOne(talkTallInfo.getNumOfOne() + 1);
                    case 2 -> talkTallInfo.setNumOfTwo(talkTallInfo.getNumOfTwo() + 1);
                    case 3 -> talkTallInfo.setNumOfThree(talkTallInfo.getNumOfThree() + 1);
                    case 4 -> talkTallInfo.setNumOfFour(talkTallInfo.getNumOfFour() + 1);
                    case 5 -> talkTallInfo.setNumOfFive(talkTallInfo.getNumOfFive() + 1);
                    case 6 -> talkTallInfo.setNumOfSix(talkTallInfo.getNumOfSix() + 1);
                }
            }
            replayInfo.setQq(QQ);
            replayInfo.setReplayMessage("你摇了摇骰盅，看了一眼，你丢出了【"+list.get(0)+"】【"+list.get(1)+"】【"+list.get(2)+"】【"+list.get(3)+"】【"+list.get(4)+"】【"+list.get(5)+"】");
            try {
                sendMessageUtil.sendFriendMsg(replayInfo);
            }catch (Exception e){
                e.printStackTrace();
                groupList.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("由于临时会话被关闭，游戏已错误退出");
                return replayInfo;
            }
            replayInfo.setReplayMessage(null);
        }
        log.info("一点："+talkTallInfo.getNumOfOne()+
                "\n两点："+talkTallInfo.getNumOfTwo()+
                "\n三点："+talkTallInfo.getNumOfThree()+
                "\n四点："+talkTallInfo.getNumOfFour()+
                "\n五点："+talkTallInfo.getNumOfFive()+
                "\n六点："+talkTallInfo.getNumOfSix());//后台显示结果，测试用
        replayInfo.setReplayMessage("所有人骰子投掷完毕，开始叫数（例如：1个3）");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        boolean close = false,right,calledOne = false;
        int a=0,b=1;
        Long QQByOpened= 0L,QQByOpen= 0L;
        String nameByOpened = "",nameByOpen= "";
        while (!close) {
            for (Iterator<Map.Entry<Long, String>> iterator = participantQQMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Long, String> entry = iterator.next();
                replayInfo.setReplayMessage("轮到"+entry.getValue()+"叫数了");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                right = false;
                while (!right) {
                    AngelinaListener angelinaListener = new AngelinaListener() {
                        @Override
                        public boolean callback(MessageInfo message) {
                            boolean reply;
                            try {
                                boolean isNum = false;
                                String s = message.getText();
                                if(!s.equals("开你")&&s.contains("个")) {
                                    int point = s.lastIndexOf("个");
                                    String s1 = s.substring(0, point);
                                    String s2 = s.substring(point + 1);
                                    if(s1.matches("[0-9]+") && s2.matches("[0-9]+")) isNum = true;
                                }
                                reply = message.getGroupId().equals(messageInfo.getGroupId()) &&
                                        entry.getKey().equals(message.getQq()) &&
                                        (isNum || s.equals("开你"));
                            }catch (NullPointerException e){
                                reply = false;
                            }
                            return reply;
                        }
                    };
                    angelinaListener.setGroupId(messageInfo.getGroupId());
                    angelinaListener.setSecond(60);
                    MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
                    if (recall == null) {
                        iterator.remove();
                        replayInfo.setReplayMessage("由于长时间无人作答，该玩家已被移除");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        if (participantQQMap.size() < 2) {
                            replayInfo.setReplayMessage("由于人数已不足，游戏被关闭");
                            groupList.remove(messageInfo.getGroupId());
                            for(Long qq : participantQQMap.keySet()) {
                                this.integralMapper.increaseIntegralByGroupId(messageInfo.getGroupId(), qq, 10);
                            }
                            return replayInfo;
                        }
                        break;
                    }
                    String s = recall.getText();
                    //判定必须要个数和点数任意一个大于前面叫的才能通过，通过时用新的数据覆盖旧的数据
                    int point = s.lastIndexOf("个");
                    if (point!=-1){
                        int A = Integer.parseInt(s.substring(0, point));
                        int B = Integer.parseInt(s.substring(point + 1));
                        if (A > a || A == a && B > b) {
                            if (B>6){
                                replayInfo.setReplayMessage("骰子中没有比6还大的呢");
                                sendMessageUtil.sendGroupMsg(replayInfo);
                                replayInfo.setReplayMessage(null);
                                continue;
                            }
                            a=A;
                            b=B;
                            QQByOpened = entry.getKey();
                            nameByOpened = entry.getValue();
                            right = true;
                            if (B==1) calledOne = true;
                        } else if(A < a){
                            replayInfo.setReplayMessage("骰子的个数不能低于上家叫的个数呢");
                            sendMessageUtil.sendGroupMsg(replayInfo);
                            replayInfo.setReplayMessage(null);
                        } else {
                            replayInfo.setReplayMessage("骰子的个数或点数要比上家叫的更大才行呢");
                            sendMessageUtil.sendGroupMsg(replayInfo);
                            replayInfo.setReplayMessage(null);
                        }
                        if ((A==participantQQMap.size()*5 && B==6)){
                            close = true;
                        }
                    }else{
                        QQByOpen = recall.getQq();
                        nameByOpen = recall.getName();
                        close = true;
                        right = true;
                    }
                }
                if (close) break;
                replayInfo.setReplayMessage("数据接收正确");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
        int num = 0;
        switch (b) {
            case 1 -> num = talkTallInfo.getNumOfOne();
            case 2 -> num = talkTallInfo.getNumOfTwo();
            case 3 -> num = talkTallInfo.getNumOfThree();
            case 4 -> num = talkTallInfo.getNumOfFour();
            case 5 -> num = talkTallInfo.getNumOfFive();
            case 6 -> num = talkTallInfo.getNumOfSix();
        }
        String s = "这局有人叫过1，";
        if(!calledOne && b!=1){//没叫过1，1代替任何数
            num = num + talkTallInfo.getNumOfOne();
            s = "这局还没有人叫过1，点数为1的骰子一共有"+talkTallInfo.getNumOfOne()+"个，加上1点的数量，";
        }
        if(a>num){
            replayInfo.setReplayMessage(s+"点数为"+b+"的骰子一共有"+num+"个，"+nameByOpened+"，你在吹牛呢，你输了");
            participantQQMap.remove(QQByOpened);
        }else{
            replayInfo.setReplayMessage(s+"点数为"+b+"的骰子一共有"+num+"个，别人没有吹牛哦，"+nameByOpen+"，你输了");
            participantQQMap.remove(QQByOpen);
        }
        for (Long QQ : participantQQMap.keySet()){
            int integral = 12;
            if(QQ.equals(QQByOpen)||QQ.equals(QQByOpened)) integral = 16;
            this.integralMapper.increaseIntegralByGroupId(messageInfo.getGroupId(),QQ,integral);

        }
        groupList.remove(messageInfo.getGroupId());
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"关闭吹牛"},description = "吹牛游戏", sort = "娱乐功能",funcClass = "吹牛")
    public ReplayInfo closeTalkTall(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean admin = AdminUtil.getAdmin(messageInfo.getQq());
        if (messageInfo.getUserAdmin().getLevel()<1 && !admin){
            replayInfo.setReplayMessage("您的权限不足");
        }else {
            AngelinaEventSource.remove(messageInfo.getGroupId());
            groupList.remove(messageInfo.getGroupId());
            replayInfo.setReplayMessage("移除成功");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"吹牛描述"},description = "吹牛的游戏规则描述", sort = "娱乐功能",funcClass = "吹牛")
    public ReplayInfo talkTallDescription(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String s = "吹牛规则如下：\n" +
                "每人一共六个骰子各摇一次（机器人自动完成），看清自己盅内的点数，猜测对方的点数，然后从第一位开始吆喝所有参与者骰盅内共有多少个某点数的骰子，" +
                "叫法为M个N(如2个3点，2个6点，3个4点等)\n" +
                "对方分析判断此叫法真实与否，信之为真则下家接着叫，叫法同样为M个N。" +
                "但M不可小于上家，当M不变时，N只能增加不可减少，M增加时N可叫任意点数" +
                "（比如上家叫的是3个3，下家可以叫3个5或者4个1。叫3个1或1个5都属违规）\n"+
                "若下家不信则开盅验证，合计所有人的骰盅内的有该点数的骰子个数之和，若确至少有 M个N点，则上家赢，" +
                "反之则下家赢(如上家叫5个6，开盅时若只有4个6点，则上家输，若有5个或更多个6点，则下家输)。\n" +
                "另外，1点可变作任意点数，但一旦被叫过便只能作回自己；\n" +
                "如果骰子出现顺子（即出现123456点数情况）机器人会自动重摇。\n";
        replayInfo.setReplayMessage(s);
        return replayInfo;
    }

    /**
     * 查询当前积分是否异常
     *
     * @param qq  被查询积分者
     * @return a  0 正常，1 无积分，2 积分不够
     */
    private int inquireIntegral(Long qq){
        int a = 0;
        int realIntegral = 0;
        try{
            realIntegral= this.integralMapper.selectByQQ(qq);
        }catch (NullPointerException e){
            log.error(e.toString());
            a = 1;
        }
        //如果积分不足也跳出循环
        if (realIntegral < 10){
            a = 2;
        }
        return a;
    }

}
