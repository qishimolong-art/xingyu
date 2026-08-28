package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * ERP 商城商品库存汇总 BO
 */
@Data
@Accessors(chain = true)
public class ErpMallStockSummaryBO {

    /**
     * 商城 SPU 编号
     */
    private Long spuId;
    /**
     * 是否已绑定 ERP 产品
     */
    private Boolean mapped;
    /**
     * 绑定关系数量
     */
    private Integer mappingCount;
    /**
     * ERP 可用库存汇总
     */
    private BigDecimal totalAvailableCount;

}
