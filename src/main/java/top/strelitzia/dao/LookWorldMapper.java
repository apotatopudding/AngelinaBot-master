package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookWorldMapper {
    //查询当前群组订阅状态
    Integer selectStateByGroupId(Long groupId);

    //更新订阅状态
    Integer insertStateByGroupId(@Param("groupId")Long groupId, @Param("lookWorld")Integer lookWorld);

    //查询已订阅的群组
    List<Long> selectGroupIdBySubscribe();

    //查询当前群组查阅次数
    Integer selectTimeByGroupId(Long groupId);

    //更新当日群组查阅次数
    Integer insertTimeByGroupId(@Param("groupId")Long groupId);

    //清空当日所有群组查阅次数
    Integer updateTime();
}
