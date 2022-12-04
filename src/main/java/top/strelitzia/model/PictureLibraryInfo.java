package top.strelitzia.model;

public class PictureLibraryInfo {
    private Integer pictureId;
    private String folder;
    private Integer type;//0为疑似，1为通过
    private Integer audit;//0为未审核，1为已审核
    private String format;

    public Integer getPictureId() { return pictureId; }

    public void setPictureId(Integer pictureId) { this.pictureId = pictureId; }

    public String getFolder() { return folder; }

    public void setFolder(String folder) { this.folder = folder; }

    public Integer getType() { return type; }

    public void setType(Integer type) { this.type = type; }

    public Integer getAudit() { return audit; }

    public void setAudit(Integer audit) { this.audit = audit; }

    public String getFormat() { return format; }

    public void setFormat(String format) { this.format = format; }
}
