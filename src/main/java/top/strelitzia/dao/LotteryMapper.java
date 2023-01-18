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

    //写入中奖历史
    void insertInfoToHistory(@Param("date") Long date,@Param("lotteryCode") Integer lotteryCode,@Param("qq") Long qq,@Param("name") String name);

    //查询上期中奖中奖号码
    Integer selectLastCode();

    //查询上期中奖中奖QQ
    Long selectLastQQ();

    //查询上期中奖者确认情况
    Integer selectLastVerify();

    //修改上期中奖者确认情况
    void updateLastVerify();

}
