package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/** 套件组装在指定出货仓的可组装数量。 */
@Data
@Accessors(chain = true)
public class ErpAssemblyAvailabilityRespVO {

    private BigDecimal quantityPerSetTotal;
    private Integer maxAssemblableCount;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Long productId;
        private BigDecimal quantityPerSet;
        private BigDecimal stockCount;
        private BigDecimal occupiedCount;
        private BigDecimal availableCount;
        private Integer maxAssemblableCount;
        private String shelf;
    }
}
