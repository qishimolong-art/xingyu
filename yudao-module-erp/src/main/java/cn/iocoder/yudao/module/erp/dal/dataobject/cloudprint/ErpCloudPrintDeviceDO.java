package cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_cloud_print_device")
@KeySequence("erp_cloud_print_device_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCloudPrintDeviceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String devid;
    private String devKey;
    private String nickname;
    private Integer devType;
    private Integer contentType;
    private Integer printWidth;
    private Integer printHeight;
    private Integer paperType;
    private Integer rotate;
    private Integer copies;
    private Long deptId;
    private Boolean defaulted;
    private Integer status;
    private Integer onlineState;
    private Integer lastStatusCode;
    private String lastStatusMessage;
    private LocalDateTime lastStatusTime;
    private Boolean queuePaused;
    private String queuePauseReason;
    private Long queuePauseTaskId;
    private String remark;

}
