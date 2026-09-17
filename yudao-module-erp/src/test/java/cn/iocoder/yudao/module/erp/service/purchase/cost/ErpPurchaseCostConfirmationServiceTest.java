package cn.iocoder.yudao.module.erp.service.purchase.cost;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostCalculator;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpPurchaseCostConfirmationServiceTest {
    @Test void amountFirstPreserves650ForThreeUnits() {
        ErpDualCostCalculator.Change result=ErpDualCostCalculator.calculateWithAmount(new BigDecimal("2"),new BigDecimal("200"),new BigDecimal("3"),new BigDecimal("650"));
        assertEquals(0,new BigDecimal("650").compareTo(result.getMovement()));
        assertEquals(0,new BigDecimal("850").compareTo(result.getAmount()));
        ErpDualCostCalculator.Change cleared=ErpDualCostCalculator.calculate(result.getQuantity(),result.getAmount(),new BigDecimal("-5"),null);
        assertEquals(0,cleared.getAmount().signum());
    }
    @Test void zeroIsExplicitAndMissingOrExcessPrecisionRejected() {
        assertEquals(0,ErpDualCostCalculator.calculateWithAmount(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ZERO).getAmount().signum());
        assertThrows(IllegalArgumentException.class,()->ErpDualCostCalculator.calculateWithAmount(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ONE,null));
        assertThrows(IllegalArgumentException.class,()->ErpDualCostCalculator.calculateWithAmount(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ONE,new BigDecimal("0.0000001")));
    }
    @Test void flowStatusAndRemarkDoNotInvalidateButAmountAndDepartmentDo() {
        ErpPurchaseInDO header=new ErpPurchaseInDO().setId(1L).setStatus(10).setDeptId(8L).setInTime(LocalDateTime.of(2026,9,9,0,0));
        List<ErpPurchaseInItemDO> items=Collections.singletonList(new ErpPurchaseInItemDO().setId(2L).setInId(1L).setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("250")));
        String old=ErpPurchaseCostSignature.snapshot(header,items);
        header.setStatus(20).setRemark("审核操作备注").setPaymentPrice(new BigDecimal("1"));
        assertEquals(old,ErpPurchaseCostSignature.snapshot(header,items));
        header.setDeptId(9L);assertNotEquals(old,ErpPurchaseCostSignature.snapshot(header,items));
        header.setDeptId(8L);items.get(0).setProductPrice(new BigDecimal("251"));assertNotEquals(old,ErpPurchaseCostSignature.snapshot(header,items));
    }
    @Test void sourceAndRequestSignaturesIgnorePageOrderAndNumericScale() {
        ErpPurchaseInDO header=new ErpPurchaseInDO().setId(1L);
        ErpPurchaseInItemDO a=new ErpPurchaseInItemDO().setId(2L).setCount(new BigDecimal("3"));
        ErpPurchaseInItemDO b=new ErpPurchaseInItemDO().setId(3L).setCount(BigDecimal.ONE);
        String signature=ErpPurchaseCostSignature.snapshot(header,Arrays.asList(a,b));
        a.setCount(new BigDecimal("3.000000"));
        assertEquals(signature,ErpPurchaseCostSignature.snapshot(header,Arrays.asList(b,a)));
        ConfirmRequest r=request();String first=ErpPurchaseCostSignature.requestHash(r);
        r.getItems().get(0).setConfirmedNetTotalAmount(new BigDecimal("650.000000"));
        assertEquals(first,ErpPurchaseCostSignature.requestHash(r));
        r.setFeeTreatment("改为另行费用");assertNotEquals(first,ErpPurchaseCostSignature.requestHash(r));
    }
    @Test void falseFlagDoesNotAccessNewTables() {
        ErpPurchaseCostConfirmationService service=new ErpPurchaseCostConfirmationService();
        ErpPurchaseCostConfirmationRepository repository=mock(ErpPurchaseCostConfirmationRepository.class);
        ReflectionTestUtils.setField(service,"repository",repository);
        assertTrue(service.prepareApproval(new ErpPurchaseInDO(),Collections.emptyList()).isEmpty());
        verifyNoInteractions(repository);
    }
    @Test void completeEvidenceAndExplicitAmountAreRequired() {
        ConfirmRequest request=request();assertDoesNotThrow(()->ErpPurchaseCostConfirmationService.validateRequest(request));
        request.getItems().get(0).setConfirmedNetTotalAmount(null);
        assertThrows(ServiceException.class,()->ErpPurchaseCostConfirmationService.validateRequest(request));
        request.getItems().get(0).setConfirmedNetTotalAmount(BigDecimal.ZERO);
        assertDoesNotThrow(()->ErpPurchaseCostConfirmationService.validateRequest(request));
        request.setFeeTreatment(" ");assertThrows(ServiceException.class,()->ErpPurchaseCostConfirmationService.validateRequest(request));
    }
    @Test void manySourceRowsUseOneStockPermissionBatchAndOneProductBatchPerPage() {
        ErpPurchaseCostConfirmationService service=new ErpPurchaseCostConfirmationService();
        cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper headers=mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper.class);
        cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper items=mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper.class);
        cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper stocks=mock(cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper.class);
        cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper products=mock(cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        ReflectionTestUtils.setField(service,"purchaseInMapper",headers);ReflectionTestUtils.setField(service,"itemMapper",items);
        ReflectionTestUtils.setField(service,"stockMapper",stocks);ReflectionTestUtils.setField(service,"productMapper",products);
        ReflectionTestUtils.setField(service,"warehouseService",warehouses);
        ReflectionTestUtils.setField(service,"permissionApi",mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class));
        ReflectionTestUtils.setField(service,"repository",mock(ErpPurchaseCostConfirmationRepository.class));
        ErpPurchaseInDO header=new ErpPurchaseInDO().setId(1L).setStatus(10).setDeptId(2L);
        List<ErpPurchaseInItemDO> rows=new ArrayList<>();
        List<cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO> stockRows=new ArrayList<>();
        for(long id=1;id<=120;id++) {
            rows.add(new ErpPurchaseInItemDO().setId(id).setInId(1L).setProductId(id).setWarehouseId(20L).setCount(BigDecimal.ONE));
            stockRows.add(new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO().setId(id).setProductId(id).setWarehouseId(20L).setDeptId(2L));
        }
        when(headers.selectById(1L)).thenReturn(header);when(items.selectListByInId(1L)).thenReturn(rows);
        when(stocks.selectListByProductIdsAndWarehouseIds(anyCollection(),anyCollection())).thenReturn(stockRows);
        try(org.mockito.MockedStatic<cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils> security=mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            security.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            String signature=ErpPurchaseCostSignature.hash(ErpPurchaseCostSignature.snapshot(header,rows));
            assertEquals(100,service.itemPage(1L,signature,1,100).getList().size());
            assertEquals(20,service.itemPage(1L,signature,2,100).getList().size());
            verify(stocks,times(2)).selectListByProductIdsAndWarehouseIds(anyCollection(),anyCollection());
            verify(warehouses,times(2)).validateCurrentUserStockPermission(argThat(collection->collection.size()==120));
            verify(products,times(2)).selectByIds(anyCollection());verify(products,never()).selectById(anyLong());
            stockRows.add(stockRows.get(0));
            assertThrows(ServiceException.class,()->service.itemPage(1L,signature,1,100));
        } finally {cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    }
    private ConfirmRequest request() {
        ConfirmRequest r=new ConfirmRequest();r.setPurchaseInId(1L);r.setExpectedSignature("source");r.setExpectedRevision(0);
        r.setRequestKey("request_1");r.setEvidence("人工核对未税金额");r.setFeeTreatment("折扣已计入逐行金额，运费另列未计入");
        ConfirmItem i=new ConfirmItem();i.setSourceItemId(2L);i.setConfirmedNetTotalAmount(new BigDecimal("650"));i.setEvidence("成本核对单");r.setItems(Collections.singletonList(i));return r;
    }
}
