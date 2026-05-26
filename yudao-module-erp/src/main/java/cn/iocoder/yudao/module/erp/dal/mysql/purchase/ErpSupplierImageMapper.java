package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierImageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierImageMapper extends BaseMapperX<ErpSupplierImageDO> {

    default List<ErpSupplierImageDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierImageDO>()
                .eq(ErpSupplierImageDO::getSupplierId, supplierId)
                .orderByAsc(ErpSupplierImageDO::getSort)
                .orderByDesc(ErpSupplierImageDO::getId));
    }

}
