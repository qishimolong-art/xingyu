package cn.iocoder.yudao.module.erp.service.attachment;

import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanSessionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanUploadRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface ErpAttachmentScanService {

    ErpAttachmentScanCreateRespVO createAttachmentScan(ErpAttachmentScanCreateReqVO reqVO, String frontendBaseUrl,
                                                       boolean hashRoute);

    ErpAttachmentScanSessionRespVO getSession(String ticket);

    ErpAttachmentScanUploadRespVO upload(String ticket, MultipartFile file);

    ErpAttachmentScanStatusRespVO getStatus(Long sessionId);

    void cancel(Long sessionId);

}
