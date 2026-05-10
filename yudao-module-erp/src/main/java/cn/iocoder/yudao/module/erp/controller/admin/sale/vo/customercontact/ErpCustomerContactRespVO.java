package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户联系人 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCustomerContactRespVO {

    @Schema(description = "编号")
    @ExcelProperty("编号")
    private Long id;
    private Long customerId;
    @ExcelProperty("联系人")
    private String name;
    @ExcelProperty("手机号")
    private String mobile;
    private String telephone;
    private String email;
    private String position;
    private String wechat;
    private String qq;
    private String address;
    private Boolean primaryContact;
    private Boolean receiverContact;
    private Boolean settleContact;
    private Boolean messageContact;
    private String businessCardFrontUrl;
    private String businessCardBackUrl;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

}
