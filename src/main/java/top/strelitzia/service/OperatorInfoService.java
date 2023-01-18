package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.arknightsDao.OperatorInfoMapper;
import top.strelitzia.dao.NickNameMapper;
import top.strelitzia.model.OperatorBasicInfo;
import top.strelitzia.model.TalentInfo;

import java.util.*;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/
@Service
public class OperatorInfoService {

    @Autowired
    private OperatorInfoMapper operatorInfoMapper;

    @Autowired
    private NickNameMapper nickNameMapper;

    final Map<String, Integer> rarity = new HashMap<>();

    final Map<String, Integer> opClass = new HashMap<>();

    {
        rarity.put("六星", 6);
        rarity.put("五星", 5);
        rarity.put("四星", 4);
        rarity.put("三星", 4);
        rarity.put("二星", 2);
        rarity.put("一星", 1);

        rarity.put("6", 6);
        rarity.put("5", 5);
        rarity.put("4", 4);
        rarity.put("3", 4);
        rarity.put("2", 2);
        rarity.put("1", 1);

        opClass.put("先锋", 1);
        opClass.put("近卫", 2);
        opClass.put("重装", 3);
        opClass.put("狙击", 4);
        opClass.put("术士", 5);
        opClass.put("术师", 5);
        opClass.put("辅助", 6);
        opClass.put("医疗", 7);
        opClass.put("特种", 8);
    }


    @AngelinaGroup(keyWords = {"干员搜索", "搜索干员"}, description = "根据条件搜索干员名字", sort = "查询功能", funcClass = "干员信息查询")
    public ReplayInfo getOperatorByInfos(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<String> infos = messageInfo.getArgs();
        List<String> operators = operatorInfoMapper.getAllOperator();
        StringBuilder s = new StringBuilder("符合 ");
        for (int i = 1; i < infos.size(); i++) {
            String info = infos.get(i);
            if (info == null) {
                break;
            }

            String realName = nickNameMapper.selectNameByNickName(info);
            if (realName != null && !realName.equals(""))
                info = realName;

            if (rarity.containsKey(info)) {
                operators.retainAll(operatorInfoMapper.getOperatorNameByRarity(rarity.get(info)));
            } else if (opClass.containsKey(info)) {
                operators.retainAll(operatorInfoMapper.getOperatorNameByClass(opClass.get(info)));
            } else {
                operators.retainAll(operatorInfoMapper.getOperatorNameByInfo(info));
            }
            s.append(info).append(" ");
        }
        s.append("条件的干员为：\n");
        for (String name : operators) {
            s.append(name).append("\n");
        }
        if (infos.contains("叶莲娜")||infos.contains("霜星")) {
            s.append("霜星").append("\n");
        }
        replayInfo.setReplayMessage(s.toString());
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"档案信息", "查询档案", "干员档案", "档案查询"}, description = "查询干员档案信息", sort = "查询功能", funcClass = "干员信息查询")
    public ReplayInfo getOperatorInfo(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String name = messageInfo.getArgs().get(1);
            String realName = nickNameMapper.selectNameByNickName(name);
            if (realName != null && !realName.equals("")) name = realName;
            OperatorBasicInfo operatorInfoByName = operatorInfoMapper.getOperatorInfoByName(name);
            String s;
            if(operatorInfoByName == null){
                s = "未找到该干员档案信息";
            }else {
                s = name + "干员的档案为：\n";
                if (messageInfo.getArgs().size() == 2) {
                    s += "基础档案：\n" +
                            "画师：" + operatorInfoByName.getDrawName() + '\t' +
                            "声优：" + operatorInfoByName.getCvNameOfJP() + '\n' +
                            "代号：" + operatorInfoByName.getCodeName() + '\t' +
                            "性别：" + operatorInfoByName.getSex() + '\t' +
                            "出身地：" + operatorInfoByName.getComeFrom() + '\n' +
                            "生日：" + operatorInfoByName.getBirthday() + '\t' +
                            "种族：" + operatorInfoByName.getRace() + '\t' +
                            "身高：" + operatorInfoByName.getHeight() + '\n' +
                            "矿石病感染情况：" + operatorInfoByName.getInfection();
                } else {
                    switch (messageInfo.getArgs().get(2)) {
                        case "全部档案":
                            s += operatorInfoByName.toString();
                            break;
                        case "基础档案":
                            s += "基础档案：\n" +
                                    "画师：" + operatorInfoByName.getDrawName() + '\t' +
                                    "声优：" + operatorInfoByName.getCvNameOfJP() + '\n' +
                                    "代号：" + operatorInfoByName.getCodeName() + '\t' +
                                    "性别：" + operatorInfoByName.getSex() + '\t' +
                                    "出身地：" + operatorInfoByName.getComeFrom() + '\n' +
                                    "生日：" + operatorInfoByName.getBirthday() + '\t' +
                                    "种族：" + operatorInfoByName.getRace() + '\t' +
                                    "身高：" + operatorInfoByName.getHeight() + '\n' +
                                    "矿石病感染情况：" + operatorInfoByName.getInfection();
                            break;
                        case "综合体检测试":
                            s += operatorInfoByName.getComprehensiveTest();
                            break;
                        case "客观履历":
                            s += operatorInfoByName.getObjectiveResume();
                            break;
                        case "临床诊断分析":
                            s += operatorInfoByName.getClinicalDiagnosis();
                            break;
                        case "档案资料一":
                            s += operatorInfoByName.getArchives1();
                            break;
                        case "档案资料二":
                            s += operatorInfoByName.getArchives2();
                            break;
                        case "档案资料三":
                            s += operatorInfoByName.getArchives3();
                            break;
                        case "档案资料四":
                            s += operatorInfoByName.getArchives4();
                            break;
                        case "晋升记录":
                        case "晋升资料":
                            s += operatorInfoByName.getPromotionInfo();
                            break;
                    }
                }
            }

            replayInfo.setReplayMessage(s);
        } else {
            replayInfo.setReplayMessage("请输入干员名称");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"声优查询", "查询声优"}, description = "根据条件查询声优信息", sort = "查询功能", funcClass = "干员信息查询")
    public ReplayInfo getCVByName(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<String> allCV = new ArrayList<>();
        StringBuilder s = new StringBuilder();
        if (messageInfo.getArgs().size() > 1) {
            switch (messageInfo.getArgs().get(1)){
                case "普通话" -> allCV = operatorInfoMapper.getAllInfoName("CN_mandarin");
                case "方言" -> allCV = operatorInfoMapper.getAllInfoName("CN_topolect");
                case "日配" -> allCV = operatorInfoMapper.getAllInfoName("JP");
                case "韩配" -> allCV = operatorInfoMapper.getAllInfoName("KR");
                case "英配" -> allCV = operatorInfoMapper.getAllInfoName("EN");
                default -> {
                    List<String> areaList = new ArrayList<>(Arrays.asList("CN_mandarin","CN_topolect","JP","KR","EN"));
                    for (String area :areaList) {
                        allCV.addAll(operatorInfoMapper.getAllInfoNameLikeStr(area,messageInfo.getArgs().get(1)));
                    }
                }
            }
            int i=0;
            for (String name : allCV) {
                if(name == null) continue;
                s.append(name).append(" ".repeat(3));
                i++;
                if(i==3){
                    s.append('\n');
                    i = 0;
                }
            }
            replayInfo.setReplayMessage(s.toString());
        } else {
            replayInfo.setReplayMessage("请输入要查询的内容，可以输入配音版本（普通话、方言、中配、日配、韩配、英配），也可以对声优名字进行模糊查询");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"画师查询", "查询画师"}, description = "根据条件查询画师信息", sort = "查询功能", funcClass = "干员信息查询")
    public ReplayInfo getDrawByName(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        List<String> allDraw;
        StringBuilder s = new StringBuilder();
        if (messageInfo.getArgs().size() > 1) {
            allDraw = operatorInfoMapper.getAllDrawNameLikeStr(messageInfo.getArgs().get(1));
        } else {
            allDraw = operatorInfoMapper.getAllDrawName();
        }
        for (String name : allDraw) {
            s.append(name).append('\n');
        }
        replayInfo.setReplayMessage(s.toString());
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"天赋查询", "干员天赋"}, description = "查询干员的天赋信息", sort = "查询功能", funcClass = "干员信息查询")
    public ReplayInfo getTalentByName(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String name = messageInfo.getArgs().get(1);
            String realName = nickNameMapper.selectNameByNickName(name);
            if (realName != null && !realName.equals(""))
                name = realName;

            List<TalentInfo> operatorTalent = operatorInfoMapper.getOperatorTalent(name);
            if (operatorTalent != null && operatorTalent.size() > 0) {
                StringBuilder s = new StringBuilder(name).append("干员的天赋为：");
                for (TalentInfo t : operatorTalent) {
                    s.append("\n").append(t.getTalentName()).append("\t解锁条件：精英化").append(t.getPhase()).append("等级")
                            .append(t.getLevel()).append("潜能").append(t.getPotential())
                            .append("\n\t").append(t.getDescription());
                }
                replayInfo.setReplayMessage(s.toString());
            } else {
                replayInfo.setReplayMessage("未找到该干员的天赋");
            }
        } else {
            replayInfo.setReplayMessage("请输入干员名称");
        }
        return replayInfo;
    }
}
