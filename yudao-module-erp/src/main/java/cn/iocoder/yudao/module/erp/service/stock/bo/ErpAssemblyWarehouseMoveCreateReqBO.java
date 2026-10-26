package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套件组装完成后自动生成并完成仓库移货单的内部参数。
 */
@Data
@Accessors(chain = true)
public class ErpAssemblyWarehouseMoveCreateReqBO {

    private Long assemblyOrderId;
    private String assemblyOrderNo;
    private Long productId;
    private Long productUnitId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String fromShelf;
    private String toShelf;
    private String batchNo;
    private BigDecimal count;
    /** 组装成品入来源仓后形成的来源仓移动平均成本。 */
    private BigDecimal costPrice;
    private Long deptId;
    private Long handlerId;
    private LocalDateTime moveTime;
    private String remark;

}
