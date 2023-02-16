package top.strelitzia.arknightsDao;

import org.springframework.stereotype.Repository;
import top.strelitzia.model.BuildingSkill;

import java.util.List;

/**
 * @author wangzy
 * @Date 2021/3/31 16:19
 **/
@Repository
public interface BuildingSkillMapper {
    Integer insertBuildingSkill(BuildingSkill buildingSkill);

    List<BuildingSkill> getBuildingSkillByInfo(String Info);

    List<BuildingSkill> getAllBuildingSkill();
}
