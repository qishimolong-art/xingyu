package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ErpCustomerMemberMapper extends BaseMapperX<ErpCustomerMemberDO> {

    default List<ErpCustomerMemberDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerMemberDO>()
                .eq(ErpCustomerMemberDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerMemberDO::getId));
    }

    default List<ErpCustomerMemberDO> selectListByMemberUserId(Long memberUserId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerMemberDO>()
                .eq(ErpCustomerMemberDO::getMemberUserId, memberUserId)
                .orderByDesc(ErpCustomerMemberDO::getId));
    }

    default ErpCustomerMemberDO selectByCustomerIdAndMemberUserId(Long customerId, Long memberUserId) {
        return selectOne(new LambdaQueryWrapperX<ErpCustomerMemberDO>()
                .eq(ErpCustomerMemberDO::getCustomerId, customerId)
                .eq(ErpCustomerMemberDO::getMemberUserId, memberUserId));
    }

    default ErpCustomerMemberDO selectEnabledByMemberUserId(Long memberUserId) {
        return selectOne(new LambdaQueryWrapperX<ErpCustomerMemberDO>()
                .eq(ErpCustomerMemberDO::getMemberUserId, memberUserId)
                .eq(ErpCustomerMemberDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

    @Update("UPDATE erp_customer_member "
            + "SET deleted = b'1', active_key = id, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = b'0'")
    int deleteLogicById(@Param("id") Long id);

}
