package cn.iocoder.yudao.module.erp.dal.mysql.autoorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 采购建议单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchaseSuggestionMapper extends BaseMapperX<ErpPurchaseSuggestionDO> {

    default PageResult<ErpPurchaseSuggestionDO> selectPage(ErpPurchaseSuggestionPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPurchaseSuggestionDO> wrapper = new LambdaQueryWrapperX<ErpPurchaseSuggestionDO>()
                .likeIfPresent(ErpPurchaseSuggestionDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseSuggestionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchaseSuggestionDO::getWarehouseId, reqVO.getWarehouseId())
                .orderByDesc(ErpPurchaseSuggestionDO::getId);
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(), ErpPurchaseSuggestionDO::getNo);
        return selectPage(reqVO, wrapper);
    }

}
