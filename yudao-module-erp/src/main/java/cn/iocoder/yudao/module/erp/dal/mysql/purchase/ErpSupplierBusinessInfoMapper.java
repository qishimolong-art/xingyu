package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierBusinessInfoMapper extends BaseMapperX<ErpSupplierBusinessInfoDO> {

    default List<ErpSupplierBusinessInfoDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierBusinessInfoDO>()
                .eq(ErpSupplierBusinessInfoDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierBusinessInfoDO::getId));
    }

}
