package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpPurchaseInvoiceOcrItemMapper extends BaseMapperX<ErpPurchaseInvoiceOcrItemDO> {

    default List<ErpPurchaseInvoiceOcrItemDO> selectListByBatchId(Long batchId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchaseInvoiceOcrItemDO>()
                .eq(ErpPurchaseInvoiceOcrItemDO::getBatchId, batchId)
                .orderByAsc(ErpPurchaseInvoiceOcrItemDO::getId));
    }

}
