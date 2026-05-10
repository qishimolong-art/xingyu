package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerImageMapper extends BaseMapperX<ErpCustomerImageDO> {

    default PageResult<ErpCustomerImageDO> selectPage(ErpCustomerImagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerImageDO>()
                .eqIfPresent(ErpCustomerImageDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpCustomerImageDO::getImageType, reqVO.getImageType())
                .orderByAsc(ErpCustomerImageDO::getSort)
                .orderByDesc(ErpCustomerImageDO::getId));
    }

    default List<ErpCustomerImageDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerImageDO>()
                .eq(ErpCustomerImageDO::getCustomerId, customerId)
                .orderByAsc(ErpCustomerImageDO::getSort)
                .orderByDesc(ErpCustomerImageDO::getDefaulted)
                .orderByDesc(ErpCustomerImageDO::getId));
    }

}
