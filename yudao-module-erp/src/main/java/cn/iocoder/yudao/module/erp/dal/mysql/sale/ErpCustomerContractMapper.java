package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerContractMapper extends BaseMapperX<ErpCustomerContractDO> {

    default PageResult<ErpCustomerContractDO> selectPage(ErpCustomerContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerContractDO>()
                .eqIfPresent(ErpCustomerContractDO::getCustomerId, reqVO.getCustomerId())
                .likeIfPresent(ErpCustomerContractDO::getContractNo, reqVO.getContractNo())
                .eqIfPresent(ErpCustomerContractDO::getContractType, reqVO.getContractType())
                .orderByDesc(ErpCustomerContractDO::getId));
    }

    default List<ErpCustomerContractDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerContractDO>()
                .eq(ErpCustomerContractDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerContractDO::getContractDate)
                .orderByDesc(ErpCustomerContractDO::getId));
    }

}
