package cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 配件品牌 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpProductBrandRespVO {

    @Schema(description = "品牌编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("品牌编号")
    private Long id;

    @Schema(description = "品牌名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "博世")
    @ExcelProperty("品牌名称")
    private String name;

    @Schema(description = "品牌状态", example = "0")
    @ExcelProperty("品牌状态")
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
