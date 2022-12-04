package top.strelitzia.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import top.angelinaBot.annotation.AngelinaGroup;
import top.angelinaBot.model.MessageInfo;
import top.angelinaBot.model.ReplayInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class ruralFunctionByApi {

    private static final String token = "720106bf8bec69051f352b3e531675f2";

    private static final List<String> typeList = new ArrayList<>(
            List.of("夏日棒冰","奇异触手","蒲公英","鸽子羽毛","金属零件","折扇","玫瑰弓箭","湛蓝药丸","烧瓶","猫爪")
    );

    @AngelinaGroup(keyWords = {"cp宇宙"}, sort = "API功能")
    public ReplayInfo CPUniverse(MessageInfo messageInfo){
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size()>1){
            String text = messageInfo.getArgs().get(1);
            int index = text.lastIndexOf("和");
            if (index == -1){
                replayInfo.setReplayMessage("您的输入有误");
                return replayInfo;
            }
            String name1 = text.substring(0,index);
            String name2 = text.substring(index+1);
            String type = typeList.get(new Random().nextInt(typeList.size()));
            String url = "https://api.xingzhige.com/API/cp_generate_2/?type="+type+"&name1="+name1+"&name2="+name2;
            try {
                JSONObject json = readJsonFromUrl(url);
                JSONObject data = json.getJSONObject("data");
                String cp = data.getString("content").replace("【"," ").replace("】"," ");
                replayInfo.setReplayMessage(cp);
            }catch (IOException | JSONException e){
                log.error(e.toString());
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"cp"}, sort = "API功能")
    public ReplayInfo CP(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String text = messageInfo.getArgs().get(1);
            int index = text.lastIndexOf("和");
            if (index == -1){
                replayInfo.setReplayMessage("您的输入有误");
                return replayInfo;
            }
            String g = text.substring(0, index);
            String s = text.substring(index + 1);
            String url = "https://api.xingzhige.com/API/cp_generate/?g="+g+"&s="+s;
            try {
                JSONObject json = readJsonFromUrl(url);
                JSONObject data = json.getJSONObject("data");
                String cp = data.getString("msg");
                replayInfo.setReplayMessage(cp);
            }catch (IOException | JSONException e){
                log.error(e.toString());
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"缘分"}, sort = "API功能")
    public ReplayInfo destiny(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        if (messageInfo.getArgs().size() > 1) {
            String text = messageInfo.getArgs().get(1);
            int index = text.lastIndexOf("和");
            if (index == -1){
                replayInfo.setReplayMessage("您的输入有误");
                return replayInfo;
            }
            String name1 = text.substring(0, index);
            String name2 = text.substring(index + 1);
            String url = "https://api.xingzhige.com/API/yuanfen/?name1="+name1+"&name2="+name2;
            try {
                StringBuilder s = new StringBuilder();
                JSONObject json = readJsonFromUrl(url);
                if(json.getString("msg").equals("成功")) {
                    JSONObject data = json.getJSONObject("data");
                    s.append("缘分物语：").append(data.getString("yan"));
                    s.append("\n缘分解析：").append(data.getString("text"));
                    replayInfo.setReplayMessage(s.toString());
                }else {
                    replayInfo.setReplayMessage(json.getString("msg"));
                }
            }catch (IOException | JSONException e){
                log.error(e.toString());
            }
        }
        return replayInfo;
    }

    @AngelinaGroup(keyWords = {"小情话"}, sort = "API功能")
    public ReplayInfo honeyWords(MessageInfo messageInfo) {
        ReplayInfo replayInfo = new ReplayInfo(messageInfo);
        String url = "http://api.tianapi.com/saylove/index?key="+token;
        try {
            JSONObject json = readJsonFromUrl(url);
            JSONArray newsList = json.getJSONArray("newslist");
            JSONObject a = (JSONObject) newsList.get(0);
            String s = a.getString("content");
            replayInfo.setReplayMessage(s);
        }catch (IOException | JSONException e){
            log.error(e.toString());
        }
        return replayInfo;
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader out = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            int cp;
            while ((cp = out.read()) != -1) {
                s.append((char) cp);
            }
            return new JSONObject(s.toString());
        }
    }
}
