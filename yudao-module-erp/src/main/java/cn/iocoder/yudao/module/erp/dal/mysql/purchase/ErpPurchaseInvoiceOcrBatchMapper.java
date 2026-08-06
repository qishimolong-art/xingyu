package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrBatchPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrBatchDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPurchaseInvoiceOcrBatchMapper extends BaseMapperX<ErpPurchaseInvoiceOcrBatchDO> {

    default PageResult<ErpPurchaseInvoiceOcrBatchDO> selectPage(ErpPurchaseInvoiceOcrBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPurchaseInvoiceOcrBatchDO>()
                .likeIfPresent(ErpPurchaseInvoiceOcrBatchDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(ErpPurchaseInvoiceOcrBatchDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseInvoiceOcrBatchDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(ErpPurchaseInvoiceOcrBatchDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpPurchaseInvoiceOcrBatchDO::getId));
    }

}
