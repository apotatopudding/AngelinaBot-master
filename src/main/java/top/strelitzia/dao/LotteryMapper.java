package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.LotteryInfo;

import java.util.List;

@Repository
public interface LotteryMapper {

    //查询所有已添加的抽奖码合集
    List<Integer> selectLotteryCode();

    //查询指定用户已添加的抽奖码合集
    Integer selectLotteryCodeByQQ(@Param("qq") Long qq);

    //查询指定ID的所有信息
    LotteryInfo selectInfo(Integer lotteryCode);

    //添加指定的抽奖码
    void insertInfo(LotteryInfo lotteryInfo);

    //清除所有抽奖码
    void cleanAll();

    //创建一个本月的中奖历史信息方便写入捐助人
    void insertInfoToHistory(@Param("date") Long date, @Param("benefactor") String benefactor);

    //更新中奖历史信息
    void updateInfoToHistory(LotteryInfo lotteryInfo);

    //更新捐助者名单
    void updateBenefactorToHistory(@Param("date") Long date, @Param("benefactor") String benefactor);

    //查询指定时间的所有信息
    LotteryInfo selectInfoByDate(Long date);

    //查询指定时间的所有捐助者信息
    String selectAllBenefactorByMonth(Long date);

    //修改上期中奖者确认情况
    void updateLastVerify();

    //查询所有已订阅的群组号
    List<Long> selectAllGroupWithSubscribe();

    //添加订阅群组
    void insertGroupIdAboutSubscribe(Long groupId);

    //删除订阅群组
    void deleteGroupIdAboutSubscribe(Long groupId);
}
