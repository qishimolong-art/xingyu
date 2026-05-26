package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierContactMapper extends BaseMapperX<ErpSupplierContactDO> {

    default List<ErpSupplierContactDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierContactDO>()
                .eq(ErpSupplierContactDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierContactDO::getId));
    }

}
