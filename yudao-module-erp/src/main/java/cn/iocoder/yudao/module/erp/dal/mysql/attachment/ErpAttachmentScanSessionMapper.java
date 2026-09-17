package cn.iocoder.yudao.module.erp.dal.mysql.attachment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanSessionDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpAttachmentScanSessionMapper extends BaseMapperX<ErpAttachmentScanSessionDO> {

    default ErpAttachmentScanSessionDO selectByTicket(String ticket) {
        return selectOne(ErpAttachmentScanSessionDO::getTicket, ticket);
    }

    default ErpAttachmentScanSessionDO selectByTicketForUpdate(String ticket) {
        return selectOne(new LambdaQueryWrapperX<ErpAttachmentScanSessionDO>()
                .eq(ErpAttachmentScanSessionDO::getTicket, ticket)
                .last("FOR UPDATE"));
    }

    default ErpAttachmentScanSessionDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpAttachmentScanSessionDO>()
                .eq(ErpAttachmentScanSessionDO::getId, id)
                .last("FOR UPDATE"));
    }

    default int updateStatus(Long id, Integer status) {
        return update(new ErpAttachmentScanSessionDO().setStatus(status),
                new LambdaUpdateWrapper<ErpAttachmentScanSessionDO>()
                        .eq(ErpAttachmentScanSessionDO::getId, id));
    }

    default int updateUploadResult(Long id, Integer uploadedCount, Integer status) {
        return update(new ErpAttachmentScanSessionDO()
                        .setUploadedCount(uploadedCount)
                        .setStatus(status),
                new LambdaUpdateWrapper<ErpAttachmentScanSessionDO>()
                        .eq(ErpAttachmentScanSessionDO::getId, id));
    }

}
