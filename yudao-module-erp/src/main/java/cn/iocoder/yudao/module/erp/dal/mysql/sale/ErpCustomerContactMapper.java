package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerContactMapper extends BaseMapperX<ErpCustomerContactDO> {

    default PageResult<ErpCustomerContactDO> selectPage(ErpCustomerContactPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerContactDO>()
                .eqIfPresent(ErpCustomerContactDO::getCustomerId, reqVO.getCustomerId())
                .likeIfPresent(ErpCustomerContactDO::getName, reqVO.getName())
                .eqIfPresent(ErpCustomerContactDO::getMobile, reqVO.getMobile())
                .orderByDesc(ErpCustomerContactDO::getId));
    }

    default List<ErpCustomerContactDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerContactDO>()
                .eq(ErpCustomerContactDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerContactDO::getPrimaryContact)
                .orderByDesc(ErpCustomerContactDO::getId));
    }

}
