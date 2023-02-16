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

//    //特殊专用，用于转移全部订阅的数据字段到新的remind表
//    void create();
//    TempInfo selectAllBirthday();
//    void insertAllBirthday(TempInfo tempInfo);
//    TempInfo selectAllBili();
//    void insertAllBili(TempInfo tempInfo);
//    Long selectAllLookWorld();
//    void insertAllLookWorld(Long groupId);
//    void deleteAllBirthday();
//    void deleteAllBili();
//    void deleteAllLookWorld();
//    Integer selectExist(String name);
}
