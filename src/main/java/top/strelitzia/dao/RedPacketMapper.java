package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.RedPacketInfo;

@Repository
public interface RedPacketMapper {
    //新添加一个红包剩余情况表
    void insectRemain(RedPacketInfo redPacketInfo);

    //更新红包剩余情况表
    void updateRemain(@Param("remain") Integer remain,@Param("id") Integer id);

    //查询红包剩余情况表内信息
    RedPacketInfo selectInfo(@Param("id") Integer id);

    //查询是否存在对应id的红包表
    Integer selectID(@Param("groupId") Long groupId, @Param("id") Integer id);

    //删除对应id的红包信息
    void deleteRemain(@Param("id") Integer id);

    //清空全部红包信息
    void deleteAllRemain();
}
