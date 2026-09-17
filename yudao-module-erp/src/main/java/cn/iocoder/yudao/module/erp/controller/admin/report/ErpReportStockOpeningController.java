package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostPostingService;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpOpeningDateTimeDeserializer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 经营报表切换期初核对入口；权限不由经营报表父菜单自动授予。 */
@RestController
@RequestMapping("/erp/report-stock-opening")
@Validated
public class ErpReportStockOpeningController {
    @Resource
    private ErpDualCostPostingService postingService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;

    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPermission('erp:report-stock-opening:confirm')")
    public CommonResult<Boolean> confirm(@Valid @RequestBody OpeningRequest request) {
        warehouseService.validateCurrentUserStockWarehousePermission(Collections.singleton(request.getWarehouseId()));
        Long operator = getLoginUserId();
        if (operator == null) {
            throw new IllegalStateException("期初核对必须由已登录的授权人员操作");
        }
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(request.getProductId(), request.getWarehouseId()));
        if (stock == null) {
            throw new IllegalStateException("请先建立可核对的库存记录");
        }
        warehouseService.validateCurrentUserStockPermission(Collections.singleton(stock));
        postingService.confirmOpening(request.getProductId(), request.getWarehouseId(), request.getQuantity(),
                request.getFinancialAmount(), request.getSettlementAmount(), request.getCutoverAt(),
                request.getEvidence(), operator);
        return success(true);
    }

    @Data
    public static class OpeningRequest {
        @NotNull @Positive private Long productId;
        @NotNull @Positive private Long warehouseId;
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 6) private BigDecimal quantity;
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 6) private BigDecimal financialAmount;
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 6) private BigDecimal settlementAmount;
        @NotNull @JsonDeserialize(using=ErpOpeningDateTimeDeserializer.class) private LocalDateTime cutoverAt;
        @NotBlank @Size(max = 500) private String evidence;
    }
}
