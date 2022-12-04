package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.PersonBlackBoxInfo;

import java.util.*;

@Slf4j
@Service
public class PersonalBlackBox {

    @Autowired
    SendMessageUtil sendMessageUtil;

    @Autowired
    IntegralMapper integralMapper;

    private final Map<String,Long> timeOfGroup =new HashMap<>();

    private final Map<String, PersonBlackBoxInfo> boxList = new HashMap<>();

    /**
     * 创建卡池并录取卡池信息，随后创建子线程以查询，符合条件则删除卡池
     * 每次创建卡池需要扣除五分积分
     */
    @AngelinaGroup(keyWords = {"创建卡池"},description = "新建一个自定义卡池", sort = "娱乐功能",funcClass = "自定义卡池")
    public ReplayInfo createBox(MessageInfo messageInfo){
        ReplayInfo replayInfo =new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            String boxName =messageInfo.getArgs().get(1);
            if (boxList.containsKey(boxName)){
                replayInfo.setReplayMessage("您要创建的卡池名已经存在了，换一个名字吧");
            }
            //查询积分够不够开启卡池，足够则扣五分
            Integer integral = this.integralMapper.selectByQQ(messageInfo.getQq());
            if(integral==null){
                replayInfo.setReplayMessage("您还没有积分呢，试着参与活动以获取积分吧");
            }else if(integral<5){
                replayInfo.setReplayMessage("您的积分不足以开启活动，多多争取吧");
            }else {
                integral = integral - 5;
                this.integralMapper.integralByGroupId(messageInfo.getGroupId(),messageInfo.getName(),messageInfo.getQq(),integral);
                List<String> cardList = new ArrayList<>();
                replayInfo.setReplayMessage("卡池创建成功，请牢记您的卡池名，然后录入卡片信息");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
                PersonBlackBoxInfo boxInfo = new PersonBlackBoxInfo();
                //循环以记录预读卡牌信息
                while(true) {
                    AngelinaListener angelinaListener = new AngelinaListener() {
                        @Override
                        public boolean callback(MessageInfo message) {
                            return message.getQq().equals(messageInfo.getQq()) &&
                                    message.getGroupId().equals(messageInfo.getGroupId());
                        }
                    };
                    angelinaListener.setGroupId(messageInfo.getGroupId());
                    angelinaListener.setSecond(30);
                    MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
                    if (recall == null) {
                        replayInfo.setReplayMessage("回复时间超时，卡池停止收录，卡池创建完成");
                        break;
                    }
                    String name = recall.getText();
                    if(name.equals("创建完成")){
                        replayInfo.setReplayMessage("卡池停止接收输入，创建完成");
                        break;
                    }else {
                        replayInfo.setReplayMessage("收录成功，请继续输入");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        cardList.add(name);
                    }
                }
                boxInfo.setCardList(cardList);
                boxInfo.setGroupId(messageInfo.getGroupId());
                boxInfo.setQQ(messageInfo.getQq());
                boxList.put(boxName,boxInfo);
                timeOfGroup.put(boxName,System.currentTimeMillis());//从创建时开始记录时间
                //开启子线程，十秒判断一次，当超时或者开关关闭时停止循环
                new Thread(() -> {
                    boolean timeSwitch = true;
                    try {
                        while(timeSwitch){
                            try {
                                Thread.sleep(10000);
                            }catch (Exception e){
                                log.error(e.getMessage());
                            }
                            if ( boxList.get(boxName).isExit() ){
                                throw new InterruptedException();
                            }else {
                                //超时停止并关闭
                                Long time = timeOfGroup.get(boxName);
                                if((System.currentTimeMillis() - time)/1000 > 120){
                                    replayInfo.setReplayMessage("博士，您的"+boxName+"卡池已经太久没人使用了，为了腾出空间，我已经拿去销毁掉了");
                                    sendMessageUtil.sendGroupMsg(replayInfo);
                                    replayInfo.setReplayMessage(null);
                                    timeSwitch = false;
                                    boxList.remove(boxName);
                                    timeOfGroup.remove(boxName);
                                }
                            }
                        }
                    }catch (InterruptedException e){
                        boxList.remove(boxName);
                        timeOfGroup.remove(boxName);
                        replayInfo.setReplayMessage("博士，您要我销毁的"+boxName+"卡池已经粉碎好了");
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setReplayMessage(null);
                        log.info(e.getMessage()+boxName+"的线程已结束");
                    }
                }).start();
            }
        }else {
            replayInfo.setReplayMessage("烦请告知一下您要创建的新卡池名字呢");
        }
        return replayInfo;
    }

    /**
     * 抽卡需要判断条件，有卡池，本群组，每次抽卡完毕需要记录最后抽卡的时间
     */
    @AngelinaGroup(keyWords = {"抽卡"},description = "抽取自定义的卡池", sort = "娱乐功能",funcClass = "自定义卡池")
    public ReplayInfo drawCard(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            String boxName = messageInfo.getArgs().get(1);
            if (boxList.containsKey(boxName)){
                PersonBlackBoxInfo boxInfo = boxList.get(boxName);
                if(boxInfo.getGroupId().equals(messageInfo.getGroupId())){
                    List<String> cardList = boxInfo.getCardList();
                    int i = new Random().nextInt(cardList.size());
                    replayInfo.setReplayMessage("您抽到的卡片信息为：" + cardList.get(i));
                    //更新最后抽卡时间
                    timeOfGroup.put(boxName,System.currentTimeMillis());
                }else replayInfo.setReplayMessage("您不能使用这个卡池呢，换一个试试吧");
            }else replayInfo.setReplayMessage("对不起博士，我没找到这个名字的卡池呢");
        }else replayInfo.setReplayMessage("请告知您要抽取的卡池名字");
        return replayInfo;
    }

    /**
     * 销毁卡池需要满足条件，有卡池，本人创建，每次主动销毁给销毁人奖励性加分两分
     */
    @AngelinaGroup(keyWords = {"销毁卡池"},description = "销毁固定的卡池", sort = "娱乐功能",funcClass = "自定义卡池")
    public ReplayInfo deleteBox(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            String boxName = messageInfo.getArgs().get(1);
            if (boxList.containsKey(boxName)){
                PersonBlackBoxInfo boxInfo = boxList.get(boxName);
                if(boxInfo.getGroupId().equals(messageInfo.getGroupId())){
                    if(boxInfo.getQQ().equals(messageInfo.getQq())){
                        boxInfo.setExit(true);
                        boxList.put(boxName,boxInfo);
                        replayInfo.setReplayMessage("好的博士，放在那里吧，我把手上的文件整理完就去");
                        //奖励加分
                        Integer integral = this.integralMapper.selectByQQ(messageInfo.getQq());
                        try{integral = integral + 2;
                        }catch (NullPointerException e){
                            integral = 2;
                        }
                        this.integralMapper.integralByGroupId(messageInfo.getGroupId(),messageInfo.getName(),messageInfo.getQq(),integral);
                    }else replayInfo.setReplayMessage("抱歉博士，这个卡池不是您的呢，麻烦通知创建人吧");
                }else replayInfo.setReplayMessage("博士，这里没有你要找的这个卡池呢，会不会在别的地方");
            }else replayInfo.setReplayMessage("对不起博士，我没找到这个名字的卡池呢");
        }else replayInfo.setReplayMessage("请告知您要销毁的卡池名字");
        return replayInfo;
    }

    /**
     * 添加卡片需要满足条件，有卡池，本群组，本人创建，添加卡片需要同时告知卡池和卡牌信息
     */
    @AngelinaGroup(keyWords = {"添加卡片"},description = "给已知卡池添加一张新的卡片", sort = "娱乐功能",funcClass = "自定义卡池")
    public ReplayInfo addBox(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            String boxName = messageInfo.getArgs().get(1);
            if (boxList.containsKey(boxName)){
                PersonBlackBoxInfo boxInfo = boxList.get(boxName);
                if(boxInfo.getGroupId().equals(messageInfo.getGroupId())){
                    if(boxInfo.getQQ().equals(messageInfo.getQq())){
                        if (messageInfo.getArgs().size()>2){
                            String card = messageInfo.getArgs().get(2);
                            List<String> cardList = boxInfo.getCardList();
                            cardList.add(card);
                            boxInfo.setCardList(cardList);
                            boxList.put(boxName,boxInfo);
                        }else replayInfo.setReplayMessage("请告诉我您要添加的卡片信息");
                    }else replayInfo.setReplayMessage("抱歉博士，这个卡池不是您的呢，麻烦通知创建人吧");
                }else replayInfo.setReplayMessage("博士，这里没有你要找的这个卡池呢，会不会在别的地方");
            }else replayInfo.setReplayMessage("对不起博士，我没找到这个名字的卡池呢");
        }else replayInfo.setReplayMessage("请告诉我您要执行添加的卡池名字");
        return replayInfo;
    }

    /**
     * 删除卡片需要满足条件，有卡池，本群组，本人创建，添加卡片需要同时告知卡池和卡牌信息
     */
    @AngelinaGroup(keyWords = {"删除卡片"},description = "给已知卡池删除一张新的卡片", sort = "娱乐功能",funcClass = "自定义卡池")
    public ReplayInfo removeBox(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            String boxName = messageInfo.getArgs().get(1);
            if (boxList.containsKey(boxName)){
                PersonBlackBoxInfo boxInfo = boxList.get(boxName);
                if(boxInfo.getGroupId().equals(messageInfo.getGroupId())){
                    if(boxInfo.getQQ().equals(messageInfo.getQq())){
                        if (messageInfo.getArgs().size()>2){
                            String card = messageInfo.getArgs().get(2);
                            List<String> cardList = boxInfo.getCardList();
                            if(cardList.contains(card)){
                                cardList.remove(card);
                                boxInfo.setCardList(cardList);
                                boxList.put(boxName,boxInfo);
                            }else replayInfo.setReplayMessage("卡池里没有您要删除的卡片信息呢");
                        }else replayInfo.setReplayMessage("请告诉我您要删除的卡片信息");
                    }else replayInfo.setReplayMessage("抱歉博士，这个卡池不是您的呢，麻烦通知创建人吧");
                }else replayInfo.setReplayMessage("博士，这里没有你要找的这个卡池呢，会不会在别的地方");
            }else replayInfo.setReplayMessage("对不起博士，我没找到这个名字的卡池呢");
        }else replayInfo.setReplayMessage("请告诉我您要执行删除的卡池名字");
        return replayInfo;
    }

}
