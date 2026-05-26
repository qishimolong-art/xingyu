package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendInfoDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpSupplierExtendInfoMapper extends BaseMapperX<ErpSupplierExtendInfoDO> {

    default ErpSupplierExtendInfoDO selectBySupplierId(Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<ErpSupplierExtendInfoDO>()
                .eq(ErpSupplierExtendInfoDO::getSupplierId, supplierId));
    }

}
