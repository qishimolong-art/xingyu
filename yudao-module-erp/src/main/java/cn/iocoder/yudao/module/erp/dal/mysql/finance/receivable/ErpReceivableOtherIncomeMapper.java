package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableOtherIncomeMapper extends BaseMapperX<ErpReceivableOtherIncomeDO> {

    default PageResult<ErpReceivableOtherIncomeDO> selectPage(ErpReceivableOtherIncomePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpReceivableOtherIncomeDO>()
                .inIfPresent(ErpReceivableOtherIncomeDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableOtherIncomeDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getIncomeType, reqVO.getIncomeType())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getHandlerId, reqVO.getHandlerId())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getParty, reqVO.getParty())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpReceivableOtherIncomeDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableOtherIncomeDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableOtherIncomeDO>()
                .eq(ErpReceivableOtherIncomeDO::getId, id)
                .eq(ErpReceivableOtherIncomeDO::getStatus, status));
    }

    default ErpReceivableOtherIncomeDO selectByNo(String no) {
        return selectOne(ErpReceivableOtherIncomeDO::getNo, no);
    }
}
