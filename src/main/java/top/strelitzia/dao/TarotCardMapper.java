package top.strelitzia.dao;

import org.springframework.stereotype.Repository;
import top.strelitzia.model.TarotCardInfo;

import java.util.List;

@Repository
public interface TarotCardMapper {

    List<TarotCardInfo> selectTarotCardByID(String tarotCardId);

}
