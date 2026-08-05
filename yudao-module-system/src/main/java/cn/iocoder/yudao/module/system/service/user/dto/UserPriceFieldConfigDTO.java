package cn.iocoder.yudao.module.system.service.user.dto;

import lombok.Data;

/**
 * User personal price field visibility config.
 */
@Data
public class UserPriceFieldConfigDTO {

    private Long id;

    private Long userId;

    private String priceFieldCode;

    private String priceFieldLabel;

    private Boolean visible;

    private Integer sort;

}
