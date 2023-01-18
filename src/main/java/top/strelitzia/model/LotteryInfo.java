package top.strelitzia.model;

public class LotteryInfo {
    private Integer LotteryCode;
    private Long groupId;
    private Long qq;
    private String name;

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
}
