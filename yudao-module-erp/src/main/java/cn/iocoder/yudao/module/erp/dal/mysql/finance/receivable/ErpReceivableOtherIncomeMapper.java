package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableOtherIncomeMapper extends BaseMapperX<ErpReceivableOtherIncomeDO> {

    default PageResult<ErpReceivableOtherIncomeDO> selectPage(ErpReceivableOtherIncomePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpReceivableOtherIncomeDO> wrapper = new LambdaQueryWrapperX<ErpReceivableOtherIncomeDO>()
                .inIfPresent(ErpReceivableOtherIncomeDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableOtherIncomeDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getIncomeType, reqVO.getIncomeType())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getVoucherNo, reqVO.getVoucherNo())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getHandlerId, reqVO.getHandlerId())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getParty, reqVO.getParty())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getCreator, reqVO.getCreator());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpReceivableOtherIncomeDO::getNo,
                ErpReceivableOtherIncomeDO::getSettleMethod,
                ErpReceivableOtherIncomeDO::getVoucherNo,
                ErpReceivableOtherIncomeDO::getIncomeType,
                ErpReceivableOtherIncomeDO::getParty,
                ErpReceivableOtherIncomeDO::getRelatedBiz,
                ErpReceivableOtherIncomeDO::getDocType,
                ErpReceivableOtherIncomeDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_receivable_other_income",
                "no", "bizTime", "incomeType", "accountId", "deptId", "handlerId", "party",
                "totalAmount", "status", "remark", "creator", "relatedBiz", "updater", "createTime", "updateTime");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableOtherIncomeDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableOtherIncomeDO>()
                .eq(ErpReceivableOtherIncomeDO::getId, id)
                .eq(ErpReceivableOtherIncomeDO::getStatus, status));
    }

    default ErpReceivableOtherIncomeDO selectByNo(String no) {
        return selectOne(ErpReceivableOtherIncomeDO::getNo, no);
    }

    default ErpReceivableOtherIncomeDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpReceivableOtherIncomeDO>()
                .eq(ErpReceivableOtherIncomeDO::getId, id)
                .last("FOR UPDATE"));
    }
}
