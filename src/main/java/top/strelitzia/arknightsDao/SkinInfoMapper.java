package top.strelitzia.arknightsDao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.strelitzia.model.SkinGroupInfo;
import top.strelitzia.model.SkinInfo;

import java.util.List;

/**
 * @author wangzy
 * @Date 2021/4/7 17:14
 **/

@Repository
public interface SkinInfoMapper {

    //根据char_id查找干员id和名字写入SkinInfo
    SkinInfo getOperatorInfoByChar(String charId);

    //根据char_id查找特殊召唤物id和名字写入SkinInfo
    SkinInfo getSummonerInfoByChar(String charId);

    //匹配干员名以获取立绘
    List<SkinInfo> selectSkinByName(String operatorName);

    //增量写入封装好的皮肤信息
    void insertBySkinInfo(SkinInfo skinInfo);

    //获取所有已收录时装的skinId
    List<String> selectAllSkinId();

    //根据时装ID获取指定时装的存放路径
    String selectSkinById(String skinId);

    //根据干员名获取干员charId
    String selectCharIdByName(String name);

    //将皮肤系列信息写入表内
    void insertSkinGroupInfo(SkinGroupInfo skinGroupInfo);

    //查询所有已录入的BrandId
    List<String> selectAllBrandId();

    //更新指定时装的系列号
    void updateBrandIdBySkinGroupId(String brandId,String skinGroupId);

    //查询指定名字的时装系列信息
    SkinGroupInfo selectSkinGroupByName(String brandName);

    //查询所有时装系列名字
    List<String> selectAllSkinGroupName();

    //根据时装系列编号查询所有该系列的时装id与名字
    List<SkinInfo> selectSkinInfoByBrandId(String BrandId);

//    List<Integer> selectBase64IsUrl();
//
//    Integer updateBaseStrById(@Param("id") Integer id, @Param("skinBase64") String skinBase64);
}
