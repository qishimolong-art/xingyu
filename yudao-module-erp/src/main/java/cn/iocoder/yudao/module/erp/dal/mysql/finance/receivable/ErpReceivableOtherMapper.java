package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableOtherMapper extends BaseMapperX<ErpReceivableOtherDO> {

    default PageResult<ErpReceivableOtherDO> selectPage(ErpReceivableOtherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .likeIfPresent(ErpReceivableOtherDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpReceivableOtherDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableOtherDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpReceivableOtherDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpReceivableOtherDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableOtherDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getId, id)
                .eq(ErpReceivableOtherDO::getStatus, status));
    }

    default ErpReceivableOtherDO selectByNo(String no) {
        return selectOne(ErpReceivableOtherDO::getNo, no);
    }
}
