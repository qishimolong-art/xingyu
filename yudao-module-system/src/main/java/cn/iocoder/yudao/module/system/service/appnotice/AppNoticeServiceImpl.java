package cn.iocoder.yudao.module.system.service.appnotice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.appnotice.AppNoticeDO;
import cn.iocoder.yudao.module.system.dal.mysql.appnotice.AppNoticeMapper;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.APP_NOTICE_NOT_EXISTS;

/**
 * 小程序公告 Service 实现类
 */
@Service
@Validated
public class AppNoticeServiceImpl implements AppNoticeService {

    @Resource
    private AppNoticeMapper appNoticeMapper;

    @Override
    public Long createAppNotice(AppNoticeSaveReqVO createReqVO) {
        AppNoticeDO appNotice = BeanUtils.toBean(createReqVO, AppNoticeDO.class);
        if (appNotice.getSort() == null) {
            appNotice.setSort(0);
        }
        appNoticeMapper.insert(appNotice);
        return appNotice.getId();
    }

    @Override
    public void updateAppNotice(AppNoticeSaveReqVO updateReqVO) {
        validateAppNoticeExists(updateReqVO.getId());
        AppNoticeDO updateObj = BeanUtils.toBean(updateReqVO, AppNoticeDO.class);
        if (updateObj.getSort() == null) {
            updateObj.setSort(0);
        }
        appNoticeMapper.updateById(updateObj);
    }

    @Override
    public void deleteAppNotice(Long id) {
        validateAppNoticeExists(id);
        appNoticeMapper.deleteById(id);
    }

    @Override
    public AppNoticeDO getAppNotice(Long id) {
        return appNoticeMapper.selectById(id);
    }

    @Override
    public PageResult<AppNoticeDO> getAppNoticePage(AppNoticePageReqVO pageReqVO) {
        return appNoticeMapper.selectPage(pageReqVO);
    }

    @Override
    public AppNoticeDO getHomeAppNotice() {
        return appNoticeMapper.selectHomeNotice();
    }

    @Override
    public List<AppNoticeDO> getHomeAppNoticeList() {
        return appNoticeMapper.selectHomeNoticeList();
    }

    @Override
    public AppNoticeDO getVisibleAppNotice(Long id) {
        AppNoticeDO appNotice = appNoticeMapper.selectVisibleNotice(id);
        if (appNotice == null) {
            throw exception(APP_NOTICE_NOT_EXISTS);
        }
        return appNotice;
    }

    @VisibleForTesting
    public void validateAppNoticeExists(Long id) {
        if (id == null) {
            return;
        }
        AppNoticeDO appNotice = appNoticeMapper.selectById(id);
        if (appNotice == null) {
            throw exception(APP_NOTICE_NOT_EXISTS);
        }
    }

}
