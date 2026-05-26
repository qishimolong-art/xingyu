package cn.iocoder.yudao.module.erp.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.module.erp.dal.redis.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;


/**
 * Erp 订单序号的 Redis DAO
 *
 * @author HUIHUI
 */
@Repository
public class ErpNoRedisDAO {

    /**
     * 其它入库 {@link cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO}
     */
    public static final String STOCK_IN_NO_PREFIX = "QTRK";
    /**
     * 其它出库 {@link cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO}
     */
    public static final String STOCK_OUT_NO_PREFIX = "QCKD";

    /**
     * 库存调拨 {@link cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO}
     */
    public static final String STOCK_MOVE_NO_PREFIX = "QCDB";

    /**
     * 库存盘点 {@link cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO}
     */
    public static final String STOCK_CHECK_NO_PREFIX = "QCPD";

    /**
     * 销售订单 {@link cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO}
     */
    public static final String SALE_ORDER_NO_PREFIX = "XSDD";
    /**
     * 销售报价订单 {@link cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO}
     */
    public static final String SALE_QUOTE_NO_PREFIX = "XSBJ";
    /**
     * 销售手推车 {@link cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO}
     */
    public static final String SALE_CART_NO_PREFIX = "XSST";
    /**
     * 销售出库 {@link cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO}
     */
    public static final String SALE_OUT_NO_PREFIX = "XSCK";
    /**
     * 销售退货 {@link cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO}
     */
    public static final String SALE_RETURN_NO_PREFIX = "TH";

    /**
     * 采购订单 {@link cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO}
     */
    public static final String PURCHASE_ORDER_NO_PREFIX = "CGDD";
    /**
     * 采购入库 {@link cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO}
     */
    public static final String PURCHASE_IN_NO_PREFIX = "CGRK";
    /**
     * 采购退货 {@link cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO}
     */
    public static final String PURCHASE_RETURN_NO_PREFIX = "CGTH";

    /**
     * 付款单 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO}
     */
    public static final String FINANCE_PAYMENT_NO_PREFIX = "FKD";
    /**
     * 收款单 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO}
     */
    public static final String FINANCE_RECEIPT_NO_PREFIX = "SKD";

    /**
     * 采购调价单
     */
    public static final String PURCHASE_PRICE_ADJUST_NO_PREFIX = "CGTJ";
    /**
     * 销售调价单
     */
    public static final String SALE_PRICE_ADJUST_NO_PREFIX = "XSTJ";
    /**
     * 连锁开单
     */
    public static final String CHAIN_ORDER_NO_PREFIX = "LSKD";
    /**
     * 采购建议单
     */
    public static final String PURCHASE_SUGGESTION_NO_PREFIX = "CGJY";
    /**
     * 其他应收单
     */
    public static final String OTHER_RECEIVABLE_NO_PREFIX = "QTYS";
    /**
     * 预收账款单
     */
    public static final String PRE_RECEIVABLE_NO_PREFIX = "YSZK";
    /**
     * 系统开账编号 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO}
     */
    public static final String BOOK_OPEN_NO_PREFIX = "KZ";
    /**
     * 凭证字（默认"记"），与 {@link #generateMonthly(String)} 配合生成"记-202605-000001"格式
     */
    public static final String VOUCHER_WORD_DEFAULT = "记";

    /**
     * 配件编码 {@link cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO} 的前缀（纯流水）
     */
    public static final String PRODUCT_CODE_PREFIX = "P";
    /**
     * 客户编码
     */
    public static final String CUSTOMER_NO_PREFIX = "C";
    /**
     * 会员编码
     */
    public static final String MEMBER_NO_PREFIX = "M";
    /**
     * 平台唯一码
     */
    public static final String PLATFORM_NO_PREFIX = "P";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，使用当前日期，格式为 {PREFIX} + yyyyMMdd + 6 位自增
     * 例如说：QTRK 202109 000001 （没有中间空格）
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generate(String prefix) {
        // 递增序号
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

    /**
     * 生成纯流水号，格式 {PREFIX} + 6 位自增（跨日期持续累加，不自动过期）。
     *
     * 用于配件编码 {@link cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO#getCode()}。
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generatePlain(String prefix) {
        String key = RedisKeyConstants.NO + prefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        if (no == null) {
            no = 1L;
        }
        return prefix + String.format("%06d", no);
    }

    /**
     * 生成月度自增序号，格式 {PREFIX} + "-" + yyyyMM + "-" + 6 位月度自增。
     *
     * 例如：generateMonthly("记") → "记-202605-000001"。
     *
     * 用于会计凭证编号 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO#getVoucherNo()}。
     * 月度重置：每月第一张凭证从 000001 开始；过期时间 35 天，自然跨月失效。
     *
     * @param prefix 前缀（凭证字，如"记"）
     * @return 序号
     */
    public String generateMonthly(String prefix) {
        return generateMonthly(prefix, YearMonth.now());
    }

    /**
     * 生成月度自增序号（指定业务月份），格式 {PREFIX} + "-" + yyyyMM + "-" + 6 位月度自增。
     *
     * S3 修复：调用方传入业务月份而非系统当前月份，避免跨月凭证号穿越。
     *
     * @param prefix    前缀（凭证字，如"记"）
     * @param yearMonth 业务月份
     * @return 序号
     */
    public String generateMonthly(String prefix, YearMonth yearMonth) {
        String ym = yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisKeyConstants.NO + prefix + "-" + ym;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofDays(35L));
        return prefix + "-" + ym + "-" + String.format("%06d", no);
    }

}
