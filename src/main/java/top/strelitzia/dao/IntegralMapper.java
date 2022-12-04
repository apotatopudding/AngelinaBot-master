package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.IntegralInfo;

import java.util.List;


@Repository
public interface IntegralMapper {

    //根据姓名查询积分
    List<Integer> selectByName(String name);

    //根据QQ查询积分
    Integer selectByQQ(Long QQ);

    //查询前五的积分榜
    List<IntegralInfo> selectFiveByName();

    //更新积分榜
    Integer integralByGroupId(@Param("groupId") Long groupId, @Param("name") String name, @Param("QQ") Long QQ, @Param("integral") Integer integral);

    //增加指定数量积分
    Integer increaseIntegralByGroupId(Long groupId,Long QQ,Integer num);

    //扣除指定数量积分
    Integer reduceIntegralByGroupId(Long groupId,Long QQ,Integer num);

    //清空清空本月积分
    Integer cleanThisMonth();

    //根据QQ查询当日签到
    Integer selectDayCountByQQ(Long QQ);

    //每日签到清空
    Integer cleanSignCount();

    //每日签到状态改变
    Integer updateByQQ(@Param("qq") Long qq, @Param("name") String name);

}
