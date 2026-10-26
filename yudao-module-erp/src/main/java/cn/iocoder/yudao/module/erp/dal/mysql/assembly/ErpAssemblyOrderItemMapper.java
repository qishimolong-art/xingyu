package cn.iocoder.yudao.module.erp.dal.mysql.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpAssemblyOrderItemMapper extends BaseMapperX<ErpAssemblyOrderItemDO> {

    default List<ErpAssemblyOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<ErpAssemblyOrderItemDO>()
                .eq(ErpAssemblyOrderItemDO::getOrderId, orderId)
                .orderByAsc(ErpAssemblyOrderItemDO::getSort)
                .orderByAsc(ErpAssemblyOrderItemDO::getId));
    }

    default PageResult<ErpAssemblyOrderItemDO> selectPage(ErpAssemblyOrderItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssemblyOrderItemDO>()
                .eq(ErpAssemblyOrderItemDO::getOrderId, reqVO.getOrderId())
                .orderByAsc(ErpAssemblyOrderItemDO::getSort)
                .orderByAsc(ErpAssemblyOrderItemDO::getId));
    }

    default List<ErpAssemblyOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpAssemblyOrderItemDO>()
                .in(ErpAssemblyOrderItemDO::getOrderId, orderIds));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpAssemblyOrderItemDO::getOrderId, orderId);
    }

    default List<Map<String, Object>> selectCountRowsByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectCountRowsByOrderIdsInternal(orderIds);
    }

    @Select({"<script>",
            "SELECT order_id AS orderId, COUNT(*) AS itemCount FROM erp_assembly_order_item",
            "WHERE deleted = b'0' AND order_id IN",
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "GROUP BY order_id",
            "</script>"})
    List<Map<String, Object>> selectCountRowsByOrderIdsInternal(
            @Param("orderIds") Collection<Long> orderIds);

}
