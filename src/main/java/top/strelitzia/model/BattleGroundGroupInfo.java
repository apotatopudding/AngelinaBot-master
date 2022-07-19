package top.strelitzia.model;

import java.util.ArrayList;
import java.util.List;

public class BattleGroundGroupInfo {
    private List<Integer> closeArea = new ArrayList<>();
    private Boolean groupSwitch = false;
    private Boolean groupTempSwitch = false;
    private Boolean alreadyBegan = false;
    public volatile Boolean exit = false;

    public List<Integer> getCloseArea() {
        return closeArea;
    }

    public void setCloseArea(List<Integer> closeArea) {
        this.closeArea = closeArea;
    }

    public Boolean getGroupSwitch() {
        return groupSwitch;
    }

    public void setGroupSwitch(Boolean groupSwitch) {
        this.groupSwitch = groupSwitch;
    }

    public Boolean getGroupTempSwitch() {
        return groupTempSwitch;
    }

    public void setGroupTempSwitch(Boolean groupTempSwitch) {
        this.groupTempSwitch = groupTempSwitch;
    }

    public Boolean getAlreadyBegan() {
        return alreadyBegan;
    }

    public void setAlreadyBegan(Boolean alreadyBegan) {
        this.alreadyBegan = alreadyBegan;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }
}
