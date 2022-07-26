package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.util.AudioUtil;

import java.io.File;
import java.text.SimpleDateFormat;

@Service
@Slf4j
public class test {

    @AngelinaGroup(keyWords = {"戳他"}, description = "测试")
    public ReplayInfo chuoMe(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Long a = null;
        if (messageInfo.getArgs().size() > 1) {
            a = Long.valueOf(messageInfo.getArgs().get(1));
        }
        replayInfo.setNudged(a);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"禁言自己"}, description = "测试")
    public ReplayInfo nudeSelf(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("臣妾做不到啊");
        return replayInfo;
    }

    //@AngelinaGroup(keyWords = {"呼叫"}, description = "测试")
    public ReplayInfo hujiao(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setReplayMessage("杰哥今天寄了，琴柳在执勤，可以呼叫琴柳和稀音哦");
        return replayInfo;
    }

    //@AngelinaGroup(keyWords = {"转换"}, description = "测试")
    public ReplayInfo changeFormat(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        File sourceFile = new File("runFile/Audio/source.mp3");//输入
        File targetFile = new File("runFile/Audio/target.amr");//输出
        AudioUtil.amrToMp3(sourceFile, targetFile);//转换
        replayInfo.setReplayMessage("转换完成");
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"全体禁言"}, description = "测试")
    public ReplayInfo muteAll(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setMutedAll(true);
        replayInfo.setReplayMessage("禁言开启");
        return replayInfo;
    }
    @AngelinaGroup(keyWords = {"权限查询"}, description = "测试")
    public ReplayInfo botPermission(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        replayInfo.setPermission(true);
        return replayInfo;
    }

    //@AngelinaGroup(keyWords = {"时间转换"}, description = "测试")
    public ReplayInfo timeChangeFormit(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = new SimpleDateFormat("ss").format(currentTimeMillis);
        replayInfo.setReplayMessage(currentTime);
        return replayInfo;
    }
}


