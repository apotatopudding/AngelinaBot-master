package top.strelitzia.model;

import java.util.List;

public class PersonBlackBoxInfo {
    private List<String> cardList;
    private Long QQ;
    private Long groupId;
    private boolean isExit = false;

    public List<String> getCardList() { return cardList; }

    public void setCardList(List<String> cardList) { this.cardList = cardList; }

    public Long getQQ() { return QQ; }

    public void setQQ(Long QQ) { this.QQ = QQ; }

    public Long getGroupId() { return groupId; }

    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public boolean isExit() { return isExit; }

    public void setExit(boolean exit) { isExit = exit; }
}
