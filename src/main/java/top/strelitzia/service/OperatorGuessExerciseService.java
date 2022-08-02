package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.dao.NickNameMapper;
import top.strelitzia.model.OperatorBasicInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class OperatorGuessExerciseService {

    @Autowired
    OperatorInfoMapper operatorInfoMapper;

    @Autowired
    NickNameMapper nickNameMapper;

    //群组选择的干员
    private final Map<Long,String> operatorList = new HashMap<>();

    //猜测尝试次数表
    private final Map<Long, Integer> GuessNum = new HashMap<>();

    @AngelinaGroup(keyWords = {"档案补全"}, description = "根据提示猜测干员")
    public ReplayInfo beginTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //随机挑选一个干员
        List<String> allOperator = operatorInfoMapper.getAllOperator();
        String name = allOperator.get(new Random().nextInt(allOperator.size()));
        // 将ID放入新数据库中，输出提示内容
        replayInfo.setReplayMessage("博士，伊芙利特不小心点着了档案库，不少档案已经缺损了，凯尔希女士希望您能依靠记忆补全一些档案资料，如果实在想不起来也没关系，工程部的干员会为我们修复的");
        operatorList.put(messageInfo.getGroupId(),name);
        OperatorBasicInfo operatorInfoTrue = this.operatorInfoMapper.getOperatorInfoByName(name);
        String className = switch (operatorInfoTrue.getOperatorClass()) {
            case 1 -> "先锋";
            case 2 -> "近卫";
            case 3 -> "重装";
            case 4 -> "狙击";
            case 5 -> "术士";
            case 6 -> "辅助";
            case 7 -> "医疗";
            case 8 -> "特种";
            default -> "";
        };
        TextLine textLine = new TextLine(100);
        textLine.addString("人员档案");
        textLine.nextLine();
        textLine.addString("代号：▉▉▉◣");
        textLine.nextLine();
        textLine.addString("画师：" + operatorInfoTrue.getDrawName());
        textLine.nextLine();
        textLine.addString("星级：" + operatorInfoTrue.getOperatorRarity());
        textLine.nextLine();
        textLine.addString("性别：" + operatorInfoTrue.getSex());
        textLine.nextLine();
        textLine.addString("出生地：" + operatorInfoTrue.getComeFrom());
        textLine.nextLine();
        textLine.addString("种族：" + operatorInfoTrue.getRace());
        textLine.nextLine();
        textLine.addString("感染情况：" + operatorInfoTrue.getInfection());
        textLine.nextLine();
        textLine.addString("职业：" + className);
        textLine.nextLine();
        replayInfo.setReplayImg(textLine.drawImage());
        replayInfo.setRecallTime(60);
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"我知道了"}, description = "抢答当前提问题目")
    public ReplayInfo answerTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String SelectName = operatorList.get(messageInfo.getGroupId());
        if (SelectName == null){
            replayInfo.setReplayMessage("啊是博士啊，档案已经修复完了，谢谢您了");
            return replayInfo;
        }
        GuessNum.putIfAbsent(messageInfo.getGroupId(),0);
        Integer tryNum = GuessNum.get(messageInfo.getGroupId());
        if (messageInfo.getArgs().size() > 1) {
            String answerName = messageInfo.getArgs().get(1);//取得回答的答案
            String realName = nickNameMapper.selectNameByNickName(answerName);
            if (realName != null && !realName.equals("")) {
                answerName = realName;
            }
            // 利用输入的名字进行比对，输出比对结果是或否，如果猜中直接判断为是
            boolean answer = answerName.equals(SelectName);
            // 当结果为ture时，结束
            if(answer) {
                GuessNum.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("真棒"+messageInfo.getName()+"博士，修复信息匹配成功，正是干员"+ SelectName);
                operatorList.remove(messageInfo.getGroupId());
            }else {
                //当结果为false，tryNum+1并继续抢答
                replayInfo.setReplayMessage("不对不对，博士，再想想吧");
                GuessNum.put(messageInfo.getGroupId(),tryNum+1);
                // 判断tryNum是否达到十次，超过十次则公布答案并进行下一题
                if (tryNum > 10) {
                    GuessNum.remove(messageInfo.getGroupId());
                    replayInfo.setReplayMessage("啊博士，工程部的干员们修复了这份档案，是干员" + SelectName + "呢，辛苦了博士");
                }
            }
        }else{
            replayInfo.setReplayMessage("博士？抱歉我没听见，请您大点声说");
        }
        return replayInfo;
    }

}
