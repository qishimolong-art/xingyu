package cn.iocoder.yudao.module.erp.dal.mysql.attachment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpAttachmentScanFileMapper extends BaseMapperX<ErpAttachmentScanFileDO> {

    default List<ErpAttachmentScanFileDO> selectListBySessionId(Long sessionId) {
        return selectList(new LambdaQueryWrapperX<ErpAttachmentScanFileDO>()
                .eq(ErpAttachmentScanFileDO::getSessionId, sessionId)
                .orderByDesc(ErpAttachmentScanFileDO::getCreateTime));
    }

}
