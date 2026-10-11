package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售单按批次拣货送货人员明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpSaleOutPickDeliveryOperationItemRespVO {

    private Long id;
    private Long submitId;
    private Integer type;
    private Long submitUserId;
    private String submitUserName;
    private LocalDateTime submitTime;
    private Long itemId;
    private String productCode;
    private String productName;
    private String standard;
    private Long warehouseId;
    private String warehouseName;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal quantity;

}
