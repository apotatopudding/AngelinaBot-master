package top.strelitzia.dao;


import org.apache.ibatis.annotations.Param;
import top.strelitzia.model.OperatorGuessInfo;

public interface OperatorGuessMapper {

    //根据名字查找干员档案
    OperatorGuessInfo getOperatorInfoByName(@Param("name")String name);

    //根据ID查找名字
    OperatorGuessInfo getOperatorInfoById(@Param("operatorId")Integer operatorId);

    //随机挑选出一个干员
    OperatorGuessInfo getOperatorName();
}
