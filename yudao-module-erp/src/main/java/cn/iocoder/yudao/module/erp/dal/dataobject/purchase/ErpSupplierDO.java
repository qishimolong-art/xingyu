package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 供应商 DO
 *
 * @author 芋道源码
 */
@TableName("erp_supplier")
@KeySequence("erp_supplier_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierDO extends BaseDO {

    /**
     * 供应商编号
     */
    @TableId
    private Long id;
    /**
     * 供应商名称
     */
    private String name;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 联系电话
     */
    private String telephone;
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 传真
     */
    private String fax;
    /**
     * 备注
     */
    private String remark;
    /**
     * 开启状态
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 纳税人识别号
     */
    private String taxNo;
    /**
     * 税率
     */
    private BigDecimal taxPercent;
    /**
     * 开户行
     */
    private String bankName;
    /**
     * 开户账号
     */
    private String bankAccount;
    /**
     * 开户地址
     */
    private String bankAddress;

    // ========== 汽配扩展字段 ==========
    /**
     * 编码(自动生成)
     */
    private String code;
    /**
     * 旧编码
     */
    private String oldCode;
    /**
     * 简称
     */
    private String shortName;
    /**
     * 外文名
     */
    private String foreignName;
    /**
     * 区域
     */
    private String region;
    /**
     * 往来类别
     */
    private String category;
    /**
     * 账户
     */
    private String account;
    /**
     * 结算方式
     */
    private String settleMethod;
    /**
     * 结算锁定
     */
    private Boolean settleLocked;
    /**
     * 供应商类型
     */
    private String supplierType;
    /**
     * 到货周期(天)
     */
    private Integer arrivalCycle;
    /**
     * 采购提前期(天)
     */
    private Integer purchaseLeadDays;
    /**
     * 运输方式
     */
    private String transportMethod;
    /**
     * 运费类型
     */
    private String freightType;
    /**
     * 五笔码
     */
    private String wubiCode;
    /**
     * 拼音码
     */
    private String pinyinCode;
    /**
     * 采购员
     */
    private String purchaser;
    /**
     * 公司性质
     */
    private String companyNature;
    /**
     * 淘汰
     */
    private Boolean obsolete;
    /**
     * 淘汰日期（当 obsolete=true 时使用）
     */
    private LocalDateTime obsoleteDate;
    /**
     * 开票类型
     */
    private String invoiceType;
    /**
     * 集团供应商
     */
    private Boolean groupSupplier;
    /**
     * 是否允许分店开单
     */
    private Boolean allowBranchOrder;
    /**
     * 物流公司
     */
    private String logisticsCompany;
    /**
     * 到货点
     */
    private String arrivalPoint;
    /**
     * 邮政编码
     */
    private String postalCode;
    /**
     * 会员编码(自动生成)
     */
    private String memberCode;
    /**
     * 地址
     */
    private String address;
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 区/县
     */
    private String district;
    /**
     * 网址
     */
    private String website;
    /**
     * 法定代表
     */
    private String legalPerson;
    /**
     * 统一信用代码
     */
    private String creditCode;
    /**
     * 采购管控
     */
    private String purchaseControl;
    /**
     * 浮动是否更新供应商最后进价
     */
    private String floatUpdateLastPrice;
    /**
     * 纳税人识别号
     */
    private String taxpayerId;
    /**
     * 开票银行
     */
    private String invoiceBank;
    /**
     * 开票银行账号
     */
    private String invoiceBankAccount;
    /**
     * 开票地址
     */
    private String invoiceAddress;
    /**
     * 开票电话
     */
    private String invoicePhone;
    /**
     * 开票单位
     */
    private String invoiceCompany;
    /**
     * 财务联系电话
     */
    private String financePhone;
    /**
     * 绩效考核利润参考依据
     */
    private String performanceProfitRef;

}