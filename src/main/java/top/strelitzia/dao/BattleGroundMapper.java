package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.BattleGroundInfo;

import java.util.List;

/**
 * @author wangzy
 * @Date 2020/12/26 0:38
 **/
@Repository
public interface BattleGroundMapper {

    //创建新的成员行信息
    Integer insertInfo(BattleGroundInfo battleGroundInfo);

    //查询所有群组是否有对应的群组(有则返回数量）
    Integer selectAllGroup(@Param("groupId")Long groupId);

    //查询对应群组内对应QQ号
    List<Long> selectAllQQByGroup(@Param("groupId")Long groupId,@Param("QQ")Long QQ);

    //查询所有同区域的人
    List<Long> selectQQBySameArea(@Param("groupId")Long groupId,@Param("location")Integer location);

    //查询所有血量不为0的人
    List<Long> selectQQByHealth(@Param("groupId")Long groupId);

    //查询个人信息属性
    BattleGroundInfo selectInfoByGroupAndQQ(@Param("groupId")Long groupId,@Param("QQ")Long QQ);

    //更新属性数据
    Integer updateInfoByGroupAndQQ(BattleGroundInfo battleGroundInfo);

    //更新地点数据
    Integer updateLocationByGroupAndQQ(BattleGroundInfo battleGroundInfo);

    //按群组删除
    Integer deleteInfoByGroup(@Param("groupId")Long groupId);

}
