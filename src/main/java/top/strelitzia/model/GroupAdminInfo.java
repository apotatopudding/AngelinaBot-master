package top.strelitzia.model;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/
public class GroupAdminInfo {
    private Long groupId;
    private Integer found;
    private Long setupIntegral;
    private int lookWorld;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Integer getFound() {
        return found;
    }

    public void setFound(Integer found) {
        this.found = found;
    }

    public Long getSetupIntegral() { return setupIntegral; }

    public void setSetupIntegral(Long setupIntegral) { this.setupIntegral = setupIntegral; }

    public int getLookWorld() { return lookWorld; }

    public void setLookWorld(int lookWorld) { this.lookWorld = lookWorld; }
}
