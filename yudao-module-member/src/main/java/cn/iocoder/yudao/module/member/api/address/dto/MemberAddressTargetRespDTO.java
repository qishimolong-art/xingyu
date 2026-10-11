package cn.iocoder.yudao.module.member.api.address.dto;

import lombok.Data;

import java.util.List;

/**
 * 按收货地区筛选出的商城会员。
 */
@Data
public class MemberAddressTargetRespDTO {

    private Long userId;
    private String nickname;
    private String mobile;
    private Integer status;
    private List<Long> matchedAreaIds;
    private List<String> matchedAreaNames;

}
