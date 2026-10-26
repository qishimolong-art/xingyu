package cn.iocoder.yudao.module.erp.service.product;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUniversalDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ErpProductUniversalChangesTest {
    private final ErpProductServiceImpl service = new ErpProductServiceImpl();
    private final ErpProductUniversalMapper mapper = mock(ErpProductUniversalMapper.class);
    private final ErpProductMapper products = mock(ErpProductMapper.class);
    ErpProductUniversalChangesTest() {
        ReflectionTestUtils.setField(service,"productUniversalMapper",mapper);
        ReflectionTestUtils.setField(service,"productMapper",products);
    }
    private ProductSaveReqVO.Universal row(Long id, String code) {
        ProductSaveReqVO.Universal row = new ProductSaveReqVO.Universal(); row.setId(id); row.setUniversalCode(code); return row;
    }
    private void apply(ProductSaveReqVO.UniversalChanges changes) { ReflectionTestUtils.invokeMethod(service,"applyUniversalChanges",1L,changes); }
    @Test void emptyChangesNeverDeleteUnloadedRows() {
        apply(new ProductSaveReqVO.UniversalChanges()); verifyNoInteractions(mapper);
    }
    @Test void rejectsForeignOrMissingRowBeforeWriting() {
        ProductSaveReqVO.UniversalChanges changes = new ProductSaveReqVO.UniversalChanges();
        changes.setDeletedIds(Collections.singletonList(7L));
        when(mapper.selectByIds(anyCollection())).thenReturn(Collections.singletonList(new ErpProductUniversalDO().setId(7L).setProductId(99L)));
        assertThrows(IllegalArgumentException.class,()->apply(changes)); verify(mapper, never()).deleteByIds(anyCollection());
    }
    @Test void savesOnlyExplicitChanges() {
        ProductSaveReqVO.UniversalChanges changes = new ProductSaveReqVO.UniversalChanges();
        changes.setDeletedIds(Collections.singletonList(8L)); changes.setUpserts(Arrays.asList(row(7L,"A"),row(null,"B")));
        when(mapper.selectByIds(anyCollection())).thenReturn(Arrays.asList(new ErpProductUniversalDO().setId(7L).setProductId(1L), new ErpProductUniversalDO().setId(8L).setProductId(1L)));
        when(products.selectListByCodes(anyCollection())).thenReturn(Arrays.asList(new ErpProductDO().setCode("A"),new ErpProductDO().setCode("B")));
        apply(changes); verify(mapper).deleteByIds(Collections.singletonList(8L)); verify(mapper).updateById(any(ErpProductUniversalDO.class)); verify(mapper).insert(any(ErpProductUniversalDO.class));
        verify(mapper, never()).deleteByProductId(anyLong());
    }
    @Test void rejectsDuplicateUpdatesAndBlankCodes() {
        ProductSaveReqVO.UniversalChanges changes = new ProductSaveReqVO.UniversalChanges();
        changes.setUpserts(Arrays.asList(row(7L,"A"),row(7L,"B")));
        assertThrows(IllegalArgumentException.class,()->apply(changes));
        changes.setUpserts(Collections.singletonList(row(null," ")));
        assertThrows(IllegalArgumentException.class,()->apply(changes)); verifyNoInteractions(mapper);
    }
}
