package cn.iocoder.yudao.module.erp.service.purchase.cost;

import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 固定版本、排序及长度编码；签名不依赖UI分页或可变更新时间。 */
public final class ErpPurchaseCostSignature {
    private ErpPurchaseCostSignature() { }
    public static String snapshot(ErpPurchaseInDO h,List<ErpPurchaseInItemDO> items) {
        StringBuilder s=new StringBuilder();
        append(s,"PURCHASE_COST_SOURCE_V1",h.getId(),h.getNo(),h.getSupplierId(),h.getDeptId(),h.getOrderId(),
                h.getInTime(),h.getTotalCount(),h.getTotalProductPrice(),h.getTotalPrice(),h.getDiscountPercent(),
                h.getDiscountPrice(),h.getFeeAmount(),h.getOtherPrice(),h.getPurchaseDiscount(),
                h.getTotalFreight1(),h.getTotalFreight2(),h.getTaxRate(),h.getTotalTaxPrice(),"APPROVAL_STOCK_POSTING");
        items.stream().sorted(Comparator.comparing(ErpPurchaseInItemDO::getId)).forEach(i -> append(s,
                i.getId(),i.getInId(),i.getOrderItemId(),i.getProductId(),i.getWarehouseId(),i.getDeptId(),
                i.getCount(),i.getProductPrice(),i.getTotalPrice(),i.getGift(),i.getBatchNo(),i.getProductUnitId(),
                i.getPackageQty(),i.getWholeQty(),i.getTaxPercent(),i.getTaxPrice(),i.getOriginalProductPrice(),i.getAdjustId()));
        return s.toString();
    }
    public static String requestHash(ConfirmRequest r) {
        StringBuilder s=new StringBuilder();
        append(s,r.getPurchaseInId(),r.getExpectedSignature(),r.getExpectedRevision(),r.getEvidence(),r.getFeeTreatment());
        r.getItems().stream().sorted(Comparator.comparing(ConfirmItem::getSourceItemId)).forEach(i ->
                append(s,i.getSourceItemId(),i.getConfirmedNetTotalAmount(),i.getEvidence()));
        return hash(s.toString());
    }
    public static String hash(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder s=new StringBuilder(); for(byte b:bytes) s.append(String.format("%02x",b & 255));
            return s.toString();
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static void append(StringBuilder s,Object... values) {
        for(Object value:values) {
            String v=value==null?"":value instanceof BigDecimal?((BigDecimal)value).stripTrailingZeros().toPlainString():value.toString();
            s.append(value==null?-1:v.length()).append(':').append(v);
        }
    }
}
