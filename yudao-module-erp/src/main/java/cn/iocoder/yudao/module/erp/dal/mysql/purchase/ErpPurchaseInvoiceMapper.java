package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPurchaseInvoiceMapper extends BaseMapperX<ErpPurchaseInvoiceDO> {

    default PageResult<ErpPurchaseInvoiceDO> selectPage(ErpPurchaseInvoicePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseInvoiceDO> query = new MPJLambdaWrapperX<ErpPurchaseInvoiceDO>()
                .likeIfPresent(ErpPurchaseInvoiceDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseInvoiceDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpPurchaseInvoiceDO::getInvoiceDate, reqVO.getInvoiceDate())
                .eqIfPresent(ErpPurchaseInvoiceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchaseInvoiceDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpPurchaseInvoiceDO::getInvoiceNo, reqVO.getInvoiceNo())
                .likeIfPresent(ErpPurchaseInvoiceDO::getInvoiceType, reqVO.getInvoiceType())
                .likeIfPresent(ErpPurchaseInvoiceDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseInvoiceDO::getCreator, reqVO.getCreator());
        if (Integer.valueOf(0).equals(reqVO.getInvoiceStatus())) {
            query.ne(ErpPurchaseInvoiceDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        } else if (Integer.valueOf(1).equals(reqVO.getInvoiceStatus())) {
            query.eq(ErpPurchaseInvoiceDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        }
        if (reqVO.getProductId() != null || reqVO.getSourceInNo() != null) {
            query.leftJoin(ErpPurchaseInvoiceItemDO.class, ErpPurchaseInvoiceItemDO::getInvoiceId, ErpPurchaseInvoiceDO::getId)
                    .eq(reqVO.getProductId() != null, ErpPurchaseInvoiceItemDO::getProductId, reqVO.getProductId())
                    .likeIfPresent(ErpPurchaseInvoiceItemDO::getSourceInNo, reqVO.getSourceInNo())
                    .groupBy(ErpPurchaseInvoiceDO::getId);
        }
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpPurchaseInvoiceDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpPurchaseInvoiceDO> wrapper, ErpPurchaseInvoicePageReqVO reqVO) {
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (direction == null) {
            wrapper.orderByDesc(ErpPurchaseInvoiceDO::getId);
            return;
        }
        String expression = getOrderExpression(reqVO.getOrderField());
        if (expression != null) {
            wrapper.last("ORDER BY " + expression + " " + direction + ", t.id DESC");
            return;
        }
        SFunction<ErpPurchaseInvoiceDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpPurchaseInvoiceDO::getId);
            return;
        }
        if ("ASC".equals(direction)) {
            wrapper.orderByAsc(orderColumn);
            return;
        }
        wrapper.orderByDesc(orderColumn);
    }

    static String normalizeOrderDirection(String orderDirection) {
        if (orderDirection == null) {
            return null;
        }
        if ("asc".equalsIgnoreCase(orderDirection.trim())) {
            return "ASC";
        }
        if ("desc".equalsIgnoreCase(orderDirection.trim())) {
            return "DESC";
        }
        return null;
    }

    static String getOrderExpression(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "taxPercent":
            case "displayTaxPercent":
                return "(SELECT MAX(pii.tax_percent) FROM erp_purchase_invoice_item pii "
                        + "WHERE pii.deleted = 0 AND pii.tenant_id = t.tenant_id AND pii.invoice_id = t.id)";
            default:
                return null;
        }
    }

    static SFunction<ErpPurchaseInvoiceDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpPurchaseInvoiceDO::getNo;
            case "supplierId":
            case "supplierName":
            case "supplierType":
                return ErpPurchaseInvoiceDO::getSupplierId;
            case "invoiceNo":
                return ErpPurchaseInvoiceDO::getInvoiceNo;
            case "invoiceType":
                return ErpPurchaseInvoiceDO::getInvoiceType;
            case "totalAmount":
                return ErpPurchaseInvoiceDO::getTotalAmount;
            case "taxAmount":
                return ErpPurchaseInvoiceDO::getTaxAmount;
            case "taxExclusiveAmount":
                return ErpPurchaseInvoiceDO::getTaxExclusiveAmount;
            case "status":
                return ErpPurchaseInvoiceDO::getStatus;
            case "creator":
            case "creatorName":
                return ErpPurchaseInvoiceDO::getCreator;
            case "createTime":
                return ErpPurchaseInvoiceDO::getCreateTime;
            case "invoiceDate":
                return ErpPurchaseInvoiceDO::getInvoiceDate;
            case "auditUserName":
            case "updater":
            case "updaterName":
                return ErpPurchaseInvoiceDO::getUpdater;
            case "auditTime":
            case "updateTime":
                return ErpPurchaseInvoiceDO::getUpdateTime;
            case "remark":
                return ErpPurchaseInvoiceDO::getRemark;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPurchaseInvoiceDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchaseInvoiceDO>()
                .eq(ErpPurchaseInvoiceDO::getId, id)
                .eq(ErpPurchaseInvoiceDO::getStatus, status));
    }

    default ErpPurchaseInvoiceDO selectByNo(String no) {
        return selectOne(ErpPurchaseInvoiceDO::getNo, no);
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPurchaseInvoiceDO::getSupplierId, supplierId);
    }

    default String selectFirstNoBySupplierId(Long supplierId) {
        ErpPurchaseInvoiceDO invoice = selectOne(new MPJLambdaWrapperX<ErpPurchaseInvoiceDO>()
                .eq(ErpPurchaseInvoiceDO::getSupplierId, supplierId)
                .orderByDesc(ErpPurchaseInvoiceDO::getId)
                .last("LIMIT 1"));
        return invoice == null ? null : invoice.getNo();
    }
}
