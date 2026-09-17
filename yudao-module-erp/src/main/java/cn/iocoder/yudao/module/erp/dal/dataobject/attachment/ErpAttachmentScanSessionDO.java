package cn.iocoder.yudao.module.erp.dal.dataobject.attachment;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_attachment_scan_session")
@KeySequence("erp_attachment_scan_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpAttachmentScanSessionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String ticket;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String title;

    private Integer status;

    private Integer uploadedCount;

    private Integer maxFileCount;

    private LocalDateTime expireTime;

}
