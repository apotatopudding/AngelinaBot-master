package top.strelitzia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaEvent;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.EventEnum;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;
import top.strelitzia.util.PetPetUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

@Service
public class PetPetService {

    @Autowired
    private PetPetUtil petPetUtil;

    @AngelinaEvent(event = EventEnum.NudgeEvent, description = "发送头像的摸头动图")
    @AngelinaGroup(keyWords = {"摸头", "摸我", "摸摸"}, description = "发送头像的摸头动图", sort = "娱乐功能")
    public ReplayInfo PetPet(MessageInfo messageInfo) {

        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        //BufferedImage userImage = ImageUtil.Base64ToImageBuffer(ImageUtil.getImageBase64ByUrl("http://q.qlogo.cn/headimg_dl?dst_uin=" + messageInfo.getQq() + "&spec=100"));
        //String path = "runFile/petpet/frame.gif";
        //petPetUtil.getGif(path, userImage);
        //replayInfo.setReplayImg(new File(path));
        //return replayInfo;
        BufferedImage userImage;
        try {
            userImage = ImageIO.read(new URL("http://q.qlogo.cn/headimg_dl?dst_uin=" + messageInfo.getQq() + "&spec=100"));
            String path = "runFile/petpet/frame.gif";
            petPetUtil.getGif(path, userImage);
            replayInfo.setRecallTime(60);
            replayInfo.setReplayImg(new File(path));
            return replayInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
