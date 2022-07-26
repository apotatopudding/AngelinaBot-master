package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import top.strelitzia.model.OperatorSelectInfo;


public interface OperatorSelectMapper {

    //根据群组ID查询当前题目选中的干员名字
    OperatorSelectInfo selectIdByGroupId(Long groupId);

    //根据群组ID查询当前题目序号以及尝试次数
    OperatorSelectInfo OperatorNumByGroupID(Long groupId);

    //根据群组ID放入选中的干员ID
    Integer selectId1ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId2ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId3ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId4ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId5ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId6ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId7ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId8ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId9ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);
    Integer selectId10ByGroupId(@Param("groupId") Long groupId, @Param("selectId") int selectId);

    //根据群组ID更新题目的序号（自加一）
    Integer topicNumByGroupId(@Param("groupId") Long groupId);

    //根据群组ID更新猜测的次数（自加一）
    Integer tryNumByGroupId(@Param("groupId") Long groupId);

    //清空清空猜干员所有数据
    Integer cleanAllInfo(@Param("groupId") Long groupId);

    //清空清空猜干员尝试次数
    Integer cleanTryNum(@Param("groupId") Long groupId);

}
