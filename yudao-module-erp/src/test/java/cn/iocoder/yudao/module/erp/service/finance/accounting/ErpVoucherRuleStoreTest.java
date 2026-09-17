package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;

@ExtendWith(MockitoExtension.class)
class ErpVoucherRuleStoreTest {
    @InjectMocks ErpVoucherRuleStore store;
    @Mock ErpVoucherRuleStateMapper mapper;
    @Mock ErpAccountingSubjectService subjects;
    @Mock ErpVoucherSourceReader sources;

    @Test void newTenantStartsUnconfiguredWithEveryTemplateDisabled() {
        Config c=store.config();assertEquals("UNSET",c.getTaxpayer());assertTrue(c.getEnabledScenarios().isEmpty());assertTrue(c.getMappings().isEmpty());
        verify(mapper,never()).insert(any(ErpVoucherRuleStateDO.class));
    }
    @Test void taxableTemplateCannotBeEnabledWithoutTaxpayerIdentity() {
        Config c=new Config();c.getEnabledScenarios().add("SALE");
        assertThrows(RuntimeException.class,()->store.saveConfig(c));verify(mapper,never()).insert(any(ErpVoucherRuleStateDO.class));
    }
    @Test void duplicateCategoryScopeIsRejected() {
        when(subjects.getSubject(1L)).thenReturn(new ErpAccountingSubjectDO().setId(1L).setEnable(true).setIsLeaf(true));
        Mapping m=new Mapping();m.setScenario("SALE");m.setRole("REVENUE");m.setSubjectId(1L);
        Config c=new Config();c.getMappings().add(m);c.getMappings().add(m);
        assertThrows(RuntimeException.class,()->store.saveConfig(c));verify(mapper,never()).insert(any(ErpVoucherRuleStateDO.class));
    }
    @Test void disabledOrNonLeafSubjectsCannotBeConfigured() {
        when(subjects.getSubject(1L)).thenReturn(new ErpAccountingSubjectDO().setId(1L).setEnable(false).setIsLeaf(true));
        assertThrows(RuntimeException.class,()->store.subject(1L));
        when(subjects.getSubject(1L)).thenReturn(new ErpAccountingSubjectDO().setId(1L).setEnable(true).setIsLeaf(false));
        assertThrows(RuntimeException.class,()->store.subject(1L));
    }
    @Test void staleConfigVersionCannotOverwriteSavedRules() {
        Config c=new Config();c.setVersion(2L);ErpVoucherRuleStateDO row=new ErpVoucherRuleStateDO();row.setVersion(3L);row.setPayload(JsonUtils.toJsonString(new Config()));
        when(mapper.get(eq("CONFIG"),anyBoolean())).thenReturn(row);
        assertThrows(RuntimeException.class,()->store.saveConfig(c));verify(mapper,never()).insert(any(ErpVoucherRuleStateDO.class));
    }
    @Test void accountRenameKeepsExplicitSubjectBinding() {
        Config c=new Config();AccountMapping binding=new AccountMapping();binding.setAccountId(10L);binding.setSubjectId(100L);c.getAccounts().add(binding);
        ErpVoucherRuleStateDO row=new ErpVoucherRuleStateDO();row.setVersion(1L);row.setPayload(JsonUtils.toJsonString(c));
        when(mapper.get(eq("CONFIG"),anyBoolean())).thenReturn(row);
        store.bindNewFundAccount(10L,200L);verifyNoInteractions(subjects);verify(mapper,never()).insert(any(ErpVoucherRuleStateDO.class));
    }
    @Test void firstConfigSaveCreatesAuditableSnapshotWithoutChangingSubjects() {
        store.saveConfig(new Config());ArgumentCaptor<ErpVoucherRuleStateDO> captor=ArgumentCaptor.forClass(ErpVoucherRuleStateDO.class);
        verify(mapper,times(2)).insert(captor.capture());assertEquals("CONFIG",captor.getAllValues().get(0).getStateKey());assertTrue(captor.getAllValues().get(1).getStateKey().startsWith("AUDIT:"));verifyNoInteractions(subjects,sources);
    }
    @Test void templateCanBePausedEvenAfterItsSubjectBecomesInvalid() {
        Config c=new Config();c.getEnabledScenarios().add("SALE");
        Mapping m=new Mapping();m.setScenario("SALE");m.setRole("REVENUE");m.setSubjectId(999L);c.getMappings().add(m);
        ErpVoucherRuleStateDO row=new ErpVoucherRuleStateDO();row.setId(1L);row.setVersion(1L);row.setPayload(JsonUtils.toJsonString(c));
        when(mapper.get(eq("CONFIG"),anyBoolean())).thenReturn(row);when(mapper.update(any(ErpVoucherRuleStateDO.class),any())).thenReturn(1);
        EnableRequest req=new EnableRequest();req.setScenario("SALE");req.setEnabled(false);req.setVersion(1L);store.setEnabled(req);
        ArgumentCaptor<ErpVoucherRuleStateDO> captor=ArgumentCaptor.forClass(ErpVoucherRuleStateDO.class);
        verify(mapper).update(captor.capture(),any());assertTrue(JsonUtils.parseObject(captor.getValue().getPayload(),Config.class).getEnabledScenarios().isEmpty());verifyNoInteractions(subjects);
    }
}
