package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPurchaseInvoiceMapper extends BaseMapperX<ErpPurchaseInvoiceDO> {

    default PageResult<ErpPurchaseInvoiceDO> selectPage(ErpPurchaseInvoicePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseInvoiceDO> query = new MPJLambdaWrapperX<ErpPurchaseInvoiceDO>()
                .likeIfPresent(ErpPurchaseInvoiceDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseInvoiceDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpPurchaseInvoiceDO::getInvoiceDate, reqVO.getInvoiceDate())
                .eqIfPresent(ErpPurchaseInvoiceDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseInvoiceDO::getInvoiceNo, reqVO.getInvoiceNo())
                .likeIfPresent(ErpPurchaseInvoiceDO::getInvoiceType, reqVO.getInvoiceType())
                .likeIfPresent(ErpPurchaseInvoiceDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseInvoiceDO::getCreator, reqVO.getCreator())
                .orderByDesc(ErpPurchaseInvoiceDO::getId);
        if (reqVO.getProductId() != null || reqVO.getSourceInNo() != null) {
            query.leftJoin(ErpPurchaseInvoiceItemDO.class, ErpPurchaseInvoiceItemDO::getInvoiceId, ErpPurchaseInvoiceDO::getId)
                    .eq(reqVO.getProductId() != null, ErpPurchaseInvoiceItemDO::getProductId, reqVO.getProductId())
                    .likeIfPresent(ErpPurchaseInvoiceItemDO::getSourceInNo, reqVO.getSourceInNo())
                    .groupBy(ErpPurchaseInvoiceDO::getId);
        }
        return selectJoinPage(reqVO, ErpPurchaseInvoiceDO.class, query);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPurchaseInvoiceDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchaseInvoiceDO>()
                .eq(ErpPurchaseInvoiceDO::getId, id)
                .eq(ErpPurchaseInvoiceDO::getStatus, status));
    }

    default ErpPurchaseInvoiceDO selectByNo(String no) {
        return selectOne(ErpPurchaseInvoiceDO::getNo, no);
    }
}
