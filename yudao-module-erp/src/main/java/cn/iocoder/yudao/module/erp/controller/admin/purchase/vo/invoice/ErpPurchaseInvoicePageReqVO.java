package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购票据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInvoicePageReqVO extends PageParam {

    @Schema(description = "票据单号", example = "CGPJ202605270001")
    private String no;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "开票日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] invoiceDate;

    @Schema(description = "单据状态", example = "10")
    private Integer status;

    @Schema(description = "开票完成状态：0-未完成，1-已完成", example = "0")
    private Integer invoiceStatus;

    @Schema(description = "部门编号", example = "10")
    private Long deptId;

    @Schema(description = "发票号", example = "033001900111")
    private String invoiceNo;

    @Schema(description = "票据类型", example = "增值税专用发票")
    private String invoiceType;

    @Schema(description = "备注", example = "首批")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "经手人用户编号", example = "1")
    private Long handlerId;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "来源入库单号", example = "CGRK202605270001")
    private String sourceInNo;

    @Schema(description = "勾选导出的采购票据编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "排序字段", example = "createTime")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

}
