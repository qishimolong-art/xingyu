package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableMiscMapper extends BaseMapperX<ErpReceivableMiscDO> {

    default PageResult<ErpReceivableMiscDO> selectPage(ErpReceivableMiscPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpReceivableMiscDO> wrapper = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .inIfPresent(ErpReceivableMiscDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableMiscDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpReceivableMiscDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpReceivableMiscDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpReceivableMiscDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableMiscDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpReceivableMiscDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpReceivableMiscDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpReceivableMiscDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptNameAndCustomer(wrapper, reqVO.getKeyword(),
                ErpReceivableMiscDO::getNo,
                ErpReceivableMiscDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_receivable_misc",
                "no", "bizTime", "customerId", "accountId", "deptId", "handlerId", "amount", "status",
                "creator", "updater", "createTime", "updateTime", "remark");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableMiscDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getId, id)
                .eq(ErpReceivableMiscDO::getStatus, status));
    }

    default ErpReceivableMiscDO selectByNo(String no) {
        return selectOne(ErpReceivableMiscDO::getNo, no);
    }

    default ErpReceivableMiscDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getId, id)
                .last("FOR UPDATE"));
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpReceivableMiscDO::getCustomerId, customerId);
    }

}
