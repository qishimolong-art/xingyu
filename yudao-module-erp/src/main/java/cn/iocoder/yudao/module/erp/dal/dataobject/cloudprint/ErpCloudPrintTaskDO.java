package cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_cloud_print_task")
@KeySequence("erp_cloud_print_task_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCloudPrintTaskDO extends BaseDO {

    @TableId
    private Long id;
    private String reqid;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Long warehouseId;
    private String warehouseName;
    private String devid;
    private Long deviceId;
    private Long templateId;
    private Integer contentType;
    private Integer copies;
    private String fileUrl;
    private String fileName;
    private Integer status;
    private String submitReq;
    private String submitResp;
    private LocalDateTime submitTime;
    private Integer callbackCode;
    private String callbackMsg;
    private LocalDateTime callbackTime;
    private Long retryOf;
    private String errorMsg;

}
