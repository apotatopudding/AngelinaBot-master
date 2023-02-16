package top.strelitzia.arknightsDao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.*;

/**
 * @author strelitzia
 * @Date 2020/12/19 18:42
 **/
@Repository
public interface UpdateMapper {

    //插入一个干员信息
    void insertOperator(OperatorInfo operatorInfo);

    //插入一个召唤物信息
    void insertSummoner(OperatorInfo operatorInfo);

    //根据名字查询一个干员id
    Integer selectOperatorIdByName(String name);

    //根据charId查询一个干员数据库编号
    Integer selectOperatorIdByCharId(String charId);

    //根据charId查询一个召唤物数据库编号
    Integer selectSummonerIdByCharId(String charId);

    //插入一个干员精英化材料信息
    void insertOperatorEvolve(OperatorEvolveInfo operatorEvolveInfo);

    //插入一个干员技能信息
    void insertOperatorSkill(OperatorSkillInfo operatorSkillInfo);

    //插入一个干员天赋信息
    void insertOperatorTalent(TalentInfo talentInfo);

    //根据技能名获取技能id
    Integer selectSkillIdByName(String SkillName);

    //插入一个技能升级材料信息
    void insertSkillMater(SkillMaterInfo skillMaterInfo);

    //插入一个材料合成公式
    Integer insertMaterialMade(@Param("materialId") String material_id, @Param("useMaterialId") Integer useMaterialId, @Param("useNumber") Integer useNumber);

    //清空未知charId的数据
    Integer clearUnknownData();

    //清空地图信息
    Integer clearMatrixData();

    //查询地图掉落条数
    Integer selectMatrixCount();

    //更新干员面板数据
    void updateOperatorData(OperatorData operatorData);

    //更新干员面板数据
    void updateSummonerData(OperatorData operatorData);

    //更新地图数据
    Integer updateStageData(MapJson mapJson);

    //更新章节数据
    Integer updateZoneData(ZoneJson zoneJson);

    //更新材料数据
    Integer updateItemData(@Param("id") String id, @Param("name") String name, @Param("icon") String icon);

    //更新掉落数据
    Integer updateMatrixData(@Param("stageId") String stageId, @Param("itemId") Integer itemId
            , @Param("quantity") Integer quantity, @Param("times") Integer times);

    Integer updateEnemy(EnemyInfo enemyInfo);

    Integer insertTags(@Param("charId") String charId,@Param("name") String name, @Param("rarity") Integer rarity, @Param("tags") String tags);

    String getVersion();

    Integer updateVersion(String newVersion);

    Integer insertVersion();

    //更新干员基础信息
    void updateOperatorInfo(OperatorBasicInfo operatorInfo);

    Integer updateCVNameByOperatorId(OperatorBasicInfo operatorBasicInfo);

    //插入干员技能信息
    void updateSkillDesc(SkillDesc skillDesc);

//    //写入卡池信息
//    void insertGachePool(GachePoolInfo gachePoolInfo);

}
