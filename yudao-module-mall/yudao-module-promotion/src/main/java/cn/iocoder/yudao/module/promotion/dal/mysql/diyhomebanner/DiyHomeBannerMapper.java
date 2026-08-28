package cn.iocoder.yudao.module.promotion.dal.mysql.diyhomebanner;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerPageReqVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 首页轮播图 Mapper
 */
@Mapper
public interface DiyHomeBannerMapper extends BaseMapperX<DiyHomeBannerDO> {

    default PageResult<DiyHomeBannerDO> selectPage(DiyHomeBannerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DiyHomeBannerDO>()
                .likeIfPresent(DiyHomeBannerDO::getTitle, reqVO.getTitle())
                .eqIfPresent(DiyHomeBannerDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DiyHomeBannerDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DiyHomeBannerDO::getSort)
                .orderByDesc(DiyHomeBannerDO::getId));
    }

    default List<DiyHomeBannerDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<DiyHomeBannerDO>()
                .eq(DiyHomeBannerDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(DiyHomeBannerDO::getSort)
                .orderByDesc(DiyHomeBannerDO::getId));
    }

}
