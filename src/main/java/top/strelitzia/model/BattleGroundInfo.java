package top.strelitzia.model;

public class BattleGroundInfo {
    private Long groupId;
    private Long QQ;
    private String name;
    private Integer health = 1000;
    private Integer healthPoints = 1000;
    private Integer physicsAttack = 60;
    private Integer magicAttack = 0;
    private Integer physicsArmor = 0;
    private Integer magicArmor = 0;
    private Integer realDamage = 0;
    private Integer reduceDamage = 0;
    private Integer location = 0;
    private String defeatedBy;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getQQ() {
        return QQ;
    }

    public void setQQ(Long QQ) {
        this.QQ = QQ;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getHealthPoints() {
        return healthPoints;
    }

    public void setHealthPoints(Integer healthPoints) {
        this.healthPoints = healthPoints;
    }

    public Integer getPhysicsAttack() {
        return physicsAttack;
    }

    public void setPhysicsAttack(Integer physicsAttack) {
        this.physicsAttack = physicsAttack;
    }

    public Integer getMagicAttack() {
        return magicAttack;
    }

    public void setMagicAttack(Integer magicAttack) {
        this.magicAttack = magicAttack;
    }

    public Integer getPhysicsArmor() {
        return physicsArmor;
    }

    public void setPhysicsArmor(Integer physicsArmor) {
        this.physicsArmor = physicsArmor;
    }

    public Integer getMagicArmor() {
        return magicArmor;
    }

    public void setMagicArmor(Integer magicArmor) {
        this.magicArmor = magicArmor;
    }

    public Integer getRealDamage() {
        return realDamage;
    }

    public void setRealDamage(Integer realDamage) {
        this.realDamage = realDamage;
    }

    public Integer getReduceDamage() {
        return reduceDamage;
    }

    public void setReduceDamage(Integer reduceDamage) {
        this.reduceDamage = reduceDamage;
    }

    public Integer getLocation() {
        return location;
    }

    public void setLocation(Integer location) {
        this.location = location;
    }

    public String getDefeatedBy() {
        return defeatedBy;
    }

    public void setDefeatedBy(String defeatedBy) {
        this.defeatedBy = defeatedBy;
    }
}
