package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpCustomerExtendInfoMapper extends BaseMapperX<ErpCustomerExtendInfoDO> {

    default ErpCustomerExtendInfoDO selectByCustomerId(Long customerId) {
        return selectOne(new LambdaQueryWrapperX<ErpCustomerExtendInfoDO>()
                .eq(ErpCustomerExtendInfoDO::getCustomerId, customerId));
    }

}
