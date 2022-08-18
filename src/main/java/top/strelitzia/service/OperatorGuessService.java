package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.MemberPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.container.AngelinaEventSource;
import top.angelinaBot.container.AngelinaListener;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.model.TextLine;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.dao.AdminUserMapper;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.dao.NickNameMapper;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.model.OperatorBasicInfo;
import top.strelitzia.util.AdminUtil;

import java.util.*;

@Service
@Slf4j
public class OperatorGuessService {

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private NickNameMapper nickNameMapper;

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private static final Set<Long> groupList = new HashSet<>();

    @AngelinaGroup(keyWords = {"的小小茶话会"}, description = "茶话会干员竞猜,默认十位（要出多的题可以在后面追加 □ （数字）")
    public ReplayInfo beginTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        int topicNum = 10 ;
        if(messageInfo.getArgs().size()>1){
            topicNum = Integer.parseInt(messageInfo.getArgs().get(1));
        }
        if(topicNum > 99){
            replayInfo.setReplayMessage("博士，这间屋子容不下那么多人的啦");
            return replayInfo;
        }else if(topicNum < 1) {
            replayInfo.setReplayMessage("博士？");
            return replayInfo;
        }
        List<String> operatorList = new ArrayList<>();
        if (groupList.contains(messageInfo.getGroupId())){
            replayInfo.setReplayMessage("博士，这场茶话会还没有结束，我们还需要等待嘉宾们到达哦");
        }else{
            List<String> allOperator = operatorInfoMapper.getAllOperator();
            for (int i = 0; i< topicNum; i++){
                String name = allOperator.get(new Random().nextInt(allOperator.size()));
                operatorList.add(name);
            }
            log.info(operatorList.get(0));
            replayInfo.setReplayMessage("博士，我和风笛邀请了几位罗德岛干员来参加我们的下午茶聚会，他们还未到达，博士可以试着猜猜是谁要来参加我们的茶会呢" +
                    "\n那茶会正式开始了，博士您猜猜谁会第一个光临这小小的茶话会呢");
            groupList.add(messageInfo.getGroupId());
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
            answerTopic(messageInfo,operatorList);
            groupList.remove(messageInfo.getGroupId());
        }
        return replayInfo;
    }

    public void answerTopic(MessageInfo messageInfo,List<String> operatorList) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<String> allOperator = operatorInfoMapper.getAllOperator();
        int topicNum= 0, tryNum = 0;
        boolean overtime = false;
        while (topicNum<operatorList.size()){
            AngelinaListener angelinaListener = new AngelinaListener(){
                @Override
                public boolean callback(MessageInfo message) {
                    boolean reply;
                    try{String text = message.getText();
                        String end = text.substring(text.length()-1);
                        String sentence = text.substring(0,text.length()-1);
                        String realName = nickNameMapper.selectNameByNickName(sentence);
                        if (realName != null && !realName.equals("")) sentence = realName;
                        reply = message.getGroupId().equals(messageInfo.getGroupId()) &&
                                (end.equals("！") || end.equals("!")) && allOperator.contains(sentence);
                    }
                    catch (NullPointerException | StringIndexOutOfBoundsException e){
                        reply = false;
                    }
                    return reply;
                }
            };
            angelinaListener.setGroupId(messageInfo.getGroupId());
            angelinaListener.setSecond(60);
            MessageInfo recall = AngelinaEventSource.waiter(angelinaListener).getMessageInfo();
            if (recall == null) {
                if(!overtime){
                    replayInfo.setReplayMessage("博士博士，你看，干员" + operatorList.get(topicNum) + "已经到了呢" +
                            "\n让我们续上茶水，继续猜测下一位嘉宾是谁吧");
                    topicNum = topicNum + 1;
                    tryNum = 0;
                    overtime = true;
                }else {
                    replayInfo.setReplayMessage("博士你是不是困了，可能是整理文件太忙碌了吧，要不你先回去休息休息，下次有空再来参加我们的茶话会吧");
                    sendMessageUtil.sendGroupMsg(replayInfo);
                    replayInfo.setReplayMessage(null);
                    break;
                }
            }else {
                overtime = false;
                String text = recall.getText();
                String answerName = text.substring(0,text.length()-1);
                String realName = nickNameMapper.selectNameByNickName(answerName);
                if (realName != null && !realName.equals(""))
                    answerName = realName;
                // 利用输入的信息进行比对，依次输出比对结果是或否
                boolean answer =false, DrawName = false, OperatorRarity =false, sex = false, ComeFrom = false, Race = false, Infection = false, profession = false;
                //判断是否猜中
                if(answerName.equals(operatorList.get(topicNum))){
                    answer =true;
                }else{
                    OperatorBasicInfo operatorInfoGuess = this.operatorInfoMapper.getOperatorInfoByName(answerName);
                    OperatorBasicInfo operatorInfoTrue = this.operatorInfoMapper.getOperatorInfoByName(operatorList.get(topicNum));//取得对应题号的干员信息
                    //判断干员档案各个条件是否相同
                    if(operatorInfoGuess.getDrawName().equals(operatorInfoTrue.getDrawName())){
                        DrawName =true;
                    }
                    if(operatorInfoGuess.getOperatorRarity().equals(operatorInfoTrue.getOperatorRarity())){
                        OperatorRarity =true;
                    }
                    if(operatorInfoGuess.getSex().equals(operatorInfoTrue.getSex())){
                        sex =true;
                    }
                    if(operatorInfoGuess.getComeFrom().equals(operatorInfoTrue.getComeFrom())){
                        ComeFrom =true;
                    }
                    if(operatorInfoGuess.getRace().equals(operatorInfoTrue.getRace())){
                        Race =true;
                    }
                    //由于非感染者本身包含感染者字符，所以只用非感染者作为判定条件，未知直接判定为不符合
                    //当都包含非感染者或者都不包含非感染者时作为通过，余下只为一非感染和一感染
                    if( (operatorInfoGuess.getInfection().contains("非感染者") && operatorInfoTrue.getInfection().contains("非感染者")) ||
                            (!operatorInfoGuess.getInfection().contains("非感染者") && !operatorInfoTrue.getInfection().contains("非感染者")) ){
                        Infection =true;
                    }
                    if(operatorInfoGuess.getOperatorClass().equals(operatorInfoTrue.getOperatorClass())){
                        profession =true;
                    }
                }
                StringBuilder s = new StringBuilder();
                // 当结果为ture，topicNum+1并返回setTopic
                if(answer) {
                    Integer integral = this.integralMapper.selectByQQ(recall.getQq());
                    //猜谜答对一次的人加三分
                    try{integral = integral + 3;
                    }catch (NullPointerException e){
                        //log.info(e.toString());
                        integral = 3;
                    }
                    this.integralMapper.integralByGroupId(recall.getGroupId(), recall.getName(), recall.getQq(), integral);
                    //更新题号和猜测次数
                    s.append("真棒").append(recall.getName()).append("博士，恭喜您回答正确，正是干员").append(operatorList.get(topicNum)).append("呢，看，他已经加入茶会中了");
                    topicNum = topicNum + 1;
                    tryNum = 0;
                }else {
                    //当结果为false，tryNum+1并返回answerTopic继续抢答
                    TextLine textLine = new TextLine(20);
                    textLine.addString("提示：");
                    textLine.nextLine();
                    if (DrawName) {
                        textLine.addString("画师：√");
                    } else {
                        textLine.addString("画师：X");
                    }
                    textLine.nextLine();
                    if (OperatorRarity) {
                        textLine.addString("星级：√");
                    } else {
                        textLine.addString("星级：X");
                    }
                    textLine.nextLine();
                    if (sex) {
                        textLine.addString("性别：√");
                    } else {
                        textLine.addString("性别：X");
                    }
                    textLine.nextLine();
                    if (ComeFrom) {
                        textLine.addString("出生地：√");
                    } else {
                        textLine.addString("出生地：X");
                    }
                    textLine.nextLine();
                    if (Race) {
                        textLine.addString("种族：√");
                    } else {
                        textLine.addString("种族：X");
                    }
                    textLine.nextLine();
                    if (Infection) {
                        textLine.addString("感染情况：√");
                    } else {
                        textLine.addString("感染情况：X");
                    }
                    textLine.nextLine();
                    if (profession) {
                        textLine.addString("职业：√");
                    } else {
                        textLine.addString("职业：X");
                    }
                    if (tryNum > 10) {
                        // 判断tryNum是否达到十次，超过十次则公布答案并进行下一题
                        s.append("都不对哦博士，你看他来了，是干员").append(operatorList.get(topicNum)).append("呢");
                        topicNum = topicNum + 1;
                        tryNum = 0;
                    }else {
                        replayInfo.setReplayImg(textLine.drawImage());
                        replayInfo.setRecallTime(60);
                        replayInfo.setReplayMessage("不对不对，" + recall.getName() + "博士，再尝试一下吧");
                        tryNum = tryNum + 1;
                        sendMessageUtil.sendGroupMsg(replayInfo);
                        replayInfo.setRecallTime(null);
                        replayInfo.setReplayMessage(null);
                        replayInfo.getReplayImg().clear();
                        continue;
                    }
                }
                if(topicNum == (operatorList.size())){
                    s.append("\n嘉宾已经全部到齐，美妙的茶话会开始了");
                }else {
                    s.append("\n让我们续上茶水，继续猜测下一位嘉宾是谁吧");
                    log.info(operatorList.get(topicNum));
                }
                replayInfo.setReplayMessage(s.toString());
            }
            sendMessageUtil.sendGroupMsg(replayInfo);
            replayInfo.setReplayMessage(null);
        }
    }

    @AngelinaGroup(keyWords = {"退出茶话会"}, description = "茶话会关闭")
    public ReplayInfo closeTopic(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        boolean sqlAdmin = AdminUtil.getSqlAdmin(messageInfo.getQq(), adminUserMapper.selectAllAdmin());
        if (messageInfo.getUserAdmin().getLevel()<1 && !sqlAdmin){
            replayInfo.setReplayMessage("（琴柳似乎沉浸在和桑葚的聊天中，并没有注意到你）");
        }else {
            groupList.remove(messageInfo.getGroupId());
            AngelinaEventSource.getInstance().listenerSet.keySet().removeIf(angelinaListener -> angelinaListener.getGroupId().equals(messageInfo.getGroupId()));
            replayInfo.setReplayMessage("博士您要走了吗，那请帮忙把这包甜司康饼带给小刻吧，下次有空记得还来玩啊");
        }
        return replayInfo;
    }

}
