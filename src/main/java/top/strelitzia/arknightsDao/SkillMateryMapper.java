package top.strelitzia.arknightsDao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.MaterialInfo;
import top.strelitzia.model.OperatorName;

import java.util.List;

/**
 * @author wangzy
 * @Date 2020/12/14 11:12
 **/

@Repository
public interface SkillMateryMapper {

    //根据技能ID和技能等级查询所需材料
    List<MaterialInfo> selectSkillUpByIdAndLevel(@Param("skillId") Integer skillId, @Param("level") Integer level);

    //干员名/技能名查询相关技能ID
    List<Integer> selectSkillIDByAgentOrSkill(@Param("name") String name);

    String selectSkillPngByName(@Param("skillId") Integer skillId);

    String selectSkillNameById(@Param("skillId") Integer skillId);

    OperatorName selectOperatorNameById(@Param("skillId") Integer skillId);
}
