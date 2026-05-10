package cn.iocoder.yudao.module.erp.dal.mysql.autoorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpAutoOrderRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 自动订货规则 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpAutoOrderRuleMapper extends BaseMapperX<ErpAutoOrderRuleDO> {

    default List<ErpAutoOrderRuleDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpAutoOrderRuleDO>()
                .eqIfPresent(ErpAutoOrderRuleDO::getStatus, status)
                .orderByDesc(ErpAutoOrderRuleDO::getId));
    }

    default List<ErpAutoOrderRuleDO> selectListByProductId(Long productId) {
        return selectList(ErpAutoOrderRuleDO::getProductId, productId);
    }

}
