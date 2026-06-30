package cn.iocoder.yudao.module.erp.controller.admin.product.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理后台 - ERP 商品分类导入结果 Response VO")
public class ErpProductCategoryImportRespVO {

    @Schema(description = "成功条数")
    private Integer successCount = 0;

    @Schema(description = "失败条数")
    private Integer failureCount = 0;

    @Schema(description = "新增条数")
    private Integer createCount = 0;

    @Schema(description = "更新条数")
    private Integer updateCount = 0;

    @Schema(description = "失败明细")
    private List<FailureItem> failureDetails = new ArrayList<>();

    @Data
    public static class FailureItem {

        @Schema(description = "行号", example = "2")
        private Integer rowNo;

        @Schema(description = "分类编码", example = "C001")
        private String code;

        @Schema(description = "失败原因")
        private String reason;

        public FailureItem(Integer rowNo, String code, String reason) {
            this.rowNo = rowNo;
            this.code = code;
            this.reason = reason;
        }
    }

}
