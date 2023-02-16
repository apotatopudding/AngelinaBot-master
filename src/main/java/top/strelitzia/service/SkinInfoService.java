package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.arknightsDao.SkinInfoMapper;
import top.strelitzia.dao.NickNameMapper;
import top.strelitzia.model.SkinGroupInfo;
import top.strelitzia.model.SkinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/
@Service
@Slf4j
public class SkinInfoService {

    @Autowired
    private SkinInfoMapper skinInfoMapper;

    @Autowired
    private NickNameMapper nickNameMapper;


    @AngelinaGroup(keyWords = {"时装查询", "立绘查询"}, description = "查询指定干员已收录的时装信息", sort = "查询功能", funcClass = "时装与立绘")
    public ReplayInfo getOperatorSkinByInfo(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String name = messageInfo.getArgs().get(1);
            String realName = nickNameMapper.selectNameByNickName(name);
            if (realName != null && !realName.equals("")) {
                name = realName;
            }
            List<SkinInfo> skinInfos = skinInfoMapper.selectSkinByName(name);
            if (skinInfos != null && skinInfos.size() > 0) {
                StringBuilder str = new StringBuilder().append("干员").append(name).append("已收录的时装信息如下：");
                for (int i = 0; i < skinInfos.size(); i++) {
                    SkinInfo skinInfo = skinInfos.get(i);
                    str.append("\n").append(i+1).append("：").append(skinInfo.getSkinName())
                            .append("\n时装编号：").append(skinInfo.getSkinId())
                            .append("\n时装系列：").append(skinInfo.getSkinGroupName())
                            .append("\n时装信息：").append(skinInfo.getContent())
                            .append("\n时装画师：").append(skinInfo.getDrawerName().replace("/","、"));
                }
                replayInfo.setReplayMessage(str.toString());
            }else {
                replayInfo.setReplayMessage("未能查询到时装信息");
            }
        } else {
            replayInfo.setReplayMessage("请输入时装的信息");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"时装","时装图","立绘","立绘图"}, description = "获取指定时装编号的时装图", sort = "查询功能", funcClass = "时装与立绘")
    public ReplayInfo getSkinPictureById(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            if(messageInfo.getArgs().size() > 2){
                String name = messageInfo.getArgs().get(1);
                String realName = nickNameMapper.selectNameByNickName(name);
                if (realName != null && !realName.equals("")) {
                    name = realName;
                }
                String charId = skinInfoMapper.selectCharIdByName(name);
                if (charId != null){
                    String num = messageInfo.getArgs().get(2);
                    String skinId;
                    if (num.matches("^#[1-2][+]?$")){
                        //完全符合"#000"格式，即默认服装类
                        skinId = charId + num;
                        getSkinPic(replayInfo, skinId);
                    } else if (num.matches("^[a-zA-Z]+(#)[0-9]+$")){
                        //完全符合"小写字母或大写字母#0000"格式，即带有时装系列表示时装款，需要添加@
                        skinId = charId + "@" + num;
                        getSkinPic(replayInfo, skinId);
                    }else {
                        //其实是格式错了，我们不告诉他，假装查不到
                        replayInfo.setReplayMessage("未能查询到指定时装信息");
                    }
                }else {
                    replayInfo.setReplayMessage("不存在该干员立绘");
                }
            }else {
                //以获取到的字符为编号查询图片本地地址以获取图片
                String skinId = messageInfo.getArgs().get(1);
                getSkinPic(replayInfo, skinId);
            }
        }else {
            replayInfo.setReplayMessage("请输入需要获取的时装编号");
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"时装系列"}, description = "获取指定时装系列的时装信息，输列表可获取所有系列名", sort = "查询功能", funcClass = "时装与立绘")
    public ReplayInfo getSkinBrandInfo(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            if (messageInfo.getArgs().get(1).equals("列表")){
                List<String> nameList = skinInfoMapper.selectAllSkinGroupName();
                StringBuilder sb = new StringBuilder().append("当前已收录的时装系列有：").append("\n");
                for (String name : nameList) {
                    sb.append(name).append("、");
                }
                sb.deleteCharAt(sb.length()-1);
                replayInfo.setReplayMessage(sb.toString());
            }else {
                String name = messageInfo.getArgs().get(1);
                SkinGroupInfo skinGroupInfo = skinInfoMapper.selectSkinGroupByName(name);
                if (skinGroupInfo != null) {
                    StringBuilder sb = new StringBuilder()
                            .append("时装系列《").append(name).append("》信息如下：")
                            .append("\n时装描述：")
                            .append("\n").append(skinGroupInfo.getDescription())
                            .append("\n该系列包含以下时装：");
                    List<SkinInfo> skinInfoList = skinInfoMapper.selectSkinInfoByBrandId(skinGroupInfo.getBrandId());
                    for (int i = 0; i < skinInfoList.size(); i++) {
                        SkinInfo skinInfo = skinInfoList.get(i);
                        sb.append("\n").append(i + 1).append("：").append("时装名：").append(skinInfo.getSkinGroupName()).append(" ").append(skinInfo.getSkinName())
                                .append("\n时装属于：").append(skinInfo.getOperatorName())
                                .append("\n时装编号：").append(skinInfo.getSkinId());
                    }
                    replayInfo.setReplayMessage(sb.toString());
                } else {
                    replayInfo.setReplayMessage("未获取到指定的时装系列信息");
                }
            }
        }else {
            replayInfo.setReplayMessage("请输入想要查询的时装系列信息");
        }
        return replayInfo;
    }

    /**
     * 以指定的立绘ID查询数据库中收录的该立绘的本地路径并将图片以流形式封装入回复信息
     * @param replayInfo 提供当前回复信息封装以封装图片或文字信息
     * @param skinId 指定的立绘ID
     */
    private void getSkinPic(ReplayInfo replayInfo, String skinId) {
        String path = skinInfoMapper.selectSkinById(skinId);
        if (path != null){
            try {
                InputStream input = Files.newInputStream(Paths.get(path));
                replayInfo.setReplayImg(input);
            } catch (IOException e) {
                replayInfo.setReplayMessage("未获取到指定立绘图片");
                log.error("立绘图片获取失败，问题原因："+e);
            }
        }else {
            replayInfo.setReplayMessage("未能查询到指定时装信息");
        }
    }

}
