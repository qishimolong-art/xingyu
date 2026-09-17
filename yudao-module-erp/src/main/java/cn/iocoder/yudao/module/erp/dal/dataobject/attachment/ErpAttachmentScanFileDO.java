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

@TableName("erp_attachment_scan_file")
@KeySequence("erp_attachment_scan_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpAttachmentScanFileDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long sessionId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

}
