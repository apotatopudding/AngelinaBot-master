package top.strelitzia.model;

public class HorseRaceInfo {
    private Long QQ;
    //选手
    private Integer contestant;
    //积分
    private Integer integral;

    public Long getQQ() {
        return QQ;
    }

    public void setQQ(Long QQ) {
        this.QQ = QQ;
    }

    public Integer getContestant() {
        return contestant;
    }

    public void setContestant(Integer contestant) {
        this.contestant = contestant;
    }

    public Integer getIntegral() {
        return integral;
    }

    public void setIntegral(Integer integral) {
        this.integral = integral;
    }
}
