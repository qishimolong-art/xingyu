package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 会计科目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAccountingSubjectPageReqVO extends PageParam {

    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称", example = "现金")
    private String subjectName;

    @Schema(description = "科目大类：1-资产 2-负债 3-共同 4-权益 5-成本 6-损益", example = "1")
    private Integer subjectCategory;

    @Schema(description = "父科目编码", example = "1001")
    private String parentCode;

    @Schema(description = "层级", example = "1")
    private Integer subjectLevel;

    @Schema(description = "是否末级", example = "true")
    private Boolean isLeaf;

    @Schema(description = "是否启用", example = "true")
    private Boolean enable;

    @Schema(description = "凭证类型：1-客户 2-连锁", example = "1")
    private Integer voucherType;

}
