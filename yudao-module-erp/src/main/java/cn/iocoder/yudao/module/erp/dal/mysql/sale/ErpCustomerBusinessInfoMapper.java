package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerBusinessInfoMapper extends BaseMapperX<ErpCustomerBusinessInfoDO> {

    default PageResult<ErpCustomerBusinessInfoDO> selectPage(ErpCustomerBusinessInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerBusinessInfoDO>()
                .eqIfPresent(ErpCustomerBusinessInfoDO::getCustomerId, reqVO.getCustomerId())
                .likeIfPresent(ErpCustomerBusinessInfoDO::getCreditCode, reqVO.getCreditCode())
                .likeIfPresent(ErpCustomerBusinessInfoDO::getLegalPerson, reqVO.getLegalPerson())
                .orderByDesc(ErpCustomerBusinessInfoDO::getId));
    }

    default List<ErpCustomerBusinessInfoDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerBusinessInfoDO>()
                .eq(ErpCustomerBusinessInfoDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerBusinessInfoDO::getId));
    }

}
