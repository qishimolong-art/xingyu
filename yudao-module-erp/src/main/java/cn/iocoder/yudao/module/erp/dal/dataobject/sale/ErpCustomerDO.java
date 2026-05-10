package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 客户 DO
 *
 * @author 芋道源码
 */
@TableName("erp_customer")
@KeySequence("erp_customer_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerDO extends BaseDO {

    /**
     * 客户编号
     */
    @TableId
    private Long id;
    /**
     * 客户编码
     */
    private String code;
    /**
     * 旧编码
     */
    private String oldCode;
    /**
     * 客户名称
     */
    private String name;
    /**
     * 简称
     */
    private String shortName;
    /**
     * 外文名
     */
    private String foreignName;
    /**
     * 往来类别
     */
    private Integer relationType;
    /**
     * 区域编号
     */
    private Long areaId;
    /**
     * 客户类型
     */
    private Integer customerType;
    /**
     * 公司性质
     */
    private String companyNature;
    /**
     * 是否集团客户
     */
    private Boolean groupCustomer;
    /**
     * 企业匹配状态
     */
    private Integer enterpriseMatchStatus;
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
     * 财务联系电话
     */
    private String financeTelephone;
    /**
     * 地址区域编号
     */
    private Long addressAreaId;
    /**
     * 地址
     */
    private String address;
    /**
     * 详细地址
     */
    private String detailAddress;
    /**
     * 邮政编码
     */
    private String postCode;
    /**
     * 发货区
     */
    private Integer deliveryArea;
    /**
     * 到货点
     */
    private String arrivalPoint;
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
     * 账户
     */
    private String accountName;
    /**
     * 结算方式
     */
    private Integer settleMethod;
    /**
     * 结算锁定
     */
    private Boolean settleLocked;
    /**
     * 开票类型
     */
    private Integer invoiceType;
    /**
     * 起订金额
     */
    private BigDecimal minOrderAmount;
    /**
     * 运费类型
     */
    private Integer freightType;
    /**
     * 佣金比例
     */
    private BigDecimal commissionRate;
    /**
     * 启用佣金机制
     */
    private Boolean commissionEnabled;
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
    /**
     * 统一信用代码
     */
    private String unifiedCreditCode;
    /**
     * 开票银行
     */
    private String invoiceBankName;
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
    private String invoiceTelephone;
    /**
     * 开票单位
     */
    private String invoiceCompany;
    /**
     * 所属业务员
     */
    private Long saleUserId;
    /**
     * 所属开发员
     */
    private Long developerUserId;
    /**
     * 所属部门
     */
    private Long deptId;
    /**
     * 价格级别
     */
    private Integer priceLevel;
    /**
     * 价格锁定
     */
    private Boolean priceLocked;
    /**
     * 绩效考核利润参考依据
     */
    private Integer profitReferencePriceLevel;
    /**
     * 电商支付
     */
    private Integer ecommercePayment;
    /**
     * 是否物流
     */
    private Boolean logistics;
    /**
     * 运输方式
     */
    private Integer transportMethod;
    /**
     * 线路编号
     */
    private Long routeId;
    /**
     * 运费说明编号
     */
    private Long freightExplainId;
    /**
     * 五笔码
     */
    private String wubiCode;
    /**
     * 拼音码
     */
    private String pinyinCode;
    /**
     * 会员编码
     */
    private String memberCode;
    /**
     * 平台唯一码
     */
    private String platformCode;

}
