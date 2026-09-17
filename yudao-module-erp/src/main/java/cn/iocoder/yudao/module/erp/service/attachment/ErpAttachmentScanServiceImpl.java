package cn.iocoder.yudao.module.erp.service.attachment;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanSessionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanUploadRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanFileDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanSessionDO;
import cn.iocoder.yudao.module.erp.dal.mysql.attachment.ErpAttachmentScanFileMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.attachment.ErpAttachmentScanSessionMapper;
import cn.iocoder.yudao.module.erp.enums.attachment.ErpAttachmentScanStatusEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_CANCELED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_FILE_COUNT_EXCEEDED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_FILE_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_NOT_OWNER;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_SESSION_NOT_EXISTS;

@Service
@Validated
public class ErpAttachmentScanServiceImpl implements ErpAttachmentScanService {

    private static final int DEFAULT_MAX_FILE_COUNT = 9;
    private static final int EXPIRE_MINUTES = 10;
    private static final long MAX_FILE_SIZE = 16L * 1024 * 1024;
    private static final String DEFAULT_TITLE = "扫码上传附件";
    private static final String FILE_DIRECTORY = "erp/attachment-scan";

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "pdf", "doc", "docx", "xls", "xlsx"));
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    @Resource
    private ErpAttachmentScanSessionMapper attachmentScanSessionMapper;
    @Resource
    private ErpAttachmentScanFileMapper attachmentScanFileMapper;
    @Resource
    private FileApi fileApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpAttachmentScanCreateRespVO createAttachmentScan(ErpAttachmentScanCreateReqVO reqVO,
                                                             String frontendBaseUrl,
                                                             boolean hashRoute) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpAttachmentScanSessionDO session = new ErpAttachmentScanSessionDO()
                .setTicket(generateTicket())
                .setBizType(reqVO.getBizType())
                .setBizId(reqVO.getBizId())
                .setBizNo(reqVO.getBizNo())
                .setTitle(StrUtil.blankToDefault(reqVO.getTitle(), DEFAULT_TITLE))
                .setStatus(ErpAttachmentScanStatusEnum.WAITING.getStatus())
                .setUploadedCount(0)
                .setMaxFileCount(reqVO.getMaxFileCount() == null ? DEFAULT_MAX_FILE_COUNT : reqVO.getMaxFileCount())
                .setExpireTime(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        attachmentScanSessionMapper.insert(session);

        return new ErpAttachmentScanCreateRespVO()
                .setSessionId(session.getId())
                .setTicket(session.getTicket())
                .setQrUrl(buildQrUrl(frontendBaseUrl, session.getTicket(), tenantId, hashRoute))
                .setExpireTime(session.getExpireTime())
                .setStatus(session.getStatus());
    }

    @Override
    public ErpAttachmentScanSessionRespVO getSession(String ticket) {
        ErpAttachmentScanSessionDO session = validateSessionExists(ticket);
        refreshExpiredStatus(session);
        return toSessionRespVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpAttachmentScanUploadRespVO upload(String ticket, MultipartFile file) {
        ErpAttachmentScanSessionDO session = validateAvailableSessionForUpload(ticket);
        validateFile(file);

        byte[] content;
        try {
            content = IoUtil.readBytes(file.getInputStream());
        } catch (IOException ex) {
            throw new ServiceException(ATTACHMENT_SCAN_FILE_TYPE_INVALID);
        }
        String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "attachment");
        String fileUrl = fileApi.createFile(content, fileName, FILE_DIRECTORY, file.getContentType());

        ErpAttachmentScanFileDO fileDO = new ErpAttachmentScanFileDO()
                .setSessionId(session.getId())
                .setFileName(fileName)
                .setFileUrl(fileUrl)
                .setFileType(file.getContentType())
                .setFileSize(file.getSize());
        attachmentScanFileMapper.insert(fileDO);

        int uploadedCount = session.getUploadedCount() + 1;
        attachmentScanSessionMapper.updateUploadResult(session.getId(), uploadedCount,
                ErpAttachmentScanStatusEnum.UPLOADED.getStatus());
        fileDO.setCreateTime(LocalDateTime.now());

        return (ErpAttachmentScanUploadRespVO) new ErpAttachmentScanUploadRespVO()
                .setUploadedCount(uploadedCount)
                .setStatus(ErpAttachmentScanStatusEnum.UPLOADED.getStatus())
                .setFileId(fileDO.getId())
                .setFileName(fileDO.getFileName())
                .setFileUrl(fileDO.getFileUrl())
                .setFileType(fileDO.getFileType())
                .setFileSize(fileDO.getFileSize())
                .setUploadTime(fileDO.getCreateTime());
    }

    @Override
    public ErpAttachmentScanStatusRespVO getStatus(Long sessionId) {
        ErpAttachmentScanSessionDO session = validateSessionExists(sessionId);
        validateCreator(session);
        refreshExpiredStatus(session);
        List<ErpAttachmentScanFileRespVO> files = toFileRespVOList(
                attachmentScanFileMapper.selectListBySessionId(session.getId()));
        return (ErpAttachmentScanStatusRespVO) new ErpAttachmentScanStatusRespVO()
                .setFiles(files)
                .setSessionId(session.getId())
                .setTitle(session.getTitle())
                .setBizType(session.getBizType())
                .setBizNo(session.getBizNo())
                .setExpireTime(session.getExpireTime())
                .setStatus(session.getStatus())
                .setUploadedCount(session.getUploadedCount())
                .setMaxFileCount(session.getMaxFileCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long sessionId) {
        ErpAttachmentScanSessionDO session = validateSessionExists(sessionId);
        validateCreator(session);
        if (ErpAttachmentScanStatusEnum.EXPIRED.getStatus().equals(session.getStatus())) {
            throw exception(ATTACHMENT_SCAN_EXPIRED);
        }
        attachmentScanSessionMapper.updateStatus(sessionId, ErpAttachmentScanStatusEnum.CANCELED.getStatus());
    }

    private String generateTicket() {
        String ticket;
        do {
            ticket = IdUtil.fastSimpleUUID();
        } while (attachmentScanSessionMapper.selectByTicket(ticket) != null);
        return ticket;
    }

    private ErpAttachmentScanSessionDO validateAvailableSessionForUpload(String ticket) {
        ErpAttachmentScanSessionDO session = attachmentScanSessionMapper.selectByTicketForUpdate(ticket);
        if (session == null) {
            throw exception(ATTACHMENT_SCAN_SESSION_NOT_EXISTS);
        }
        refreshExpiredStatus(session);
        if (ErpAttachmentScanStatusEnum.EXPIRED.getStatus().equals(session.getStatus())) {
            throw exception(ATTACHMENT_SCAN_EXPIRED);
        }
        if (ErpAttachmentScanStatusEnum.CANCELED.getStatus().equals(session.getStatus())) {
            throw exception(ATTACHMENT_SCAN_CANCELED);
        }
        if (session.getUploadedCount() >= session.getMaxFileCount()) {
            throw exception(ATTACHMENT_SCAN_FILE_COUNT_EXCEEDED, session.getMaxFileCount());
        }
        return session;
    }

    private ErpAttachmentScanSessionDO validateSessionExists(String ticket) {
        ErpAttachmentScanSessionDO session = attachmentScanSessionMapper.selectByTicket(ticket);
        if (session == null) {
            throw exception(ATTACHMENT_SCAN_SESSION_NOT_EXISTS);
        }
        return session;
    }

    private ErpAttachmentScanSessionDO validateSessionExists(Long sessionId) {
        ErpAttachmentScanSessionDO session = attachmentScanSessionMapper.selectById(sessionId);
        if (session == null) {
            throw exception(ATTACHMENT_SCAN_SESSION_NOT_EXISTS);
        }
        return session;
    }

    private void validateCreator(ErpAttachmentScanSessionDO session) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || !Objects.equals(String.valueOf(loginUserId), session.getCreator())) {
            throw exception(ATTACHMENT_SCAN_NOT_OWNER);
        }
    }

    private void refreshExpiredStatus(ErpAttachmentScanSessionDO session) {
        if (ErpAttachmentScanStatusEnum.CANCELED.getStatus().equals(session.getStatus())
                || ErpAttachmentScanStatusEnum.EXPIRED.getStatus().equals(session.getStatus())
                || !LocalDateTime.now().isAfter(session.getExpireTime())) {
            return;
        }
        attachmentScanSessionMapper.updateStatus(session.getId(), ErpAttachmentScanStatusEnum.EXPIRED.getStatus());
        session.setStatus(ErpAttachmentScanStatusEnum.EXPIRED.getStatus());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(ATTACHMENT_SCAN_FILE_TYPE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw exception(ATTACHMENT_SCAN_FILE_SIZE_EXCEEDED, "16MB");
        }
        String contentType = StrUtil.nullToEmpty(file.getContentType()).toLowerCase();
        String extension = StrUtil.nullToEmpty(FileUtil.extName(file.getOriginalFilename())).toLowerCase();
        if (contentType.startsWith("image/")
                || ALLOWED_CONTENT_TYPES.contains(contentType)
                || ALLOWED_EXTENSIONS.contains(extension)) {
            return;
        }
        throw exception(ATTACHMENT_SCAN_FILE_TYPE_INVALID);
    }

    private String buildQrUrl(String frontendBaseUrl, String ticket, Long tenantId, boolean hashRoute) {
        String routePrefix = hashRoute ? "/#/erp/attachment-scan/mobile" : "/erp/attachment-scan/mobile";
        return StrUtil.removeSuffix(frontendBaseUrl, "/") + routePrefix
                + "?ticket=" + URLUtil.encodeQuery(ticket)
                + "&tenantId=" + tenantId;
    }

    private ErpAttachmentScanSessionRespVO toSessionRespVO(ErpAttachmentScanSessionDO session) {
        return new ErpAttachmentScanSessionRespVO()
                .setSessionId(session.getId())
                .setTitle(session.getTitle())
                .setBizType(session.getBizType())
                .setBizNo(session.getBizNo())
                .setExpireTime(session.getExpireTime())
                .setStatus(session.getStatus())
                .setUploadedCount(session.getUploadedCount())
                .setMaxFileCount(session.getMaxFileCount());
    }

    private List<ErpAttachmentScanFileRespVO> toFileRespVOList(List<ErpAttachmentScanFileDO> files) {
        return files.stream().map(file -> new ErpAttachmentScanFileRespVO()
                .setFileId(file.getId())
                .setFileName(file.getFileName())
                .setFileUrl(file.getFileUrl())
                .setFileType(file.getFileType())
                .setFileSize(file.getFileSize())
                .setUploadTime(file.getCreateTime())).toList();
    }

}
