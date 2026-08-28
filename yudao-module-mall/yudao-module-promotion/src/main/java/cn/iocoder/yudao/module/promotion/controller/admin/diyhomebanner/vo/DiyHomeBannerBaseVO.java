package cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 首页轮播图 Base VO
 */
@Data
public class DiyHomeBannerBaseVO {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "跳转链接")
    private String url;

    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "图片地址不能为空")
    private String picUrl;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "备注")
    private String memo;

}
