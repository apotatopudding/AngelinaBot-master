package top.strelitzia.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.angelinaBot.model.ReplayInfo;
import top.angelinaBot.util.MiraiFrameUtil;
import top.angelinaBot.util.SendMessageUtil;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.dao.BirthdayRemindMapper;
import top.strelitzia.dao.LookWorldMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author wangzy
 * @Date 2020/12/16 14:10
 **/

@Component
@Slf4j
public class RemindJob {

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private BirthdayRemindMapper birthdayRemindMapper;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private MiraiFrameUtil miraiFrameUtil;

    @Autowired
    private LookWorldMapper lookWorldMapper;

    //每天早8点发送当日生日提醒
    @Scheduled(cron = "${scheduled.birthdayJob}")
    @Async
    public void birthdayJob() {
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("dd");
        Date date = new Date();

        String monthStr = month.format(date);
        String dayStr = day.format(date);
        if (monthStr.startsWith("0")) {
            monthStr = monthStr.substring(1);
        }
        if (dayStr.startsWith("0")) {
            dayStr = dayStr.substring(1);
        }
        String today = monthStr + "月" + dayStr + "日";

        List<String> operatorByBirthday = operatorInfoMapper.getOperatorByBirthday(today);
        if (operatorByBirthday != null && operatorByBirthday.size() > 0) {
            //今日有干员过生日,启动推送判断
            Map<Long,List<String>> birthdayMap = new HashMap<>();//群组对名字的map
            for (String name : operatorByBirthday) {//遍历每一个今天过生日的干员
                List<Long> groupList = birthdayRemindMapper.selectGroupIdByName(name);//查找加入了生日提醒的群组集合
                if (groupList.size()>0){//当集合内有群组时
                    for(Long groupId :groupList){//遍历群组集合取出每一个群组编号
                        List<String> nameList = new ArrayList<>();//干员名字的集合
                        if(birthdayMap.containsKey(groupId)) nameList = birthdayMap.get(groupId);//查找map是否已有该群组信息，如果有则取出
                        nameList.add(name);//把干员名字写入集合内
                        birthdayMap.put(groupId,nameList);//写入map
                    }
                }
            }
            //向服务器发起一次请求获取bot现在能查到的群组
            Map<Long,Long> groupList = miraiFrameUtil.BotGroupMap();
            for(Long groupId : birthdayMap.keySet()){
                StringBuilder s = new StringBuilder("今天是" + today + "祝 ");
                List<String> nameList = birthdayMap.get(groupId);
                for (String name : nameList){
                    s.append(name).append(" ");
                }
                s.append("干员生日快乐");
                ReplayInfo replayInfo = new ReplayInfo();
                replayInfo.setReplayMessage(s.toString());
                replayInfo.setGroupId(groupId);
                //只发给查询到的最新的群组列表
                if (groupList.containsKey(groupId)) {
                    replayInfo.setLoginQQ(MiraiFrameUtil.messageIdMap.get(groupId));
                    sendMessageUtil.sendGroupMsg(replayInfo);
                }
                try {
                    //线程休眠数秒再发送，减少群发频率
                    Thread.sleep(new Random().nextInt(5)*1000);
                }catch (InterruptedException e){
                    log.error(e.toString());
                }
            }
            log.info("{}每日干员生日推送发送成功", new Date());
        }
    }

    //每天早七点半给订阅用户发送每日看世界
    @Scheduled(cron = "${scheduled.lookWorldJob}")
    @Async
    public void lookWorldJob() {
        ReplayInfo replayInfo = new ReplayInfo();
        List<Long> groupList = lookWorldMapper.selectAllGroup();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            BufferedImage image = ImageIO.read(new File("runFile/news/" + sdf.format(new Date()) + ".jpg"));
            replayInfo.setReplayImg(image);
            replayInfo.setGroupId(groupList);
            sendMessageUtil.sendGroupMsg(replayInfo);
        }catch (IOException e){
            log.error(e +"图片获取失败");
        }
        log.info("每日看世界推送完成");
    }

    @Scheduled(cron = "${scheduled.exterminateJob}")
    @Async
    public void exterminateJob() {
        ReplayInfo replayInfo = new ReplayInfo();
        String text = "剿灭提醒\n" +
                "我是本群剿灭小助手，今天是本周最后一天，博士不要忘记打剿灭哦❤\n" +
                "道路千万条，剿灭第一条\n" +
                "剿灭忘记打，博士两行泪\n";
        replayInfo.setReplayMessage(text);
        List<Long> list = new ArrayList<>(MiraiFrameUtil.messageIdMap.keySet());
        replayInfo.setGroupId(list);
        sendMessageUtil.sendGroupMsg(replayInfo);
    }

}
