package cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_cloud_print_callback_log")
@KeySequence("erp_cloud_print_callback_log_seq")
@TenantIgnore
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCloudPrintCallbackLogDO extends BaseDO {

    @TableId
    private Long id;
    private String rawBody;
    private String method;
    private String devid;
    private String reqid;
    private Integer code;
    private Boolean matched;
    private Integer processStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime processStartedTime;
    private LocalDateTime processedTime;
    private String processToken;
    private String processMessage;

}
