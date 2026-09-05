package cn.iocoder.yudao.module.promotion.controller.app.reward;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.promotion.controller.admin.reward.vo.RewardActivityPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.reward.vo.AppRewardActivityRespVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.reward.RewardActivityDO;
import cn.iocoder.yudao.module.promotion.service.reward.RewardActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 满减送活动")
@RestController
@RequestMapping("/promotion/reward-activity")
@Validated
public class AppRewardActivityController {

    @Resource
    private RewardActivityService rewardActivityService;

    @GetMapping("/get")
    @Operation(summary = "获得满减送活动")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PermitAll
    public CommonResult<AppRewardActivityRespVO> getRewardActivity(@RequestParam("id") Long id) {
        RewardActivityDO activity = rewardActivityService.getRewardActivity(id);
        if (activity == null) {
            return success(null);
        }
        return success(convertRewardActivity(activity));
    }

    @GetMapping("/page")
    @Operation(summary = "获得满减送活动分页")
    @PermitAll
    public CommonResult<PageResult<AppRewardActivityRespVO>> getRewardActivityPage(@Valid RewardActivityPageReqVO pageVO) {
        pageVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<RewardActivityDO> pageResult = rewardActivityService.getRewardActivityPage(pageVO);
        PageResult<AppRewardActivityRespVO> result = BeanUtils.toBean(pageResult, AppRewardActivityRespVO.class);
        result.setList(pageResult.getList().stream()
                .map(this::convertRewardActivity)
                .collect(Collectors.toList()));
        return success(result);
    }

    private AppRewardActivityRespVO convertRewardActivity(RewardActivityDO activity) {
        AppRewardActivityRespVO activityVO = BeanUtils.toBean(activity, AppRewardActivityRespVO.class);
        if (activityVO.getRules() == null || activity.getRules() == null) {
            return activityVO;
        }
        for (int i = 0; i < activityVO.getRules().size() && i < activity.getRules().size(); i++) {
            AppRewardActivityRespVO.Rule ruleVO = activityVO.getRules().get(i);
            RewardActivityDO.Rule rule = activity.getRules().get(i);
            ruleVO.setDescription(rewardActivityService.getRewardActivityRuleDescription(activity.getConditionType(), rule));
        }
        return activityVO;
    }

}
