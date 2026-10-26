package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierExtendMapper extends BaseMapperX<ErpSupplierExtendDO> {

    default PageResult<ErpSupplierExtendDO> selectPage(ErpSupplierItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSupplierExtendDO>()
                .eq(ErpSupplierExtendDO::getSupplierId, reqVO.getSupplierId())
                .orderByAsc(ErpSupplierExtendDO::getSort)
                .orderByDesc(ErpSupplierExtendDO::getId));
    }

    default List<ErpSupplierExtendDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierExtendDO>()
                .eq(ErpSupplierExtendDO::getSupplierId, supplierId)
                .orderByAsc(ErpSupplierExtendDO::getSort)
                .orderByDesc(ErpSupplierExtendDO::getId));
    }

}
