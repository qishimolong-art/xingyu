package cn.iocoder.yudao.module.promotion.convert.diyhomebanner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerRespVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerUpdateReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.diyhomebanner.vo.AppDiyHomeBannerRespVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DiyHomeBannerConvert {

    DiyHomeBannerConvert INSTANCE = Mappers.getMapper(DiyHomeBannerConvert.class);

    DiyHomeBannerDO convert(DiyHomeBannerCreateReqVO createReqVO);

    DiyHomeBannerDO convert(DiyHomeBannerUpdateReqVO updateReqVO);

    DiyHomeBannerRespVO convert(DiyHomeBannerDO banner);

    PageResult<DiyHomeBannerRespVO> convertPage(PageResult<DiyHomeBannerDO> pageResult);

    List<AppDiyHomeBannerRespVO> convertAppList(List<DiyHomeBannerDO> bannerList);

}
