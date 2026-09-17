package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPayableReportMapper extends BaseMapperX<ErpPayableMiscDO> {

    @Select({
            "<script>",
            "SELECT s.id AS supplierId,",
            "       s.name AS supplierName,",
            "       s.contact AS contact,",
            "       s.mobile AS mobile,",
            "       COALESCE(po.deptId, s.dept_id) AS deptId,",
            "       d.name AS deptName,",
            "       po.handlerId AS handlerId,",
            "       u.nickname AS handlerName,",
            "       IFNULL(po.otherPayableAmount, 0) AS otherPayableAmount,",
            "       IFNULL(po.settledAmount, 0) AS settledAmount,",
            "       IFNULL(po.otherPayableAmount, 0) - IFNULL(po.settledAmount, 0) AS balance,",
            "       po.lastBizTime AS lastBizTime",
            "  FROM erp_supplier s",
            "  LEFT JOIN (",
            "       SELECT pm.supplier_id,",
            "              MAX(pm.dept_id) AS deptId,",
            "              MAX(pm.handler_id) AS handlerId,",
            "              SUM(pm.amount) AS otherPayableAmount,",
            "              SUM(IFNULL(pa.paymentAmount, 0)) AS settledAmount,",
            "              CAST(MAX(pm.biz_time) AS DATETIME) AS lastBizTime",
            "         FROM erp_payable_misc pm",
            "         LEFT JOIN (",
            "              SELECT fpi.biz_id, SUM(fpi.payment_price) AS paymentAmount",
            "                FROM erp_finance_payment_item fpi",
            "                INNER JOIN erp_finance_payment fp ON fp.id = fpi.payment_id",
            "                 AND fp.deleted = 0 AND fp.status = 20 AND fp.tenant_id = fpi.tenant_id",
            "               WHERE fpi.deleted = 0 AND fpi.biz_type = 14 AND fpi.write_off_status = 1",
            "               GROUP BY fpi.biz_id",
            "         ) pa ON pa.biz_id = pm.id",
            "        WHERE pm.deleted = 0 AND pm.status = 20",
            "          <if test='!documentAll'>",
            "          AND (",
            "            <choose>",
            "              <when test='documentDeptIds != null and documentDeptIds.size() > 0'>",
            "                pm.dept_id IN <foreach collection='documentDeptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "                <if test='documentSelfUserId != null'> OR pm.handler_id = #{documentSelfUserId}</if>",
            "              </when>",
            "              <when test='documentSelfUserId != null'>pm.handler_id = #{documentSelfUserId}</when>",
            "              <otherwise>1 = 0</otherwise>",
            "            </choose>",
            "          )",
            "          </if>",
            "          <if test='reqVO.startTime != null'> AND pm.biz_time &gt;= #{reqVO.startTime} </if>",
            "          <if test='reqVO.endTime != null'> AND pm.biz_time &lt;= #{reqVO.endTime} </if>",
            "        GROUP BY pm.supplier_id",
            "  ) po ON po.supplier_id = s.id",
            "  LEFT JOIN system_dept d ON d.id = COALESCE(po.deptId, s.dept_id)",
            "  LEFT JOIN system_users u ON u.id = po.handlerId",
            " WHERE s.deleted = 0",
            "   <if test='reqVO.showZeroBalance == null or !reqVO.showZeroBalance'>",
            "   AND IFNULL(po.otherPayableAmount, 0) - IFNULL(po.settledAmount, 0) &lt;&gt; 0",
            "   </if>",
            "   <if test='reqVO.supplierId != null'> AND s.id = #{reqVO.supplierId} </if>",
            "   <if test='reqVO.supplierName != null and reqVO.supplierName != \"\"'> AND s.name LIKE CONCAT('%', #{reqVO.supplierName}, '%') </if>",
            "   <if test='reqVO.deptId != null'>",
            "   AND (s.dept_id = #{reqVO.deptId}",
            "        OR po.deptId = #{reqVO.deptId}",
            "        OR (s.allow_multi_dept = true",
            "            AND EXISTS (SELECT 1 FROM erp_supplier_dept esd",
            "                         WHERE esd.supplier_id = s.id",
            "                           AND esd.deleted = b'0'",
            "                           AND esd.tenant_id = s.tenant_id",
            "                           AND esd.dept_id = #{reqVO.deptId})))",
            "   </if>",
            "   <if test='reqVO.handlerId != null'> AND po.handlerId = #{reqVO.handlerId} </if>",
            "   <if test='!all'>",
            "   AND (",
            "        <choose>",
            "        <when test='deptIds != null and deptIds.size() > 0'>",
            "        (s.dept_id IN",
            "         <foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>#{deptId}</foreach>",
            "         OR (s.allow_multi_dept = true",
            "             AND EXISTS (SELECT 1 FROM erp_supplier_dept esd_scope",
            "                          WHERE esd_scope.supplier_id = s.id",
            "                            AND esd_scope.deleted = b'0'",
            "                            AND esd_scope.tenant_id = s.tenant_id",
            "                            AND esd_scope.dept_id IN",
            "                            <foreach collection='deptIds' item='scopeDeptId' open='(' separator=',' close=')'>#{scopeDeptId}</foreach>)))",
            "        </when>",
            "        <otherwise>1 = 0</otherwise>",
            "        </choose>",
            "        <if test='selfUserId != null'> OR s.creator = #{selfUserId} </if>",
            "   )",
            "   </if>",
            "   <if test='reqVO.keyword != null and reqVO.keyword != \"\"'>",
            "   AND (s.code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.short_name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.contact LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.mobile LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.telephone LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.pinyin_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR s.wubi_code LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR d.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR u.nickname LIKE CONCAT('%', #{reqVO.keyword}, '%'))",
            "   </if>",
            " <choose>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"supplierName\"'>ORDER BY supplierName ASC, s.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"supplierName\"'>ORDER BY supplierName DESC, s.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"otherPayableAmount\"'>ORDER BY otherPayableAmount ASC, s.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"otherPayableAmount\"'>ORDER BY otherPayableAmount DESC, s.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime ASC, s.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime DESC, s.id DESC</when>",
            "   <otherwise>ORDER BY otherPayableAmount DESC, s.id DESC</otherwise>",
            " </choose>",
            "</script>"
    })
    List<ErpPayableReportRespVO> selectList(@Param("reqVO") ErpPayableReportPageReqVO reqVO,
                                            @Param("deptIds") Collection<Long> deptIds,
                                            @Param("selfUserId") String selfUserId,
                                            @Param("all") boolean all,
                                            @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                            @Param("documentSelfUserId") Long documentSelfUserId,
                                            @Param("documentAll") boolean documentAll);

    default PageResult<ErpPayableReportRespVO> selectPage(ErpPayableReportPageReqVO reqVO,
                                                          Collection<Long> deptIds,
                                                          String selfUserId,
                                                          boolean all,
                                                          Collection<Long> documentDeptIds,
                                                          Long documentSelfUserId,
                                                          boolean documentAll) {
        List<ErpPayableReportRespVO> list = selectList(reqVO, deptIds, selfUserId, all,
                documentDeptIds, documentSelfUserId, documentAll);
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return new PageResult<>(list, (long) list.size());
        }
        int fromIndex = Math.max(0, (reqVO.getPageNo() - 1) * reqVO.getPageSize());
        int toIndex = Math.min(list.size(), fromIndex + reqVO.getPageSize());
        if (fromIndex >= list.size()) {
            return PageResult.empty((long) list.size());
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) list.size());
    }
}
