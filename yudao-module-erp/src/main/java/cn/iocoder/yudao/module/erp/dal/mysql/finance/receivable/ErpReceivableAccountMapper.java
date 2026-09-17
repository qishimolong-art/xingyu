package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpReceivableAccountMapper extends BaseMapperX<ErpReceivableAccountDO> {

    String SALE_OUT_ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION = ErpSaleOutMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION;

    @SelectProvider(type = SqlProvider.class, method = "selectList")
    List<ErpReceivableAccountDO> selectList(@Param("reqVO") ErpReceivableAccountPageReqVO reqVO,
                                            @Param("deptIds") Collection<Long> deptIds,
                                            @Param("selfUserId") String selfUserId,
                                            @Param("all") boolean all,
                                            @Param("docDeptIds") Collection<Long> docDeptIds,
                                            @Param("docSelfUserId") Long docSelfUserId,
                                            @Param("docAll") boolean docAll);

    default PageResult<ErpReceivableAccountDO> selectPage(ErpReceivableAccountPageReqVO reqVO) {
        return selectPage(reqVO, null, null, true, null, null, true);
    }

    default PageResult<ErpReceivableAccountDO> selectPage(ErpReceivableAccountPageReqVO reqVO,
                                                          Collection<Long> deptIds,
                                                          String selfUserId,
                                                          boolean all, Collection<Long> docDeptIds,
                                                          Long docSelfUserId, boolean docAll) {
        List<ErpReceivableAccountDO> list = selectList(reqVO, deptIds, selfUserId, all,
                docDeptIds, docSelfUserId, docAll);
        long total = list.size();
        int fromIndex = Math.max(0, (reqVO.getPageNo() - 1) * reqVO.getPageSize());
        int toIndex = Math.min(list.size(), fromIndex + reqVO.getPageSize());
        if (fromIndex >= list.size()) {
            return PageResult.empty((long) total);
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) total);
    }

    @Select({
            "<script>",
            "SELECT c.id AS customerId,",
            "       c.name AS customerName,",
            "       c.contact AS contact,",
            "       c.mobile AS mobile,",
            "       c.area_id AS areaId,",
            "       c.customer_type AS customerType,",
            "       c.sale_user_id AS saleUserId,",
            "       u.nickname AS saleUserName,",
            "       c.dept_id AS deptId,",
            "       d.name AS deptName,",
            "       c.route_id AS routeId,",
            "       IFNULL(ext.base_amount, 0) AS baseAmount,",
            "       c.credit_limit AS creditLimit,",
            "       c.credit_term_days AS creditTermDays,",
            "       IFNULL(so.saleOutAmount, 0) AS saleOutAmount,",
            "       IFNULL(sr.saleReturnAmount, 0) AS saleReturnAmount,",
            "       IFNULL(pa.priceAdjustAmount, 0) AS priceAdjustAmount,",
            "       IFNULL(rc.receiptAmount, 0) AS receiptAmount,",
            "       IFNULL(wo.writeOffAmount, 0) AS writeOffAmount,",
            "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
            "       IFNULL(rm.miscReceivableAmount, 0) AS miscReceivableAmount,",
            "       IFNULL(ext.advance_amount, 0) AS preAdvanceAmount,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) AS receivableAmount,",
            "       IFNULL(rc.receiptAmount, 0) AS receivedAmount,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) AS unreceivedAmount,",
            "       '应收账款' AS billType,",
            "       CASE WHEN IFNULL(rc.receiptAmount, 0) = 0 THEN '未收款'",
            "            WHEN IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) > 0 THEN '部分收款'",
            "            ELSE '已收款' END AS receiveStatus,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) AS receivableBalance,",
            "       CASE WHEN c.credit_limit IS NULL THEN NULL ELSE c.credit_limit - (IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0)) END AS creditBalance,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) + IFNULL(ext.advance_amount, 0) AS totalReceivable,",
            "       lastBiz.lastBizTime AS lastBizTime",
            "  FROM erp_customer c",
            "  LEFT JOIN system_users u ON u.id = c.sale_user_id",
            "  LEFT JOIN system_dept d ON d.id = c.dept_id",
            "  LEFT JOIN erp_customer_extend_info ext ON ext.customer_id = c.id",
            "  LEFT JOIN (SELECT t.customer_id, SUM(" + SALE_OUT_ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION + ") AS saleOutAmount, MAX(t.out_time) AS lastBizTime FROM erp_sale_out t WHERE t.deleted = 0 AND t.status = 20 AND t.customer_id = #{customerId} GROUP BY t.customer_id) so ON so.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(total_price) AS saleReturnAmount FROM erp_sale_return WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id) sr ON sr.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(total_adjust_price) AS priceAdjustAmount FROM erp_sale_price_adjust WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id) pa ON pa.customer_id = c.id",
            "  LEFT JOIN (SELECT fr.customer_id, SUM(CASE WHEN EXISTS (SELECT 1 FROM erp_receivable_other dro WHERE dro.deleted = 0 AND dro.status = 20 AND dro.source_type = '收款单优惠' AND dro.source_id = fr.id) THEN fr.receipt_price ELSE fr.total_price END) AS receiptAmount FROM erp_finance_receipt fr WHERE fr.deleted = 0 AND fr.status = 20 AND fr.customer_id = #{customerId} GROUP BY fr.customer_id) rc ON rc.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(write_off_amount) AS writeOffAmount FROM erp_receivable_writeoff WHERE deleted = 0 AND customer_id = #{customerId} GROUP BY customer_id) wo ON wo.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(receivable_amount) AS otherReceivableAmount FROM erp_receivable_other WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id) ro ON ro.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(amount) AS miscReceivableAmount FROM erp_receivable_misc WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id) rm ON rm.customer_id = c.id",
            "  LEFT JOIN (",
            "       SELECT customer_id, MAX(last_biz_time) AS lastBizTime",
            "         FROM (",
            "               SELECT customer_id, MAX(out_time) AS last_biz_time FROM erp_sale_out WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(return_time) AS last_biz_time FROM erp_sale_return WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(adjust_date) AS last_biz_time FROM erp_sale_price_adjust WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(receipt_time) AS last_biz_time FROM erp_finance_receipt WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(write_off_time) AS last_biz_time FROM erp_receivable_writeoff WHERE deleted = 0 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(biz_time) AS last_biz_time FROM erp_receivable_other WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(biz_time) AS last_biz_time FROM erp_receivable_misc WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} GROUP BY customer_id",
            "         ) t",
            "        GROUP BY customer_id",
            "  ) lastBiz ON lastBiz.customer_id = c.id",
            " WHERE c.deleted = 0 AND c.id = #{customerId}",
            "</script>"
    })
    ErpReceivableAccountDO selectByCustomerId(@Param("customerId") Long customerId);

    @Select({
            "<script>",
            "SELECT c.id AS customerId,",
            "       c.name AS customerName,",
            "       c.dept_id AS deptId,",
            "       IFNULL(so.saleOutAmount, 0) AS saleOutAmount,",
            "       IFNULL(sr.saleReturnAmount, 0) AS saleReturnAmount,",
            "       IFNULL(pa.priceAdjustAmount, 0) AS priceAdjustAmount,",
            "       IFNULL(rc.receiptAmount, 0) AS receiptAmount,",
            "       IFNULL(wo.writeOffAmount, 0) AS writeOffAmount,",
            "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
            "       IFNULL(rm.miscReceivableAmount, 0) AS miscReceivableAmount,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) AS receivableAmount,",
            "       IFNULL(rc.receiptAmount, 0) AS receivedAmount,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) AS unreceivedAmount,",
            "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0) AS receivableBalance,",
            "       lastBiz.lastBizTime AS lastBizTime",
            "  FROM erp_customer c",
            "  LEFT JOIN (SELECT t.customer_id, SUM(" + SALE_OUT_ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION + ") AS saleOutAmount, MAX(t.out_time) AS lastBizTime FROM erp_sale_out t WHERE t.deleted = 0 AND t.status = 20 AND t.customer_id = #{customerId} AND t.dept_id = #{deptId} GROUP BY t.customer_id) so ON so.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(total_price) AS saleReturnAmount FROM erp_sale_return WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id) sr ON sr.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(total_adjust_price) AS priceAdjustAmount FROM erp_sale_price_adjust WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id) pa ON pa.customer_id = c.id",
            "  LEFT JOIN (SELECT fr.customer_id, SUM(CASE WHEN EXISTS (SELECT 1 FROM erp_receivable_other dro WHERE dro.deleted = 0 AND dro.status = 20 AND dro.source_type = '收款单优惠' AND dro.source_id = fr.id) THEN fr.receipt_price ELSE fr.total_price END) AS receiptAmount FROM erp_finance_receipt fr WHERE fr.deleted = 0 AND fr.status = 20 AND fr.customer_id = #{customerId} AND fr.dept_id = #{deptId} GROUP BY fr.customer_id) rc ON rc.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(write_off_amount) AS writeOffAmount FROM erp_receivable_writeoff WHERE deleted = 0 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id) wo ON wo.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(receivable_amount) AS otherReceivableAmount FROM erp_receivable_other WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id) ro ON ro.customer_id = c.id",
            "  LEFT JOIN (SELECT customer_id, SUM(amount) AS miscReceivableAmount FROM erp_receivable_misc WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id) rm ON rm.customer_id = c.id",
            "  LEFT JOIN (",
            "       SELECT customer_id, MAX(last_biz_time) AS lastBizTime",
            "         FROM (",
            "               SELECT customer_id, MAX(out_time) AS last_biz_time FROM erp_sale_out WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(return_time) AS last_biz_time FROM erp_sale_return WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(adjust_date) AS last_biz_time FROM erp_sale_price_adjust WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(receipt_time) AS last_biz_time FROM erp_finance_receipt WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(write_off_time) AS last_biz_time FROM erp_receivable_writeoff WHERE deleted = 0 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(biz_time) AS last_biz_time FROM erp_receivable_other WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "               UNION ALL",
            "               SELECT customer_id, MAX(biz_time) AS last_biz_time FROM erp_receivable_misc WHERE deleted = 0 AND status = 20 AND customer_id = #{customerId} AND dept_id = #{deptId} GROUP BY customer_id",
            "         ) t",
            "        GROUP BY customer_id",
            "  ) lastBiz ON lastBiz.customer_id = c.id",
            " WHERE c.deleted = 0 AND c.id = #{customerId}",
            "</script>"
    })
    ErpReceivableAccountDO selectByCustomerIdAndDeptId(@Param("customerId") Long customerId,
                                                       @Param("deptId") Long deptId);

    class SqlProvider {

        private static final String BALANCE_EXPR = "IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) "
                + "+ IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) "
                + "- IFNULL(sr.saleReturnAmount, 0) - IFNULL(rc.receiptAmount, 0)";

        public String selectList() {
            return String.join("\n",
                    "<script>",
                    "SELECT CONCAT(c.id, ':', IFNULL(CAST(accountKeys.deptId AS CHAR), 'none')) AS accountKey,",
                    "       CASE WHEN accountKeys.deptId IS NULL THEN 'none' ELSE CAST(accountKeys.deptId AS CHAR) END AS deptKey,",
                    "       c.id AS customerId,",
                    "       c.name AS customerName,",
                    "       c.contact AS contact,",
                    "       c.mobile AS mobile,",
                    "       c.area_id AS areaId,",
                    "       c.customer_type AS customerType,",
                    "       c.sale_user_id AS saleUserId,",
                    "       u.nickname AS saleUserName,",
                    "       accountKeys.deptId AS deptId,",
                    "       IFNULL(d.name, '未归属部门') AS deptName,",
                    "       c.route_id AS routeId,",
                    "       IFNULL(ext.base_amount, 0) AS baseAmount,",
                    "       COALESCE(cdc.credit_limit, c.credit_limit) AS creditLimit,",
                    "       COALESCE(cdc.credit_term_days, c.credit_term_days) AS creditTermDays,",
                    "       IFNULL(so.saleOutAmount, 0) AS saleOutAmount,",
                    "       IFNULL(sr.saleReturnAmount, 0) AS saleReturnAmount,",
                    "       IFNULL(pa.priceAdjustAmount, 0) AS priceAdjustAmount,",
                    "       IFNULL(rc.receiptAmount, 0) AS receiptAmount,",
                    "       IFNULL(wo.writeOffAmount, 0) AS writeOffAmount,",
                    "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
                    "       IFNULL(rm.miscReceivableAmount, 0) AS miscReceivableAmount,",
                    "       IFNULL(ext.advance_amount, 0) AS preAdvanceAmount,",
                    "       IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(ro.otherReceivableAmount, 0) + IFNULL(rm.miscReceivableAmount, 0) - IFNULL(sr.saleReturnAmount, 0) AS receivableAmount,",
                    "       IFNULL(rc.receiptAmount, 0) AS receivedAmount,",
                    "       " + BALANCE_EXPR + " AS unreceivedAmount,",
                    "       '应收账款' AS billType,",
                    "       CASE WHEN IFNULL(rc.receiptAmount, 0) = 0 THEN '未收款'",
                    "            WHEN " + BALANCE_EXPR + " > 0 THEN '部分收款'",
                    "            ELSE '已收款' END AS receiveStatus,",
                    "       " + BALANCE_EXPR + " AS receivableBalance,",
                    "       CASE WHEN COALESCE(cdc.credit_limit, c.credit_limit) IS NULL THEN NULL ELSE COALESCE(cdc.credit_limit, c.credit_limit) - (" + BALANCE_EXPR + ") END AS creditBalance,",
                    "       " + BALANCE_EXPR + " + IFNULL(ext.advance_amount, 0) AS totalReceivable,",
                    "       lastBiz.lastBizTime AS lastBizTime",
                    "  FROM erp_customer c",
                    "  INNER JOIN (",
                    sourceKeySql("erp_sale_out", "so_key", "out_time", "sale_user_id", true),
                    "       UNION",
                    sourceKeySql("erp_sale_return", "sr_key", "return_time", "sale_user_id", true),
                    "       UNION",
                    sourceKeySql("erp_sale_price_adjust", "pa_key", "adjust_date", "adjust_user_id", true),
                    "       UNION",
                    sourceKeySql("erp_finance_receipt", "rc_key", "receipt_time", "finance_user_id", true),
                    "       UNION",
                    sourceKeySql("erp_receivable_writeoff", "wo_key", "write_off_time", "operator_user_id", false),
                    "       UNION",
                    sourceKeySql("erp_receivable_other", "ro_key", "biz_time", "handler_id", true),
                    "       UNION",
                    sourceKeySql("erp_receivable_misc", "rm_key", "biz_time", "handler_id", true),
                    "  ) accountKeys ON accountKeys.customer_id = c.id",
                    "  LEFT JOIN system_users u ON u.id = c.sale_user_id",
                    "  LEFT JOIN system_dept d ON d.id = accountKeys.deptId",
                    "  LEFT JOIN erp_customer_extend_info ext ON ext.customer_id = c.id",
                    "  LEFT JOIN erp_customer_dept_credit cdc ON cdc.customer_id = c.id AND cdc.dept_id = accountKeys.deptId AND cdc.deleted = b'0' AND cdc.tenant_id = c.tenant_id",
                    saleOutAmountJoinSql(),
                    amountJoinSql("erp_sale_return", "sr", "total_price", "saleReturnAmount", "return_time", "sale_user_id", true),
                    amountJoinSql("erp_sale_price_adjust", "pa", "total_adjust_price", "priceAdjustAmount", "adjust_date", "adjust_user_id", true),
                    receiptJoinSql(),
                    amountJoinSql("erp_receivable_writeoff", "wo", "write_off_amount", "writeOffAmount", "write_off_time", "operator_user_id", false),
                    amountJoinSql("erp_receivable_other", "ro", "receivable_amount", "otherReceivableAmount", "biz_time", "handler_id", true),
                    amountJoinSql("erp_receivable_misc", "rm", "amount", "miscReceivableAmount", "biz_time", "handler_id", true),
                    lastBizJoinSql(),
                    " WHERE c.deleted = 0",
                    "   <if test='reqVO.showZeroBalance == null or !reqVO.showZeroBalance'>",
                    "   AND (" + BALANCE_EXPR + ") &lt;&gt; 0",
                    "   </if>",
                    "   <if test='reqVO.customerId != null'> AND c.id = #{reqVO.customerId} </if>",
                    "   <if test='reqVO.customerName != null and reqVO.customerName != \"\"'> AND c.name LIKE CONCAT('%', #{reqVO.customerName}, '%') </if>",
                    "   <if test='reqVO.customerType != null'> AND c.customer_type = #{reqVO.customerType} </if>",
                    "   <if test='reqVO.saleUserId != null'> AND c.sale_user_id = #{reqVO.saleUserId} </if>",
                    customerScopeSql(),
                    "   <if test='reqVO.keyword != null and reqVO.keyword != \"\"'>",
                    "   AND (c.code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.short_name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.contact LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.mobile LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.telephone LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.pinyin_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.wubi_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.member_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR c.platform_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR u.nickname LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR d.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
                    "        OR DATE_FORMAT(c.create_time, '%Y-%m-%d %H:%i:%s') LIKE CONCAT('%', #{reqVO.keyword}, '%'))",
                    "   </if>",
                    orderBySql(),
                    "</script>");
        }

        private String sourceKeySql(String tableName, String alias, String timeColumn, String userColumn,
                                    boolean approvedOnly) {
            return "       SELECT " + alias + ".customer_id, " + alias + ".dept_id AS deptId"
                    + " FROM " + tableName + " " + alias
                    + " WHERE " + alias + ".deleted = 0"
                    + (approvedOnly ? " AND " + alias + ".status = 20" : "")
                    + documentScopeSql(alias, userColumn)
                    + timeSql(alias, timeColumn)
                    + requestedDeptSql(alias)
                    + " GROUP BY " + alias + ".customer_id, " + alias + ".dept_id";
        }

        private String amountJoinSql(String tableName, String alias, String amountColumn, String amountAlias,
                                     String timeColumn, String userColumn, boolean approvedOnly) {
            return String.join("\n",
                    "  LEFT JOIN (",
                    "       SELECT " + alias + ".customer_id, " + alias + ".dept_id AS deptId,",
                    "              SUM(" + alias + "." + amountColumn + ") AS " + amountAlias,
                    "         FROM " + tableName + " " + alias,
                    "        WHERE " + alias + ".deleted = 0" + (approvedOnly ? " AND " + alias + ".status = 20" : ""),
                    documentScopeSql(alias, userColumn),
                    timeSql(alias, timeColumn),
                    requestedDeptSql(alias),
                    "        GROUP BY " + alias + ".customer_id, " + alias + ".dept_id",
                    "  ) " + alias + " ON " + alias + ".customer_id = c.id AND " + sameDeptSql(alias));
        }

        private String saleOutAmountJoinSql() {
            return String.join("\n",
                    "  LEFT JOIN (",
                    "       SELECT t.customer_id, t.dept_id AS deptId,",
                    "              SUM(" + SALE_OUT_ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION + ") AS saleOutAmount",
                    "         FROM erp_sale_out t",
                    "        WHERE t.deleted = 0 AND t.status = 20",
                    documentScopeSql("t", "sale_user_id"),
                    timeSql("t", "out_time"),
                    requestedDeptSql("t"),
                    "        GROUP BY t.customer_id, t.dept_id",
                    "  ) so ON so.customer_id = c.id AND " + sameDeptSql("so"));
        }

        private String receiptJoinSql() {
            return String.join("\n",
                    "  LEFT JOIN (",
                    "       SELECT rc.customer_id, rc.dept_id AS deptId,",
                    "              SUM(CASE WHEN EXISTS (",
                    "                    SELECT 1 FROM erp_receivable_other dro",
                    "                     WHERE dro.deleted = 0 AND dro.status = 20",
                    "                       AND dro.source_type = '收款单优惠'",
                    "                       AND dro.source_id = rc.id",
                    "                  ) THEN rc.receipt_price ELSE rc.total_price END) AS receiptAmount",
                    "         FROM erp_finance_receipt rc",
                    "        WHERE rc.deleted = 0 AND rc.status = 20",
                    documentScopeSql("rc", "finance_user_id"),
                    timeSql("rc", "receipt_time"),
                    requestedDeptSql("rc"),
                    "        GROUP BY rc.customer_id, rc.dept_id",
                    "  ) rc ON rc.customer_id = c.id AND " + sameDeptSql("rc"));
        }

        private String lastBizJoinSql() {
            return String.join("\n",
                    "  LEFT JOIN (",
                    "       SELECT customer_id, deptId, MAX(last_biz_time) AS lastBizTime",
                    "         FROM (",
                    lastBizSourceSql("erp_sale_out", "so_last", "out_time", "sale_user_id", true),
                    "               UNION ALL",
                    lastBizSourceSql("erp_sale_return", "sr_last", "return_time", "sale_user_id", true),
                    "               UNION ALL",
                    lastBizSourceSql("erp_sale_price_adjust", "pa_last", "adjust_date", "adjust_user_id", true),
                    "               UNION ALL",
                    lastBizSourceSql("erp_finance_receipt", "rc_last", "receipt_time", "finance_user_id", true),
                    "               UNION ALL",
                    lastBizSourceSql("erp_receivable_writeoff", "wo_last", "write_off_time", "operator_user_id", false),
                    "               UNION ALL",
                    lastBizSourceSql("erp_receivable_other", "ro_last", "biz_time", "handler_id", true),
                    "               UNION ALL",
                    lastBizSourceSql("erp_receivable_misc", "rm_last", "biz_time", "handler_id", true),
                    "         ) t",
                    "        GROUP BY customer_id, deptId",
                    "  ) lastBiz ON lastBiz.customer_id = c.id AND "
                            + "(lastBiz.deptId = accountKeys.deptId OR (lastBiz.deptId IS NULL AND accountKeys.deptId IS NULL))");
        }

        private String lastBizSourceSql(String tableName, String alias, String timeColumn, String userColumn,
                                        boolean approvedOnly) {
            return "               SELECT " + alias + ".customer_id, " + alias + ".dept_id AS deptId, "
                    + "MAX(" + alias + "." + timeColumn + ") AS last_biz_time"
                    + " FROM " + tableName + " " + alias
                    + " WHERE " + alias + ".deleted = 0"
                    + (approvedOnly ? " AND " + alias + ".status = 20" : "")
                    + documentScopeSql(alias, userColumn)
                    + timeSql(alias, timeColumn)
                    + requestedDeptSql(alias)
                    + " GROUP BY " + alias + ".customer_id, " + alias + ".dept_id";
        }

        private String documentScopeSql(String alias, String userColumn) {
            return " <if test='!docAll'><choose><when test='docDeptIds != null and docDeptIds.size() > 0'>"
                    + " AND (" + alias + ".dept_id IN "
                    + "<foreach collection='docDeptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
                    + "<if test='docSelfUserId != null'> OR " + alias + "." + userColumn + " = #{docSelfUserId}</if>)"
                    + "</when><when test='docSelfUserId != null'> AND " + alias + "." + userColumn
                    + " = #{docSelfUserId}</when><otherwise> AND 1 = 0</otherwise></choose></if>";
        }

        private String timeSql(String alias, String timeColumn) {
            return " <if test='reqVO.startTime != null and reqVO.endTime != null'> AND " + alias + "." + timeColumn
                    + " BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}</if>";
        }

        private String requestedDeptSql(String alias) {
            return " <if test='reqVO.deptId != null'> AND " + alias + ".dept_id = #{reqVO.deptId}</if>";
        }

        private String sameDeptSql(String alias) {
            return "(" + alias + ".deptId = accountKeys.deptId OR ("
                    + alias + ".deptId IS NULL AND accountKeys.deptId IS NULL))";
        }

        private String customerScopeSql() {
            return String.join("\n",
                    "   <if test='!all'>",
                    "   AND (",
                    "        <choose>",
                    "        <when test='deptIds != null and deptIds.size() > 0'>",
                    "        (c.dept_id IN",
                    "         <foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>#{deptId}</foreach>",
                    "         OR (c.allow_multi_dept = true",
                    "             AND EXISTS (SELECT 1 FROM erp_customer_dept ecd_scope",
                    "                          WHERE ecd_scope.customer_id = c.id",
                    "                            AND ecd_scope.deleted = b'0'",
                    "                            AND ecd_scope.tenant_id = c.tenant_id",
                    "                            AND ecd_scope.dept_id IN",
                    "                            <foreach collection='deptIds' item='scopeDeptId' open='(' separator=',' close=')'>#{scopeDeptId}</foreach>)))",
                    "        </when>",
                    "        <otherwise>1 = 0</otherwise>",
                    "        </choose>",
                    "        <if test='selfUserId != null'> OR c.creator = #{selfUserId} </if>",
                    "   )",
                    "   </if>");
        }

        private String orderBySql() {
            return String.join("\n",
                    " <choose>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"customerName\"'>ORDER BY customerName ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"customerName\"'>ORDER BY customerName DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"contact\"'>ORDER BY contact ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"contact\"'>ORDER BY contact DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"mobile\"'>ORDER BY mobile ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"mobile\"'>ORDER BY mobile DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"areaId\"'>ORDER BY c.area_id ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"areaId\"'>ORDER BY c.area_id DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"customerType\"'>ORDER BY c.customer_type ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"customerType\"'>ORDER BY c.customer_type DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"deptName\"'>ORDER BY deptName ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"deptName\"'>ORDER BY deptName DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"saleUserName\"'>ORDER BY saleUserName ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"saleUserName\"'>ORDER BY saleUserName DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"routeId\"'>ORDER BY c.route_id ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"routeId\"'>ORDER BY c.route_id DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"baseAmount\"'>ORDER BY baseAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"baseAmount\"'>ORDER BY baseAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"creditLimit\"'>ORDER BY creditLimit ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"creditLimit\"'>ORDER BY creditLimit DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"creditBalance\"'>ORDER BY creditBalance ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"creditBalance\"'>ORDER BY creditBalance DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"creditTermDays\"'>ORDER BY creditTermDays ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"creditTermDays\"'>ORDER BY creditTermDays DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"billType\"'>ORDER BY billType ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"billType\"'>ORDER BY billType DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"receivableAmount\"'>ORDER BY receivableAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"receivableAmount\"'>ORDER BY receivableAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"receivableBalance\"'>ORDER BY receivableBalance ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"receivableBalance\"'>ORDER BY receivableBalance DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"receivedAmount\"'>ORDER BY receivedAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"receivedAmount\"'>ORDER BY receivedAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"unreceivedAmount\"'>ORDER BY unreceivedAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"unreceivedAmount\"'>ORDER BY unreceivedAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"receiveStatus\"'>ORDER BY receiveStatus ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"receiveStatus\"'>ORDER BY receiveStatus DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"saleOutAmount\"'>ORDER BY saleOutAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"saleOutAmount\"'>ORDER BY saleOutAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"saleReturnAmount\"'>ORDER BY saleReturnAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"saleReturnAmount\"'>ORDER BY saleReturnAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"priceAdjustAmount\"'>ORDER BY priceAdjustAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"priceAdjustAmount\"'>ORDER BY priceAdjustAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"receiptAmount\"'>ORDER BY receiptAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"receiptAmount\"'>ORDER BY receiptAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"writeOffAmount\"'>ORDER BY writeOffAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"writeOffAmount\"'>ORDER BY writeOffAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"otherReceivableAmount\"'>ORDER BY otherReceivableAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"otherReceivableAmount\"'>ORDER BY otherReceivableAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"preAdvanceAmount\"'>ORDER BY preAdvanceAmount ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"preAdvanceAmount\"'>ORDER BY preAdvanceAmount DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime ASC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime DESC, c.id DESC, accountKeys.deptId DESC</when>",
                    "   <otherwise>ORDER BY receivableBalance DESC, c.id DESC, accountKeys.deptId DESC</otherwise>",
                    " </choose>");
        }

    }

}
