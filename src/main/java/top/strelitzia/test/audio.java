package top.strelitzia.test;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

public class audio {
    private int volume=100;// 声音：1到100
    private int rate=0;// 频率：-10到10
    private int voice=0;// 语音库序号
    private int audio=0;// 输出设备序号
    private ActiveXComponent ax=null;
    private Dispatch spVoice=null;// 声音对象
    private Dispatch spFileStream=null;// 音频文件输出流对象，在读取或保存音频文件时使用
    private Dispatch spAudioFormat=null;// 音频格式对象
    private Dispatch spMMAudioOut=null;// 音频输出对象
    private int formatType=22;// 音频的输出格式，默认为：SAFT22kHz16BitMono

    public void init()
    {
        ComThread.InitSTA();
        if(ax==null)
        {
            ax=new ActiveXComponent("Sapi.SpVoice");
            spVoice=ax.getObject();
        }
    }
    /**
     * 改变语音库
     * @param voice 语音库序号
    */
    public void changeVoice(int voice)
    {
        if(this.voice != voice) {
            this.voice=voice;
        }
        Dispatch voiceItems=Dispatch.call(spVoice,"GetVoices").toDispatch();
        int count=Integer.parseInt(Dispatch.call(voiceItems,"Count").toString());
        if(count>0)
        {
            Dispatch voiceItem=Dispatch.call(voiceItems,"Item",new Variant(this.voice)).toDispatch();
            Dispatch.put(spVoice,"Voice",voiceItem);
        }
    }

    /**
     * 改变音频输出
     * @param audio 音频设备序号
     */
    public void changeAudioOutput(int audio)
    {
        if(this.audio != audio)
        {
            this.audio=audio;
        }
        Dispatch audioOutputs=Dispatch.call(spVoice,"GetAudioOutputs").toDispatch();
        int count=Integer.parseInt(Dispatch.call(audioOutputs,"Count").toString());
        if(count > 0)
        {
            Dispatch audioOutput=Dispatch.call(audioOutputs,"Item",new Variant(this.audio)).toDispatch();
            Dispatch.put(spVoice,"AudioOutput",audioOutput);
        }
    }
}
