package cn.iocoder.yudao.module.erp.dal.mysql.report;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportRankRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportTrendRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpSystemReportMapper {

    List<Map<String, Object>> selectSupplierOptions(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                    @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                    @Param("partySelfUserId") String partySelfUserId,
                                                    @Param("partyAll") boolean partyAll);

    List<Map<String, Object>> selectCustomerOptions(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                    @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                    @Param("partySelfUserId") String partySelfUserId,
                                                    @Param("partyAll") boolean partyAll);

    List<Map<String, Object>> selectWarehouseOptions(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                     @Param("warehouseIds") Collection<Long> warehouseIds);

    List<Map<String, Object>> selectCategoryOptions(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                    @Param("warehouseIds") Collection<Long> warehouseIds);

    ErpSystemReportSummaryRespVO selectPurchaseSummary(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                       @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                       @Param("partySelfUserId") String partySelfUserId,
                                                       @Param("partyAll") boolean partyAll,
                                                       @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                       @Param("documentSelfUserId") String documentSelfUserId,
                                                       @Param("documentAll") boolean documentAll);

    List<ErpSystemReportTrendRespVO> selectPurchaseTrend(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                         @Param("periodExpr") String periodExpr,
                                                         @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                         @Param("partySelfUserId") String partySelfUserId,
                                                         @Param("partyAll") boolean partyAll,
                                                         @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                         @Param("documentSelfUserId") String documentSelfUserId,
                                                         @Param("documentAll") boolean documentAll);

    List<ErpSystemReportRankRespVO> selectPurchaseRank(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                       @Param("orderBy") String orderBy,
                                                       @Param("limit") int limit,
                                                       @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                       @Param("partySelfUserId") String partySelfUserId,
                                                       @Param("partyAll") boolean partyAll,
                                                       @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                       @Param("documentSelfUserId") String documentSelfUserId,
                                                       @Param("documentAll") boolean documentAll);

    Long countPurchasePage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                           @Param("dimension") String dimension,
                           @Param("partyDeptIds") Collection<Long> partyDeptIds,
                           @Param("partySelfUserId") String partySelfUserId,
                           @Param("partyAll") boolean partyAll,
                           @Param("documentDeptIds") Collection<Long> documentDeptIds,
                           @Param("documentSelfUserId") String documentSelfUserId,
                           @Param("documentAll") boolean documentAll);

    List<Map<String, Object>> selectPurchasePage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                 @Param("dimension") String dimension,
                                                 @Param("orderBy") String orderBy,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset,
                                                 @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                 @Param("partySelfUserId") String partySelfUserId,
                                                 @Param("partyAll") boolean partyAll,
                                                 @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                 @Param("documentSelfUserId") String documentSelfUserId,
                                                 @Param("documentAll") boolean documentAll);

    ErpSystemReportSummaryRespVO selectSaleSummary(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                   @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                   @Param("partySelfUserId") String partySelfUserId,
                                                   @Param("partyAll") boolean partyAll,
                                                   @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                   @Param("documentSelfUserId") String documentSelfUserId,
                                                   @Param("documentAll") boolean documentAll);

    List<ErpSystemReportTrendRespVO> selectSaleTrend(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                     @Param("periodExpr") String periodExpr,
                                                     @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                     @Param("partySelfUserId") String partySelfUserId,
                                                     @Param("partyAll") boolean partyAll,
                                                     @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                     @Param("documentSelfUserId") String documentSelfUserId,
                                                     @Param("documentAll") boolean documentAll);

    List<ErpSystemReportRankRespVO> selectSaleRank(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                   @Param("orderBy") String orderBy,
                                                   @Param("limit") int limit,
                                                   @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                                   @Param("partySelfUserId") String partySelfUserId,
                                                   @Param("partyAll") boolean partyAll,
                                                   @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                                   @Param("documentSelfUserId") String documentSelfUserId,
                                                   @Param("documentAll") boolean documentAll);

    Long countSalePage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                       @Param("dimension") String dimension,
                       @Param("partyDeptIds") Collection<Long> partyDeptIds,
                       @Param("partySelfUserId") String partySelfUserId,
                       @Param("partyAll") boolean partyAll,
                       @Param("documentDeptIds") Collection<Long> documentDeptIds,
                       @Param("documentSelfUserId") String documentSelfUserId,
                       @Param("documentAll") boolean documentAll);

    List<Map<String, Object>> selectSalePage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                             @Param("dimension") String dimension,
                                             @Param("orderBy") String orderBy,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset,
                                             @Param("partyDeptIds") Collection<Long> partyDeptIds,
                                             @Param("partySelfUserId") String partySelfUserId,
                                             @Param("partyAll") boolean partyAll,
                                             @Param("documentDeptIds") Collection<Long> documentDeptIds,
                                             @Param("documentSelfUserId") String documentSelfUserId,
                                             @Param("documentAll") boolean documentAll);

    ErpSystemReportSummaryRespVO selectStockSummary(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                    @Param("warehouseIds") Collection<Long> warehouseIds);

    List<ErpSystemReportTrendRespVO> selectStockTrend(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                      @Param("periodExpr") String periodExpr,
                                                      @Param("warehouseIds") Collection<Long> warehouseIds);

    List<ErpSystemReportRankRespVO> selectStockRank(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                                    @Param("orderBy") String orderBy,
                                                    @Param("limit") int limit,
                                                    @Param("warehouseIds") Collection<Long> warehouseIds);

    Long countStockPage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                        @Param("dimension") String dimension,
                        @Param("warehouseIds") Collection<Long> warehouseIds);

    List<Map<String, Object>> selectStockPage(@Param("reqVO") ErpSystemReportReqVO reqVO,
                                              @Param("dimension") String dimension,
                                              @Param("orderBy") String orderBy,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset,
                                              @Param("warehouseIds") Collection<Long> warehouseIds);

}
