package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierContractMapper extends BaseMapperX<ErpSupplierContractDO> {

    default PageResult<ErpSupplierContractDO> selectPage(ErpSupplierItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSupplierContractDO>()
                .eq(ErpSupplierContractDO::getSupplierId, reqVO.getSupplierId())
                .orderByDesc(ErpSupplierContractDO::getId));
    }

    default List<ErpSupplierContractDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierContractDO>()
                .eq(ErpSupplierContractDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierContractDO::getId));
    }

}
