package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.strelitzia.dao.TarotCardMapper;
import top.strelitzia.model.TarotCardInfo;

import java.io.IOException;
import java.util.List;

@Service
public class TarotExplainService {

    @Autowired
    private TarotCardMapper tarotCardMapper;


    @AngelinaFriend(keyWords = {"远山小姐解牌"}, description = "查询塔罗牌解牌信息")
    public ReplayInfo getOperatorTarot(MessageInfo messageInfo) throws IOException {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String tarotCardId = messageInfo.getArgs().get(1);
            List<TarotCardInfo> tarotCardInfos = tarotCardMapper.selectTarotCardByID(tarotCardId);
            if (tarotCardInfos.size() > 0) {
                TextLine textLine = new TextLine(100);
                for (TarotCardInfo tarotCardInfo:tarotCardInfos) {
                    textLine.addString("您咨询的卡牌为：" + tarotCardId);
                    textLine.nextLine();
                    textLine.addString("牌义：");
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardMean1());
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardMean2());
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardMean3());
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardMean4());
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardMean5());
                    textLine.nextLine();
                    textLine.addString("关键语：");
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardKeyword());
                    textLine.nextLine();
                    textLine.addString("正位含义为：");
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardForwardPosition());
                    textLine.nextLine();
                    textLine.addString("反位含义为：");
                    textLine.nextLine();
                    textLine.addString("" + tarotCardInfo.getTarotCardReversePosition());
                    textLine.nextLine();
                }
                replayInfo.setReplayImg(textLine.drawImage());
                return replayInfo;
            }else{
                replayInfo.setReplayMessage("您这可难倒我了，您说的这个卡牌名字我都不知道呢，您看看您是不是说错了");
            }
        }else {
            replayInfo.setReplayMessage("还请您告知我您需要咨询的卡牌，我还不至于能占卜出您抽的那张牌呢");
        }
        return  replayInfo;
    }
}
