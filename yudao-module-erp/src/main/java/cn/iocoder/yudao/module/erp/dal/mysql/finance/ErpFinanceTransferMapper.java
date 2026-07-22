package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 银行转账�?Mapper
 */
@Mapper
public interface ErpFinanceTransferMapper extends BaseMapperX<ErpFinanceTransferDO> {

    default PageResult<ErpFinanceTransferDO> selectPage(ErpFinanceTransferPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpFinanceTransferDO> wrapper = new LambdaQueryWrapperX<ErpFinanceTransferDO>()
                .likeIfPresent(ErpFinanceTransferDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpFinanceTransferDO::getTransferTime, reqVO.getTransferTime())
                .eqIfPresent(ErpFinanceTransferDO::getOutAccountId, reqVO.getOutAccountId())
                .eqIfPresent(ErpFinanceTransferDO::getInAccountId, reqVO.getInAccountId())
                .eqIfPresent(ErpFinanceTransferDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinanceTransferDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpFinanceTransferDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpFinanceTransferDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinanceTransferDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpFinanceTransferDO::getNo,
                ErpFinanceTransferDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_finance_transfer",
                "no", "transferTime", "outAccountId", "inAccountId", "financeUserId", "deptId",
                "transferPrice", "status", "creator", "updater", "createTime", "updateTime", "remark");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpFinanceTransferDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpFinanceTransferDO>()
                .eq(ErpFinanceTransferDO::getId, id)
                .eq(ErpFinanceTransferDO::getStatus, status));
    }

    default ErpFinanceTransferDO selectByNo(String no) {
        return selectOne(ErpFinanceTransferDO::getNo, no);
    }

}
