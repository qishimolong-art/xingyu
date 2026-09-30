package cn.iocoder.yudao.module.member.api.address.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户收件地址 Response DTO
 *
 * @author 芋道源码
 */
@Data
public class MemberAddressRespDTO {

    /**
     * 编号
     */
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 收件人名称
     */
    private String name;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 地区编号
     */
    private Integer areaId;
    /**
     * 收件详细地址
     */
    private String detailAddress;
    /**
     * 是否默认
     */
    private Boolean defaultStatus;

    /** 收货位置（GCJ-02），旧地址允许为空。 */
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String mapName;
    private String mapAddress;

}
