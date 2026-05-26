package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 凭证字字典 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpVoucherWordMapper extends BaseMapperX<ErpVoucherWordDO> {

    default ErpVoucherWordDO selectByCode(String code) {
        return selectOne(ErpVoucherWordDO::getCode, code);
    }

    default PageResult<ErpVoucherWordDO> selectPage(ErpVoucherWordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpVoucherWordDO>()
                .likeIfPresent(ErpVoucherWordDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpVoucherWordDO::getName, reqVO.getName())
                .eqIfPresent(ErpVoucherWordDO::getEnable, reqVO.getEnable())
                .orderByAsc(ErpVoucherWordDO::getSort)
                .orderByDesc(ErpVoucherWordDO::getId));
    }

    default List<ErpVoucherWordDO> selectListByEnable(Boolean enable) {
        return selectList(new LambdaQueryWrapperX<ErpVoucherWordDO>()
                .eq(ErpVoucherWordDO::getEnable, enable)
                .orderByAsc(ErpVoucherWordDO::getSort));
    }

}
