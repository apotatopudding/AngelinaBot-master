package top.strelitzia.dao;

import top.strelitzia.model.GroupAdminInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strelitzia
 * @Date 2021/3/17 17:52
 **/
public interface GroupAdminInfoMapper {

    GroupAdminInfo getGroupAdminNum(Long groupId);

    List<GroupAdminInfo> getAllGroupAdmin(Integer current);

    Integer getAllGroupAdminCount();

    Integer insertGroupId(Long groupId);

    Integer updateGroupAdmin(GroupAdminInfo groupAdminInfo);

    Integer existGroupId(@Param("groupId")Long groupId);

    //查询当前群组特设积分状态
    Integer selectBySetting(Long groupId);

    //更新特设积分
    Integer insertByGroupId(@Param("groupId")Long groupId,@Param("integral")Integer integral);

}
