package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.SendMessageUtil;

import java.util.HashSet;
import java.util.Set;

//@Service
public class drawAndGuessService {

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private final Set<Long> groupList = new HashSet<>();

    //@AngelinaGroup(keyWords = {"你画我猜"},description = "你画我猜游戏")
    public ReplayInfo drawAndGuess(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(groupList.contains(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("当前群组已有你画我猜正在进行，请等待结束后再发起");
            return replayInfo;
        }
        groupList.add(messageInfo.getGroupId());
        replayInfo.setReplayMessage(messageInfo.getName()+"发起游戏你画我猜成功，请私聊琴柳，先发送“答案□XX（答案内容）”，再发送图片");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        boolean right = false,image = false;
        String answer = null;
        while (!right) {
            boolean finalImage = image;
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    if(finalImage){
                        return message.getGroupId().equals(messageInfo.getGroupId()) &&
                                message.getQq().equals(messageInfo.getQq()) &&
                                message.getImgUrlList() != null;
                    }else {
                        boolean reply ;
                        try{
                            reply = message.getGroupId().equals(messageInfo.getGroupId()) &&
                                    message.getQq().equals(messageInfo.getQq()) &&
                                    (message.getArgs().get(1) != null && message.getArgs().get(0).equals("答案"));
                        }catch (NullPointerException e){
                            reply = false;
                        }
                        return reply;
                    }
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            MessageInfo recall = AngelinaEventSource.waiter2(angelinaListener).getMessageInfo();
            ReplayInfo replayInfo1 = new ReplayInfo(messageInfo);
            if (recall == null) {
                replayInfo.setReplayMessage("猜图发布超时，你画我猜已关闭");
                groupList.remove(messageInfo.getGroupId());
                return replayInfo;
            }
            if (!image){
                replayInfo1.setReplayMessage("答案设置正确，请发送题目图片");
                answer = recall.getArgs().get(1);
                image = true;
            }else {
                if (recall.getImgUrlList().size() > 2) {
                    replayInfo1.setReplayMessage("图片太多了，只能发送一张哦");
                } else {
                    replayInfo1.setReplayMessage("图片发布正确，正在向群里发布图片");
                    replayInfo.setReplayImg(recall.getImgUrlList().get(0));
                    right = true;
                }
            }
            sendMessageUtil.sendFriendMsg(replayInfo1);
            replayInfo1.setReplayMessage(null);
        }
        replayInfo.setReplayMessage("感谢"+messageInfo.getName()+"为我们提供的题目，请各位根据下图，答出你心目中的答案吧");
        sendMessageUtil.sendGroupMsg(replayInfo);
        replayInfo.setReplayMessage(null);
        replayInfo.getReplayImg().clear();
        right = false;
        while (!right){
            AngelinaListener angelinaListener = new AngelinaListener() {
                @Override
                public boolean callback(MessageInfo message) {
                    boolean reply;
                    try{
                       reply = message.getGroupId().equals(messageInfo.getGroupId())&&
                               message.getArgs().get(0).equals("我猜");
                    }catch (NullPointerException e){
                        reply = false;
                    }
                    return reply;
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(120);
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall==null){
                replayInfo.setReplayMessage("这么长时间还是没人猜出来呢，答案是"+answer+"哦");
                groupList.remove(messageInfo.getGroupId());
                return replayInfo;
            }
            if(recall.getArgs().size()>1){
                if (recall.getArgs().get(1).equals(answer)){
                    replayInfo.setReplayMessage("恭喜回答正确");
                    groupList.remove(messageInfo.getGroupId());
                    right = true;
                }else {
                    replayInfo.setReplayMessage("不对哦，再猜猜");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                }
            }else {
                replayInfo.setReplayMessage("？？？");
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setReplayMessage(null);
            }
        }
        return replayInfo;
    }
}
