package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSupplierDeptMapper extends BaseMapperX<ErpSupplierDeptDO> {

    default List<ErpSupplierDeptDO> selectListBySupplierId(Long supplierId) {
        return selectList(ErpSupplierDeptDO::getSupplierId, supplierId);
    }

    default List<ErpSupplierDeptDO> selectListBySupplierIds(Collection<Long> supplierIds) {
        return selectList(ErpSupplierDeptDO::getSupplierId, supplierIds);
    }

    default void deleteBySupplierId(Long supplierId) {
        delete(ErpSupplierDeptDO::getSupplierId, supplierId);
    }

    default void deleteBySupplierIds(Collection<Long> supplierIds) {
        deleteBatch(ErpSupplierDeptDO::getSupplierId, supplierIds);
    }

}
