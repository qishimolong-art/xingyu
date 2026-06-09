package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 销售手推车 Mapper
 */
@Mapper
public interface ErpSaleCartMapper extends BaseMapperX<ErpSaleCartDO> {

    default PageResult<ErpSaleCartDO> selectPage(ErpSaleCartPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleCartDO> queryWrapper = new LambdaQueryWrapperX<ErpSaleCartDO>()
                .likeIfPresent(ErpSaleCartDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleCartDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleCartDO::getSaleUserId, reqVO.getSaleUserId())
                .eqIfPresent(ErpSaleCartDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleCartDO::getCartTime, reqVO.getCartTime())
                .eqIfPresent(ErpSaleCartDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleCartDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSaleCartDO::getId, reqVO.getIds())
                .orderByDesc(ErpSaleCartDO::getId);
        if (reqVO.getStatus() == null && Boolean.TRUE.equals(reqVO.getIncludeCompleted())) {
            queryWrapper.in(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        } else if (reqVO.getStatus() == null) {
            queryWrapper.notIn(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        }
        return selectPage(reqVO, queryWrapper);
    }

    default ErpSaleCartDO selectByNo(String no) {
        return selectOne(ErpSaleCartDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleCartDO::getCustomerId, customerId);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleCartDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleCartDO>()
                .eq(ErpSaleCartDO::getId, id).eq(ErpSaleCartDO::getStatus, status));
    }

}
