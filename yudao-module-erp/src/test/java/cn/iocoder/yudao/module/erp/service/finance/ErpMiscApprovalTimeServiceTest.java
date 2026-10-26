package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableMiscServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableMiscServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpMiscApprovalTimeServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableMiscServiceImpl payableService;
    @InjectMocks
    private ErpReceivableMiscServiceImpl receivableService;
    @Mock
    private ErpPayableMiscMapper payableMapper;
    @Mock
    private ErpReceivableMiscMapper receivableMapper;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void approvePayableMisc_persistsApproveTime() {
        when(payableMapper.selectById(1L)).thenReturn(new ErpPayableMiscDO().setId(1L).setNo("QTYFM1")
                .setStatus(ErpPayableMiscStatusEnum.PROCESS.getStatus()));
        when(payableMapper.updateByIdAndStatus(eq(1L),
                eq(ErpPayableMiscStatusEnum.PROCESS.getStatus()), any())).thenReturn(1);

        payableService.updatePayableMiscStatus(1L, ErpPayableMiscStatusEnum.APPROVE.getStatus());

        ArgumentCaptor<ErpPayableMiscDO> captor = ArgumentCaptor.forClass(ErpPayableMiscDO.class);
        verify(payableMapper).updateByIdAndStatus(eq(1L),
                eq(ErpPayableMiscStatusEnum.PROCESS.getStatus()), captor.capture());
        assertThat(captor.getValue().getApproveTime()).isNotNull();
    }

    @Test
    void approveReceivableMisc_persistsApproveTime() {
        when(receivableMapper.selectById(2L)).thenReturn(new ErpReceivableMiscDO().setId(2L).setNo("QTYSM2")
                .setStatus(ErpReceivableMiscStatusEnum.PROCESS.getStatus()));
        when(receivableMapper.updateByIdAndStatus(eq(2L),
                eq(ErpReceivableMiscStatusEnum.PROCESS.getStatus()), any())).thenReturn(1);

        receivableService.updateReceivableMiscStatus(2L, ErpReceivableMiscStatusEnum.APPROVE.getStatus());

        ArgumentCaptor<ErpReceivableMiscDO> captor = ArgumentCaptor.forClass(ErpReceivableMiscDO.class);
        verify(receivableMapper).updateByIdAndStatus(eq(2L),
                eq(ErpReceivableMiscStatusEnum.PROCESS.getStatus()), captor.capture());
        assertThat(captor.getValue().getApproveTime()).isNotNull();
    }
}
