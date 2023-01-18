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
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.dao.LotteryMapper;
import top.strelitzia.model.LotteryInfo;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

    private final Integer amountOfLottery = 4096;

    @AngelinaGroup(keyWords = {"抽奖码"},description = "获取一张当月抽奖的抽奖码",sort = "娱乐功能",funcClass = "抽奖")
    public ReplayInfo getLottery(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(integralMapper.selectByQQ(messageInfo.getQq())<20){
            replayInfo.setReplayMessage("您的积分不足");
        }else {
            List<Integer> list = lotteryMapper.selectLotteryCode();
            if (list.size() > amountOfLottery) {
                replayInfo.setReplayMessage("本期的抽奖名额已经全部发放完毕，敬请期待");
                return replayInfo;
            }
            Integer count = lotteryMapper.selectLotteryCodeByQQ(messageInfo.getQq());
            if (count>49){
                replayInfo.setReplayMessage("您本期已兑换五十个抽奖码，留点机会给别人吧");
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
                integralMapper.reduceIntegralByGroupId(messageInfo.getGroupId(), messageInfo.getQq(), 20);
                replayInfo.setReplayMessage("兑换抽奖码成功，抽奖码编号为" + Integer.toHexString(id) + "，请牢记您的抽奖码，这将是您兑换物品的唯一凭证");
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"上期抽奖结果"}, description = "查询上期抽奖的中奖人",sort = "娱乐功能",funcClass = "抽奖")
    public ReplayInfo inquireLast(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Integer code =lotteryMapper.selectLastCode();
        if(code==null){
            replayInfo.setReplayMessage("暂无中奖结果");
        }else {
            replayInfo.setReplayMessage("上期抽奖中奖号码为"+Integer.toHexString(code));
        }
        return replayInfo;
    }


    //当期抽奖程序
    public void pullLottery(){
        List<Integer> list = lotteryMapper.selectLotteryCode();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        Date date = new Date(System.currentTimeMillis());
        Long today = Long.valueOf(formatter.format(date));
        if (list.size()==0){
            lotteryMapper.insertInfoToHistory(today, null, null, "无人中奖");
            lotteryMapper.cleanAll();
        }else {
            //Integer lastCode = list.get(new Random().nextInt(list.size()));//随机在现有人里抽出一个
            Integer lastCode = new SecureRandom().nextInt(amountOfLottery)+1;
            LotteryInfo lotteryInfo = lotteryMapper.selectInfo(lastCode);
            if (lotteryInfo == null){
                lotteryMapper.insertInfoToHistory(today, lastCode, null, "无人中奖");
                lotteryMapper.cleanAll();
            }else {
                lotteryMapper.insertInfoToHistory(today, lastCode, lotteryInfo.getQq(), lotteryInfo.getName());
                lotteryMapper.cleanAll();
                ReplayInfo replayInfo = new ReplayInfo();
                replayInfo.setGroupId(lotteryInfo.getGroupId());
                replayInfo.setQq(lotteryInfo.getQq());
                replayInfo.setReplayMessage("恭喜中奖，请对琴柳发送”信息填写“命令以填写收件地址，收件人，电话信息");
                sendMessageUtil.sendGroupTempMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
    }

    private boolean info = false;

    @AngelinaFriend(keyWords = {"信息填写"})
    public ReplayInfo fillInformation(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(!lotteryMapper.selectLastVerify().equals(1)) {
            if (info) {
                replayInfo.setReplayMessage("请勿重复开启");
                return replayInfo;
            } else {
                info = true;
            }
            if (lotteryMapper.selectLastQQ().equals(messageInfo.getQq())) {
                replayInfo.setReplayMessage("恭喜中奖，请将收件地址，收件人，收件电话这三项信息发送给琴柳，确认无误后发送确认即可");
                sendMessageUtil.sendGroupTempMsg(replayInfo);
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
                    if (callback.getText().equals("确认")) {
                        replayInfo.setReplayMessage("当期中奖人填写的信息为" + s);
                        replayInfo.setQq(Long.valueOf(not.QQSetInstance()));
                        Iterator<Long> it = MiraiFrameUtil.messageIdMap.values().iterator();
                        replayInfo.setLoginQQ(it.next());
                        sendMessageUtil.sendFriendMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        replayInfo.setQq(messageInfo.getQq());
                        replayInfo.setLoginQQ(messageInfo.getLoginQq());
                        replayInfo.setReplayMessage("信息已确认并发送");
                        lotteryMapper.updateLastVerify();
                        info = false;
                        finish = false;
                    } else {
                        replayInfo.setReplayMessage("您填写的信息为：\n" + callback.getText() + "\n请您确认信息是否填写正确，确认无误后信息将会发送");
                        sendMessageUtil.sendGroupTempMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        s = callback.getText();
                    }
                }
            }
        }
        return replayInfo;
    }
}
