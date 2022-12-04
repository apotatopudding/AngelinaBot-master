package top.strelitzia.dao;

import org.springframework.stereotype.Repository;
import top.strelitzia.model.AdminUserInfo;

import java.util.List;

/**
 * @author wangzy
 * @Date 2020/12/26 0:38
 **/
@Repository
public interface AdminUserMapper {

    //查询所有的用户，以及对应的权限信息
    List<AdminUserInfo> selectAllAdmin();

}
