package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.dao.RedPacketMapper;
import top.strelitzia.model.RedPacketInfo;

import java.util.*;

@Slf4j
@Service
public class RedPacketService {

    @Autowired
    private RedPacketMapper redPacketMapper;

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    //ID对应的红包map
    private final Map<Integer, List<Integer>> idOfRedPacket = new HashMap<>();

    //存放每个红包ID抽过的QQ号map
    private final Map<Integer, List<Long>> idOfQQ = new HashMap<>();

    //红包的时序对应map
    private final Map<Integer,Long> timeOfId =new HashMap<>();

    @AngelinaGroup(keyWords = {"积分红包"}, description = "发放一定数量的积分红包", sort = "娱乐功能",funcClass = "积分红包")
    public ReplayInfo integerPacket(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>2){
            if (messageInfo.getArgs().get(1).matches("^[1-9]\\d*$")&&messageInfo.getArgs().get(2).matches("^[1-9]\\d*$")){
                int integral = Integer.parseInt(messageInfo.getArgs().get(2));
                if(this.integralMapper.selectByQQ(messageInfo.getQq())<integral){
                    replayInfo.setReplayMessage("您的积分不足以支付红包，请重新输入");
                    return replayInfo;
                }
                int num = Integer.parseInt(messageInfo.getArgs().get(1));
                if (integral/num<1){
                    replayInfo.setReplayMessage("错误，有空红包存在，请重新核对后输入");
                    return replayInfo;
                }
                List<Integer> listOfDistribution = getRandomInteger(num,integral);
                int id;
                do{
                    id = new Random().nextInt(4096);
                }while (idOfRedPacket.containsKey(id));
                //写入内存
                idOfRedPacket.put(id,listOfDistribution);
                idOfQQ.put(id,new ArrayList<>());
                RedPacketInfo redPacketInfo = new RedPacketInfo();
                redPacketInfo.setGroupId(messageInfo.getGroupId());
                redPacketInfo.setQQ(messageInfo.getQq());
                redPacketInfo.setId(id);
                redPacketInfo.setRemain(integral);
                redPacketMapper.insectRemain(redPacketInfo);
                integralMapper.reduceIntegralByGroupId(messageInfo.getGroupId(),messageInfo.getQq(),integral);
                replayInfo.setReplayMessage("红包创建完毕,红包编号为"+id+"，一共"+num+"个，请输入对应编号领取");
                if (timeOfId.isEmpty()){
                    timeOfId.put(id,System.currentTimeMillis());//从创建时开始记录时间
                    thread();
                }else {
                    timeOfId.put(id,System.currentTimeMillis());//从创建时开始记录时间
                }
            }else {
                replayInfo.setReplayMessage("请输入一个正整数的红包个数和总积分");
            }
        }else {
            replayInfo.setReplayMessage("请告知红包个数与总积分,先输入个数后输入积分，注意用空格间隔");
        }
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"红包","开红包","抢红包"}, description = "拆开指定编号的红包", sort = "娱乐功能",funcClass = "积分红包")
    public ReplayInfo openPacket(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1&&messageInfo.getArgs().get(1).matches("^[1-9]\\d*$")){
            Integer id = Integer.valueOf(messageInfo.getArgs().get(1));
            if(redPacketMapper.selectID(messageInfo.getGroupId(),id)>0){
                if(idOfRedPacket.containsKey(id)){
                    List<Long> QQList = idOfQQ.get(id);
                    if (QQList.contains(messageInfo.getQq())){
                        replayInfo.setReplayMessage("您已经抽过这个红包了呢，换个别的红包试试吧");
                        return replayInfo;
                    }else {
                        QQList.add(messageInfo.getQq());
                        idOfQQ.put(id,QQList);
                    }
                    List<Integer> list = idOfRedPacket.get(id);
                    Integer integral = list.get(0);
                    list.remove(0);
                    if (list.isEmpty()){
                        idOfQQ.remove(id);
                        idOfRedPacket.remove(id);
                        timeOfId.remove(id);
                        integralMapper.increaseIntegralByGroupId(messageInfo.getGroupId(),messageInfo.getQq(),integral);
                        replayInfo.setReplayMessage("恭喜您抽到了最后一个红包，获得了"+integral+"点积分");
                        redPacketMapper.deleteRemain(id);
                    }else {
                        idOfRedPacket.put(id,list);
                        integralMapper.increaseIntegralByGroupId(messageInfo.getGroupId(),messageInfo.getQq(),integral);
                        replayInfo.setReplayMessage("恭喜你抽到了一个红包，获得了"+integral+"点积分");
                        RedPacketInfo redPacketInfo = redPacketMapper.selectInfo(id);
                        redPacketMapper.updateRemain(redPacketInfo.getRemain()-integral,id);
                    }
                }else {
                    replayInfo.setReplayMessage("故障！红包信息丢失，请联系超级管理员");
                }
            }else {
                replayInfo.setReplayMessage("该红包id不存在，请输入正确的红包ID");
            }
        }else {
            replayInfo.setReplayMessage("请输入正确的红包编号");
        }
        return replayInfo;
    }

    private void thread(){
        new Thread(() -> {
            while(!timeOfId.isEmpty()) {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                //循环遍历红包池，移除超时红包
                for (Iterator<Integer> it = timeOfId.keySet().iterator();it.hasNext();){
                    Integer id = it.next();
                    Long time = timeOfId.get(id);
                    if ((System.currentTimeMillis() - time) / 1000 > 300) {
                        it.remove();
                        idOfQQ.remove(id);
                        idOfRedPacket.remove(id);
                        RedPacketInfo redPacketInfo = redPacketMapper.selectInfo(id);
                        redPacketMapper.deleteRemain(id);
                        integralMapper.increaseIntegralByGroupId(redPacketInfo.getGroupId(),redPacketInfo.getQQ(),redPacketInfo.getRemain());
                        ReplayInfo replayInfo = new ReplayInfo();
                        replayInfo.setReplayMessage("ID为" + id + "的红包已发出超过五分钟，即将自动清理，所有红包剩余积分将会返还");
                        replayInfo.setGroupId(redPacketInfo.getGroupId());
                        sendMessageUtil.sendGroupMsg(replayInfo);
                    }
                }
            }
        }).start();
    }

    //月底清零
    public void clean(){
        idOfQQ.clear();
        timeOfId.clear();
        idOfRedPacket.clear();
        redPacketMapper.deleteAllRemain();
    }

    private List<Integer> getRandomInteger(int num,int inergral){
        int remain = inergral - num;
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < num; i++){
            int redPacket;
            if(i == num-1){
                redPacket = remain;
            }else {
                int a = (int) ((2L *remain)/(num-i));
                redPacket = new Random().nextInt(a);
            }
            if(remain>redPacket){
                remain = remain-redPacket;
            }else {
                remain = 0;
            }
            list.add(redPacket+1);
        }
        return list;
    }
}
