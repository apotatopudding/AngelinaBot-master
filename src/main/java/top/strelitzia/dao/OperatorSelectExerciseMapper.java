package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import top.strelitzia.model.OperatorSelectExerciseInfo;


public interface OperatorSelectExerciseMapper {

    //根据群组ID查询当前题目选中的干员名字
    OperatorSelectExerciseInfo selectIdByGroupId(Long groupId);

    //根据群组ID查询当前尝试次数
    OperatorSelectExerciseInfo OperatorNumByGroupID(Long groupId);

    //根据群组ID放入选中的干员ID
    Integer selectId(@Param("groupId") Long groupId, @Param("selectId") Integer selectId);

    //根据群组ID更新猜测的次数（自加一）
    Integer tryNumByGroupId(@Param("groupId") Long groupId);

    //清空清空所有数据
    Integer cleanNum(@Param("groupId") Long groupId);

}
