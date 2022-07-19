package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import top.strelitzia.model.IntegralInfo;

import java.util.List;


public interface IntegralMapper {

    //根据姓名查询积分
    Integer selectByName(String name);

    //根据QQ查询积分
    Integer selectByQQ(Long QQ);

    //查询前五的积分榜
    List<IntegralInfo> selectFiveByName();

    //更新积分榜
    Integer integralByGroupId(@Param("groupId") Long groupId, @Param("name") String name, @Param("QQ") Long QQ, @Param("integral") Integer integral);

    //清空清空所有数据
    Integer cleanThisWeek();

    //扣除十点积分以开始绝地作战
    Integer minusTenPointsByGroupId(@Param("groupId") Long groupId,@Param("QQ") Long QQ);

}
