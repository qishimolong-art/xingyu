package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 调拨出入库台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockTransferLedgerPageReqVO extends PageParam {

    @Schema(description = "业务日期范围", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务日期范围不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] moveTime;

    private Long fromDeptId;
    private Long toDeptId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Long productId;
    private String productKeyword;
    private String transferOutNo;
    private String transferInNo;
    private String sourceNo;

    @InEnum(ErpAuditStatus.class)
    private Integer status;

    @Schema(description = "匹配状态：NORMAL、ABNORMAL")
    private String matchStatus;
    private String exceptionCode;
    private String keyword;
    private String orderField;
    private String orderDirection;

}
