package top.strelitzia.dao;

import org.springframework.stereotype.Repository;
import top.strelitzia.model.GroupAdminInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strelitzia
 * @Date 2021/3/17 17:52
 **/
@Repository
public interface GroupAdminInfoMapper {

    GroupAdminInfo getGroupAdminNum(Long groupId);

    List<GroupAdminInfo> getAllGroupAdmin(Integer current);

    Integer getAllGroupAdminCount();

    Integer insertGroupId(Long groupId);

    Integer updateGroupAdmin(GroupAdminInfo groupAdminInfo);

    Integer existGroupId(@Param("groupId")Long groupId);

    //查询当前群组特设积分状态
    Integer selectIntegralBySetting(Long groupId);

    //更新特设积分
    Integer insertIntegralByGroupId(@Param("groupId")Long groupId,@Param("integral")Integer integral);

}
