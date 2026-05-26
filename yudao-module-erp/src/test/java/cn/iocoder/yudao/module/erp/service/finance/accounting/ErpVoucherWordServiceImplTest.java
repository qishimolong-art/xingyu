package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherWordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_WORD_NOT_EXISTS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpVoucherWordServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpVoucherWordServiceImpl voucherWordService;

    @Mock
    private ErpVoucherWordMapper voucherWordMapper;

    // ==================== create ====================

    @Test
    void testCreateVoucherWord_success() {
        // 准备参数
        ErpVoucherWordSaveReqVO reqVO = new ErpVoucherWordSaveReqVO();
        reqVO.setCode("记");
        reqVO.setName("记账凭证");
        reqVO.setEnable(true);
        reqVO.setSort(1);

        // mock: insert 时设置 ID
        doAnswer(invocation -> {
            ErpVoucherWordDO arg = invocation.getArgument(0);
            arg.setId(123L);
            return null;
        }).when(voucherWordMapper).insert(any(ErpVoucherWordDO.class));

        // 调用
        Long id = voucherWordService.createVoucherWord(reqVO);

        // 断言
        assertThat(id).isEqualTo(123L);
        verify(voucherWordMapper).insert(any(ErpVoucherWordDO.class));
    }

    // ==================== update ====================

    @Test
    void testUpdateVoucherWord_success() {
        // 准备参数
        ErpVoucherWordSaveReqVO reqVO = new ErpVoucherWordSaveReqVO();
        reqVO.setId(100L);
        reqVO.setCode("收");
        reqVO.setName("收款凭证");
        reqVO.setEnable(true);
        reqVO.setSort(2);

        // mock: selectById 返回已有记录
        ErpVoucherWordDO existing = new ErpVoucherWordDO();
        existing.setId(100L);
        existing.setCode("记");
        when(voucherWordMapper.selectById(100L)).thenReturn(existing);

        // 调用
        voucherWordService.updateVoucherWord(reqVO);

        // 断言
        verify(voucherWordMapper).updateById(any(ErpVoucherWordDO.class));
    }

    @Test
    void testUpdateVoucherWord_notExists() {
        // 准备参数
        ErpVoucherWordSaveReqVO reqVO = new ErpVoucherWordSaveReqVO();
        reqVO.setId(999L);
        reqVO.setCode("付");
        reqVO.setName("付款凭证");
        reqVO.setEnable(true);
        reqVO.setSort(3);

        // mock: selectById 返回 null
        when(voucherWordMapper.selectById(999L)).thenReturn(null);

        // 调用并断言异常
        assertServiceException(() -> voucherWordService.updateVoucherWord(reqVO), VOUCHER_WORD_NOT_EXISTS);
    }

    // ==================== delete ====================

    @Test
    void testDeleteVoucherWord_success() {
        // mock: selectById 返回已有记录
        ErpVoucherWordDO existing = new ErpVoucherWordDO();
        existing.setId(100L);
        when(voucherWordMapper.selectById(100L)).thenReturn(existing);

        // 调用
        voucherWordService.deleteVoucherWord(100L);

        // 断言
        verify(voucherWordMapper).deleteById(100L);
    }

    @Test
    void testDeleteVoucherWord_notExists() {
        // mock: selectById 返回 null
        when(voucherWordMapper.selectById(999L)).thenReturn(null);

        // 调用并断言异常
        assertServiceException(() -> voucherWordService.deleteVoucherWord(999L), VOUCHER_WORD_NOT_EXISTS);
    }

    // ==================== get ====================

    @Test
    void testGetVoucherWord() {
        // mock
        ErpVoucherWordDO voucherWord = new ErpVoucherWordDO();
        voucherWord.setId(100L);
        voucherWord.setCode("记");
        voucherWord.setName("记账凭证");
        when(voucherWordMapper.selectById(100L)).thenReturn(voucherWord);

        // 调用
        ErpVoucherWordDO result = voucherWordService.getVoucherWord(100L);

        // 断言
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getCode()).isEqualTo("记");
    }

    // ==================== page ====================

    @Test
    void testGetVoucherWordPage() {
        // 准备参数
        ErpVoucherWordPageReqVO pageReqVO = new ErpVoucherWordPageReqVO();
        pageReqVO.setCode("记");

        // mock
        ErpVoucherWordDO word = new ErpVoucherWordDO();
        word.setId(1L);
        word.setCode("记");
        PageResult<ErpVoucherWordDO> expected = new PageResult<>(singletonList(word), 1L);
        when(voucherWordMapper.selectPage(pageReqVO)).thenReturn(expected);

        // 调用
        PageResult<ErpVoucherWordDO> result = voucherWordService.getVoucherWordPage(pageReqVO);

        // 断言
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getCode()).isEqualTo("记");
    }

    // ==================== list ====================

    @Test
    void testGetEnabledVoucherWordList() {
        // mock
        ErpVoucherWordDO word1 = new ErpVoucherWordDO();
        word1.setId(1L);
        word1.setCode("记");
        word1.setEnable(true);
        ErpVoucherWordDO word2 = new ErpVoucherWordDO();
        word2.setId(2L);
        word2.setCode("收");
        word2.setEnable(true);
        when(voucherWordMapper.selectListByEnable(true)).thenReturn(Arrays.asList(word1, word2));

        // 调用
        List<ErpVoucherWordDO> result = voucherWordService.getEnabledVoucherWordList();

        // 断言
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("记");
        assertThat(result.get(1).getCode()).isEqualTo("收");
    }

}
