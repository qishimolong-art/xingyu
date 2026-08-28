package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpReceivableReportMapper extends BaseMapperX<ErpReceivableOtherDO> {

    @Select({
            "<script>",
            "SELECT c.id AS customerId,",
            "       c.name AS customerName,",
            "       c.contact AS contact,",
            "       c.mobile AS mobile,",
            "       COALESCE(ro.deptId, c.dept_id) AS deptId,",
            "       d.name AS deptName,",
            "       ro.handlerId AS handlerId,",
            "       u.nickname AS handlerName,",
            "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
            "       IFNULL(ro.settledAmount, 0) AS settledAmount,",
            "       IFNULL(ro.otherReceivableAmount, 0) - IFNULL(ro.settledAmount, 0) AS balance,",
            "       ro.lastBizTime AS lastBizTime",
            "  FROM erp_customer c",
            "  LEFT JOIN (",
            "       SELECT customer_id,",
            "              MAX(dept_id) AS deptId,",
            "              MAX(handler_id) AS handlerId,",
            "              SUM(receivable_amount) AS otherReceivableAmount,",
            "              SUM(IFNULL(settled_amount, 0)) AS settledAmount,",
            "              CAST(MAX(biz_time) AS DATETIME) AS lastBizTime",
            "         FROM erp_receivable_other",
            "        WHERE deleted = 0 AND status = 20",
            "          <if test='!documentAll'>",
            "          AND (",
            "            <choose>",
            "              <when test='documentDeptIds != null and documentDeptIds.size() > 0'>",
            "                dept_id IN <foreach collection='documentDeptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "                <if test='documentSelfUserId != null'> OR handler_id = #{documentSelfUserId}</if>",
            "              </when>",
            "              <when test='documentSelfUserId != null'>handler_id = #{documentSelfUserId}</when>",
            "              <otherwise>1 = 0</otherwise>",
            "            </choose>",
            "          )",
            "          </if>",
            "          <if test='reqVO.startTime != null'> AND biz_time &gt;= DATE(#{reqVO.startTime}) </if>",
            "          <if test='reqVO.endTime != null'> AND biz_time &lt;= DATE(#{reqVO.endTime}) </if>",
            "        GROUP BY customer_id",
            "  ) ro ON ro.customer_id = c.id",
            "  LEFT JOIN system_dept d ON d.id = COALESCE(ro.deptId, c.dept_id)",
            "  LEFT JOIN system_users u ON u.id = ro.handlerId",
            " WHERE c.deleted = 0",
            "   <if test='reqVO.showZeroBalance == null or !reqVO.showZeroBalance'>",
            "   AND (IFNULL(ro.otherReceivableAmount, 0) - IFNULL(ro.settledAmount, 0)) &lt;&gt; 0",
            "   </if>",
            "   <if test='reqVO.customerId != null'> AND c.id = #{reqVO.customerId} </if>",
            "   <if test='reqVO.customerName != null and reqVO.customerName != \"\"'> AND c.name LIKE CONCAT('%', #{reqVO.customerName}, '%') </if>",
            "   <if test='reqVO.deptId != null'>",
            "   AND (c.dept_id = #{reqVO.deptId}",
            "        OR ro.deptId = #{reqVO.deptId}",
            "        OR (c.allow_multi_dept = true",
            "            AND EXISTS (SELECT 1 FROM erp_customer_dept ecd",
            "                         WHERE ecd.customer_id = c.id",
            "                           AND ecd.deleted = b'0'",
            "                           AND ecd.tenant_id = c.tenant_id",
            "                           AND ecd.dept_id = #{reqVO.deptId})))",
            "   </if>",
            "   <if test='reqVO.handlerId != null'> AND ro.handlerId = #{reqVO.handlerId} </if>",
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
            "   </if>",
            "   <if test='reqVO.keyword != null and reqVO.keyword != \"\"'>",
            "   AND (c.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR c.contact LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR c.mobile LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR d.name LIKE CONCAT('%', #{reqVO.keyword}, '%')",
            "        OR u.nickname LIKE CONCAT('%', #{reqVO.keyword}, '%'))",
            "   </if>",
            " <choose>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"customerName\"'>ORDER BY customerName ASC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"customerName\"'>ORDER BY customerName DESC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"otherReceivableAmount\"'>ORDER BY otherReceivableAmount ASC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"otherReceivableAmount\"'>ORDER BY otherReceivableAmount DESC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"settledAmount\"'>ORDER BY settledAmount ASC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"settledAmount\"'>ORDER BY settledAmount DESC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"balance\"'>ORDER BY balance ASC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"balance\"'>ORDER BY balance DESC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"asc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime ASC, c.id DESC</when>",
            "   <when test='reqVO.orderDirection == \"desc\" and reqVO.orderField == \"lastBizTime\"'>ORDER BY lastBizTime DESC, c.id DESC</when>",
            "   <otherwise>ORDER BY balance DESC, c.id DESC</otherwise>",
            " </choose>",
            "</script>"
    })
    List<ErpReceivableReportRespVO> selectList(@Param("reqVO") ErpReceivableReportPageReqVO reqVO,
                                               @Param("deptIds") Collection<Long> deptIds,
                                               @Param("selfUserId") String selfUserId,
                                               @Param("all") boolean all,
                                               @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                               @Param("documentSelfUserId") Long documentSelfUserId,
                                               @Param("documentAll") boolean documentAll);

    default PageResult<ErpReceivableReportRespVO> selectPage(ErpReceivableReportPageReqVO reqVO,
                                                             Collection<Long> deptIds,
                                                             String selfUserId,
                                                             boolean all,
                                                             Collection<Long> documentDeptIds,
                                                             Long documentSelfUserId,
                                                             boolean documentAll) {
        List<ErpReceivableReportRespVO> list = selectList(reqVO, deptIds, selfUserId, all,
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
