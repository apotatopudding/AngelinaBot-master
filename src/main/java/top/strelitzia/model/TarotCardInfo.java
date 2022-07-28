package top.strelitzia.model;

public class TarotCardInfo {
    private String tarotCardID;
    private String tarotCardMean;
    private String tarotCardKeyword;
    private String tarotCardForwardPosition;
    private String tarotCardReversePosition;

    public String getTarotCardID() {
        return tarotCardID;
    }

    public void setTarotCardID(String tarotCardID) {
        this.tarotCardID = tarotCardID;
    }

    public String getTarotCardMean() { return tarotCardMean; }

    public void setTarotCardMean(String tarotCardMean) { this.tarotCardMean = tarotCardMean; }

    public String getTarotCardKeyword() {
        return tarotCardKeyword;
    }

    public void setTarotCardKeyword(String tarotCardKeyword) {
        this.tarotCardKeyword = tarotCardKeyword;
    }

    public String getTarotCardForwardPosition() {
        return tarotCardForwardPosition;
    }

    public void setTarotCardForwardPosition(String tarotCardForwardPosition) {
        this.tarotCardForwardPosition = tarotCardForwardPosition;
    }

    public String getTarotCardReversePosition() {
        return tarotCardReversePosition;
    }

    public void setTarotCardReversePosition(String tarotCardReversePosition) {
        this.tarotCardReversePosition = tarotCardReversePosition;
    }
}
