package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.AdminUtil;
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.dao.LotteryMapper;
import top.strelitzia.model.LotteryInfo;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class LotteryService {

    @Autowired
    private LotteryMapper lotteryMapper;

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private NotClassifiedService not;

    private final Integer amountOfLottery = 100;//奖池数量

    @AngelinaGroup(keyWords = {"抽奖码"},description = "获取一张当月抽奖的抽奖码",sort = "娱乐功能",funcClass = "抽奖")
    public ReplayInfo getLottery(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);

        //单张奖票所需积分
        final Integer lotteryPrice = 100;
        //单人可获取抽奖码数量
        final Integer lotteryNum = 10;

        if(integralMapper.selectByQQ(messageInfo.getQq())< lotteryPrice){
            replayInfo.setReplayMessage("您的积分不足");
        }else {
            List<Integer> list = lotteryMapper.selectLotteryCode();
            if (list.size() > amountOfLottery) {
                replayInfo.setReplayMessage("本期的抽奖名额已经全部发放完毕，敬请期待");
                return replayInfo;
            }
            Integer count = lotteryMapper.selectLotteryCodeByQQ(messageInfo.getQq());
            if (count>lotteryNum){
                replayInfo.setReplayMessage("您本期已兑换"+lotteryNum+"个抽奖码了，留点机会给别人吧");
            }else {
                int id;
                do {
                    id = new Random().nextInt(amountOfLottery) + 1;
                } while (list.contains(id));
                LotteryInfo lotteryInfo = new LotteryInfo();
                lotteryInfo.setLotteryCode(id);
                lotteryInfo.setGroupId(messageInfo.getGroupId());
                lotteryInfo.setQq(messageInfo.getQq());
                lotteryInfo.setName(messageInfo.getName());
                lotteryMapper.insertInfo(lotteryInfo);
                integralMapper.reduceIntegralByGroupId(messageInfo.getGroupId(), messageInfo.getQq(), lotteryPrice);
                replayInfo.setReplayMessage("兑换抽奖码成功，抽奖码编号为" + id + "，请牢记您的抽奖码，这将是您兑换物品的唯一凭证");
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"上期抽奖结果"}, description = "查询上期抽奖的中奖人",sort = "娱乐功能",funcClass = "抽奖")
    public ReplayInfo inquireLast(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Integer code = lotteryMapper.selectInfoByDate(lastMonth()).getLotteryCode();
        if(code == null){
            replayInfo.setReplayMessage("暂无中奖结果");
        }else {
            replayInfo.setReplayMessage("上期抽奖中奖号码为"+code);
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"捐助者"}, description = "查询上期抽奖的中奖人",sort = "权限功能")
    public ReplayInfo insertBenefactor(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean admin = AdminUtil.getAdmin(replayInfo.getQq());
        if (!admin){
            replayInfo.setReplayMessage("权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1) {
            String name = messageInfo.getArgs().get(1);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
            String benefactors = lotteryMapper.selectAllBenefactorByMonth(Long.valueOf(formatter.format(new Date())));
            if (benefactors == null) {
                lotteryMapper.insertInfoToHistory(Long.valueOf(formatter.format(new Date())),name);
            }else {
                benefactors = benefactors + "/" + name;
                lotteryMapper.updateBenefactorToHistory(Long.valueOf(formatter.format(new Date())),benefactors);
            }
            replayInfo.setReplayMessage("添加成功");
        }else {
            replayInfo.setReplayMessage("还未填写捐助者的名字");
        }
        return replayInfo;
    }

    //当期抽奖程序
    public void pullLottery() {
        Integer lastCode = new SecureRandom().nextInt(amountOfLottery) + 1;
        LotteryInfo lotteryInfo = lotteryMapper.selectInfo(lastCode);
        //Integer lastCode = list.get(new Random().nextInt(list.size()));//随机在现有人里抽出一个
        if (lotteryInfo != null) {
            ReplayInfo replayInfo = new ReplayInfo();
            replayInfo.setGroupId(lotteryInfo.getGroupId());
            replayInfo.setQq(lotteryInfo.getQq());
            replayInfo.setReplayMessage("恭喜中奖，请向琴柳发送”信息填写“命令以填写收件地址，收件人，电话信息");
            sendMessageUtil.sendFriendMsg(replayInfo);
        }else {
            lotteryInfo = new LotteryInfo();
            lotteryInfo.setName("无人中奖");
            lotteryInfo.setQq(null);
            lotteryInfo.setGroupId(null);
        }
        lotteryInfo.setLotteryCode(lastCode);

        Long lastMonth = lastMonth();
        lotteryInfo.setDate(lastMonth);
        lotteryMapper.updateInfoToHistory(lotteryInfo);
        lotteryMapper.cleanAll();
        //创建一个新的字段容纳本月的捐助信息
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        lotteryMapper.insertInfoToHistory(Long.valueOf(formatter.format(new Date())),null);
        sendLotteryInfo(lotteryInfo);
    }

    //抽奖对订阅群发送消息
    private void sendLotteryInfo(LotteryInfo lotteryInfo){
        List<Long> allGroupId = lotteryMapper.selectAllGroupWithSubscribe();
        if (allGroupId.size()>0){
            ReplayInfo replayInfo = new ReplayInfo();
            replayInfo.setGroupId(allGroupId);
            StringBuilder sb = new StringBuilder();
            String benefactors = lotteryMapper.selectAllBenefactorByMonth(lotteryInfo.getDate());
            String[] split = benefactors.split("/");
            if(lotteryInfo.getName().equals("无人中奖") ){
                sb.append("各位晚上好呀，本期卡池抽奖结果已经揭晓啦")
                        .append("\n").append("很可惜，本轮没有人中奖,本期的奖池已叠加到下期，希望下期的幸运儿就是你哦");
            }else {
                sb.append("各位晚上好呀，本期卡池抽奖结果已经揭晓啦，快来看看本期的幸运儿是不是你吧")
                        .append("\n").append("本期中奖人为：").append(lotteryInfo.getName());
            }
            sb.append("\n").append("感谢");
            for (String benefactor : split) {
                sb.append(benefactor).append("、");
            }
            sb.deleteCharAt(sb.length()-1).append("对本期抽奖的大力支持");
            replayInfo.setReplayMessage(sb.toString());
            sendMessageUtil.sendGroupMsg(replayInfo);
        }
    }

    @AngelinaGroup(keyWords = "抽奖信息提醒",description = "每月抽奖信息提醒订阅",sort = "娱乐功能",funcClass = "抽奖")
    public ReplayInfo lotteryRemind(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel()<1){
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1){
            switch (messageInfo.getArgs().get(1)){
                case "开启" -> {
                    List<Long> allGroupId = lotteryMapper.selectAllGroupWithSubscribe();
                    if (allGroupId.contains(messageInfo.getGroupId())){
                        replayInfo.setReplayMessage("该群组已开启抽奖提醒");
                    }else {
                        lotteryMapper.insertGroupIdAboutSubscribe(messageInfo.getGroupId());
                        replayInfo.setReplayMessage("抽奖信息提醒开启成功！");
                    }
                }
                case "关闭" -> {
                    List<Long> allGroupId = lotteryMapper.selectAllGroupWithSubscribe();
                    if (allGroupId.contains(messageInfo.getGroupId())){
                        lotteryMapper.deleteGroupIdAboutSubscribe(messageInfo.getGroupId());
                        replayInfo.setReplayMessage("抽奖信息提醒关闭成功！");
                    }else {
                        replayInfo.setReplayMessage("该群组还未开启抽奖提醒");
                    }
                }
                default -> replayInfo.setReplayMessage("无效功能命令，请输入”开启“或”关闭“");
            }
        }else {
            replayInfo.setReplayMessage("请输入需要操作的功能");
        }
        return replayInfo;
    }

    private boolean info = false;

    @AngelinaFriend(keyWords = {"信息填写"})
    public ReplayInfo fillInformation(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Long lastMonth = lastMonth();
        LotteryInfo lotteryInfo = lotteryMapper.selectInfoByDate(lastMonth);
        if(!lotteryInfo.getVerify()) {
            if (info) {
                replayInfo.setReplayMessage("请勿重复开启");
                return replayInfo;
            } else {
                info = true;
            }
            if (lotteryInfo.getQq().equals(messageInfo.getQq())) {
                replayInfo.setReplayMessage("恭喜中奖，请将收件地址，收件人，收件电话这三项信息发送给琴柳，确认无误后发送确认即可");
                sendMessageUtil.sendFriendMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                String s = "空";
                boolean finish = true;
                while (finish) {
                    AngelinaListener angelinaListener = new AngelinaListener() {
                        @Override
                        public boolean callback(MessageInfo message) {
                            return !message.getText().equals("信息填写");
                        }
                    };
                    angelinaListener.setSecond(120);
                    MessageInfo callback = AngelinaEventSource.waiter2(angelinaListener).getMessageInfo();
                    if (callback == null) {
                        info = false;
                        replayInfo.setReplayMessage("填写已超时");
                        return replayInfo;
                    }
                    if (callback.getText().equals("确认")) {//编辑信息发送给号主
                        replayInfo.setReplayMessage("当期中奖人填写的信息为" + s);
                        replayInfo.setQq(Long.valueOf(not.QQSetInstance()));
                        Iterator<Long> it = MiraiFrameUtil.messageIdMap.values().iterator();
                        replayInfo.setLoginQQ(it.next());
                        sendMessageUtil.sendFriendMsg(replayInfo);
                        //重新编辑回复信息发送给中奖人
                        replayInfo.setReplayMessage(null);
                        replayInfo.setQq(messageInfo.getQq());
                        replayInfo.setLoginQQ(messageInfo.getLoginQq());
                        replayInfo.setReplayMessage("信息已确认并发送");
                        lotteryMapper.updateLastVerify();
                        info = false;
                        finish = false;
                    } else {
                        replayInfo.setReplayMessage("您填写的信息为：" +
                                "\n" + callback.getText() +
                                "\n请您确认信息是否填写正确，确认无误后输入确认信息将会发送并锁定" +
                                "\n如需更改，直接重新编辑并回复即可");
                        sendMessageUtil.sendFriendMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        s = callback.getText();
                    }
                }
            }
        }
        return replayInfo;
    }

    private Long lastMonth(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        Calendar calendar = Calendar.getInstance();//创建一个日历对象
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -1);
        return Long.valueOf(formatter.format(calendar.getTime()));
    }
}
