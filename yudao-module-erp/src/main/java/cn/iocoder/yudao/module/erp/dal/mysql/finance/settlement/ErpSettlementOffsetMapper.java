package cn.iocoder.yudao.module.erp.dal.mysql.finance.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErpSettlementOffsetMapper extends BaseMapperX<ErpSettlementOffsetDO> {

    @Select({
            "<script>",
            "SELECT c.name AS subjectName,",
            "       c.id AS customerId,",
            "       s.id AS supplierId,",
            "       COALESCE(NULLIF(c.contact, ''), s.contact) AS contact,",
            "       COALESCE(NULLIF(c.mobile, ''), s.mobile) AS mobile,",
            "       (IFNULL(so.saleOutAmount, 0) + IFNULL(spa.priceAdjustAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(fr.receiptAmount, 0)) AS receivableBalance,",
            "       (IFNULL(pi.purchaseInAmount, 0) + IFNULL(ppa.priceAdjustAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0)) AS payableBalance,",
            "       ((IFNULL(so.saleOutAmount, 0) + IFNULL(spa.priceAdjustAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(fr.receiptAmount, 0))",
            "        - (IFNULL(pi.purchaseInAmount, 0) + IFNULL(ppa.priceAdjustAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0))) AS offsetBalance",
            "  FROM erp_customer c",
            " INNER JOIN erp_supplier s ON s.deleted = 0 AND s.name = c.name",
            "  LEFT JOIN (",
            "       SELECT customer_id, SUM(total_price) AS saleOutAmount",
            "         FROM erp_sale_out",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND out_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY customer_id",
            "  ) so ON so.customer_id = c.id",
            "  LEFT JOIN (",
            "       SELECT customer_id, SUM(total_price) AS saleReturnAmount",
            "         FROM erp_sale_return",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND return_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY customer_id",
            "  ) sr ON sr.customer_id = c.id",
            "  LEFT JOIN (",
            "       SELECT customer_id, SUM(total_adjust_price) AS priceAdjustAmount",
            "         FROM erp_sale_price_adjust",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND adjust_date BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY customer_id",
            "  ) spa ON spa.customer_id = c.id",
            "  LEFT JOIN (",
            "       SELECT customer_id, SUM(receipt_price) AS receiptAmount",
            "         FROM erp_finance_receipt",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND receipt_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY customer_id",
            "  ) fr ON fr.customer_id = c.id",
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
            "       SELECT supplier_id, SUM(total_adjust_price) AS priceAdjustAmount",
            "         FROM erp_purchase_price_adjust",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND adjust_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) ppa ON ppa.supplier_id = s.id",
            "  LEFT JOIN (",
            "       SELECT supplier_id, SUM(payment_price) AS paymentAmount",
            "         FROM erp_finance_payment",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='reqVO.startTime != null and reqVO.endTime != null'>",
            "          AND payment_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}",
            "          </if>",
            "        GROUP BY supplier_id",
            "  ) fp ON fp.supplier_id = s.id",
            " WHERE c.deleted = 0",
            "   <if test='reqVO.subjectName != null and reqVO.subjectName != \"\"'>",
            "   AND c.name LIKE CONCAT('%', #{reqVO.subjectName}, '%')",
            "   </if>",
            "   <if test='reqVO.showZeroBalance == null or !reqVO.showZeroBalance'>",
            "   AND ((IFNULL(so.saleOutAmount, 0) + IFNULL(spa.priceAdjustAmount, 0) - IFNULL(sr.saleReturnAmount, 0) - IFNULL(fr.receiptAmount, 0))",
            "    - (IFNULL(pi.purchaseInAmount, 0) + IFNULL(ppa.priceAdjustAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0))) &lt;&gt; 0",
            "   </if>",
            " ORDER BY offsetBalance DESC, c.id DESC",
            "</script>"
    })
    List<ErpSettlementOffsetDO> selectList(@Param("reqVO") ErpSettlementOffsetPageReqVO reqVO);

    default PageResult<ErpSettlementOffsetDO> selectPage(ErpSettlementOffsetPageReqVO reqVO) {
        List<ErpSettlementOffsetDO> list = selectList(reqVO);
        long total = list.size();
        int fromIndex = Math.max(0, (reqVO.getPageNo() - 1) * reqVO.getPageSize());
        int toIndex = Math.min(list.size(), fromIndex + reqVO.getPageSize());
        if (fromIndex >= list.size()) {
            return PageResult.empty(total);
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), total);
    }
}
