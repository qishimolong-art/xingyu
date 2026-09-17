package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helpers for finance list filter lazy selectors.
 */
public final class ErpFinanceSimplePageUtils {

    private ErpFinanceSimplePageUtils() {
    }

    public static PageResult<UserSimpleRespVO> buildUserSimplePage(AdminUserApi adminUserApi, PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = page.getList().stream()
                .map(user -> new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }
}
