package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErpAccountApprovalAscendingSortTest {

    @Test
    void receivableApprovalBasisDefaultsToChronologicalOrderAndStableEventKey() {
        LocalDateTime earlier = LocalDateTime.of(2026, 10, 20, 9, 0);
        LocalDateTime later = earlier.plusHours(1);
        ErpReceivableDetailRespVO laterRow = receivableRow(3L, later, "20:0003");
        ErpReceivableDetailRespVO sameTimeLaterKey = receivableRow(2L, earlier, "20:0002");
        ErpReceivableDetailRespVO sameTimeEarlierKey = receivableRow(1L, earlier, "20:0001");

        List<ErpReceivableDetailRespVO> sorted = ReflectionTestUtils.invokeMethod(
                new ErpReceivableAccountServiceImpl(), "sortDrilldownRows",
                Arrays.asList(laterRow, sameTimeLaterKey, sameTimeEarlierKey), null, null, true);

        assertThat(sorted).extracting(ErpReceivableDetailRespVO::getBizId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void payableApprovalBasisDefaultsToChronologicalOrderAndStableEventKey() {
        LocalDateTime earlier = LocalDateTime.of(2026, 10, 20, 9, 0);
        LocalDateTime later = earlier.plusHours(1);
        ErpPayableDetailRespVO laterRow = payableRow(13L, later, "20:0013");
        ErpPayableDetailRespVO sameTimeLaterKey = payableRow(12L, earlier, "20:0012");
        ErpPayableDetailRespVO sameTimeEarlierKey = payableRow(11L, earlier, "20:0011");

        List<ErpPayableDetailRespVO> sorted = ReflectionTestUtils.invokeMethod(
                new ErpPayableAccountServiceImpl(), "sortDrilldownRows",
                Arrays.asList(laterRow, sameTimeLaterKey, sameTimeEarlierKey), null, null, true);

        assertThat(sorted).extracting(ErpPayableDetailRespVO::getBizId)
                .containsExactly(11L, 12L, 13L);
    }

    private ErpReceivableDetailRespVO receivableRow(Long bizId, LocalDateTime approveTime, String eventKey) {
        ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
        row.setBizId(bizId);
        row.setApproveTime(approveTime);
        row.setEventKey(eventKey);
        return row;
    }

    private ErpPayableDetailRespVO payableRow(Long bizId, LocalDateTime approveTime, String eventKey) {
        ErpPayableDetailRespVO row = new ErpPayableDetailRespVO();
        row.setBizId(bizId);
        row.setApproveTime(approveTime);
        row.setEventKey(eventKey);
        return row;
    }
}
