package cn.iocoder.yudao.module.erp.controller.app.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.app.stock.vo.AppErpMallStockOptionRespVO;
import cn.iocoder.yudao.module.erp.controller.app.stock.vo.AppErpMallStockSummaryRespVO;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - ERP 商城库存")
@RestController
@RequestMapping("/erp/stock/app")
@Validated
public class AppErpMallStockController {

    @Resource
    private ErpMallStockService mallStockService;

    @GetMapping("/mall-summary")
    @Operation(summary = "获得商城商品库存汇总")
    @Parameter(name = "spuId", description = "商城 SPU 编号", required = true, example = "1024")
    public CommonResult<AppErpMallStockSummaryRespVO> getMallStockSummary(
            @RequestParam("spuId") Long spuId) {
        AppErpMallStockSummaryRespVO respVO = new AppErpMallStockSummaryRespVO()
                .setSpuId(spuId)
                .setTotalAvailableCount(mallStockService.getMallStockSummary(spuId));
        return success(respVO);
    }

    @GetMapping("/mall-options")
    @Operation(summary = "获得商城商品可购买库存选项")
    @Parameter(name = "spuId", description = "商城 SPU 编号", required = true, example = "1024")
    @Parameter(name = "skuId", description = "商城 SKU 编号", required = true, example = "2048")
    public CommonResult<List<AppErpMallStockOptionRespVO>> getMallStockOptions(
            @RequestParam("spuId") Long spuId,
            @RequestParam("skuId") Long skuId) {
        return success(BeanUtils.toBean(mallStockService.getMallStockOptions(spuId, skuId),
                AppErpMallStockOptionRespVO.class));
    }

}
