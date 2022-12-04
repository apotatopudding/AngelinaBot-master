package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.dao.BattleGroundMapper;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.BattleGroundGroupInfo;
import top.strelitzia.model.BattleGroundInfo;
import top.strelitzia.model.OperatorBasicInfo;

import java.text.SimpleDateFormat;
import java.util.*;


//@Service
@Slf4j
public class ArknightsBattleground {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private BattleGroundMapper battleGroundMapper;

    //群组功能表
    private static final Map<Long,BattleGroundGroupInfo> battleGroundGroup = new HashMap<>();

    //QQ消息时间戳列表
    private final Map<Long, List<Long>> qqMsgList = new HashMap<>();

    //地点表
    private static final Map<Integer,String> atlas = new HashMap<>(){
        {
            put(7,"萨尔贡");
            put(6,"哥伦比亚");
            put(5,"炎国");
            put(4,"雷姆必拓");
            put(3,"乌萨斯");
            put(2,"伊比利亚");
            put(1,"卡西米尔");
        }
    };

    @AngelinaGroup(keyWords = {"开始绝地作战"}, description = "花费十个积分，邀请十名选手，开启绝地作战", sort = "娱乐功能", funcClass = "绝地作战")
    public ReplayInfo beginBattleground(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        BattleGroundGroupInfo battleGroundGroupInfo = new BattleGroundGroupInfo();
        if (battleGroundGroup.containsKey(messageInfo.getGroupId())) {
            replayInfo.setReplayMessage("绝地作战还未结束，暂不可开启新局");
            return replayInfo;
        }
        //添加群组防止重复开启
        battleGroundGroup.put(messageInfo.getGroupId(),null);
        //检查积分是否足够
        int integral;
        try{
            integral = this.integralMapper.selectByQQ(messageInfo.getQq());
        }catch (NullPointerException e){
            e.printStackTrace();
            replayInfo.setReplayMessage("您还没有积分呢，试着参与活动以获取积分吧");
            battleGroundGroup.remove(messageInfo.getGroupId());
            return replayInfo;
        }
        if (integral < 20){
            replayInfo.setReplayMessage("您的积分不足以开启活动，多多争取吧");
            battleGroundGroup.remove(messageInfo.getGroupId());
            return replayInfo;
        }
        //检查群临时会话是否开启
        MemberPermission permission = messageInfo.getBotPermission();
        if(permission.getLevel()>0){
            replayInfo.setReplayMessage("由于"+messageInfo.getBotName()+"是管理员身份，无法判断群聊临时会话是否开启，请开启人自行检查并确认群临时会话开启" +
                    "\n" +
                    "\n如确认临时会话开启，请回复 ( 确认 ) 以开启绝地作战" +
                    "\n请务必仔细检查，如果确认开启而无法发送临时会话消息，绝地作战将会立刻关闭，扣除的积分则不再退还");
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            AngelinaListener angelinaListener = new AngelinaListener() {
                //本人回复为确认时生效
                @Override
                public boolean callback(MessageInfo message) {
                    String participant;
                    if(message.getText()==null){
                        participant = "任何非报名语句皆可";
                    }else {
                        participant = message.getText();
                    }
                    return message.getGroupId().equals(messageInfo.getGroupId()) &&
                            message.getQq().equals(messageInfo.getQq()) &&
                            participant.equals("确认");
                }
            };
            //监听等待
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(30);
            MessageInfo recall = AngelinaEventSource.waiter2(angelinaListener).getMessageInfo();
            if (recall == null) {
                battleGroundGroup.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("长时间未确认，绝地作战已关闭");
                sendMessageUtil.sendGroupTempMsg(replayInfo);
                replayInfo.setReplayMessage("由于无法确认临时会话情况，绝地作战已关闭");
                return replayInfo;
            }else {
                replayInfo.setReplayMessage("临时会话关系已确认，游戏即将开始");
                sendMessageUtil.sendGroupMsg(replayInfo);
            }
        }else {
            replayInfo.setReplayMessage("测试消息");
            try {
                sendMessageUtil.sendGroupTempMsg(replayInfo);
            }catch (IllegalStateException e){
                log.error(e.toString());
                battleGroundGroup.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("群临时会话已关闭，无法开始游戏，请开启后再试");
                return replayInfo;
            }
            replayInfo.setReplayMessage("临时会话关系已确认，游戏即将开始");
            sendMessageUtil.sendGroupMsg(replayInfo);
        }
        //开始报名
        replayInfo.setReplayMessage("绝地作战报名已开启，请各位踊跃参与吧");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        List<BattleGroundInfo> participantList =new ArrayList<>(10);//参与者信息列表
        List<Long> participantQQList =new ArrayList<>();//参与QQ的列表（只用于判断，结束销毁）
        //监听“报名”作为报名条件
        for(int i=1; i < 11; i++){
            AngelinaListener angelinaListener = new AngelinaListener() {
                //只有返回为报名时才生效
                @Override
                public boolean callback(MessageInfo message) {
                    String participant;
                    if(message.getText()==null){
                        participant = "任何非报名语句皆可";
                    }else {
                        participant = message.getText();
                    }
                    return message.getGroupId().equals(messageInfo.getGroupId()) && participant.equals("报名");
                }
            };
            //监听等待
            angelinaListener.setGroupId(messageInfo.getGroupId());
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null) {
                battleGroundGroup.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("报名超时，人数尚不足十人，暂无法开启");
                return replayInfo;
            }
            //列出已报名人员，如果重复报名直接跳过录入
            if(participantQQList.contains(recall.getQq())){
                replayInfo.setReplayMessage("请勿重复报名");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                i = i - 1;
            }else {
                BattleGroundInfo newBattleGroundInfo = new BattleGroundInfo();
                participantQQList.add(recall.getQq());
                //获取QQ和名字写入List,其他数据以初始数据为注入
                newBattleGroundInfo.setQQ(recall.getQq());
                newBattleGroundInfo.setName(recall.getName());
                newBattleGroundInfo.setGroupId(recall.getGroupId());
                //初始人物生命1000，上限1000，物理攻击60，魔法攻击0，物理护甲0，魔法护甲0，真实伤害0，单次减伤0,所在地点未确定
                participantList.add(newBattleGroundInfo);
                replayInfo.setReplayMessage(recall.getName()+"报名成功，还需"+ (10-i) + "人即可开始");
                if(i==10){
                    replayInfo.setReplayMessage("报名完毕，正在布置战场...");
                }
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
        //再次查询积分情况，如果积分被提前花费则会开启失败
        integral = this.integralMapper.selectByQQ(messageInfo.getQq());
        if (integral < 20){
            replayInfo.setReplayMessage("您的积分已经不足以开启绝地作战，可能是您提前花掉了积分，绝地作战开启失败");
            battleGroundGroup.remove(messageInfo.getGroupId());
            return replayInfo;
        }
        //把list中记录的QQ注入数据库
        for(BattleGroundInfo battleGroundInfo1 :participantList){
            this.battleGroundMapper.insertInfo(battleGroundInfo1);
        }
        //this.enableMapper.closeGroup(messageInfo.getGroupId(), 1);//停止群组消息接收（未启用）
        //报名完成，扣除发起人的二十点积分,同时开启活动开启临时会话，写入bean
        battleGroundGroupInfo.setGroupTempSwitch(true);
        battleGroundGroup.put(messageInfo.getGroupId(),battleGroundGroupInfo);
        this.integralMapper.reduceIntegralByGroupId(messageInfo.getGroupId(),messageInfo.getQq(),20);
        TextLine textLine = new TextLine(100);
        textLine.addString("绝地作战正式开始");
        textLine.nextLine();
        textLine.addString("【请私聊琴柳：「前往  XX（地点）」】");
        textLine.nextLine();
        textLine.addString("【或者：前往 X (地点序号)】");
        textLine.nextLine();
        textLine.addString("『注意：");
        textLine.nextLine();
        textLine.addString("每个动作（前往和搜索）需要十秒，请各位合理规划时间』");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("三级区域为：");
        textLine.nextLine();
        textLine.addString("7.萨尔贡|6.哥伦比亚|5.炎国|4.雷姆必拓");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("二级区域为：");
        textLine.nextLine();
        textLine.addString("3.乌萨斯|2.伊比利亚");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("一级区域为：");
        textLine.nextLine();
        textLine.addString("1.卡西米尔");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("重点提醒：不要加机器人为好友，可能会造成参与失败");
        replayInfo.setReplayImg(textLine.drawImage());
        new Thread(() -> {
            //开启子线程启动延时缩圈
            List<Integer> allList = new ArrayList<>(Arrays.asList(2,3,4,5,6,7));//地区表，维多利亚永远不抽
            try {
                Thread.sleep(180000);
            }catch (Exception e){
                log.error(e.getMessage());
            }
            try {
                for(int i=0; i<3; i++){
                    if ( battleGroundGroup.get(messageInfo.getGroupId()).isExit() ){
                        throw new InterruptedException();
                    }
                    allList = selectArea(allList,messageInfo);
                    try {
                        Thread.sleep(180000);
                    }catch (Exception e){
                        log.error(e.getMessage());
                    }
                }
                duelTime(messageInfo);
            }catch (InterruptedException e){
                log.info(e.getMessage()+messageInfo.getGroupId()+"的线程已结束");
            }
        }).start();
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"前往"}, description = "绝地作战搜宝项目")
    public ReplayInfo leaveTo(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //检查是否为好友
        if(!battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("群组查找失败，可能还没开始活动，请注意勿加琴柳为好友");
            return replayInfo;
        }
        //检查活动是否开启
        if(!battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("要先开始绝地作战哦");
            return replayInfo;
        }
        //获取QQ判断是否在所属群
        List<Long> sure = battleGroundMapper.selectAllQQByGroup(messageInfo.getGroupId(),messageInfo.getQq());
        if ( sure.size() ==0 ){
            replayInfo.setReplayMessage("请先报名参加活动哦");
            return replayInfo;
        }

        //群组消息
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());
        List<Integer> closeArea = battleGroundGroupInfo.getCloseArea();

        //个人信息
        BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),messageInfo.getQq());
        Integer health = battleGroundInfo.getHealth();
        String defeatedBy =battleGroundInfo.getDefeatedBy();

        //检查开关，打开时候开启功能，否则告诉功能未开启
        if(battleGroundGroupInfo.getGroupTempSwitch()){
            //如果死亡则告知击败者同时拒绝活动
            if( health ==0 ){
                replayInfo.setReplayMessage("抱歉您已经被淘汰了，击败您的人是:" + defeatedBy );
                return replayInfo;
            }
            //判断是否发送了地点信息
            if(messageInfo.getArgs().size()>1) {
                //判断是否在时间戳内
                if (getMsgLimit(messageInfo)){
                    String situation = messageInfo.getArgs().get(1);
                    if (atlas.containsValue(situation) || (Integer.parseInt(situation) < 7 && Integer.parseInt(situation) > 0)) {
                        boolean result = situation.matches("[0-9]+");
                        replayInfo.setReplayMessage("地点选择正确，正在前往");
                        Integer situ = 0;
                        //如果地点不是数字，则根据map找到所属数字
                        if (!result) {
                            for (Map.Entry<Integer, String> integerStringEntry : atlas.entrySet()) {
                                if (integerStringEntry.getValue().equals(situation)) {
                                    situ = integerStringEntry.getKey();
                                }
                            }
                        }else {
                            situ = Integer.valueOf(situation);
                        }
                        //判断地点是否开放
                        if(!(closeArea == null)) {
                            if (closeArea.contains(situ)) {
                                replayInfo.setReplayMessage("你千辛万苦走到边境，立刻被一群荷枪实弹的军队瞄准了：”你已接近边境，立刻返回！“\n你赶紧离开，保住了命");
                                return replayInfo;
                            }
                        }
                        //插入地点
                        battleGroundInfo.setLocation(situ);
                        this.battleGroundMapper.updateLocationByGroupAndQQ(battleGroundInfo);
                    }
                }
            }else { replayInfo.setReplayMessage("您还没有告诉我您要前往的地点呢，重新告诉我一下吧" ); }
        }else{ replayInfo.setReplayMessage("快去观看精彩的对决吧"); }//活动在办但是没有开始临时会话
        try {
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            replayInfo.setReplayMessage(null);
        }catch (IllegalStateException e){
            this.cleanGroupDate(messageInfo);
            replayInfo.setReplayMessage("很抱歉，由于群组会话已经被关闭，琴柳无法发送消息，游戏被迫终止");
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"搜索"}, description = "绝地作战搜宝项目")
    public ReplayInfo searchTreasure(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);

        //群组信息
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());

        //个人信息
        BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),messageInfo.getQq());
        Long groupId = battleGroundInfo.getGroupId();
        Long QQ = battleGroundInfo.getQQ();
        Integer health = battleGroundInfo.getHealth();
        Integer healthPoints = battleGroundInfo.getHealthPoints();
        Integer physicsAttack = battleGroundInfo.getPhysicsAttack();
        Integer magicAttack = battleGroundInfo.getMagicAttack();
        Integer physicsArmor = battleGroundInfo.getPhysicsArmor();
        Integer magicArmor = battleGroundInfo.getMagicArmor();
        Integer realDamage = battleGroundInfo.getRealDamage();
        Integer reduceDamage = battleGroundInfo.getReduceDamage();
        Integer location = battleGroundInfo.getLocation();
        String defeatedBy = battleGroundInfo.getDefeatedBy();

        //检查是否为好友
        if(!battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("群组查找失败，可能还没开始活动，请注意勿加琴柳为好友");
            return replayInfo;
        }
        //检查活动是否开启
        if( ! battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("要先开始绝地作战哦");
            return replayInfo;
        }
        //获取QQ判断是否在所属群
        List<Long> sure = battleGroundMapper.selectAllQQByGroup(messageInfo.getGroupId(),messageInfo.getQq());
        if ( sure.size() ==0 ){
            replayInfo.setReplayMessage("请先报名参加活动哦");
            return replayInfo;
        }
        //检查开关，打开时候开启功能，否则告诉活动未开启或者功能未开启
        if(battleGroundGroupInfo.getGroupTempSwitch()){
            //如果死亡则告知击败者同时拒绝活动
            if( health ==0 ){
                replayInfo.setReplayMessage("抱歉您已经被淘汰了，击败您的人是:" + defeatedBy );
                return replayInfo;
            }
            //检查地点填了没
            if (location == 0 ) {
                replayInfo.setReplayMessage("请先告知您要前往的地点");
                return replayInfo;
            }
            //判断等待时间
            if (getMsgLimit(messageInfo)){
                //选出同区域的人
                List<Long> sameAreaList = battleGroundMapper.selectQQBySameArea(groupId,location);
                sameAreaList.remove(QQ);//排除自己
                //判断是搜宝还是遭遇
                if(new Random().nextInt(sameAreaList.size() + 8) < 8 ){
                    List<String> allOperator = operatorInfoMapper.getAllOperator();
                    String name = allOperator.get(new Random().nextInt(allOperator.size()));
                    OperatorBasicInfo operatorInfo = operatorInfoMapper.getOperatorInfoByName(name);//干员信息
                    //根据干员职业和星级来获取数据
                    switch (operatorInfo.getOperatorClass()) {
                        case 1 -> {//className = "先锋";
                            Integer value = healthPoints + operatorInfo.getOperatorRarity() * 10;
                            Integer bloodValue = health + operatorInfo.getOperatorRarity();
                            battleGroundInfo.setHealthPoints(value);
                            battleGroundInfo.setHealth(bloodValue);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 10 + " 点生命上限");
                        }
                        case 2 -> {//className = "近卫";
                            Integer value = physicsAttack + operatorInfo.getOperatorRarity() * 10;
                            battleGroundInfo.setPhysicsAttack(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 10 + " 点物理攻击");
                        }
                        case 3 -> {//className = "重装";
                            Integer value = physicsArmor + operatorInfo.getOperatorRarity() * 50;
                            battleGroundInfo.setPhysicsArmor(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 50 + " 点物理护甲");
                        }
                        case 4 -> {//className = "狙击";
                            Integer value = realDamage + operatorInfo.getOperatorRarity()* 10;
                            battleGroundInfo.setRealDamage(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 10 + " 点真实伤害");
                        }
                        case 5 -> {//className = "术士";
                            Integer value = magicAttack + operatorInfo.getOperatorRarity()* 10;
                            battleGroundInfo.setMagicAttack(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 10 + " 点魔法攻击");
                        }
                        case 6 -> {//className = "辅助";
                            Integer value = magicArmor + operatorInfo.getOperatorRarity() * 50;
                            battleGroundInfo.setMagicArmor(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 50 + " 点魔法护甲");
                        }
                        case 7 -> {//className = "医疗";
                            int value = health + operatorInfo.getOperatorRarity() * 50;
                            int realValue = operatorInfo.getOperatorRarity() * 50;
                            if (value > healthPoints) {
                                realValue = healthPoints - health;
                                value = healthPoints;
                            }
                            battleGroundInfo.setHealth(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您回复了 " + realValue + " 点生命");
                        }
                        default -> {//className = "特种";
                            Integer value = reduceDamage + operatorInfo.getOperatorRarity()* 10;
                            battleGroundInfo.setReduceDamage(value);
                            replayInfo.setReplayMessage("您遇到了干员 " + name + ",ta为您提供了 " + operatorInfo.getOperatorRarity()* 10 + " 点伤害减免");
                        }
                    }
                }else {
                    //从区域内随机选出对战人员
                    Long duel = sameAreaList.get(new Random().nextInt(sameAreaList.size()));
                    //进行对战
                    againstInfo(duel,replayInfo);
                    //调取战场伤亡报告并返回报告信息
                    replayInfo = casualtyReporting(duel,replayInfo);
                }
                //查找所有还活着的人
                List<Long> allAlive = battleGroundMapper.selectQQByHealth(groupId);
                //只剩三个以下人，直接进入决斗模式
                if (allAlive.size()<=3){
                    duelTime(messageInfo);
                    battleGroundGroupInfo.setAlreadyBegan(true);
                }
            }
            battleGroundMapper.updateInfoByGroupAndQQ(battleGroundInfo);
            battleGroundGroup.put(groupId,battleGroundGroupInfo);
        }else{ replayInfo.setReplayMessage("快去观看精彩的对决吧"); }//活动在办但是没有开始临时会话
        return replayInfo;
    }

    //卡西米尔骑士对抗赛
    @AngelinaGroup(keyWords = {"出击"}, description = "战胜对手，赢得荣耀，欢呼吧！", sort = "娱乐功能", funcClass = "绝地作战")
    public ReplayInfo fieldOfHonor(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //检查活动是否开启
        if( ! battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("要先开始绝地作战哦");
            return replayInfo;
        }

        //群组信息
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());

        //个人信息
        BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),messageInfo.getQq());
        Long groupId = battleGroundInfo.getGroupId();
        String name = battleGroundInfo.getName();
        Long QQ = battleGroundInfo.getQQ();
        String defeatedBy = battleGroundInfo.getDefeatedBy();
        Integer health = battleGroundInfo.getHealth();


        //获取QQ判断是否在所属群
        List<Long> sure = battleGroundMapper.selectAllQQByGroup(messageInfo.getGroupId(),messageInfo.getQq());
        if ( sure.size() ==0 ){
            replayInfo.setReplayMessage("请先报名参加活动哦");
            return replayInfo;
        }
        //检查开关，打开时候开启功能，否则告诉功能未开启
        if(battleGroundGroupInfo.getGroupSwitch()){
            //如果死亡则告知击败者同时拒绝活动
            if( health == 0 ){
                replayInfo.setReplayMessage("抱歉您已经被淘汰了，击败您的人是:" + defeatedBy );
                return replayInfo;
            }
            //查找所有还活着的人
            List<Long> allAlive = battleGroundMapper.selectQQByHealth(groupId);
            allAlive.remove(QQ);//排除自己
            //从表内随机选出对战人员
            Long duel = allAlive.get(new Random().nextInt(allAlive.size()));
            //进行对战
            againstInfo(duel,replayInfo);
            //获取对战结束后的信息
            BattleGroundInfo newBattleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),messageInfo.getQq());
            Integer newHealth = newBattleGroundInfo.getHealth();
            BattleGroundInfo duelInfo = battleGroundMapper.selectInfoByGroupAndQQ(groupId,duel);
            Integer duelHealth = duelInfo.getHealth();
            String duelName = duelInfo.getName();
            //战报
            String message;
            if ( duelHealth == 0 ){
                if ( newHealth == 0 ){
                    message = name + "和" + duelName + "同归于尽了" + "\n";
                    duelInfo.setDefeatedBy(name);
                    newBattleGroundInfo.setDefeatedBy(duelName);
                    allAlive.remove(duel);
                }else {
                    message = name + "战胜了"+ duelName +"，还剩"+ newHealth +"点生命值"+ "\n";
                    duelInfo.setDefeatedBy(name);
                    allAlive.remove(duel);
                    allAlive.add(QQ);
                }
            }else if( newHealth == 0 ){
                message = name + "失败了，很抱歉"+ "\n";
                newBattleGroundInfo.setDefeatedBy(duelName);
            }else {
                message = name + "和"+ duelName +"展开了交战，"+name+"剩余"+ newHealth +"点生命值，"+duelName+"剩余"+duelHealth+"点生命值"+ "\n";
                allAlive.add(QQ);
            }
            //传回数据库
            battleGroundMapper.updateInfoByGroupAndQQ(newBattleGroundInfo);
            battleGroundMapper.updateInfoByGroupAndQQ(duelInfo);
            //还剩两人以下的时候进入判定
            if( allAlive.size() < 3 ){
                String award = "";
                String overGame = "";
                if (allAlive.size() < 2){
                    //二进一，可能是二变零，同归则先手获胜
                    if ( duelHealth == 0 ){
                        if ( newHealth == 0 ){
                            award = "又是仇人相见，双方都已经力竭，但在四目相对的一瞬双方似乎却又气力暴涨。"+duelName+"纵身一跃以全身力气挥出一刀，"+name+"虽试图招架，手却已经握不稳刀，一刀命中鲜血如注。胜负似乎已分，" +
                                    duelName+"颤颤巍巍站起身正欲欢呼，笑容却戛然而止，眼见他脸色由红转青，由青变白。他不可思议地回头看了一眼，一个字仿佛停在嘴角，他呜咽着想要说出口，力气却再也支撑不住，咚的一声栽倒在地上" +
                                    "，背后被血浸润的刀光映衬着"+name+"的惨笑。"+name+"轻轻张口，用微弱的声音轻叹一句'何必如此？'夕阳西下，漫天的红霞倒映在两人渐冷的尸体上，绚烂无比";
                            //第一名
                            Integer integral = this.integralMapper.selectByQQ(QQ);
                            try{integral = integral + 15;
                            }catch (NullPointerException e){
                                integral = 15;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),name,QQ,integral);
                            //第二名
                            integral = this.integralMapper.selectByQQ(duel);
                            try{integral = integral + 10;
                            }catch (NullPointerException e){
                                integral = 10;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),duelName,duel,integral);
                        }else{
                            award = "经过此前多次大战，"+name+"早已深知骄兵必败之理，他并未急于上前，转而周旋于对方身边，以守为攻，试图看穿对手的破绽，力求一击必杀。反复的周旋让"+duelName+"越来越急躁，他深知自己身中数刀，" +
                                    "时间拖得越久对他越是不利，长时的消耗战会使得他犹如深陷泥沼，挣扎而不得出。虽然刀挥舞的越来越快，但是老练的他却粗中有细，故意卖了个破绽。"+name+"得此机会，大喜过望，以为时机已到，拼尽全力" +
                                    "发起进攻。"+duelName+"一声冷笑，一个挥刺砍出，眼见就要得手。可人算不如天算，"+duelName+"已是竭尽全力，恰如强弩之末，挥刀速度已大不如前。"+name+"余光被一道刀光一晃，本能的危险察觉让他" +
                                    "飞速转身后撤。"+duelName+"的凌厉刀锋被"+name+"堪堪躲过，胸口一道血疤顿现。"+name+"汗毛倒立，顿敢后怕，但凡晚了那么半秒，自己早已命丧黄泉。虽在后怕，"+name+"手上余威却是不减，趁着"+
                                    duelName+"挥刀猛刺已无法收回的时机，轻松一刀撂翻了"+duelName+"，一员悍将就此倒下，令人扼腕叹息啊";
                            //第一名
                            Integer integral = this.integralMapper.selectByQQ(QQ);
                            try{integral = integral + 15;
                            }catch (NullPointerException e){
                                integral = 15;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),name,QQ,integral);
                            //第二名
                            integral = this.integralMapper.selectByQQ(duel);
                            try{integral = integral + 10;
                            }catch (NullPointerException e){
                                integral = 10;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),duelName,duel,integral);
                        }
                    }else if (newHealth == 0){
                        award = "已经到了决赛的关键时刻，"+name+"更是不敢大意，早早的摆好了架势临阵御敌，而对面，久负盛名的"+duelName+"同样没有轻敌。二者多次上前一番试探拼刺，却都未能刺探到对方的一丝破绽。"+name+"面色凝重，" +
                                "心道这次遇上了一个难缠的对手。很快，"+name+"改变战略，对对方发起了一阵狂风暴雨般的攻击，"+duelName+"急忙以刀接招。刚开始，"+duelName+"接招还游刃有余，很快，"+duelName+"的动作开始有些力不从心" +
                                "，难道是体力已经不支了？随着一刀旋转猛刺，"+duelName+"在接手的瞬间手腕一滑，竟是把"+name+"的刀滑向了体侧。"+name+"大喜过望，心道功夫不负有心人，眼见一刀就要毙敌于面前。说时迟那时快，谁也没有想到这" +
                                "竟然是"+duelName+"漏出的假破绽！"+name+"急于求成，已经将整个身侧暴露在了"+duelName+"的面前，"+duelName+"回身的同时，以一刀凌厉的挑刀击中了"+name+"的要害。深藏不露，高手啊！";
                        //第一名
                        Integer integral = this.integralMapper.selectByQQ(duel);
                        try{integral = integral + 15;
                        }catch (NullPointerException e){
                            integral = 15;
                        }
                        this.integralMapper.integralByGroupId(messageInfo.getGroupId(),duelName,duel,integral);
                        //第二名
                        integral = this.integralMapper.selectByQQ(QQ);
                        try{integral = integral + 10;
                        }catch (NullPointerException e){
                            integral = 10;
                        }
                        this.integralMapper.integralByGroupId(messageInfo.getGroupId(),name,QQ,integral);
                    }
                    //执行初始化
                    cleanGroupDate(messageInfo);
                    overGame = "\n比赛已经结束，感谢大家观看本次卡西米尔对抗赛，请各位观众收拾好自己的座位上的物品有序退场";
                }else{
                    //三进二，可能是四进二，同归则先手获胜
                    if ( duelHealth == 0 ){
                        if ( newHealth == 0 ){
                            //同归于尽为四进二，先手为第三
                            award = "赛场上的角逐颇为激烈，站在场上的人数也在逐渐变少。看这边，两个人发生了激烈搏斗，是"+name+"和"+duelName+"，双方都在拼尽全力，场面非常混乱，随着双方的打斗，两人都渐渐体力不支，动作慢了下来。看！"+
                                    name+"奋勇出击，以飞龙在天之势劈出一刀，"+duelName+"想要挡下这一刀却完全来不及了！看来胜负已分。等等！"+duelName+"竟然藏了一手，从腰间摸出一根弩箭从暗处刺中了"+name+"！可惜啊，自己也失血过多" +
                                    "倒在了地上，"+name+"也没能活下来，双双倒地身亡。真是一场激烈的战斗啊，让我们把镜头转向其他选手。";
                            Integer integral = this.integralMapper.selectByQQ(QQ);
                            try{integral = integral + 1;
                            }catch (NullPointerException e){
                                integral = 1;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),name,QQ,integral);
                        }else{
                            //三进二，输的为第三即后手为第三
                            award = "赛场出现了异样，场上二人联合，向"+duelName+"发起了猛烈的围攻！"+duelName+"从容接下了二人招数，但是很快啊，疲乏不堪的"+duelName+"有些不堪重负了，招架动作开始迟缓了。在堪堪躲过了一刀以后，"+
                                    duelName+"似乎试图逃出包围，他在后撤。 "+name+"猛地追了上去，踹翻了"+duelName+"，看来他想要顺势斩掉"+duelName+"了。"+name+"一刀劈下，"+duelName+"却灵活的一个翻滚躲开了刀锋。"+
                                    name+"是还想要乘胜追击吗？看来"+duelName+"察觉到了，他在干什么，他一个翻滚滚出了赛场！这就是认输了啊！很遗憾"+duelName+"被淘汰了，不过能在这种赛事下活下来也实属罕见，恭喜"+duelName+"活着获得第三名";
                            Integer integral = this.integralMapper.selectByQQ(duel);
                            try{integral = integral + 5;
                            }catch (NullPointerException e){
                                integral = 5;
                            }
                            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),duelName,duel,integral);
                        }
                    }else if(newHealth == 0){
                        //三进二，输的为第三即先手为第三
                        award = "身强力壮的"+name+"首先向"+duelName+"发起了进攻，看来是希望以最小的代价取得胜利啊，如此猛烈的攻击，"+duelName+"能不能接住呢。诶？"+duelName+"似乎完全没打算接招，闪身躲开了。"+
                                name+"反应也不慢，刀一横又是一记猛挥，看来是棋逢对手了啊，不过"+duelName+"又以一个转身躲开了。"+name+"急了，看来他似乎没想到对手能有这么灵巧的身法，狂风骤雨般的大刀向着"+name+"攻了过去，" +
                                "一下两下，接连躲过了七八刀？！"+name+"的攻击已经放慢了，什么时候，"+duelName+"竟然绕道了他的后面！一刀刺穿了"+name+"！快准狠啊，看来"+duelName+"还是棋高一着，恭喜"+name+"";
                        Integer integral = this.integralMapper.selectByQQ(QQ);
                        try{integral = integral + 5;
                        }catch (NullPointerException e){
                            integral = 5;
                        }
                        this.integralMapper.integralByGroupId(messageInfo.getGroupId(),name,QQ,integral);
                    }
                }
                message = message + award + overGame;
            }
            replayInfo.setReplayMessage(message);
        }else{ replayInfo.setReplayMessage("卡西米尔大赛还没开始呢，别急，博士"); }//活动在办但是没有开始临时会话
        return replayInfo;
    }

    //查询当前属性值
    @AngelinaGroup(keyWords = {"绝地查询"}, description = "当前个人属性查询", sort = "娱乐功能", funcClass = "绝地作战")
    @AngelinaFriend(keyWords = {"绝地查询"}, description = "当前个人属性查询")
    public ReplayInfo personalQuery(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if( ! battleGroundGroup.containsKey(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("要先开始绝地作战哦");
            return replayInfo;
        }
        if(messageInfo.getArgs().size()>1){
            String query = messageInfo.getArgs().get(1);
            if(query.equals("属性")){
                BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),messageInfo.getQq());
                replayInfo.setReplayMessage("您的生命值为："+battleGroundInfo.getHealth()+
                        "\n您的生命上限为："+battleGroundInfo.getHealthPoints()+
                        "\n您的物理攻击为："+battleGroundInfo.getPhysicsAttack()+
                        "\n您的魔法攻击为："+battleGroundInfo.getMagicAttack()+
                        "\n您的物理护甲为："+battleGroundInfo.getPhysicsArmor()+
                        "\n您的魔法护甲为："+battleGroundInfo.getMagicArmor()+
                        "\n您的真实伤害为："+battleGroundInfo.getRealDamage()+
                        "\n您的减伤为："+battleGroundInfo.getReduceDamage());
            }else if(query.equals("巡逻范围")){
                BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());
                List<Integer> areaList = battleGroundGroupInfo.getCloseArea();
                StringBuilder stringBuilder = new StringBuilder();
                if(areaList.size() == 0){
                    stringBuilder.append("暂时还没有地点被封闭");
                }else {
                    stringBuilder.append("已被封闭的地点有：\n");
                    for(Integer area :areaList){
                        String areaString = atlas.get(area);
                        stringBuilder.append(area).append("、").append(areaString).append("\n");
                    }
                }
                replayInfo.setReplayMessage(stringBuilder.toString());
            }
        }else {
            replayInfo.setReplayMessage("请输入要查询的信息");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"绝地游戏描述"}, description = "当前个人属性查询", sort = "娱乐功能", funcClass = "绝地作战")
    public ReplayInfo battleDescription(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        TextLine textLine = new TextLine(100);
        textLine.addString("绝地作战分为三部分：");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("一、\t开局报名阶段");
        textLine.nextLine();
        textLine.addString("报名阶段由发起人发送“XX（呼叫词）开启绝地作战”主动开启");
        textLine.nextLine();
        textLine.addString("当发起人成功发起绝地作战后，玩家可以发送“报名”来加入游戏。");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("发起人需要花费十点积分，如果积分不足则不能开启游戏，报名完成前积分不扣除，开始游戏则扣除积分。");
        textLine.nextLine();
        textLine.addString("需要十名玩家参与才可开启游戏。如果报名阶段一分钟内没有人发起报名，游戏就会");
        textLine.nextLine();
        textLine.addString("关闭。报名结束后，机器人将会发送开始游戏通知，则进入跑图搜宝阶段。");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("二、\t跑图搜宝阶段");
        textLine.nextLine();
        textLine.addString("跑图搜宝阶段为临时会话模式，在游戏刚开始时需要先发送一次“前往 X (地点序号或");
        textLine.nextLine();
        textLine.addString("地点名皆可)”来前往泰拉地图作战区。");
        textLine.nextLine();
        textLine.addString("在到达后可以进行发送“搜索”进行装备搜集。");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("在搜集途中，如果同地图有人也在进行搜宝，则有一定概率发生战斗。同地图人员越多，");
        textLine.nextLine();
        textLine.addString("搜集遭遇战斗概率越大。每隔三分钟，则会有两个区域进入戒严模式，同时对区域地图");
        textLine.nextLine();
        textLine.addString("上的所有人发出驱逐通知，三十秒后还未离开的人员则会被派出的治安管理队强行击杀。");
        textLine.nextLine();
        textLine.addString("同时，已经进入戒严模式的地区也将禁止任何人员再次进入。卡西米尔由于正在策划卡");
        textLine.nextLine();
        textLine.addString("西米尔对抗赛，将会始终欢迎所有人前往，此地治安官将会永远不会派出治安管理队。");
        textLine.nextLine();
        textLine.addString("当除了卡西米尔区域以外的所有区域都进入戒严状态或者存活人数仅有三人以下时，将");
        textLine.nextLine();
        textLine.addString("会进入卡西米尔对抗赛阶段");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("三、\t卡西米尔对抗赛阶段");
        textLine.nextLine();
        textLine.addString("卡西米尔对抗赛为群聊模式，进入对抗赛以后会为全体人员发送邀请函邀请观看。");
        textLine.nextLine();
        textLine.addString("尚且存活的人员可以发送“XX（呼叫词）出击”来对任意一位存活人员发起攻击。");
        textLine.nextLine();
        textLine.nextLine();
        textLine.addString("在对抗赛期间对抗赛官方将会为所有参与人员提供高强度的弩箭，每位先手攻击人员将");
        textLine.nextLine();
        textLine.addString("会获得一定真实伤害加成，同时，在搜宝阶段获得的所有真实伤害将会在第一轮攻击时全部打出。");
        textLine.nextLine();
        textLine.addString("对抗赛的前三名将会分别获得五分，三分和一分的积分奖励。");
        textLine.nextLine();
        textLine.addString("注意：任意一次对战中同归于尽的两人，以先手攻击的人作为当次战斗的胜利方");
        replayInfo.setReplayImg(textLine.drawImage());
        return replayInfo;
    }





    /**
     * 消息回复限速
     */
    private boolean getMsgLimit(MessageInfo messageInfo) {
        boolean flag = true;
        //每10秒限制三条消息,10秒内超过5条就不再提示
        //int length = 1;//消息发送限制
        int maxTips = 3;//最大提醒条数（数组最大限制）
        int second = 5;//时间限制
        long qq = messageInfo.getQq();
        String name = messageInfo.getName();
        if (!qqMsgList.containsKey(qq)) {
            //不存在的时候直接写入新时间戳，跳过检查
            List<Long> msgList = new ArrayList<>(maxTips);
            msgList.add(System.currentTimeMillis());
            qqMsgList.put(qq, msgList);
        }else{
            List<Long> limit = qqMsgList.get(qq);
            //if (limit.size() <= 1) {
            //队列未超过length直接发送消息并插入时间戳
            //    limit.add(System.currentTimeMillis());
            //} else
            if (getSecondDiff(limit.get(0), second)){
                //队列长度超过length但是距离首条消息已经大于second
                limit.add(System.currentTimeMillis());
                //写入第二条，删除第一条和后面条，第二条变为第一条
                limit.remove(0);
                while(limit.size() > 1) {
                    limit.remove(1);
                }
                qqMsgList.put(qq, limit);
            } else {
                if (limit.size() <= maxTips) {
                    //队列长度超过length未到maxTips，并且距离首条消息不足second，发出提示（未启用maxTips）
                    log.warn("{}超出单人回复速率,{}", name, limit.size());
                    ReplayInfo replayInfo = new ReplayInfo(messageInfo);
                    int time = (int) (second - ((System.currentTimeMillis() - limit.get(0)) / 1000));
                    replayInfo.setReplayMessage(messageInfo.getName() + "等等，休息一下，我太累了，休息，休息，再等"+ time +"秒再行动吧");
                    this.sendMessageUtil.sendGroupTempMsg(replayInfo);
                    limit.add(System.currentTimeMillis());
                    qqMsgList.put(qq, limit);
                } else {
                    //队列长度超出，直接忽略消息
                    log.warn("{}连续请求,已拒绝消息", name);
                }
                flag = false;
            }
        }
        //对队列进行垃圾回收
        gcMsgLimitRate();
        return flag;
    }

    /**
     * 时间差
     */
    public boolean getSecondDiff(Long timestamp, int second) {
        return (System.currentTimeMillis() - timestamp) / 1000 > second;
    }

    /**
     * 消息列表回收
     */
    public void gcMsgLimitRate() {
        //大于1024个队列的时候进行垃圾回收,大概占用24k
        if (qqMsgList.size() > 1024) {
            log.warn("开始对消息速率队列进行回收，当前map长度为：{}", qqMsgList.size());
            //回收所有超过一分钟的会话
            qqMsgList.entrySet().removeIf(entry -> getSecondDiff(entry.getValue().get(0), 60));
            log.info("消息速率队列回收结束，当前map长度为：{}", qqMsgList.size());
        }
    }

    /**
     * 缩圈提醒与打击
     */
    public List<Integer> selectArea(List<Integer> allList,MessageInfo messageInfo) {
        ReplayInfo replayInfo =new ReplayInfo(messageInfo);
        log.info("发送新一轮缩圈提醒");
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());
        List<Integer> closeArea = battleGroundGroupInfo.getCloseArea();
        if( closeArea == null){
            closeArea = new ArrayList<>();
        }
        for (int i = 0; i < 2; i++) {
            //随机选俩，选一个删一个
            int Num = new Random().nextInt(allList.size());
            Integer area = allList.get(Num);
            allList.remove(Num);
            List<Long> sameAreaList = battleGroundMapper.selectQQBySameArea(messageInfo.getGroupId(),area);
            //把删掉的地点编号加入已关闭区域
            closeArea.add(area);
            battleGroundGroupInfo.setCloseArea(closeArea);
            battleGroundGroup.put(messageInfo.getGroupId(),battleGroundGroupInfo);
            //给所有在毒圈里的人发消息
            for (Long sameAreaQQ : sameAreaList) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("你的位置暴露了，治安管理队将在三十秒后对你发起攻击，赶快离开\n" +
                        "尚且安全的地点有：\n");
                for(Integer areaList :allList){
                    String closeString = atlas.get(areaList);
                    stringBuilder.append(areaList).append("、").append(closeString).append("\n");
                }
                replayInfo.setQq(sameAreaQQ);
                replayInfo.setReplayMessage(stringBuilder.toString());
                sendMessageUtil.sendGroupTempMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
        //延时三十秒,再次检索地区，在地区内的所有人生命值归零
        try {
            Thread.sleep(30000);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        for (Integer area : closeArea) {
            List<Long> sameAreaList = battleGroundMapper.selectQQBySameArea(messageInfo.getGroupId(),area);
            for ( Long sameAreaQQ : sameAreaList ) {
                BattleGroundInfo battleGroundInfo  = battleGroundMapper.selectInfoByGroupAndQQ(messageInfo.getGroupId(),sameAreaQQ);
                //跳过所有已死的人
                if(battleGroundInfo.getHealth() == 0){
                    continue;
                }
                battleGroundInfo.setHealth(0);
                battleGroundInfo.setDefeatedBy("治安管理队");
                battleGroundMapper.updateInfoByGroupAndQQ(battleGroundInfo);
                replayInfo.setQq(sameAreaQQ);
                replayInfo.setReplayMessage("你被治安管理队乱枪打死在大街上，你不曾注意治安管理队在狞笑，很可惜本次大赛你被淘汰了");
                sendMessageUtil.sendGroupTempMsg(replayInfo);

                replayInfo.setReplayMessage( battleGroundInfo.getName() + "被治安管理队乱枪打死在大街上，死状极其惨烈，你有没有听见"+ battleGroundInfo.getName() +"的悲鸣！");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
        return allList;
    }

    /**
     *  停止搜宝，进入对抗赛
     */
    public void duelTime(MessageInfo messageInfo) {
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(messageInfo.getGroupId());
        Bot bot = Bot.getInstance(messageInfo.getLoginQq());
        Group group = bot.getGroupOrFail(messageInfo.getGroupId());
        if( ! battleGroundGroupInfo.getAlreadyBegan() ){
            List<Long> QQList = battleGroundMapper.selectAllQQByGroup(messageInfo.getGroupId(),messageInfo.getQq());
            for (Long QQ : QQList){
                Member member = group.getOrFail(QQ);
                member.sendMessage("卡西米尔骑士对抗赛已经开启，欢迎各位前往观看");
            }
            battleGroundGroupInfo.setGroupSwitch(true);
            battleGroundGroupInfo.setGroupTempSwitch(false);
            battleGroundGroupInfo.setAlreadyBegan(true);
            battleGroundGroupInfo.setExit(true);
            battleGroundGroup.put(messageInfo.getGroupId(),battleGroundGroupInfo);
            group.sendMessage("欢迎各位来到卡西米尔——在这里，你可以享受到独特的美食和迷人的风景，充分感受卡西米尔独特的风土人情——" +
                    "\n但谁在乎这些啊！此时此刻，唯一占据各位内心的，只会是骑士竞技！" +
                    "\n卡瓦莱利亚基中央赛区，呼啸竞技场，今天也将为各位带来骑士们的风采！" +
                    "\n本次赛事由呼啸守卫公司全权赞助，活下来并取得前三的选手，都将获得宝贵的积分奖励！" +
                    "\n当然没活下来的选手，积分也会由你的第一顺位继承人完整继承！" +
                    "\n在此之外，呼啸守卫公司为每个选手提供了不限量的弩箭，为了荣耀，战斗吧骑士们！");
        }
    }

    /**
     *  对战信息
     */
    public void againstInfo(Long duel, ReplayInfo replayInfo) {
        //群组信息
        BattleGroundGroupInfo battleGroundGroupInfo = battleGroundGroup.get(replayInfo.getGroupId().get(0));
        boolean alreadyBegan = battleGroundGroupInfo.getAlreadyBegan();

        //个人信息
        BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(replayInfo.getGroupId().get(0),replayInfo.getQq());
        Integer health = battleGroundInfo.getHealth();
        Integer physicsAttack = battleGroundInfo.getPhysicsAttack();
        Integer magicAttack = battleGroundInfo.getMagicAttack();
        Integer physicsArmor = battleGroundInfo.getPhysicsArmor();
        Integer magicArmor = battleGroundInfo.getMagicArmor();
        Integer realDamage = battleGroundInfo.getRealDamage();
        Integer reduceDamage = battleGroundInfo.getReduceDamage();

        //对手信息
        BattleGroundInfo duelInfo = battleGroundMapper.selectInfoByGroupAndQQ(replayInfo.getGroupId().get(0),duel);
        Integer duelHealth = duelInfo.getHealth();
        Integer duelPhysicsAttack = duelInfo.getPhysicsAttack();
        Integer duelMagicAttack = duelInfo.getMagicAttack();
        Integer duelPhysicsArmor = duelInfo.getPhysicsArmor();
        Integer duelMagicArmor = duelInfo.getMagicArmor();
        Integer duelRealDamage = duelInfo.getRealDamage();
        Integer duelReduceDamage = duelInfo.getReduceDamage();

        int harm ;
        //当决斗开始时，先手者将获得十点真实伤害加成，然后真伤对射
        if(alreadyBegan){
            realDamage = realDamage + 50;//先手者将获得五十点真实伤害加成
            duelHealth = duelHealth - (realDamage-duelReduceDamage);
            health = health - (duelRealDamage-reduceDamage);
            replayInfo.setReplayMessage("双方发起了激烈的弩箭对射，"+replayInfo.getName()+"给对方造成了"+realDamage+"点破甲伤害，受到了"+duelRealDamage+"点破甲伤害");
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            //减伤归零
            reduceDamage = 0;
            duelReduceDamage = 0;
            //真伤只用这一次，直接归零写回bean
            battleGroundInfo.setRealDamage(0);
            duelInfo.setRealDamage(0);
        }
        //生命不为0，计算对战伤害
        if (health > 0 && duelHealth > 0){
            //对遭遇人员的伤害
            //物理伤害计算
            if( duelPhysicsArmor >= physicsAttack ){
                //防御>攻击，扣除指定攻击护甲
                duelPhysicsArmor = duelPhysicsArmor - physicsAttack;
                replayInfo.setReplayMessage("你未能击穿对方的物理护甲，对方的物理护甲受到了"+physicsAttack+"点伤害");
            }else {
                //防御<攻击，扣血查找减伤，还需要扣血则扣除生命值，护甲归零
                harm = physicsAttack-duelPhysicsArmor-duelReduceDamage;
                if(harm<0){ harm = 0; }
                duelPhysicsArmor = 0;
                duelHealth = duelHealth - harm;
                duelReduceDamage = 0;
                replayInfo.setReplayMessage("你攻击了对方，对方受到了"+harm+"点伤害");
            }
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            //法术伤害计算
            if( duelMagicArmor >= magicAttack ){
                //防御>攻击，扣除指定攻击护甲
                duelMagicArmor = duelMagicArmor - magicAttack;
                replayInfo.setReplayMessage("你未能击穿对方的法术护甲，对方的法术护甲受到了"+magicAttack+"点伤害");
            }else {
                //防御<攻击，扣血查找减伤，还需要扣血则扣除生命值，护甲归零
                harm = magicAttack-duelMagicArmor-duelReduceDamage;
                if(harm<0){ harm = 0; }
                duelMagicArmor = 0;
                duelHealth = duelHealth - harm;
                duelReduceDamage = 0;
                replayInfo.setReplayMessage("你攻击了对方，对方受到了"+harm+"点法术伤害");
            }
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            //如果伤害溢出，则血量归零
            if(duelHealth<0){
                duelHealth = 0;
            }
            //战斗结果写进对决者信息上传
            duelInfo.setHealth(duelHealth);
            duelInfo.setPhysicsAttack(duelPhysicsAttack);
            duelInfo.setMagicAttack(duelMagicAttack);
            duelInfo.setPhysicsArmor(duelPhysicsArmor);
            duelInfo.setMagicArmor(duelMagicArmor);
            duelInfo.setReduceDamage(duelReduceDamage);
            battleGroundMapper.updateInfoByGroupAndQQ(duelInfo);

            //自身受到的伤害
            //物理伤害计算
            if( duelPhysicsArmor >= physicsAttack ){
                //防御>攻击，扣除指定攻击护甲
                physicsArmor = physicsArmor -duelPhysicsAttack;
                replayInfo.setReplayMessage("对方未能击穿你的物理护甲，你的物理护甲受到了"+duelPhysicsAttack+"点伤害");
            }else {
                //防御<攻击，护甲归零，扣血查找减伤，还需要扣血则扣除生命值
                harm = duelPhysicsAttack - physicsArmor;
                if(harm > reduceDamage){
                    health = health - (harm-reduceDamage);
                    reduceDamage = 0;
                }else if(harm == reduceDamage){
                    reduceDamage = 0;
                }else if(harm != 0){
                    reduceDamage = 0;
                }
                physicsArmor = 0;
                replayInfo.setReplayMessage("对方反击了你，你受到了"+harm+"点物理伤害");
            }
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            replayInfo.setReplayMessage(null);

            //法术伤害计算
            if( magicArmor >= duelMagicAttack ){
                //防御>攻击，扣除指定攻击护甲
                magicArmor = magicArmor - duelMagicAttack;
                replayInfo.setReplayMessage("对方未能击穿你的法术护甲，你的法术护甲受到了"+duelMagicAttack+"点伤害");
            }else {
                //防御<攻击，护甲归零，扣血查找减伤，还需要扣血则扣除生命值
                harm = duelMagicAttack - magicArmor;
                if(harm > reduceDamage){
                    health = health - (harm-reduceDamage);
                    reduceDamage = 0;
                }else if(harm == reduceDamage){
                    reduceDamage = 0;
                }else if(harm != 0){
                    reduceDamage = 0;
                }
                magicArmor = 0;
                replayInfo.setReplayMessage("对方反击了你，你受到了"+harm+"点法术伤害");
            }
            sendMessageUtil.sendGroupTempMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            if(health<0){
                health = 0;
            }
            //战斗结果写进个人信息上传
            battleGroundInfo.setHealth(health);
            battleGroundInfo.setPhysicsAttack(physicsAttack);
            battleGroundInfo.setMagicAttack(magicAttack);
            battleGroundInfo.setPhysicsArmor(physicsArmor);
            battleGroundInfo.setMagicArmor(magicArmor);
            battleGroundInfo.setReduceDamage(reduceDamage);
            battleGroundMapper.updateInfoByGroupAndQQ(battleGroundInfo);

        }else {
            //对射任意死了一个，直接停止对决输出结果导入数据库
            replayInfo.setReplayMessage("什么！有人在弩箭对射时刻就负伤倒地了！");
            sendMessageUtil.sendGroupMsg(replayInfo);
            if(health<0){ health = 0; }
            if(duelHealth<0){ duelHealth = 0; }
            replayInfo.setReplayMessage(null);
            battleGroundInfo.setHealth(health);
            battleGroundInfo.setRealDamage(0);
            duelInfo.setHealth(duelHealth);
            duelInfo.setRealDamage(0);
            battleGroundMapper.updateInfoByGroupAndQQ(battleGroundInfo);
            battleGroundMapper.updateInfoByGroupAndQQ(duelInfo);
        }
    }

    /**
     *  战场伤亡报告
     */
    public ReplayInfo casualtyReporting(Long duel,ReplayInfo replayInfo){
        //个人信息
        BattleGroundInfo battleGroundInfo = battleGroundMapper.selectInfoByGroupAndQQ(replayInfo.getGroupId().get(0),replayInfo.getQq());
        Integer health = battleGroundInfo.getHealth();
        String name = battleGroundInfo.getName();

        //对手信息
        BattleGroundInfo duelInfo = battleGroundMapper.selectInfoByGroupAndQQ(replayInfo.getGroupId().get(0),duel );
        Integer duelHealth = duelInfo.getHealth();
        String duelName = duelInfo.getName();

        //记录信息以便发送
        Bot bot = Bot.getInstance(replayInfo.getLoginQQ());
        Group group = bot.getGroupOrFail(replayInfo.getGroupId().get(0));
        if ( duelHealth==0 ){
            if ( health==0 ){
                replayInfo.setReplayMessage("战报："+ name +"与"+ duelName +"在遭遇战中破釜沉舟，视死如归。但很可惜，由于势均力敌，他们双双殒命，真是一场精彩的对决");
                group.sendMessage(replayInfo.getReplayMessage());
                replayInfo.setReplayMessage("你和对方同归于尽了");
                duelInfo.setDefeatedBy(name);
                battleGroundInfo.setDefeatedBy(duelName);
            }else {
                replayInfo.setReplayMessage("战报："+ name +"在探险中偷袭了"+ duelName +"，虽然竭力反抗，但由于双方实力差距过大，"+ duelName +"最终不敌。琴柳重申，公平竞争，偷袭可耻！");
                group.sendMessage(replayInfo.getReplayMessage());
                replayInfo.setReplayMessage("你战胜了对方，还剩"+ health +"点生命值");
                duelInfo.setDefeatedBy(name);
            }
        }else if( health==0 ){
            replayInfo.setReplayMessage("战报："+ name +"在探险中遭遇了"+ duelName +"，"+ name +"试图从背后发起偷袭，谁料"+ duelName +"醉翁之意不在酒，一记回马枪击杀了"+ name +"，唉，真可惜");
            group.sendMessage(replayInfo.getReplayMessage());
            replayInfo.setReplayMessage("你失败了，很抱歉");
            battleGroundInfo.setDefeatedBy(duelName);
        }else {
            Member member = group.getOrFail(duel);
            String replayMessage = "你遭到了"+ name +"的攻击，剩余"+ duelHealth +"点生命值";
            member.sendMessage(new PlainText(replayMessage));
            replayInfo.setReplayMessage("你和"+ duelName +"展开了交战，你剩余"+ health +"点生命值");
        }
        return  replayInfo;
    }

    /**
     * 群组数据初始化
     */
    public void cleanGroupDate(MessageInfo messageInfo) {
        battleGroundGroup.remove(messageInfo.getGroupId());
        qqMsgList.remove(messageInfo.getGroupId());
        this.battleGroundMapper.deleteInfoByGroup(messageInfo.getGroupId());
    }

    /**
     * 时间格式转换得到延长的分钟
     */
    public void timeCalculate() {
        long currentTimeMillis = System.currentTimeMillis();
        String currentMinute = new SimpleDateFormat("mm").format(currentTimeMillis);
        String currentHour = new SimpleDateFormat("mm").format(currentTimeMillis);
        int Minute,hour;
        if(Integer.parseInt(currentMinute)+11>59){
            hour = Integer.parseInt(currentHour)+1;
            currentHour = String.valueOf(hour);
            if(hour>23){
                currentHour = "0";
            }
             Minute = Integer.parseInt(currentMinute) + 11 - 60;
        }else {
            Minute = Integer.parseInt(currentMinute) + 11;
        }
        currentMinute = String.valueOf(Minute);
        String timeOfDelay = ("0 "+currentMinute+" "+currentHour+" * * ?");
    }

}