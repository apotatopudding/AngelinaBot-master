package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.HorseRaceInfo;

import java.util.*;

@Slf4j
@Service
public class HorseRaceService {

    @Autowired
    SendMessageUtil sendMessageUtil;

    @Autowired
    IntegralMapper integralMapper;

    private static final Set<Long> groupList = new HashSet<>();//群组信息表

    //群组赛马选手信息
    private static final Map<Integer,String> contestantMap = new HashMap<>(){
        {
            put(1,"瑕光");
            put(2,"临光");
            put(3,"鞭刃");
            put(4,"格拉尼");
            put(5,"流星");
            put(6,"芬");
            put(7,"玛恩纳");
            put(8,"野鬃");
        }
    };

    @AngelinaGroup(keyWords = {"赛马娘"},description = "观看紧张刺激的赛马娘比赛", sort = "娱乐功能")
    public ReplayInfo signUp(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (groupList.contains(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("赛马比赛还未结束呢，耐心观看吧");
            return replayInfo;
        }
        replayInfo.setReplayMessage("各位，精彩的赛马大赛就要开始了，有请各位选手登场！");
        //绘制当场比赛状态概率表
        Random random = new Random(System.nanoTime() + System.currentTimeMillis() / messageInfo.getQq());//随机数种子采用纳秒数+毫秒/随机数
        final Map<Integer,Float> contestantStatus = new HashMap<>(){
            {
                put(1,random.nextFloat());
                put(2,random.nextFloat());
                put(3,random.nextFloat());
                put(4,random.nextFloat());
                put(5,random.nextFloat());
                put(6,random.nextFloat());
                put(7,random.nextFloat());
                put(8,random.nextFloat());
            }
        };
        //绘制选手名单图片
        TextLine textLine = new TextLine(100);
        textLine.addString("比赛选手名单：");
        for(int i=1;i<=contestantMap.size();i++){
            String s;
            if(contestantStatus.get(i)<0.2) s = "状态不佳";
            else if(contestantStatus.get(i)<0.4) s = "状态还行";
            else if(contestantStatus.get(i)<0.6) s = "状态一般";
            else if(contestantStatus.get(i)<0.8) s = "状态不错";
            else s = "状态爆表";
            textLine.nextLine();
            textLine.addString(i+ "、" + contestantMap.get(i)+"（本场状态："+s+"）");
        }
        replayInfo.setReplayImg(textLine.drawImage());
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.getReplayImg().clear();
        replayInfo.setReplayMessage("各位可以选择您喜欢的选手，发送下注 XX（选手名字或编号） XX（积分）为您喜欢的选手加油助威哦" +
                "\n动态赔率规则：根据选手状态，赔率栏分为，状态爆表0.5倍（向上收整），状态不错1倍，状态一般2倍，状态还行4倍，状态不佳8倍");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        //报名
        int i = 1;
        List<Long> QQList =new ArrayList<>();//用于鉴定QQ是否重复
        List<HorseRaceInfo> participantList =new ArrayList<>();//列表以容纳加入的人的信息
        int allIntegral = 0;
        while (i<=10){
            AngelinaListener angelinaListener =new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    boolean replay;
                    try {
                        replay = message.getGroupId().equals(messageInfo.getGroupId())&&
                                message.getArgs().get(0).equals("下注");
                    }catch (NullPointerException | IndexOutOfBoundsException e){
                        replay = false;
                    }
                    return replay;
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(30);
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null){
                //检查是否足够三个人，如不足则关闭
                if(participantList.size()<3){
                    replayInfo.setReplayMessage("人数尚不足三人，本次赛马大赛关闭");
                    return replayInfo;
                }else {
                    replayInfo.setReplayMessage("等待时间超时，暂无人继续下注，比赛开始");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    break;
                }
            }
            //判断是否已经下注，已经下注则跳出
            if(QQList.contains(recall.getQq())){
                replayInfo.setReplayMessage("抱歉您已经下过注了，请耐心等待比赛完成吧");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            HorseRaceInfo horseRaceInfo = new HorseRaceInfo();
            //判断选手填入是否为空，为空跳过循环
            if(recall.getArgs().size()>1){
                //数字转换
                int Num = 0;
                boolean result = recall.getArgs().get(1).matches("[0-9]+");
                //判断是否为数字
                if (!result){
                    //非数字，判断表是否包含有该名字，有则转换为编号
                    if(contestantMap.containsValue(recall.getArgs().get(1))) {
                        for (Map.Entry<Integer, String> contestant : contestantMap.entrySet()) {
                            if (contestant.getValue().equals(recall.getArgs().get(1))) {
                                Num = contestant.getKey();
                            }
                        }
                    }else if(recall.getArgs().get(1).matches(".*[0-9].*")){
                        //没有则查找有没有包含数字，有则提取
                        StringBuilder s = new StringBuilder();
                        char[] arr=recall.getArgs().get(1).toCharArray();
                        for(char c :arr){
                            if (c>=48&&c<=57){
                                s.append(c - '0');
                            }
                        }
                        Num = Integer.parseInt(s.toString());
                    }else {
                        replayInfo.setReplayMessage("抱歉博士，没有理解您要下注的对象呢，检查一下吧");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        continue;
                    }
                }else {
                    //是数字，直接转换为编号，查找表是否有该选手
                    Num = Integer.parseInt(recall.getArgs().get(1));
                }
                if(!contestantMap.containsKey(Num)){
                    replayInfo.setReplayMessage("博士，您的选手选择输入有误，请重新输入试试吧");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    continue;
                }
                horseRaceInfo.setContestant(Num);
            }else{
                replayInfo.setReplayMessage("您还没有输入您选择的选手呢");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            //判断积分填入是否为空，为空跳过循环
            if(recall.getArgs().size()>2){
                //数字转换
                int integral ;
                boolean result = recall.getArgs().get(2).matches("[0-9]+");
                if (!result){
                    StringBuilder s = new StringBuilder();
                    char[] arr=recall.getArgs().get(2).toCharArray();
                    for(char c :arr){
                        if (c>=48&&c<=57){
                            s.append(c - '0');
                        }
                    }
                    if (s.toString().equals("")){
                        replayInfo.setReplayMessage("抱歉博士，积分需要是数字呢");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        continue;
                    }
                    integral = Integer.parseInt(s.toString());
                }else {
                    integral = Integer.parseInt(recall.getArgs().get(2));
                }
                if(integral<=0){
                    replayInfo.setReplayMessage("抱歉博士，下注的积分不能小于零呢");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    continue;
                }
                //获取现有积分，如果没积分则跳出循环
                int realIntegral;
                try{
                    realIntegral= this.integralMapper.selectByQQ(recall.getQq());
                }catch (NullPointerException e){
                    log.info(e.toString());
                    replayInfo.setReplayMessage("您还没有积分呢，试着参与活动以获取积分吧");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    continue;
                }
                //如果积分不足也跳出循环
                if (realIntegral < integral){
                    replayInfo.setReplayMessage("您的积分不足");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    continue;
                }
                horseRaceInfo.setIntegral(integral);//记录投注的积分
            }else{
                replayInfo.setReplayMessage("您还没有输入您要下注的积分呢");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                continue;
            }
            replayInfo.setReplayMessage(recall.getName()+"下注成功，对"+horseRaceInfo.getContestant()+"号选手下注"+horseRaceInfo.getIntegral()+"点积分,还剩"+(10-i)+"个名额");
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            QQList.add(recall.getQq());//QQ记录表写入
            horseRaceInfo.setQQ(recall.getQq());//写入下注人QQ
            participantList.add(horseRaceInfo);
            allIntegral = allIntegral + horseRaceInfo.getIntegral();
            i++;
        }
        if(i==11) replayInfo.setReplayMessage("参加人数已满十人\n比赛开始");
        else replayInfo.setReplayMessage("比赛开始");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        //开始计算并获取随机计算的结果，然后计算积分
        int selectNum = horseRace(messageInfo,contestantStatus);
        for(HorseRaceInfo horseRaceInfo : participantList){
            int integral = this.integralMapper.selectByQQ(horseRaceInfo.getQQ());
            int settlement = integral - horseRaceInfo.getIntegral();
            if(horseRaceInfo.getContestant().equals(selectNum)) {
                if (contestantStatus.get(selectNum) < 0.2) settlement = integral + horseRaceInfo.getIntegral() * 8;
                else if (contestantStatus.get(selectNum) < 0.4) settlement = integral + horseRaceInfo.getIntegral() * 4;
                else if (contestantStatus.get(selectNum) < 0.6) settlement = integral + horseRaceInfo.getIntegral() * 2;
                else if (contestantStatus.get(selectNum) < 0.8) settlement = integral + horseRaceInfo.getIntegral();
                else settlement = (int) Math.ceil(integral + horseRaceInfo.getIntegral() * 0.5);
            }
            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),"",horseRaceInfo.getQQ(),settlement);
        }
        return replayInfo;
    }

    /**
     * 赛马计算信息，由于需要只有一个选手夺冠，所以当出现并列到8时需要控制选出一人冲线
     * 同时，由于最大为7时冲线至少需要2次前进，跑到6的选手可能会跑出两个2使得并列冲线
     * 所以如果有选手到7时，必须控制下一刻最大只能到8，然后在最大为8时跳出循环进行选择
     * @param messageInfo 传递消息来源以发送
    **/
    private int horseRace(MessageInfo messageInfo,Map<Integer,Float> contestantStatus){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Map<Integer,List<Integer>> stepMap = new HashMap<>();//选手编号与单次步数表
        Map<Integer,Integer> distanceMap = new HashMap<>();//选手编号与总距离表
        int max = 0;
        boolean correction = false;
        while(max < 8){//当有选手跑到8时，脱离循环
            //以总选手列表为条件，循环给所有选手随机增加步数
            for(int i=1;i<=contestantMap.size();i++){
                //首次进行时新建
                if(!stepMap.containsKey(i)){
                    List<Integer> tempList = new ArrayList<>();
                    stepMap.put(i,tempList);
                    distanceMap.put(i,0);
                }
                //获取步数，将单步与前进总距离写入集合
                int distance = distanceMap.get(i);
                List<Integer> stepList = stepMap.get(i);
                int step = randomStep(contestantStatus.get(i));//用状态表去获取随机步数
                //当修正开关开启时对所有距离到7修正，使最大只能到8而脱离循环
                if (correction && distance == 7) step = 1;
                stepList.add(step);
                stepMap.put(i,stepList);//写入当前步数
                distance = distance + step;
                distanceMap.put(i,distance);
                if(max < distance){
                    max = distance;
                }
            }
            if(max == 7){
                correction = true;
            }
        }
        //获取所有已经到8的选手
        List<Integer> NumList = new ArrayList<>();
        for(Integer Num :distanceMap.keySet()){
            if (distanceMap.get(Num)==8) NumList.add(Num);
        }
        //随机筛选一个选手，记录并移出到8集合
        Integer selectNum = NumList.get(new Random().nextInt(NumList.size()));
        NumList.remove(selectNum);
        distanceMap.clear();//使用完成，清除距离表
        //给每个选手计入最后一步
        for(int i=1;i<=contestantMap.size();i++){
            List<Integer> stepList = stepMap.get(i);
            //给步数一个随机数，选中选手锁定为2，其他前排选手锁定为1
            int step = randomStep(contestantStatus.get(i));//用状态表去获取随机步数
            if(i==selectNum) step = 2;
            if(NumList.contains(i)) step = 1;
            stepList.add(step);
            stepMap.put(i,stepList);
        }
        //开启子线程并延时发送赛马信息
        new Thread(()->{
            //发送第一张开局图片
            StringBuilder first = new StringBuilder();
            first.append("哨声响起，赛马大赛开始了\n");
            for (int i=0;i<stepMap.size();i++) {
                first.append("=".repeat(10));
                first.append("\uD83D\uDC0E");
                first.append("\n");
            }
            replayInfo.setReplayMessage(first.toString());
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            //随便挑选一个读入比赛时间（即list长度），然后以读入的时间作为发消息的次数
            List<Integer> list = stepMap.get(1);
            for (int i=0;i<list.size();i++) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                StringBuilder s = new StringBuilder();
                //每有一个选手，就加入一排
                for(int j=1;j<=stepMap.size();j++){
                    List<Integer> stepList =stepMap.get(j);
                    //计算已经完成的路程
                    int add = 0;
                    for(int k=0;k<=i;k++){
                        add = add + stepList.get(k);
                    }
                    //先输出前面剩余路程
                    int before =10-add;
                    s.append("=".repeat(before));
                    s.append("\uD83D\uDC0E");//插入马
                    //输出已经完成的路程
                    s.append("=".repeat(add));
                    s.append("\n");
                }
                replayInfo.setReplayMessage(s.toString());
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
            groupList.remove(messageInfo.getGroupId());
            replayInfo.setReplayMessage("恭喜"+contestantMap.get(selectNum)+"拔得本场比赛的头筹！");
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
        }).start();
        return selectNum;
    }

    /**
     * 给选手计算前进步数的方法
     *
     * @param status 选手比赛状态概率，用于提供状态数据
     * @return step 步数结果
     */
    private int randomStep(Float status){
        int step;//随机步数
        if (new Random().nextFloat() <= status){
            step = 2;
        }else {
            step = 1;
        }
        return step;
    }

}
