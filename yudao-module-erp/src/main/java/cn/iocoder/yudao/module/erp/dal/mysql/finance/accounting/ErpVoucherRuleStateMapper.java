package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherRuleStateDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ErpVoucherRuleStateMapper extends BaseMapperX<ErpVoucherRuleStateDO> {
    default ErpVoucherRuleStateDO get(String key, boolean lock) {
        LambdaQueryWrapper<ErpVoucherRuleStateDO> q = new LambdaQueryWrapper<ErpVoucherRuleStateDO>()
            .eq(ErpVoucherRuleStateDO::getStateKey,key);
        if(lock) q.last("FOR UPDATE");
        return selectOne(q);
    }
}
