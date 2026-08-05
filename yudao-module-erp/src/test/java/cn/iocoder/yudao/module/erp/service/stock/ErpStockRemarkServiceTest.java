package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.validation.Validation;
import javax.validation.Validator;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockRemarkServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockCheckServiceImpl stockCheckService;
    @InjectMocks
    private ErpStockInServiceImpl stockInService;
    @InjectMocks
    private ErpStockOutServiceImpl stockOutService;
    @InjectMocks
    private ErpStockMoveServiceImpl stockMoveService;
    @InjectMocks
    private ErpWarehouseMoveServiceImpl warehouseMoveService;

    @Mock
    private ErpStockCheckMapper stockCheckMapper;
    @Mock
    private ErpStockInMapper stockInMapper;
    @Mock
    private ErpStockOutMapper stockOutMapper;
    @Mock
    private ErpStockMoveMapper stockMoveMapper;
    @Mock
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    public void testUpdateRemarkRequest_validationBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ErpStockUpdateRemarkReqVO reqVO = req(1L, null);
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark(new String(new char[501]).replace('\0', 'a'));
        assertEquals(1, validator.validate(reqVO).size());

        reqVO.setRemark("");
        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    public void testUpdateStockCheckRemark_approvedSuccess() {
        when(stockCheckMapper.selectById(eq(1L))).thenReturn(new ErpStockCheckDO()
                .setId(1L).setNo("KCPD001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        stockCheckService.updateStockCheckRemark(req(1L, "审批后备注"));
        assertPartialUpdate(capture(stockCheckMapper, ErpStockCheckDO.class), 1L, "审批后备注");
    }

    @Test
    public void testUpdateStockInRemark_approvedSuccess() {
        when(stockInMapper.selectById(eq(2L))).thenReturn(new ErpStockInDO()
                .setId(2L).setNo("QTRK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        stockInService.updateStockInRemark(req(2L, "审批后备注"));
        assertPartialUpdate(capture(stockInMapper, ErpStockInDO.class), 2L, "审批后备注");
    }

    @Test
    public void testUpdateStockOutRemark_approvedSuccess() {
        when(stockOutMapper.selectById(eq(3L))).thenReturn(new ErpStockOutDO()
                .setId(3L).setNo("QTCK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        stockOutService.updateStockOutRemark(req(3L, "审批后备注"));
        assertPartialUpdate(capture(stockOutMapper, ErpStockOutDO.class), 3L, "审批后备注");
    }

    @Test
    public void testUpdateStockMoveRemark_approvedSuccess() {
        when(stockMoveMapper.selectById(eq(4L))).thenReturn(new ErpStockMoveDO()
                .setId(4L).setNo("DBCK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        stockMoveService.updateStockTransferOutRemark(req(4L, "审批后备注"));
        assertPartialUpdate(capture(stockMoveMapper, ErpStockMoveDO.class), 4L, "审批后备注");
    }

    @Test
    public void testUpdateWarehouseMoveRemark_approvedSuccess() {
        when(warehouseMoveMapper.selectById(eq(5L))).thenReturn(new ErpWarehouseMoveDO()
                .setId(5L).setNo("CKYH001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        warehouseMoveService.updateWarehouseMoveRemark(req(5L, "审批后备注"));
        assertPartialUpdate(capture(warehouseMoveMapper, ErpWarehouseMoveDO.class), 5L, "审批后备注");
    }

    @Test
    public void testUpdateRemark_unapprovedAndClearSuccess() {
        when(stockInMapper.selectById(eq(6L))).thenReturn(new ErpStockInDO()
                .setId(6L).setNo("QTRK002").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        stockInService.updateStockInRemark(req(6L, ""));
        assertPartialUpdate(capture(stockInMapper, ErpStockInDO.class), 6L, "");
    }

    @Test
    public void testUpdateStockCheckRemark_notExists() {
        assertMissing(STOCK_CHECK_NOT_EXISTS.getCode(),
                () -> stockCheckService.updateStockCheckRemark(req(1L, "备注")));
        verify(stockCheckMapper, never()).updateById(any(ErpStockCheckDO.class));
    }

    @Test
    public void testUpdateStockInRemark_notExists() {
        assertMissing(STOCK_IN_NOT_EXISTS.getCode(),
                () -> stockInService.updateStockInRemark(req(2L, "备注")));
        verify(stockInMapper, never()).updateById(any(ErpStockInDO.class));
    }

    @Test
    public void testUpdateStockOutRemark_notExists() {
        assertMissing(STOCK_OUT_NOT_EXISTS.getCode(),
                () -> stockOutService.updateStockOutRemark(req(3L, "备注")));
        verify(stockOutMapper, never()).updateById(any(ErpStockOutDO.class));
    }

    @Test
    public void testUpdateStockMoveRemark_notExists() {
        assertMissing(STOCK_MOVE_NOT_EXISTS.getCode(),
                () -> stockMoveService.updateStockMoveRemark(req(4L, "备注")));
        verify(stockMoveMapper, never()).updateById(any(ErpStockMoveDO.class));
    }

    @Test
    public void testUpdateWarehouseMoveRemark_notExists() {
        assertMissing(WAREHOUSE_MOVE_NOT_EXISTS.getCode(),
                () -> warehouseMoveService.updateWarehouseMoveRemark(req(5L, "备注")));
        verify(warehouseMoveMapper, never()).updateById(any(ErpWarehouseMoveDO.class));
    }

    private static ErpStockUpdateRemarkReqVO req(Long id, String remark) {
        ErpStockUpdateRemarkReqVO reqVO = new ErpStockUpdateRemarkReqVO();
        reqVO.setId(id);
        reqVO.setRemark(remark);
        return reqVO;
    }

    private static void assertMissing(int expectedCode, Runnable action) {
        ServiceException ex = assertThrows(ServiceException.class, action::run);
        assertEquals(expectedCode, ex.getCode());
    }

    private static <T> T capture(Object mapper, Class<T> type) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(type);
        if (mapper instanceof ErpStockCheckMapper) {
            verify((ErpStockCheckMapper) mapper).updateById((ErpStockCheckDO) captor.capture());
        } else if (mapper instanceof ErpStockInMapper) {
            verify((ErpStockInMapper) mapper).updateById((ErpStockInDO) captor.capture());
        } else if (mapper instanceof ErpStockOutMapper) {
            verify((ErpStockOutMapper) mapper).updateById((ErpStockOutDO) captor.capture());
        } else if (mapper instanceof ErpStockMoveMapper) {
            verify((ErpStockMoveMapper) mapper).updateById((ErpStockMoveDO) captor.capture());
        } else {
            verify((ErpWarehouseMoveMapper) mapper).updateById((ErpWarehouseMoveDO) captor.capture());
        }
        return captor.getValue();
    }

    private static void assertPartialUpdate(Object update, Long id, String remark) {
        if (update instanceof ErpStockCheckDO) {
            ErpStockCheckDO entity = (ErpStockCheckDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpStockInDO) {
            ErpStockInDO entity = (ErpStockInDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpStockOutDO) {
            ErpStockOutDO entity = (ErpStockOutDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else if (update instanceof ErpStockMoveDO) {
            ErpStockMoveDO entity = (ErpStockMoveDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        } else {
            ErpWarehouseMoveDO entity = (ErpWarehouseMoveDO) update;
            assertEquals(id, entity.getId());
            assertEquals(remark, entity.getRemark());
            assertNull(entity.getStatus());
            assertNull(entity.getNo());
        }
    }

}
