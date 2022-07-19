package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.strelitzia.dao.OperatorGuessMapper;
import top.strelitzia.model.OperatorGuessInfo;

import java.util.HashMap;
import java.util.Map;

@Service
public class OperatorGuessExerciseService {

    @Autowired
    private OperatorGuessMapper operatorGuessMapper;

    //干员表
    private static final Map<Long,Integer> operatorList = new HashMap<>();

    //猜测表
    private static final Map<Long, Integer> GuessNum = new HashMap<>();

    @AngelinaGroup(keyWords = {"档案补全"}, description = "根据提示猜测干员")
    public ReplayInfo beginTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //随机挑选一个干员ID
        OperatorGuessInfo idInfo = this.operatorGuessMapper.getOperatorName();
        Integer Id = idInfo.getOperatorId();
        // 将ID放入新数据库中，输出提示内容
        replayInfo.setReplayMessage("博士，伊芙利特不小心点着了档案库，不少档案已经缺损了，凯尔希女士希望您能依靠记忆补全一些档案资料，如果实在想不起来也没关系，工程部的干员会为我们修复的");
        operatorList.put(messageInfo.getGroupId(),Id);
        OperatorGuessInfo operatorName = this.operatorGuessMapper.getOperatorInfoById(Id);
        OperatorGuessInfo operatorInfoTrue = this.operatorGuessMapper.getOperatorInfoByName(operatorName.getOperatorName());
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
        textLine.addString("职业：" + operatorInfoTrue.getProfession());
        textLine.nextLine();
        replayInfo.setReplayImg(textLine.drawImage());
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"我知道了"}, description = "抢答当前提问题目")
    public ReplayInfo answerTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        Integer SelectId = operatorList.get(messageInfo.getGroupId());
        if (SelectId == null){
            replayInfo.setReplayMessage("啊是博士啊，档案已经修复完了，谢谢您了");
            return replayInfo;
        }
        GuessNum.putIfAbsent(messageInfo.getGroupId(), 0);
        Integer tryNum = GuessNum.get(messageInfo.getGroupId());
        if (messageInfo.getArgs().size() > 1) {
            String answerName = messageInfo.getArgs().get(1);//取得回答的答案
            // 利用输入的名字进行比对，输出比对结果是或否
            boolean answer =false;
            OperatorGuessInfo operatorName = this.operatorGuessMapper.getOperatorInfoById(SelectId);
            //判断是否猜中
            if(answerName.equals(operatorName.getOperatorName())){
                answer =true;
            }
            // 当结果为ture时，结束
            if(answer) {
                GuessNum.remove(messageInfo.getGroupId());
                replayInfo.setReplayMessage("真棒"+messageInfo.getName()+"博士，修复信息匹配成功，正是干员"+operatorName.getOperatorName());
            }else {
                //当结果为false，tryNum+1并继续抢答
                replayInfo.setReplayMessage("不对不对，博士，再想想吧");
                GuessNum.put(messageInfo.getGroupId(),tryNum+1);
                // 判断tryNum是否达到十次，超过十次则公布答案并进行下一题
                if (tryNum > 10) {
                    GuessNum.remove(messageInfo.getGroupId());
                    replayInfo.setReplayMessage("啊博士，工程部的干员们修复了这份档案，是干员" + operatorName.getOperatorName() + "呢，辛苦了博士");
                }
            }
        }else{
            replayInfo.setReplayMessage("博士？抱歉我没听见，请您大点声说");
        }
        return replayInfo;
    }

}
