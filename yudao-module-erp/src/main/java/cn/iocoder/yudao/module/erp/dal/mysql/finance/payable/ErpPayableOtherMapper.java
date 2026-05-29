package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPayableOtherMapper extends BaseMapperX<ErpPayableOtherDO> {

    default PageResult<ErpPayableOtherDO> selectPage(ErpPayableOtherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .likeIfPresent(ErpPayableOtherDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPayableOtherDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPayableOtherDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpPayableOtherDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpPayableOtherDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPayableOtherDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getId, id)
                .eq(ErpPayableOtherDO::getStatus, status));
    }

    default ErpPayableOtherDO selectByNo(String no) {
        return selectOne(ErpPayableOtherDO::getNo, no);
    }

}
