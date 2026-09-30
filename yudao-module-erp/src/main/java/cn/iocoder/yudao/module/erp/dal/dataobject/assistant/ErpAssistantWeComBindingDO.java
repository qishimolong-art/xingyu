package cn.iocoder.yudao.module.erp.dal.dataobject.assistant;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_assistant_wecom_binding")
@KeySequence("erp_assistant_wecom_binding_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ErpAssistantWeComBindingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String corpId;
    private String botId;
    private String wecomUserId;
    private Long userId;
    private String mobile;
    private String conversationId;
    private LocalDateTime bindTime;
    private LocalDateTime lastActiveTime;

}
