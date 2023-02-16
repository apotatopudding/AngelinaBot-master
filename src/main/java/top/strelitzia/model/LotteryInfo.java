package top.strelitzia.model;

public class LotteryInfo {
    private Long date;
    private Integer LotteryCode;
    private Long groupId;
    private Long qq;
    private String name;
    private Boolean verify;
    private String benefactor;

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Integer getLotteryCode() {
        return LotteryCode;
    }

    public void setLotteryCode(Integer lotteryCode) {
        LotteryCode = lotteryCode;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getQq() {
        return qq;
    }

    public void setQq(Long qq) {
        this.qq = qq;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getVerify() {
        return verify;
    }

    public void setVerify(Integer verify) {
        this.verify = (verify == 1);
    }

    public String getBenefactor() {
        return benefactor;
    }

    public void setBenefactor(String benefactor) {
        this.benefactor = benefactor;
    }
}
