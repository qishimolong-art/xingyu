package cn.iocoder.yudao.module.erp.dal.mysql.chain;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 连锁开单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpChainOrderMapper extends BaseMapperX<ErpChainOrderDO> {

    default PageResult<ErpChainOrderDO> selectPage(ErpChainOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpChainOrderDO>()
                .likeIfPresent(ErpChainOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpChainOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpChainOrderDO::getHqTenantId, reqVO.getHqTenantId())
                .eqIfPresent(ErpChainOrderDO::getBranchTenantId, reqVO.getBranchTenantId())
                .eqIfPresent(ErpChainOrderDO::getBranchType, reqVO.getBranchType())
                .betweenIfPresent(ErpChainOrderDO::getOrderTime, reqVO.getOrderTime())
                .orderByDesc(ErpChainOrderDO::getId));
    }

}
