package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount.ErpSupplierAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact.ErpSupplierContactSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo.ErpSupplierExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask.ErpSupplierTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierContactMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierExtendInfoMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpSupplierBaseTableServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSupplierContactServiceImpl supplierContactService;
    @InjectMocks
    private ErpSupplierAccountServiceImpl supplierAccountService;
    @InjectMocks
    private ErpSupplierTaskServiceImpl supplierTaskService;
    @InjectMocks
    private ErpSupplierExtendInfoServiceImpl supplierExtendInfoService;

    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpSupplierContactMapper supplierContactMapper;
    @Mock
    private ErpSupplierAccountMapper supplierAccountMapper;
    @Mock
    private ErpSupplierTaskMapper supplierTaskMapper;
    @Mock
    private ErpSupplierExtendInfoMapper supplierExtendInfoMapper;

    @Test
    public void createContact_whenCompanyIdBlank_useSupplierId() {
        when(supplierService.getSupplier(eq(10L))).thenReturn(new ErpSupplierDO().setId(10L));
        ErpSupplierContactSaveReqVO reqVO = new ErpSupplierContactSaveReqVO();
        reqVO.setSupplierId(10L);
        reqVO.setName("张三");
        reqVO.setMobile("13800000000");

        supplierContactService.createSupplierContact(reqVO);

        verify(supplierContactMapper).insert(argThat((cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContactDO contact) ->
                Long.valueOf(10L).equals(contact.getSupplierId())
                        && Long.valueOf(10L).equals(contact.getCompanyId())));
    }

    @Test
    public void createAccount_whenDefaulted_clearOtherDefaultAccounts() {
        when(supplierService.getSupplier(eq(10L))).thenReturn(new ErpSupplierDO().setId(10L));
        ErpSupplierAccountSaveReqVO reqVO = new ErpSupplierAccountSaveReqVO();
        reqVO.setSupplierId(10L);
        reqVO.setAccountName("主账户");
        reqVO.setCardNo("6222000000000000");
        reqVO.setDefaulted(true);

        supplierAccountService.createSupplierAccount(reqVO);

        verify(supplierAccountMapper).clearDefaultBySupplierId(10L, null);
        verify(supplierAccountMapper).insert(any(ErpSupplierAccountDO.class));
    }

    @Test
    public void createTask_whenMonthInvalid_throwException() {
        ErpSupplierTaskSaveReqVO reqVO = new ErpSupplierTaskSaveReqVO();
        reqVO.setSupplierId(10L);
        reqVO.setYear(2026);
        reqVO.setMonth(13);
        reqVO.setTaskAmount(BigDecimal.TEN);

        assertThrows(ServiceException.class, () -> supplierTaskService.createSupplierTask(reqVO));
        verify(supplierTaskMapper, never()).insert(any(ErpSupplierTaskDO.class));
    }

    @Test
    public void createTask_whenDuplicate_throwException() {
        when(supplierService.getSupplier(eq(10L))).thenReturn(new ErpSupplierDO().setId(10L));
        when(supplierTaskMapper.selectByUniqueKey(eq(10L), eq(2026), eq(5), eq("A")))
                .thenReturn(new ErpSupplierTaskDO().setId(99L));
        ErpSupplierTaskSaveReqVO reqVO = new ErpSupplierTaskSaveReqVO();
        reqVO.setSupplierId(10L);
        reqVO.setYear(2026);
        reqVO.setMonth(5);
        reqVO.setTaskLevel("A");
        reqVO.setTaskAmount(BigDecimal.TEN);

        assertThrows(ServiceException.class, () -> supplierTaskService.createSupplierTask(reqVO));
        verify(supplierTaskMapper, never()).insert(any(ErpSupplierTaskDO.class));
    }

    @Test
    public void saveExtendInfo_whenExists_updateExistingRecord() {
        when(supplierService.getSupplier(eq(10L))).thenReturn(new ErpSupplierDO().setId(10L));
        when(supplierExtendInfoMapper.selectBySupplierId(eq(10L)))
                .thenReturn(new ErpSupplierExtendInfoDO().setId(99L).setSupplierId(10L));
        ErpSupplierExtendInfoSaveReqVO reqVO = new ErpSupplierExtendInfoSaveReqVO();
        reqVO.setSupplierId(10L);
        reqVO.setBoss("李四");

        supplierExtendInfoService.saveSupplierExtendInfo(reqVO);

        verify(supplierExtendInfoMapper, never()).insert(any(ErpSupplierExtendInfoDO.class));
        verify(supplierExtendInfoMapper).updateById(argThat((ErpSupplierExtendInfoDO info) ->
                Long.valueOf(99L).equals(info.getId()) && "李四".equals(info.getBoss())));
    }

}
