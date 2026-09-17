package cn.iocoder.yudao.module.erp.service.attachment;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanUploadRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanFileDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.attachment.ErpAttachmentScanSessionDO;
import cn.iocoder.yudao.module.erp.dal.mysql.attachment.ErpAttachmentScanFileMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.attachment.ErpAttachmentScanSessionMapper;
import cn.iocoder.yudao.module.erp.enums.attachment.ErpAttachmentScanStatusEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_FILE_COUNT_EXCEEDED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ATTACHMENT_SCAN_NOT_OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class ErpAttachmentScanServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAttachmentScanServiceImpl service;

    @Mock
    private ErpAttachmentScanSessionMapper attachmentScanSessionMapper;
    @Mock
    private ErpAttachmentScanFileMapper attachmentScanFileMapper;
    @Mock
    private FileApi fileApi;

    @Test
    void createAttachmentScan_shouldGenerateTicketAndQrUrl() {
        ErpAttachmentScanCreateReqVO reqVO = new ErpAttachmentScanCreateReqVO();
        reqVO.setTitle("采购附件");
        reqVO.setBizType("purchase-order");
        reqVO.setBizNo("CGD001");
        reqVO.setMaxFileCount(3);
        when(attachmentScanSessionMapper.selectByTicket(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpAttachmentScanSessionDO session = invocation.getArgument(0);
            session.setId(100L);
            return 1;
        }).when(attachmentScanSessionMapper).insert(any(ErpAttachmentScanSessionDO.class));

        try (MockedStatic<TenantContextHolder> tenant = mockStatic(TenantContextHolder.class)) {
            tenant.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);
            ErpAttachmentScanCreateRespVO result = service.createAttachmentScan(reqVO, "https://erp.example.com/",
                    false);

            assertEquals(100L, result.getSessionId());
            assertNotNull(result.getTicket());
            assertEquals(ErpAttachmentScanStatusEnum.WAITING.getStatus(), result.getStatus());
            assertEquals("https://erp.example.com/erp/attachment-scan/mobile?ticket="
                    + result.getTicket() + "&tenantId=1", result.getQrUrl());
        }
    }

    @Test
    void createAttachmentScan_shouldGenerateHashQrUrl() {
        ErpAttachmentScanCreateReqVO reqVO = new ErpAttachmentScanCreateReqVO();
        when(attachmentScanSessionMapper.selectByTicket(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpAttachmentScanSessionDO session = invocation.getArgument(0);
            session.setId(101L);
            return 1;
        }).when(attachmentScanSessionMapper).insert(any(ErpAttachmentScanSessionDO.class));

        try (MockedStatic<TenantContextHolder> tenant = mockStatic(TenantContextHolder.class)) {
            tenant.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);
            ErpAttachmentScanCreateRespVO result = service.createAttachmentScan(reqVO, "https://erp.example.com",
                    true);

            assertEquals("https://erp.example.com/#/erp/attachment-scan/mobile?ticket="
                    + result.getTicket() + "&tenantId=1", result.getQrUrl());
        }
    }

    @Test
    void upload_shouldCreateFileAndUpdateSession() {
        ErpAttachmentScanSessionDO session = validSession().setMaxFileCount(2);
        when(attachmentScanSessionMapper.selectByTicketForUpdate("TICKET")).thenReturn(session);
        when(fileApi.createFile(any(), eq("invoice.pdf"), eq("erp/attachment-scan"), eq("application/pdf")))
                .thenReturn("/admin-api/infra/file/-1/get/invoice.pdf");
        doAnswer(invocation -> {
            ErpAttachmentScanFileDO file = invocation.getArgument(0);
            file.setId(200L);
            file.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(attachmentScanFileMapper).insert(any(ErpAttachmentScanFileDO.class));

        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf".getBytes());
        ErpAttachmentScanUploadRespVO result = service.upload("TICKET", file);

        assertEquals(200L, result.getFileId());
        assertEquals(1, result.getUploadedCount());
        assertEquals(ErpAttachmentScanStatusEnum.UPLOADED.getStatus(), result.getStatus());
        verify(attachmentScanSessionMapper).updateUploadResult(10L, 1,
                ErpAttachmentScanStatusEnum.UPLOADED.getStatus());
    }

    @Test
    void upload_shouldRejectWhenFileCountExceeded() {
        ErpAttachmentScanSessionDO session = validSession().setUploadedCount(2).setMaxFileCount(2);
        when(attachmentScanSessionMapper.selectByTicketForUpdate("TICKET")).thenReturn(session);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.upload("TICKET",
                        new MockMultipartFile("file", "a.pdf", "application/pdf", "pdf".getBytes())));

        assertEquals(ATTACHMENT_SCAN_FILE_COUNT_EXCEEDED.getCode(), ex.getCode());
        verify(fileApi, never()).createFile(any(), anyString(), anyString(), anyString());
    }

    @Test
    void upload_shouldExpireOldTicketBeforeUpload() {
        ErpAttachmentScanSessionDO session = validSession().setExpireTime(LocalDateTime.now().minusSeconds(1));
        when(attachmentScanSessionMapper.selectByTicketForUpdate("TICKET")).thenReturn(session);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.upload("TICKET",
                        new MockMultipartFile("file", "a.pdf", "application/pdf", "pdf".getBytes())));

        assertEquals(ATTACHMENT_SCAN_EXPIRED.getCode(), ex.getCode());
        verify(attachmentScanSessionMapper).updateStatus(10L, ErpAttachmentScanStatusEnum.EXPIRED.getStatus());
        verify(fileApi, never()).createFile(any(), anyString(), anyString(), anyString());
    }

    @Test
    void getStatus_shouldRejectOtherCreator() {
        ErpAttachmentScanSessionDO session = validSession();
        session.setCreator("105");
        when(attachmentScanSessionMapper.selectById(10L)).thenReturn(session);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.getStatus(10L));

            assertEquals(ATTACHMENT_SCAN_NOT_OWNER.getCode(), ex.getCode());
        }
    }

    @Test
    void getStatus_shouldReturnFilesForCreator() {
        ErpAttachmentScanSessionDO session = validSession();
        session.setCreator("104");
        when(attachmentScanSessionMapper.selectById(10L)).thenReturn(session);
        when(attachmentScanFileMapper.selectListBySessionId(10L)).thenReturn(Collections.singletonList(
                new ErpAttachmentScanFileDO()
                        .setId(200L)
                        .setSessionId(10L)
                        .setFileName("invoice.pdf")
                        .setFileUrl("/admin-api/infra/file/-1/get/invoice.pdf")
                        .setFileType("application/pdf")
                        .setFileSize(3L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            ErpAttachmentScanStatusRespVO result = service.getStatus(10L);

            assertEquals(10L, result.getSessionId());
            assertEquals(1, result.getFiles().size());
            assertEquals(200L, result.getFiles().get(0).getFileId());
        }
    }

    private ErpAttachmentScanSessionDO validSession() {
        ErpAttachmentScanSessionDO session = new ErpAttachmentScanSessionDO()
                .setId(10L)
                .setTicket("TICKET")
                .setTitle("扫码上传附件")
                .setStatus(ErpAttachmentScanStatusEnum.WAITING.getStatus())
                .setUploadedCount(0)
                .setMaxFileCount(9)
                .setExpireTime(LocalDateTime.now().plusMinutes(10));
        session.setCreator("104");
        return session;
    }

}
