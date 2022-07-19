package top.strelitzia.dao;

import top.strelitzia.model.TarotCardInfo;

import java.util.List;

public interface TarotCardMapper {

    List<TarotCardInfo> selectTarotCardByID(String tarotCardId);

}
