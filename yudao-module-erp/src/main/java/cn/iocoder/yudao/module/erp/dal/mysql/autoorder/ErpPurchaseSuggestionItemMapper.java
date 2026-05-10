package cn.iocoder.yudao.module.erp.dal.mysql.autoorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购建议单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchaseSuggestionItemMapper extends BaseMapperX<ErpPurchaseSuggestionItemDO> {

    default List<ErpPurchaseSuggestionItemDO> selectListBySuggestionId(Long suggestionId) {
        return selectList(ErpPurchaseSuggestionItemDO::getSuggestionId, suggestionId);
    }

    default List<ErpPurchaseSuggestionItemDO> selectListBySuggestionIds(Collection<Long> suggestionIds) {
        return selectList(ErpPurchaseSuggestionItemDO::getSuggestionId, suggestionIds);
    }

    default void deleteBySuggestionId(Long suggestionId) {
        delete(ErpPurchaseSuggestionItemDO::getSuggestionId, suggestionId);
    }

}
