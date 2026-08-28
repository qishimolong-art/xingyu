package cn.iocoder.yudao.module.system.service.appnotice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.appnotice.AppNoticeDO;

import java.util.List;

/**
 * 小程序公告 Service 接口
 */
public interface AppNoticeService {

    Long createAppNotice(AppNoticeSaveReqVO createReqVO);

    void updateAppNotice(AppNoticeSaveReqVO updateReqVO);

    void deleteAppNotice(Long id);

    AppNoticeDO getAppNotice(Long id);

    PageResult<AppNoticeDO> getAppNoticePage(AppNoticePageReqVO pageReqVO);

    AppNoticeDO getHomeAppNotice();

    List<AppNoticeDO> getHomeAppNoticeList();

    AppNoticeDO getVisibleAppNotice(Long id);

}
