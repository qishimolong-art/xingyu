package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 产品、仓库、批次维度的库存关联数量。
 */
@Data
@Accessors(chain = true)
public class ErpStockBatchQuantityDO {

    private Long productId;
    private Long warehouseId;
    private String batchNo;
    private BigDecimal count;

}
