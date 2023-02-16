package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.dao.BirthdayRemindMapper;
import top.strelitzia.dao.LookWorldMapper;
import top.strelitzia.dao.NickNameMapper;

import java.util.List;

@Service
public class RemindService {

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private BirthdayRemindMapper birthdayRemindMapper;

    @Autowired
    private NickNameMapper nickNameMapper;

    @Autowired
    private LookWorldMapper lookWorldMapper;

    @AngelinaGroup(keyWords = {"生日提醒"}, description = "给当前群组添加或移除一位指定干员的生日提醒", sort = "订阅功能",funcClass = "干员生日提醒")
    public ReplayInfo subscribeBirthday(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel()<1){
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1) {
            if (messageInfo.getArgs().size() > 2) {
                switch (messageInfo.getArgs().get(1)){
                    case "添加" -> {
                        List<String> AllOperator = operatorInfoMapper.getAllOperator();
                        String name = messageInfo.getArgs().get(2);
                        String realName = nickNameMapper.selectNameByNickName(name);
                        if (realName != null && !realName.equals("")) {
                            name = realName;
                        }
                        if (AllOperator.contains(name)) {
                            List<String> nameByGroupId = birthdayRemindMapper.selectNameByGroupId(messageInfo.getGroupId());
                            if (nameByGroupId != null && nameByGroupId.contains(name)) {
                                replayInfo.setReplayMessage("博士，您已经为这位干员设有生日提醒了");
                            } else {
                                birthdayRemindMapper.insertBirthdayRemind(messageInfo.getGroupId(), name);
                                replayInfo.setReplayMessage(name + "生日提醒添加成功！");
                            }
                        } else {
                            replayInfo.setReplayMessage("这个名字我查不到呢，再检查一下吧");
                        }
                    }
                    case "取消" -> {
                        List<String> AllOperator = operatorInfoMapper.getAllOperator();
                        String name = messageInfo.getArgs().get(2);
                        String realName = nickNameMapper.selectNameByNickName(name);
                        if (realName != null && !realName.equals("")) {
                            name = realName;
                        }
                        if (AllOperator.contains(name)) {
                            List<String> nameByGroupId = birthdayRemindMapper.selectNameByGroupId(messageInfo.getGroupId());
                            if (nameByGroupId != null && nameByGroupId.contains(name)){
                                birthdayRemindMapper.deleteBirthdayRemind(messageInfo.getGroupId(),name);
                                replayInfo.setReplayMessage( name + "生日提醒取消成功");
                            }else {
                                replayInfo.setReplayMessage("博士，您还没有为这位干员设置生日提醒呢");
                            }
                        } else {
                            replayInfo.setReplayMessage("这个名字我查不到呢，再检查一下吧");
                        }
                    }
                    default -> replayInfo.setReplayMessage("博士，"+messageInfo.getBotName()+"不理解您的意思，您可以说“添加”或者“取消”");
                }
            } else {
                replayInfo.setReplayMessage("博士，请告诉我您想要调整的干员名字呢");
            }
        }else {
            replayInfo.setReplayMessage("博士，您还没有告诉我要做什么呢");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"生日提醒列表"},description = "查看群组内已添加的生日列表", sort = "订阅功能",funcClass = "干员生日提醒")
    public ReplayInfo listOfBirthday(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<String> nameByGroupId = birthdayRemindMapper.selectNameByGroupId(messageInfo.getGroupId());
        StringBuilder s = new StringBuilder().append("当前群组已添加的生日提醒干员有：\n");
        int i = 0;
        for(String name : nameByGroupId){
            s.append(name);
            i++;
            if (i==5){
                s.append("\n");
                i=0;
            }else s.append("   ");
        }
        replayInfo.setReplayMessage(s.toString());
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"每日看世界订阅"}, description = "为当前群组开启或关闭每日看世界订阅", sort = "订阅功能",funcClass = "每日看世界")
    public ReplayInfo subscribeLookWorld(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel() < 1) {
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1){
            switch (messageInfo.getArgs().get(1)){
                case "开启" -> {
                    List<Long> groupList = lookWorldMapper.selectAllGroup();
                    if (groupList == null || !groupList.contains(messageInfo.getGroupId())){
                        lookWorldMapper.insertGroupIdWithSubscrbe(messageInfo.getGroupId());
                        replayInfo.setReplayMessage("每日看世界订阅成功，每日点将会为群里推送，如需关闭发送“每日看世界订阅 关闭”即可");
                    }else {
                        replayInfo.setReplayMessage("请勿重复订阅");
                    }
                }
                case "关闭" -> {
                    List<Long> groupList = lookWorldMapper.selectAllGroup();
                    if (groupList == null || !groupList.contains(messageInfo.getGroupId())){
                        replayInfo.setReplayMessage("当前群组尚未开启订阅");
                    }else {
                        lookWorldMapper.deleteGroupIdWithSubscrbe(messageInfo.getGroupId());
                        replayInfo.setReplayMessage("每日看世界已关闭订阅");
                    }
                }
                default -> replayInfo.setReplayMessage("无效功能命令，请输入“开启”或”关闭“");
            }
        }else {
            replayInfo.setReplayMessage("请输入需要操作的功能");
        }
        return replayInfo;
    }
}
