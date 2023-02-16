package top.strelitzia.model;

/**
 * @author strelitzia
 * @Date 2022/05/03 14:38
 * 干员详细信息
 **/
public class OperatorInfo {
    private Integer operatorId;
    private String charId;
    private String operatorName;
    private Integer operatorRarity;
    private Integer operatorClass;
    private Integer available;
    private Integer inLimit;

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

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

    public Integer getOperatorRarity() {
        return operatorRarity;
    }

    public void setOperatorRarity(Integer operatorRarity) {
        this.operatorRarity = operatorRarity;
    }

    public Integer getOperatorClass() {
        return operatorClass;
    }

    public void setOperatorClass(Integer operatorClass) {
        this.operatorClass = operatorClass;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }

    public Integer getInLimit() {
        return inLimit;
    }

    public void setInLimit(Integer inLimit) {
        this.inLimit = inLimit;
    }
}
