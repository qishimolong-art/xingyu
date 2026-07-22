package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerExtendMapper extends BaseMapperX<ErpCustomerExtendDO> {

    default PageResult<ErpCustomerExtendDO> selectPage(ErpCustomerExtendPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerExtendDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpCustomerExtendDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(ErpCustomerExtendDO::getExtendKey, reqVO.getExtendKey());
        wrapper.likeIfPresent(ErpCustomerExtendDO::getExtendName, reqVO.getExtendName());
        wrapper.orderByAsc(ErpCustomerExtendDO::getSort);
        wrapper.orderByDesc(ErpCustomerExtendDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpCustomerExtendDO::getExtendKey,
                ErpCustomerExtendDO::getExtendName,
                ErpCustomerExtendDO::getExtendValue,
                ErpCustomerExtendDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpCustomerExtendDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerExtendDO>()
                .eq(ErpCustomerExtendDO::getCustomerId, customerId)
                .orderByAsc(ErpCustomerExtendDO::getSort)
                .orderByDesc(ErpCustomerExtendDO::getId));
    }

}
