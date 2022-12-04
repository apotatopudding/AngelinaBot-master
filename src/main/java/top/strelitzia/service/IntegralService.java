package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaFriend;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.AdminUtil;
import top.strelitzia.dao.AdminUserMapper;
import top.strelitzia.dao.GroupAdminInfoMapper;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.model.IntegralInfo;

import java.util.List;
import java.util.Random;

@Service
public class IntegralService {

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private GroupAdminInfoMapper groupAdminInfoMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @AngelinaGroup(keyWords = {"签到"}, description = "每日签到", sort = "娱乐功能",funcClass = "签到")
    public ReplayInfo checkIn(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Integer special = this.groupAdminInfoMapper.selectIntegralBySetting(messageInfo.getGroupId());//特殊奖励查询
        Integer allSpecial = this.groupAdminInfoMapper.selectIntegralBySetting(999999999L);//全局特殊奖励查询
        if(allSpecial == null){
            allSpecial = 0;
            this.groupAdminInfoMapper.insertIntegralByGroupId(999999999L,allSpecial);
        }
        if(special == null) {
            special = 0;
            this.groupAdminInfoMapper.insertIntegralByGroupId(messageInfo.getGroupId(),special);
        }
        //查询当天是否已签到
        Integer today = this.integralMapper.selectDayCountByQQ(messageInfo.getQq());
        if(today == null) today = 0 ;
        if (today < 1) {
            Integer integral = this.integralMapper.selectByQQ(messageInfo.getQq());
            if(integral == null) integral = 0 ;
            int add;
            if(allSpecial!=0) add = allSpecial;//全局特殊奖励高于群组特殊奖励
            else if(special!=0) add = special;//群组特殊奖励
            else add = new Random().nextInt(6) + 5;//随机加5~10分
            integral = integral + add;
            this.integralMapper.integralByGroupId(messageInfo.getGroupId(),messageInfo.getName(),messageInfo.getQq(),integral);
            replayInfo.setReplayMessage("每日签到成功，获得积分："+add+"分");
            this.integralMapper.updateByQQ(messageInfo.getQq(), messageInfo.getName());//签到状态更新
        }else {
            replayInfo.setReplayMessage("您今日已签到，不可重复签到");
        }
        return replayInfo;
    }

    @AngelinaFriend(keyWords = {"特设积分修改"}, description = "每日特设积分修改")
    public ReplayInfo setGroupSpecial(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //先判断权限，只能由管理员设置特设积分
        if (!AdminUtil.getAdmin(messageInfo.getQq())) {
            replayInfo.setReplayMessage("您无修改权限");
        }else {
            //对第一层进行判断，如果存在数字群组号则载入，不存在或不为数字则返回告知
            if (messageInfo.getArgs().size()>1){
                boolean result = messageInfo.getArgs().get(1).matches("[0-9]+");
                if (!result){
                    String s = messageInfo.getArgs().get(1);
                    if(s.equals("全部")){
                        if (messageInfo.getArgs().size() > 2) {
                            boolean result2 = messageInfo.getArgs().get(2).matches("[0-9]+");
                            if (!result2){
                                replayInfo.setReplayMessage("您的积分数量输入有误，请输入数字积分数量");
                            }else {
                                int special = Integer.parseInt(messageInfo.getArgs().get(2));
                                this.groupAdminInfoMapper.insertIntegralByGroupId(999999999L,special);
                                replayInfo.setReplayMessage("设置成功，全局特设积分已改为"+special+"分");
                            }
                        }else {
                            replayInfo.setReplayMessage("请输入您要修改的特设积分数量");
                        }
                    }else {
                        replayInfo.setReplayMessage("您的群组号码输入有误，请输入数字群组号码");
                    }
                }else {
                    Long groupId = Long.valueOf(messageInfo.getArgs().get(1));//得到数字形式群组号
                    //对第二层判断，如果存在数字积分则载入，不存在或不为数字则返回告知
                    if(messageInfo.getArgs().size()>2){
                        boolean result2 = messageInfo.getArgs().get(2).matches("[0-9]+");
                        if (!result2){
                            replayInfo.setReplayMessage("您的积分数量输入有误，请输入数字积分数量");
                        }else {
                            int special = Integer.parseInt(messageInfo.getArgs().get(2));
                            this.groupAdminInfoMapper.insertIntegralByGroupId(groupId,special);
                            replayInfo.setReplayMessage("设置成功，群组号码"+groupId+"的特设积分已改为"+special+"分");
                        }
                    }else {
                        replayInfo.setReplayMessage("请输入您要修改的特设积分数量");
                    }
                }
            }else {
                replayInfo.setReplayMessage("请输入您要修改的群组号");
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"查询积分榜"}, description = "积分榜查询", sort = "娱乐功能",funcClass = "签到")
    public ReplayInfo inquireIntegral(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if(messageInfo.getArgs().size()>1){
            //以结果判定是不是数字，如果是数字，以QQ查询，如果不是，以名字查询
            boolean result = messageInfo.getArgs().get(1).matches("[0-9]+");
            if (!result){
                String name = messageInfo.getArgs().get(1);
                List<Integer> Integral = this.integralMapper.selectByName(name);
                //判定信息是否为空
                if(Integral.size()==0){
                    replayInfo.setReplayMessage("积分榜里没有您要查询的信息呢，您看看您是不是写错了");
                }else {
                    String remind = "";
                    if(Integral.size()>1){
                        remind = "\n由于名字出现重复，可能存在查询不准确情况，请使用QQ查询";
                    }
                    replayInfo.setReplayMessage(name + "的积分为" + Integral + remind);
                }
            }else {
                Long QQ = Long.valueOf(messageInfo.getArgs().get(1));
                Integer Integral = this.integralMapper.selectByQQ(QQ);
                //判定信息是否为空，空信息可能是因为名字为数字
                if(Integral==null){
                    //将QQ转换为名字进行二次查询，仍然查不到即没有
                    String name = String.valueOf(QQ);
                    List<Integer> nameIntegral = this.integralMapper.selectByName(name);
                    if(nameIntegral.size()==0){
                        replayInfo.setReplayMessage("积分榜里没有您要查询的信息呢，您看看您是不是写错了");
                    }else{
                        String remind = "";
                        if(nameIntegral.size()>1){
                            remind = "\n由于名字出现重复，可能存在查询不准确情况，请使用QQ查询";
                        }
                        replayInfo.setReplayMessage(name + "的积分为" + nameIntegral + remind);
                    }
                }else {
                    replayInfo.setReplayMessage(QQ+"的积分为"+Integral);
                }
            }
        }else {
            StringBuilder s = new StringBuilder();
            List<IntegralInfo> integralInfoList = this.integralMapper.selectFiveByName();
            int i = 0;
            for(IntegralInfo integralInfo : integralInfoList){
                i = i + 1;
                s.append("第").append(i).append("名为：").append(integralInfo.getName());
                s.append("   他的积分为").append(integralInfo.getIntegral()).append("\n");
            }
            replayInfo.setReplayMessage(s.toString());
        }
        return replayInfo;
    }
}
