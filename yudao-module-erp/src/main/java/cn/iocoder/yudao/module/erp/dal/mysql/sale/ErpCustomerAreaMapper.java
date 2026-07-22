package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerAreaMapper extends BaseMapperX<ErpCustomerAreaDO> {

    default PageResult<ErpCustomerAreaDO> selectPage(ErpCustomerAreaPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerAreaDO> wrapper = new LambdaQueryWrapperX<ErpCustomerAreaDO>()
                .eqIfPresent(ErpCustomerAreaDO::getCustomerId, reqVO.getCustomerId())
                .likeIfPresent(ErpCustomerAreaDO::getMapAddress, reqVO.getMapAddress())
                .orderByDesc(ErpCustomerAreaDO::getDefaulted)
                .orderByDesc(ErpCustomerAreaDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpCustomerAreaDO::getMapAddress,
                ErpCustomerAreaDO::getDetailAddress,
                ErpCustomerAreaDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpCustomerAreaDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerAreaDO>()
                .eq(ErpCustomerAreaDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerAreaDO::getDefaulted)
                .orderByDesc(ErpCustomerAreaDO::getId));
    }

}
