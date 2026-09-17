package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDeptDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpProductDeptMapper extends BaseMapperX<ErpProductDeptDO> {

    default List<ErpProductDeptDO> selectListByProductId(Long productId) {
        return selectList(ErpProductDeptDO::getProductId, productId);
    }

    default List<ErpProductDeptDO> selectListByProductIds(Collection<Long> productIds) {
        return selectList(ErpProductDeptDO::getProductId, productIds);
    }

    default int deleteByProductId(Long productId) {
        return deleteByProductId(productId, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("DELETE FROM erp_product_dept WHERE product_id = #{productId} AND tenant_id = #{tenantId}")
    int deleteByProductId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

    default int deleteByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        return deleteByProductIds(productIds, TenantContextHolder.getRequiredTenantId());
    }

    @Delete({"<script>",
            "DELETE FROM erp_product_dept WHERE tenant_id = #{tenantId}",
            "AND product_id IN",
            "<foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "#{productId}",
            "</foreach>",
            "</script>"})
    int deleteByProductIds(@Param("productIds") Collection<Long> productIds, @Param("tenantId") Long tenantId);

}
