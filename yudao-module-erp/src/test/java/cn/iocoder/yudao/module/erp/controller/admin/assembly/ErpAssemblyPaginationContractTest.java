package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ErpAssemblyPaginationContractTest {
    @Test void detailPagesRequireParentAndRejectUnboundedOrOver100Requests() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ErpAssemblyRecipeItemPageReqVO recipe = new ErpAssemblyRecipeItemPageReqVO();
            recipe.setRecipeId(1L); recipe.setPageSize(100);
            assertTrue(factory.getValidator().validate(recipe).isEmpty());
            recipe.setPageSize(101); assertFalse(factory.getValidator().validate(recipe).isEmpty());
            recipe.setPageSize(-1); assertFalse(factory.getValidator().validate(recipe).isEmpty());
            recipe.setPageSize(20); recipe.setRecipeId(null); assertFalse(factory.getValidator().validate(recipe).isEmpty());
            ErpAssemblyOrderItemPageReqVO order = new ErpAssemblyOrderItemPageReqVO();
            order.setOrderId(1L); order.setPageSize(100); assertTrue(factory.getValidator().validate(order).isEmpty());
            order.setPageSize(101); assertFalse(factory.getValidator().validate(order).isEmpty());
        }
    }
    @Test void batchPageKeepsLegacyBalanceFormulaAndBindsSearchInsteadOfInterpolatingSql() throws Exception {
        java.lang.reflect.Method method = ErpPurchaseInItemMapper.class.getMethod("selectAvailableBatchNoPage", IPage.class, Long.class, Long.class, Integer.class, String.class);
        String script = String.join(" ", method.getAnnotation(Select.class).value());
        Map<String, Object> params = new HashMap<>();
        params.put("productId", 1L); params.put("warehouseId", 2L); params.put("approveStatus", 20); params.put("keyword", "' OR 1=1 --");
        String sql = new XMLLanguageDriver().createSqlSource(new Configuration(), script, Map.class).getBoundSql(params).getSql();
        assertTrue(sql.contains("pii.deleted = 0")); assertTrue(sql.contains("soi.deleted = 0"));
        assertTrue(sql.contains("pii.product_id = ?")); assertTrue(sql.contains("pii.warehouse_id = ?"));
        assertTrue(sql.contains("t.in_count - COALESCE(o.out_count, 0) > 0"));
        assertTrue(sql.contains("t.batch_no LIKE CONCAT('%', ?, '%')")); assertFalse(sql.contains("' OR 1=1 --"));
        assertTrue(sql.contains("ORDER BY t.first_in_time ASC, t.batch_no ASC"));
        assertTrue(IPage.class.isAssignableFrom(method.getReturnType()));
    }
}
