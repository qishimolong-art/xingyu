package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售单拣货送货图片凭证 Response VO")
@Data
public class ErpSaleOutPickDeliveryFileRespVO {

    private Long id;

    private Long submitId;

    private Integer type;

    private String typeName;

    private Long submitUserId;

    private String submitUserName;

    private LocalDateTime submitTime;

    private Integer itemCount;

    private String remark;

    private String fileUrl;

    private String fileName;

    private String fileType;

    private Integer sort;

}
