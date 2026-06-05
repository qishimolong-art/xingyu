package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErpPayableAccountMapper extends BaseMapperX<ErpPayableAccountDO> {

    @Select({
            "<script>",
            "SELECT s.id AS supplierId,",
            "       s.name AS supplierName,",
            "       s.contact AS contact,",
            "       s.mobile AS mobile,",
            "       COALESCE(base.deptId, pa.deptId) AS deptId,",
            "       d.name AS deptName,",
            "       base.handlerId AS handlerId,",
            "       u.nickname AS handlerName,",
            "       IFNULL(pi.purchaseInAmount, 0) AS purchaseInAmount,",
            "       IFNULL(pr.purchaseReturnAmount, 0) AS purchaseReturnAmount,",
            "       IFNULL(pa.priceAdjustAmount, 0) AS priceAdjustAmount,",
            "       IFNULL(po.otherPayableAmount, 0) AS otherPayableAmount,",
            "       IFNULL(fp.paymentAmount, 0) AS paymentAmount,",
            "       IFNULL(pi.purchaseInAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(po.otherPayableAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0) AS balance,",
            "       IFNULL(pp.unclearedPrepayment, 0) AS unclearedPrepayment,",
            "       lb.lastBizTime AS lastBizTime",
            "  FROM erp_supplier s",
            "  LEFT JOIN (",
            "       SELECT supplier_id AS supplierId,",
            "              MAX(dept_id) AS deptId,",
            "              MAX(handler) AS handlerId",
            "         FROM erp_purchase_in",
            "        WHERE deleted = 0",
            "        GROUP BY supplier_id",
            "  ) base ON base.supplierId = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id,",
            "              SUM(total_adjust_price) AS priceAdjustAmount,",
            "              MAX(dept_id) AS deptId",
            "         FROM erp_purchase_price_adjust",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND adjust_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) pa ON pa.supplier_id = s.id",
            "  LEFT JOIN system_dept d ON d.id = COALESCE(base.deptId, pa.deptId)",
            "  LEFT JOIN system_users u ON u.id = base.handlerId",
            "  LEFT JOIN (",
            "       SELECT supplier_id, SUM(total_price) AS purchaseInAmount",
            "         FROM erp_purchase_in",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND in_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) pi ON pi.supplier_id = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id, SUM(total_price) AS purchaseReturnAmount",
            "         FROM erp_purchase_return",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND return_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) pr ON pr.supplier_id = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id, SUM(payment_price) AS paymentAmount",
            "         FROM erp_finance_payment",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND payment_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) fp ON fp.supplier_id = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id, SUM(payable_amount) AS otherPayableAmount",
            "         FROM erp_payable_other",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND biz_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) po ON po.supplier_id = s.id",
            "  LEFT JOIN (",
            "       SELECT party_id, SUM(actual_amount) AS unclearedPrepayment",
            "         FROM erp_pre_payment",
            "        WHERE deleted = 0 AND status = 20 AND party_type = 2",
            "        GROUP BY party_id",
            "  ) pp ON pp.party_id = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id, MAX(last_biz_time) AS lastBizTime",
            "         FROM (",
            "               SELECT supplier_id, MAX(in_time) AS last_biz_time FROM erp_purchase_in WHERE deleted = 0 AND status = 20 GROUP BY supplier_id",
            "               UNION ALL",
            "               SELECT supplier_id, MAX(return_time) AS last_biz_time FROM erp_purchase_return WHERE deleted = 0 AND status = 20 GROUP BY supplier_id",
            "               UNION ALL",
            "               SELECT supplier_id, MAX(adjust_time) AS last_biz_time FROM erp_purchase_price_adjust WHERE deleted = 0 AND status = 20 GROUP BY supplier_id",
            "               UNION ALL",
            "               SELECT supplier_id, MAX(payment_time) AS last_biz_time FROM erp_finance_payment WHERE deleted = 0 AND status = 20 GROUP BY supplier_id",
            "               UNION ALL",
            "               SELECT supplier_id, MAX(biz_time) AS last_biz_time FROM erp_payable_other WHERE deleted = 0 AND status = 20 GROUP BY supplier_id",
            "         ) t",
            "        GROUP BY supplier_id",
            "  ) lb ON lb.supplier_id = s.id",
            " WHERE s.deleted = 0",
            "   <if test='reqVO.supplierId != null'> AND s.id = #{reqVO.supplierId} </if>",
            "   <if test='reqVO.supplierName != null and reqVO.supplierName != \"\"'> AND s.name LIKE CONCAT('%', #{reqVO.supplierName}, '%') </if>",
            "   <if test='reqVO.deptId != null'> AND COALESCE(base.deptId, pa.deptId) = #{reqVO.deptId} </if>",
            "   <if test='reqVO.handlerId != null'> AND base.handlerId = #{reqVO.handlerId} </if>",
            "   <if test='reqVO.showZeroBalance == null or !reqVO.showZeroBalance'>",
            "   AND (IFNULL(pi.purchaseInAmount, 0) + IFNULL(pa.priceAdjustAmount, 0) + IFNULL(po.otherPayableAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0)) &lt;&gt; 0",
            "   </if>",
            " ORDER BY balance DESC, s.id DESC",
            "</script>"
    })
    List<ErpPayableAccountDO> selectList(@Param("reqVO") ErpPayableAccountPageReqVO reqVO);

    default PageResult<ErpPayableAccountDO> selectPage(ErpPayableAccountPageReqVO reqVO) {
        List<ErpPayableAccountDO> list = selectList(reqVO);
        long total = list.size();
        int fromIndex = Math.max(0, (reqVO.getPageNo() - 1) * reqVO.getPageSize());
        int toIndex = Math.min(list.size(), fromIndex + reqVO.getPageSize());
        if (fromIndex >= list.size()) {
            return PageResult.empty(total);
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), total);
    }

    @Select({
            "<script>",
            "SELECT docType, docDate, docNo, increaseAmount, paymentAmount",
            "  FROM (",
            "       SELECT '采购入库' AS docType, in_time AS docDate, no AS docNo, total_price AS increaseAmount, 0 AS paymentAmount",
            "         FROM erp_purchase_in",
            "        WHERE deleted = 0 AND status = 20 AND supplier_id = #{reqVO.supplierId}",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND in_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "       UNION ALL",
            "       SELECT '采购退货' AS docType, return_time AS docDate, no AS docNo, 0 AS increaseAmount, total_price AS paymentAmount",
            "         FROM erp_purchase_return",
            "        WHERE deleted = 0 AND status = 20 AND supplier_id = #{reqVO.supplierId}",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND return_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "       UNION ALL",
            "       SELECT '采购调价' AS docType, adjust_time AS docDate, no AS docNo,",
            "              CASE WHEN total_adjust_price &gt;= 0 THEN total_adjust_price ELSE 0 END AS increaseAmount,",
            "              CASE WHEN total_adjust_price &lt; 0 THEN ABS(total_adjust_price) ELSE 0 END AS paymentAmount",
            "         FROM erp_purchase_price_adjust",
            "        WHERE deleted = 0 AND status = 20 AND supplier_id = #{reqVO.supplierId}",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND adjust_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "       UNION ALL",
            "       SELECT '其他应付' AS docType, biz_time AS docDate, no AS docNo, payable_amount AS increaseAmount, 0 AS paymentAmount",
            "         FROM erp_payable_other",
            "        WHERE deleted = 0 AND status = 20 AND supplier_id = #{reqVO.supplierId}",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND biz_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "       UNION ALL",
            "       SELECT '付款单' AS docType, payment_time AS docDate, no AS docNo, 0 AS increaseAmount, payment_price AS paymentAmount",
            "         FROM erp_finance_payment",
            "        WHERE deleted = 0 AND status = 20 AND supplier_id = #{reqVO.supplierId}",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND payment_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "  ) t",
            " ORDER BY docDate ASC, docNo ASC",
            "</script>"
    })
    List<ErpPayableDetailDO> selectDetailList(@Param("reqVO") ErpPayableDetailReqVO reqVO);
}
