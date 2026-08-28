package cn.iocoder.yudao.module.erp.controller.admin.common;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpImportExportRecordControllerTest extends BaseMockitoUnitTest {

    private static final Long LOGIN_USER_ID = 104L;

    @InjectMocks
    private ErpImportExportRecordController controller;

    @Mock
    private ErpImportExportRecordService importExportRecordService;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void getRecordPage_exportTypeRequiresExportRecordQueryPermission() {
        ErpImportExportRecordPageReqVO reqVO = new ErpImportExportRecordPageReqVO();
        reqVO.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        when(permissionApi.hasAnyPermissions(LOGIN_USER_ID, "erp:export-record:query")).thenReturn(false);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

            assertThrows(AccessDeniedException.class, () -> controller.getRecordPage(reqVO));
        }

        verify(importExportRecordService, never()).getRecordPage(any());
    }

    @Test
    void getRecordPage_blankTypeAllowsEitherRecordQueryPermission() {
        ErpImportExportRecordPageReqVO reqVO = new ErpImportExportRecordPageReqVO();
        PageResult<ErpImportExportRecordRespVO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(permissionApi.hasAnyPermissions(
                LOGIN_USER_ID, "erp:import-record:query", "erp:export-record:query")).thenReturn(true);
        when(importExportRecordService.getRecordPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ErpImportExportRecordRespVO>> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

            result = controller.getRecordPage(reqVO);
        }

        assertEquals(pageResult, result.getData());
    }

    @Test
    void downloadFailureDetails_exportRecordIsRejected() {
        Long recordId = 300L;
        ErpImportExportRecordRespVO record = new ErpImportExportRecordRespVO();
        record.setId(recordId);
        record.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        when(importExportRecordService.getRecord(recordId)).thenReturn(record);

        assertThrows(AccessDeniedException.class,
                () -> controller.downloadFailureDetails(recordId, mock(HttpServletResponse.class)));

        verify(importExportRecordService, never()).getFailureDetailList(recordId);
    }

}
