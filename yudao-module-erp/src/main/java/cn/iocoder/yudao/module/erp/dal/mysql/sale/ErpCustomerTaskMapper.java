package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerTaskMapper extends BaseMapperX<ErpCustomerTaskDO> {

    default PageResult<ErpCustomerTaskDO> selectPage(ErpCustomerTaskPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerTaskDO> wrapper = new LambdaQueryWrapperX<ErpCustomerTaskDO>()
                .eqIfPresent(ErpCustomerTaskDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpCustomerTaskDO::getYear, reqVO.getYear())
                .eqIfPresent(ErpCustomerTaskDO::getMonth, reqVO.getMonth())
                .eqIfPresent(ErpCustomerTaskDO::getTaskLevel, reqVO.getTaskLevel())
                .orderByDesc(ErpCustomerTaskDO::getYear)
                .orderByDesc(ErpCustomerTaskDO::getMonth)
                .orderByDesc(ErpCustomerTaskDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpCustomerTaskDO::getTaskLevel,
                ErpCustomerTaskDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpCustomerTaskDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerTaskDO>()
                .eq(ErpCustomerTaskDO::getCustomerId, customerId)
                .orderByDesc(ErpCustomerTaskDO::getYear)
                .orderByDesc(ErpCustomerTaskDO::getMonth)
                .orderByDesc(ErpCustomerTaskDO::getId));
    }

}
