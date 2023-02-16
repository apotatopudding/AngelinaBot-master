package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.BiliCount;

import java.util.List;

/**
 * 关注uid放在num_1
 * @author wangzy
 * @Date 2021/1/12 17:13
 **/
@Repository
public interface BiliMapper {
    //获取所有正在监听的uid
    List<BiliCount> getBiliCountList();

    //获取指定群关注列表的名字和uid信息
    List<BiliCount> getFocusListByGroupId(@Param("groupId") Long groupId);

    //更新uid的动态列表
    void updateNewDynamic(BiliCount bili);

    //根据up主昵称获取动态
    BiliCount getOneDynamicByName(String name);

    //某群关注某uid
    void insertGroupBiliRel(@Param("groupId") Long groupId, @Param("uid") Long uid);

    //某群取关某uid
    void deleteBiliByUID(@Param("groupId") Long groupId, @Param("uid") Long uid);

    //某群取关某名字UP
    void deleteBiliByName(@Param("groupId") Long groupId, @Param("name") String name);

    //查询指定群号关注的所有uid
    List<Long> selectUidByGroup(@Param("groupId") Long groupId);

    //查询指定群号关注的所有uid
    List<String> selectNameByGroup(@Param("groupId") Long groupId);

    //查询改uid是否已监听
    Integer existBiliUid(@Param("uid") Long uid);

    //将某uid加入监听
    void insertBiliUid(@Param("uid") Long uid);

    //查询uid被哪些群关注
    List<Long> selectGroupByUid(@Param("uid") String uid);

}
