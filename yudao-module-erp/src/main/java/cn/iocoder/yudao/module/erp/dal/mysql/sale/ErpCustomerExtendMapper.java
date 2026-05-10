package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerExtendMapper extends BaseMapperX<ErpCustomerExtendDO> {

    default PageResult<ErpCustomerExtendDO> selectPage(ErpCustomerExtendPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerExtendDO>()
                .eqIfPresent(ErpCustomerExtendDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpCustomerExtendDO::getExtendKey, reqVO.getExtendKey())
                .likeIfPresent(ErpCustomerExtendDO::getExtendName, reqVO.getExtendName())
                .orderByAsc(ErpCustomerExtendDO::getSort)
                .orderByDesc(ErpCustomerExtendDO::getId));
    }

    default List<ErpCustomerExtendDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerExtendDO>()
                .eq(ErpCustomerExtendDO::getCustomerId, customerId)
                .orderByAsc(ErpCustomerExtendDO::getSort)
                .orderByDesc(ErpCustomerExtendDO::getId));
    }

}
