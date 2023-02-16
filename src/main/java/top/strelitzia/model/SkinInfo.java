package top.strelitzia.model;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 **/
public class SkinInfo {
    private Integer operatorId;
    private String operatorName;

    private String skinId;
    private String skinPath;
    private String charId;

    private String skinName;
    private String drawerName;
    private String skinGroupId;
    private String skinGroupName;
    private String brandId = "ILLUST";
    private String content;
    // 0代表立绘属于干员，1代表立绘属于召唤物
    private Integer type = 0;

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getSkinId() {
        return skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public String getSkinPath() {
        return skinPath;
    }

    public void setSkinPath(String skinPath) {
        this.skinPath = skinPath;
    }

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public String getSkinGroupId() {
        return skinGroupId;
    }

    public void setSkinGroupId(String skinGroupId) {
        this.skinGroupId = skinGroupId;
    }

    public String getSkinGroupName() {
        return skinGroupName;
    }

    public void setSkinGroupName(String skinGroupName) {
        this.skinGroupName = skinGroupName;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
