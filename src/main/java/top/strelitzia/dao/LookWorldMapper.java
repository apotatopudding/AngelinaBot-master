package top.strelitzia.dao;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author apotatopudding
 * @date 2023/2/2 21:14
 */
@Repository
public interface LookWorldMapper {
    //获取所有已订阅的群组
    List<Long> selectAllGroup();

    //添加订阅群组
    void insertGroupIdWithSubscrbe(Long groupId);

    //删除订阅群组
    void deleteGroupIdWithSubscrbe(Long groupId);
}
