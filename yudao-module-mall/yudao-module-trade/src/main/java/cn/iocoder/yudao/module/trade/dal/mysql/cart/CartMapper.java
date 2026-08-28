package cn.iocoder.yudao.module.trade.dal.mysql.cart;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.trade.dal.dataobject.cart.CartDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface CartMapper extends BaseMapperX<CartDO> {

    default CartDO selectByUserIdAndSkuId(Long userId, Long skuId) {
        return selectOne(CartDO::getUserId, userId,
                CartDO::getSkuId, skuId);
    }

    default CartDO selectByScopeAndSkuIdAndStockId(Long userId, Long customerId, Long deptId, Long skuId,
                                                   Long stockId) {
        return selectOne(new LambdaQueryWrapper<CartDO>()
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId)
                .eq(CartDO::getSkuId, skuId)
                .eq(CartDO::getStockId, stockId));
    }

    default Integer selectSumByScope(Long userId, Long customerId, Long deptId) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<CartDO>()
                .select("SUM(count) AS sumCount")
                .eq("user_id", userId)
                .eq("customer_id", customerId)
                .eq("dept_id", deptId)
                .eq("selected", true)); // 只计算选中的
        // 获得数量
        return CollUtil.getFirst(result) != null ? MapUtil.getInt(result.get(0), "sumCount") : 0;
    }

    default CartDO selectById(Long id, Long userId, Long customerId, Long deptId) {
        return selectOne(new LambdaQueryWrapper<CartDO>()
                .eq(CartDO::getId, id)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId));
    }

    default List<CartDO> selectListByIds(Collection<Long> ids, Long userId, Long customerId, Long deptId) {
        return selectList(new LambdaQueryWrapper<CartDO>()
                .in(CartDO::getId, ids)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId));
    }

    default List<CartDO> selectListByScope(Long userId, Long customerId, Long deptId) {
        return selectList(new LambdaQueryWrapper<CartDO>()
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId));
    }

    default List<CartDO> selectListByScope(Long userId, Long customerId, Long deptId, Set<Long> ids) {
        return selectList(new LambdaQueryWrapper<CartDO>()
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId)
                .in(CartDO::getId, ids));
    }

    default void updateByIds(Collection<Long> ids, Long userId, Long customerId, Long deptId, CartDO updateObj) {
        update(updateObj, new LambdaQueryWrapper<CartDO>()
                .in(CartDO::getId, ids)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getCustomerId, customerId)
                .eq(CartDO::getDeptId, deptId));
    }

}
