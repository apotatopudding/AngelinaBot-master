package top.strelitzia.model;

public class DownloadOneFileInfo {

    //下载文件名
    private String fileName;
    //下载路径
    private String url;
    //超时判定
    private int second = 600;
    //是否使用代理
    private boolean useHost;
    //代理IP
    private String hostname;
    //代理端口
    private int port;
    //压缩包调用开关，当开启时，所有解包数据直接从压缩包拉取
    private boolean pullZip;

    public DownloadOneFileInfo(){
    }

    // 只写入代理信息的方法
    public DownloadOneFileInfo(DownloadOneFileInfo downloadOneFileInfo) {
        this.useHost = downloadOneFileInfo.isUseHost();
        this.port = downloadOneFileInfo.getPort();
        this.hostname = downloadOneFileInfo.getHostname();
        this.pullZip = downloadOneFileInfo.isPullZip();
    }

    public String getFileName() { return fileName; }

    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUrl() { return url; }

    public void setUrl(String url) { this.url = url; }

    public int getSecond() { return second; }

    public void setSecond(int second) { this.second = second; }

    public boolean isUseHost() { return useHost; }

    public void setUseHost(boolean useHost) { this.useHost = useHost; }

    public String getHostname() { return hostname; }

    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPort() { return port; }

    public void setPort(int port) { this.port = port; }

    public boolean isPullZip() {
        return pullZip;
    }

    public void setPullZip(boolean pullZip) {
        this.pullZip = pullZip;
    }
}
