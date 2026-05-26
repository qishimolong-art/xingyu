package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 系统开账 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpBookOpenMapper extends BaseMapperX<ErpBookOpenDO> {

    default ErpBookOpenDO selectByNo(String no) {
        return selectOne(ErpBookOpenDO::getNo, no);
    }

    /**
     * 按 (chainName, fiscalYear, period) 三元组判重
     *
     * @param chainName  连锁名称（可能为 null）
     * @param fiscalYear 会计年度
     * @param period     开账期间
     * @return 已存在的记录
     */
    default ErpBookOpenDO selectByChainNameAndYearAndPeriod(String chainName, Integer fiscalYear, Integer period) {
        return selectOne(Wrappers.<ErpBookOpenDO>lambdaQuery()
                .eq(chainName != null, ErpBookOpenDO::getChainName, chainName)
                .isNull(chainName == null, ErpBookOpenDO::getChainName)
                .eq(ErpBookOpenDO::getFiscalYear, fiscalYear)
                .eq(ErpBookOpenDO::getPeriod, period));
    }

    default PageResult<ErpBookOpenDO> selectPage(ErpBookOpenPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpBookOpenDO>()
                .likeIfPresent(ErpBookOpenDO::getNo, reqVO.getNo())
                .likeIfPresent(ErpBookOpenDO::getChainName, reqVO.getChainName())
                .eqIfPresent(ErpBookOpenDO::getFiscalYear, reqVO.getFiscalYear())
                .eqIfPresent(ErpBookOpenDO::getPeriod, reqVO.getPeriod())
                .eqIfPresent(ErpBookOpenDO::getOpened, reqVO.getOpened())
                .orderByDesc(ErpBookOpenDO::getId));
    }

    /**
     * 按 (fiscalYear, period) 查询任一开账记录（用于自动凭证开账判断；不区分 chainName）。
     * 多条匹配时按 id 倒序取最近一条。
     */
    default ErpBookOpenDO selectByYearAndPeriod(Integer fiscalYear, Integer period) {
        return selectOne(Wrappers.<ErpBookOpenDO>lambdaQuery()
                .eq(ErpBookOpenDO::getFiscalYear, fiscalYear)
                .eq(ErpBookOpenDO::getPeriod, period)
                .orderByDesc(ErpBookOpenDO::getId)
                .last("limit 1"));
    }

}
