package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * ERP 产品库存明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockRecordMapper extends BaseMapperX<ErpStockRecordDO> {

    /**
     * 五期新方法：支持按产品维度预过滤 productIds
     * 注意：不能重载父类 selectPage(PageParam, Wrapper) 的签名，改用独立方法名。
     */
    default PageResult<ErpStockRecordDO> selectPageWithProductFilter(ErpStockRecordPageReqVO reqVO,
                                                                     Collection<Long> productIdFilter) {
        LambdaQueryWrapperX<ErpStockRecordDO> wrapper = new LambdaQueryWrapperX<ErpStockRecordDO>()
                .eqIfPresent(ErpStockRecordDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockRecordDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpStockRecordDO::getBizType, reqVO.getBizType())
                .likeIfPresent(ErpStockRecordDO::getBizNo, reqVO.getBizNo())
                .betweenIfPresent(ErpStockRecordDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(ErpStockRecordDO::getBizDate, reqVO.getBizDate());
        if (reqVO.getBizTypes() != null && !reqVO.getBizTypes().isEmpty()) {
            wrapper.in(ErpStockRecordDO::getBizType, reqVO.getBizTypes());
        }
        if (productIdFilter != null) {
            if (productIdFilter.isEmpty()) {
                return PageResult.empty(0L);
            }
            wrapper.in(ErpStockRecordDO::getProductId, productIdFilter);
        }
        wrapper.orderByDesc(ErpStockRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /** 原签名：保留向后兼容，委托到新方法 */
    default PageResult<ErpStockRecordDO> selectPage(ErpStockRecordPageReqVO reqVO) {
        return selectPageWithProductFilter(reqVO, null);
    }

    default java.util.List<ErpStockRecordDO> selectListByBiz(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<ErpStockRecordDO>()
                .eq(ErpStockRecordDO::getBizType, bizType)
                .eq(ErpStockRecordDO::getBizId, bizId));
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockRecordDO::getWarehouseId, warehouseId);
    }

}
