package cn.iocoder.yudao.module.promotion.service.diyhomebanner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerUpdateReqVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 首页轮播图 Service 接口
 */
public interface DiyHomeBannerService {

    Long createDiyHomeBanner(@Valid DiyHomeBannerCreateReqVO createReqVO);

    void updateDiyHomeBanner(@Valid DiyHomeBannerUpdateReqVO updateReqVO);

    void deleteDiyHomeBanner(Long id);

    DiyHomeBannerDO getDiyHomeBanner(Long id);

    PageResult<DiyHomeBannerDO> getDiyHomeBannerPage(DiyHomeBannerPageReqVO pageReqVO);

    List<DiyHomeBannerDO> getEnabledDiyHomeBannerList();

}
