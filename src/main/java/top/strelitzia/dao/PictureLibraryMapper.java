package top.strelitzia.dao;

import org.springframework.stereotype.Repository;
import top.strelitzia.model.PictureLibraryInfo;

import java.util.List;

@Repository
public interface PictureLibraryMapper {

    //查询所有正式发布的已审核图片
    PictureLibraryInfo selectAllPictureByType();

    //查询所有指定类型的未审核图片（审核专用）
    PictureLibraryInfo selectPictureWithoutCheckByType(Integer type);

    //根据ID查询图片信息
    PictureLibraryInfo selectPictureById(Integer pictureId);

    //查询文件夹列表
    List<String> selectFolderList();

    //查询指定文件夹的已审核图片
    List<PictureLibraryInfo> selectAllPictureByFolder(String folder);

    //增加一张图片,获取ID
    void insectPicture(PictureLibraryInfo pictureLibraryInfo);
    Integer selectId();

    //发布为正式图片
    void updateAuditAndType(Integer pictureId,Integer audit);

    //删除一张图片记录
    void deletePictureByPictureId(Integer pictureId);

    //查询ID对应的上传者QQ
    Long selectUploadQQByPictureId(Integer pictureId);

    //查找审核员
    Integer selectAudit(Long qq);

    //添加审核员
    void insertAudit(Long qq);

    //删除审核员
    void deleteAudit(Long qq);

    //查找存图黑名单
    Integer selectBlack(Long qq);

    //添加存图黑名单
    void insertBlack(Long qq);

    //删除存图黑名单
    void deleteBlack(Long qq);
}
