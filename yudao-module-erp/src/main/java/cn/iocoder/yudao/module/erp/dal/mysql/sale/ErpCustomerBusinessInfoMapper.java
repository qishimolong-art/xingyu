package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerBusinessInfoMapper extends BaseMapperX<ErpCustomerBusinessInfoDO> {

    default PageResult<ErpCustomerBusinessInfoDO> selectPage(ErpCustomerBusinessInfoPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerBusinessInfoDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpCustomerBusinessInfoDO::getCustomerId, reqVO.getCustomerId());
        wrapper.likeIfPresent(ErpCustomerBusinessInfoDO::getCreditCode, reqVO.getCreditCode());
        wrapper.likeIfPresent(ErpCustomerBusinessInfoDO::getLegalPerson, reqVO.getLegalPerson());
        wrapper.orderByDesc(ErpCustomerBusinessInfoDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpCustomerBusinessInfoDO::getCreditCode,
                ErpCustomerBusinessInfoDO::getLegalPerson,
                ErpCustomerBusinessInfoDO::getBusinessScope,
                ErpCustomerBusinessInfoDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpCustomerBusinessInfoDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerBusinessInfoDO>()
                .eq(ErpCustomerBusinessInfoDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerBusinessInfoDO::getId));
    }

}
