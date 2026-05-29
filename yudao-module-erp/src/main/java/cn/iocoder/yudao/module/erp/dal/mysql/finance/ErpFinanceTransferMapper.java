package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 银行转账单 Mapper
 */
@Mapper
public interface ErpFinanceTransferMapper extends BaseMapperX<ErpFinanceTransferDO> {

    default PageResult<ErpFinanceTransferDO> selectPage(ErpFinanceTransferPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpFinanceTransferDO>()
                .likeIfPresent(ErpFinanceTransferDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpFinanceTransferDO::getTransferTime, reqVO.getTransferTime())
                .eqIfPresent(ErpFinanceTransferDO::getOutAccountId, reqVO.getOutAccountId())
                .eqIfPresent(ErpFinanceTransferDO::getInAccountId, reqVO.getInAccountId())
                .eqIfPresent(ErpFinanceTransferDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinanceTransferDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpFinanceTransferDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinanceTransferDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpFinanceTransferDO::getId));
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
