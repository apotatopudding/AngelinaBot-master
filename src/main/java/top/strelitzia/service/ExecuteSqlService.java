package top.strelitzia.service;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.AdminUserMapper;
import top.strelitzia.dao.ExecuteSqlMapper;
import top.strelitzia.dao.UserFoundMapper;
import top.strelitzia.model.AdminUserInfo;
import top.strelitzia.util.AdminUtil;

import java.util.List;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/

@Slf4j
@Service
public class ExecuteSqlService {

    @Autowired
    private ExecuteSqlMapper executeSqlMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private MiraiFrameUtil miraiFrameUtil;


    @AngelinaGroup(keyWords = {"sql", "SQL"})
    public ReplayInfo ExecuteSql(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            List<String> text = messageInfo.getArgs();
            List<AdminUserInfo> admins = adminUserMapper.selectAllAdmin();
            boolean b = AdminUtil.getSqlAdmin(messageInfo.getQq(), admins);
            String s = "您没有sql权限";
            if (b) {
                StringBuilder sql = new StringBuilder();
                for (int i= 1; i < text.size(); i++) {
                     sql.append(" ").append(text.get(i));
                }
                s = executeSqlMapper.executeSql(sql.toString()).toString();
            }
            replayInfo.setReplayMessage(s);
        } else {
            replayInfo.setReplayMessage("请输入sql语句");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"群发消息"})
    @AngelinaFriend(keyWords = {"群发消息"})
    public ReplayInfo sendGroupMessage(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<AdminUserInfo> admins = adminUserMapper.selectAllAdmin();
        if (AdminUtil.getSqlAdmin(messageInfo.getQq(), admins)) {
            if (messageInfo.getArgs().size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < messageInfo.getArgs().size(); i++) {
                    sb.append(messageInfo.getArgs().get(i));
                }
                replayInfo.setReplayMessage(sb.toString());
            }
            for (Long groupId : miraiFrameUtil.BotGroupMap().keySet()) {
                List<String> imgUrlList = messageInfo.getImgUrlList();
                for (String url: imgUrlList) {
                    replayInfo.setReplayImg(url);
                    replayInfo.setRecallTime(110);
                }
                replayInfo.setGroupId(groupId);
                replayInfo.setLoginQQ(MiraiFrameUtil.messageIdMap.get(groupId));
                sendMessageUtil.sendGroupMsg(replayInfo);
                replayInfo.setRecallTime(null);
                replayInfo.getReplayImg().clear();
                try{
                    Thread.sleep(10000);
                }catch (InterruptedException e){
                    log.error(e.toString());
                }
            }
            replayInfo.getReplayImg().clear();
            replayInfo.setReplayMessage(null);
            replayInfo.getReplayImg().clear();
            return replayInfo;
        }else {
            replayInfo.setReplayMessage("您没有群发消息权限");
        }
        return replayInfo;
    }

}
