package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierContractMapper extends BaseMapperX<ErpSupplierContractDO> {

    default List<ErpSupplierContractDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierContractDO>()
                .eq(ErpSupplierContractDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierContractDO::getId));
    }

}
