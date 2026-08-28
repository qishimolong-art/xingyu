package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 库存明细商品快照字段补齐。
 */
@Component
public class ErpStockItemSnapshotSupport {

    public Long resolveProductUnitId(Long requestUnitId, ErpProductDO product) {
        return requestUnitId != null ? requestUnitId : product == null ? null : product.getUnitId();
    }

    public Integer resolvePackageQty(Integer requestPackageQty, ErpProductDO product) {
        return requestPackageQty != null ? requestPackageQty : product == null ? null : product.getPackageQty();
    }

    public BigDecimal resolveWeight(BigDecimal requestWeight, ErpProductDO product) {
        return requestWeight != null ? requestWeight : product == null ? null : product.getWeight();
    }

    public BigDecimal calculateTotalWeight(BigDecimal weight, BigDecimal count) {
        return MoneyUtils.priceMultiply(weight, count);
    }

    public void fillStockRecordSnapshot(ErpStockRecordCreateReqBO reqBO, ErpProductDO product) {
        reqBO.setProductUnitId(resolveProductUnitId(reqBO.getProductUnitId(), product));
        reqBO.setPackageQty(resolvePackageQty(reqBO.getPackageQty(), product));
        reqBO.setWeight(resolveWeight(reqBO.getWeight(), product));
        reqBO.setTotalWeight(calculateTotalWeight(reqBO.getWeight(), reqBO.getCount()));
    }

}
