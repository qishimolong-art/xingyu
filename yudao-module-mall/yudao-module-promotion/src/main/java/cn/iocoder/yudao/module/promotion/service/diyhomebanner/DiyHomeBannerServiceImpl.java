package cn.iocoder.yudao.module.promotion.service.diyhomebanner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerUpdateReqVO;
import cn.iocoder.yudao.module.promotion.convert.diyhomebanner.DiyHomeBannerConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;
import cn.iocoder.yudao.module.promotion.dal.mysql.diyhomebanner.DiyHomeBannerMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.promotion.enums.ErrorCodeConstants.DIY_HOME_BANNER_NOT_EXISTS;

/**
 * 首页轮播图 Service 实现类
 */
@Service
@Validated
public class DiyHomeBannerServiceImpl implements DiyHomeBannerService {

    @Resource
    private DiyHomeBannerMapper diyHomeBannerMapper;

    @Override
    public Long createDiyHomeBanner(DiyHomeBannerCreateReqVO createReqVO) {
        DiyHomeBannerDO banner = DiyHomeBannerConvert.INSTANCE.convert(createReqVO);
        fillDefaultValues(banner);
        diyHomeBannerMapper.insert(banner);
        return banner.getId();
    }

    @Override
    public void updateDiyHomeBanner(DiyHomeBannerUpdateReqVO updateReqVO) {
        validateDiyHomeBannerExists(updateReqVO.getId());
        DiyHomeBannerDO updateObj = DiyHomeBannerConvert.INSTANCE.convert(updateReqVO);
        fillDefaultValues(updateObj);
        diyHomeBannerMapper.updateById(updateObj);
    }

    @Override
    public void deleteDiyHomeBanner(Long id) {
        validateDiyHomeBannerExists(id);
        diyHomeBannerMapper.deleteById(id);
    }

    private void validateDiyHomeBannerExists(Long id) {
        if (diyHomeBannerMapper.selectById(id) == null) {
            throw exception(DIY_HOME_BANNER_NOT_EXISTS);
        }
    }

    private void fillDefaultValues(DiyHomeBannerDO banner) {
        if (banner.getTitle() == null) {
            banner.setTitle("");
        }
        if (banner.getUrl() == null) {
            banner.setUrl("");
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
    }

    @Override
    public DiyHomeBannerDO getDiyHomeBanner(Long id) {
        return diyHomeBannerMapper.selectById(id);
    }

    @Override
    public PageResult<DiyHomeBannerDO> getDiyHomeBannerPage(DiyHomeBannerPageReqVO pageReqVO) {
        return diyHomeBannerMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DiyHomeBannerDO> getEnabledDiyHomeBannerList() {
        return diyHomeBannerMapper.selectEnabledList();
    }

}
