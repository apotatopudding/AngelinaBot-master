package top.strelitzia.dao;

import org.apache.ibatis.annotations.Param;
import top.strelitzia.model.TarotInfo;

public interface TarotMapper {

    //更新塔罗牌抽取数目到数据库中
    Integer updateTarotByQQ(@Param("qq") Long qq, @Param("name") String name, @Param("tarotCount") Integer tarotCount);

    //更新塔罗牌的卡牌信息到数据库中
    Integer updateTarotCardByQQ(@Param("qq") Long qq, @Param("tarotCard1") String tarotCard1, @Param("tarotCard2") String tarotCard2,@Param("tarotCard3") String tarotCard3);

    //根据QQ查询塔罗牌抽牌信息
    TarotInfo selectTarotByQQ(Long qq);

    //清空每日塔罗牌抽牌次数数据
    Integer cleanTarotCount();

    //根据QQ查询第一张塔罗牌的抽牌信息
    TarotInfo selectCard1ByQQ(Long qq);

    //根据QQ查询第二张塔罗牌的抽牌信息
    TarotInfo selectCard2ByQQ(Long qq);

    //根据QQ查询第三张塔罗牌的抽牌信息
    TarotInfo selectCard3ByQQ(Long qq);
}
