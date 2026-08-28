package cn.iocoder.yudao.module.system.dal.dataobject.appnotice;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 小程序公告
 */
@TableName("mall_app_notice")
@KeySequence("mall_app_notice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppNoticeDO extends TenantBaseDO {

    /**
     * 公告编号
     */
    private Long id;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 公告内容
     */
    private String content;
    /**
     * 公告状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 排序，数字越大越靠前
     */
    private Integer sort;
    /**
     * 展示开始时间
     */
    private LocalDateTime startTime;
    /**
     * 展示结束时间
     */
    private LocalDateTime endTime;

}
