package top.strelitzia.util;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class AudioUtil {
    public static void amrToMp3(File source,File target){
        AudioAttributes audio = new AudioAttributes();
        //ffmpeg中的libamr_wb转换完成也无法播放，不推荐使用
        //amr-nb支持8种比特率，分别是4.75， 5.15， 5.9， 6.7， 7.4， 7.95， 10.2， 12.2kbps，采样率为8KHz
        //AMR-WB支持9种比特率，分别是6.6， 8.85， 12.65， 14.25， 15.85， 18.25， 19.85， 23.05， 23.85kbps，采样率为1.6KHz
        audio.setCodec("libamr_nb");//编码器
        audio.setBitRate(12200);//比特率
        audio.setChannels(1);//声道；1单声道，2立体声
        audio.setSamplingRate(8000);//采样率（重要！！！）

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("amr");//格式
        attrs.setAudioAttributes(audio);//音频设置
        Encoder encoder = new Encoder();
        try {
            encoder.encode(source, target, attrs);
        } catch (EncoderException e){
            e.printStackTrace();
        }
    }

}
