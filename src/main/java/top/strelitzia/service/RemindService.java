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

    @AngelinaGroup(keyWords = {"生日提醒"}, description = "给当前群组增加一位指定干员的生日提醒", sort = "订阅功能",funcClass = "干员生日提醒")
    public ReplayInfo subscribeBirthday(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel()<1){
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1){
            List<String> AllOperator = operatorInfoMapper.getAllOperator();
            String name = messageInfo.getArgs().get(1);
            String realName = nickNameMapper.selectNameByNickName(name);
            if (realName != null && !realName.equals("")) {
                name = realName;
            }
            if (AllOperator.contains(name)){
                List<String> nameByGroupId = birthdayRemindMapper.selectNameByGroupId(messageInfo.getGroupId());
                if (nameByGroupId != null && nameByGroupId.contains(name)){
                    replayInfo.setReplayMessage("博士，您已经为这位干员设有生日提醒了");
                }else {
                    birthdayRemindMapper.insertBirthdayRemind(messageInfo.getGroupId(),name);
                    replayInfo.setReplayMessage( name + "生日提醒添加成功！");
                }
            }else {
                replayInfo.setReplayMessage("这个名字我查不到呢，再检查一下吧");
            }
        }else {
            replayInfo.setReplayMessage("博士，请告诉我您想要订阅的干员名字呢");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"不再提醒"},description = "删除当前群组对指定干员的生日提醒", sort = "订阅功能",funcClass = "干员生日提醒")
    public ReplayInfo deleteSubscription(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel()<1){
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        if (messageInfo.getArgs().size()>1){
            List<String> AllOperator = operatorInfoMapper.getAllOperator();
            String name = messageInfo.getArgs().get(1);
            String realName = nickNameMapper.selectNameByNickName(name);
            if (realName != null && !realName.equals("")) {
                name = realName;
            }
            if (AllOperator.contains(name)){
                List<String> nameByGroupId = birthdayRemindMapper.selectNameByGroupId(messageInfo.getGroupId());
                if (nameByGroupId != null && nameByGroupId.contains(name)){
                    birthdayRemindMapper.deleteBirthdayRemind(messageInfo.getGroupId(),name);
                    replayInfo.setReplayMessage( name + "生日提醒取消成功");
                }else {
                    replayInfo.setReplayMessage("博士，您还没有为这位干员设置生日提醒呢");
                }
            }else {
                replayInfo.setReplayMessage("这个名字我查不到呢，再检查一下吧");
            }
        }else {
            replayInfo.setReplayMessage("博士，请告诉我您想要订阅的干员名字呢");
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

    @AngelinaGroup(keyWords = {"订阅每日看世界"}, description = "为当前群组增加一份每日看世界订阅", sort = "订阅功能",funcClass = "每日看世界")
    public ReplayInfo subscribeLookWorld(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel() < 1) {
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        Integer a = lookWorldMapper.selectStateByGroupId(messageInfo.getGroupId());
        if (a ==null || a == 0) {
            lookWorldMapper.insertStateByGroupId(messageInfo.getGroupId(), 1);
            replayInfo.setReplayMessage("每日看世界订阅成功，每日点将会为群里推送，如需关闭发送“关闭每日看世界”即可");
        } else {
            replayInfo.setReplayMessage("请勿重复订阅");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"关闭每日看世界"}, description = "关闭当前群组的每日看世界订阅", sort = "订阅功能",funcClass = "每日看世界")
    public ReplayInfo subscribeLookWorldClose(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getUserAdmin().getLevel() < 1) {
            replayInfo.setReplayMessage("您的权限不足");
            return replayInfo;
        }
        Integer a = lookWorldMapper.selectStateByGroupId(messageInfo.getGroupId());
        if (a == null||a == 0) {
            replayInfo.setReplayMessage("当前群组尚未开启订阅");
        } else {
            lookWorldMapper.insertStateByGroupId(messageInfo.getGroupId(), 0);
            replayInfo.setReplayMessage("每日看世界已关闭订阅");
        }
        return replayInfo;
    }
}
