package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionApplyReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateFromBizReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSearchSourceBizReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherAttributionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAttributionStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_ALREADY_GENERATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_BOOK_NOT_OPEN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_MONTH_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_AUTO_GENERATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_BIZ_APPROVED_EXISTS;

/**
 * ERP 凭证归属（跨月调整）Service 实现
 *
 * @author Claude
 */
@Service
@Validated
public class ErpVoucherAttributionServiceImpl implements ErpVoucherAttributionService {

    @Resource
    private ErpVoucherAttributionMapper attributionMapper;
    /**
     * 用 @Lazy 避免与 ErpVoucherService 之间的循环依赖
     */
    @Resource
    @Lazy
    private ErpVoucherService voucherService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherGenerationService generationService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader ruleSourceReader;


    // ========== 凭证生成-单据来源查询所需 Mapper ==========
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Resource
    private ErpOtherPayableMapper otherPayableMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpPreReceiptMapper preReceiptMapper;
    @Resource
    private ErpPrePaymentMapper prePaymentMapper;
    @Resource
    private ErpPreReceivableMapper preReceivableMapper;
    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    @Lazy
    private ErpCustomerService customerService;
    @Resource
    @Lazy
    private ErpSupplierService supplierService;

    /**
     * H3 修复：生成凭证前校验该期间是否开账且勾选了对应凭证类型
     */
    @Resource
    @Lazy
    private ErpBookOpenService bookOpenService;

    @Override
    public Long createVoucherAttribution(ErpVoucherAttributionSaveReqVO createReqVO) {
        ErpVoucherAttributionDO attribution = BeanUtils.toBean(createReqVO, ErpVoucherAttributionDO.class);
        // 制单日期默认按 bizDate 当天
        if (attribution.getVoucherMakeDate() == null && attribution.getBizDate() != null) {
            attribution.setVoucherMakeDate(attribution.getBizDate().toLocalDate());
        }
        // 默认未归属
        if (attribution.getAttributionStatus() == null) {
            attribution.setAttributionStatus(ErpAttributionStatusEnum.UNATTRIBUTED.getStatus());
        }
        // 校验归属年月不能晚于制单月份，也不能早于业务发生月份（H2 修复）
        validateAttributionMonth(attribution.getAttributionYear(), attribution.getAttributionMonth(),
                attribution.getVoucherMakeDate(), attribution.getBizDate());
        attributionMapper.insert(attribution);
        return attribution.getId();
    }

    @Override
    public void updateVoucherAttribution(ErpVoucherAttributionSaveReqVO updateReqVO) {
        // 校验存在
        ErpVoucherAttributionDO existing = validateVoucherAttribution(updateReqVO.getId());
        // 已生成凭证不可再编辑
        if (ErpAttributionStatusEnum.GENERATED.getStatus().equals(existing.getAttributionStatus())) {
            throw exception(VOUCHER_ATTRIBUTION_ALREADY_GENERATED);
        }
        ErpVoucherAttributionDO updateObj = BeanUtils.toBean(updateReqVO, ErpVoucherAttributionDO.class);
        if (updateObj.getVoucherMakeDate() == null && updateObj.getBizDate() != null) {
            updateObj.setVoucherMakeDate(updateObj.getBizDate().toLocalDate());
        }
        // 保留原状态与凭证关联
        updateObj.setAttributionStatus(existing.getAttributionStatus());
        updateObj.setVoucherId(existing.getVoucherId());
        // 校验归属年月（H2 修复：传入 bizDate 校验下界）
        validateAttributionMonth(updateObj.getAttributionYear(), updateObj.getAttributionMonth(),
                updateObj.getVoucherMakeDate(), updateObj.getBizDate());
        attributionMapper.updateById(updateObj);
    }

    @Override
    public void deleteVoucherAttribution(Long id) {
        ErpVoucherAttributionDO existing = validateVoucherAttribution(id);
        // 已生成凭证不可删除
        if (ErpAttributionStatusEnum.GENERATED.getStatus().equals(existing.getAttributionStatus())) {
            throw exception(VOUCHER_ATTRIBUTION_ALREADY_GENERATED);
        }
        attributionMapper.deleteById(id);
    }

    @Override
    public ErpVoucherAttributionDO getVoucherAttribution(Long id) {
        return attributionMapper.selectById(id);
    }

    @Override
    public ErpVoucherAttributionDO validateVoucherAttribution(Long id) {
        ErpVoucherAttributionDO attribution = attributionMapper.selectById(id);
        if (attribution == null) {
            throw exception(VOUCHER_ATTRIBUTION_NOT_EXISTS);
        }
        return attribution;
    }

    @Override
    public PageResult<ErpVoucherAttributionDO> getVoucherAttributionPage(ErpVoucherAttributionPageReqVO pageReqVO) {
        return attributionMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAttribution(ErpVoucherAttributionApplyReqVO reqVO) {
        List<ErpVoucherAttributionDO> attributions = attributionMapper.selectByIds(reqVO.getIds());
        if (CollUtil.isEmpty(attributions) || attributions.size() != reqVO.getIds().size()) {
            throw exception(VOUCHER_ATTRIBUTION_NOT_EXISTS);
        }
        // 1. 每条都校验：未生成 + 归属月份不晚于制单月份（H2 修复：再校验不早于业务发生月份）
        for (ErpVoucherAttributionDO attribution : attributions) {
            if (ErpAttributionStatusEnum.GENERATED.getStatus().equals(attribution.getAttributionStatus())) {
                throw exception(VOUCHER_ATTRIBUTION_ALREADY_GENERATED);
            }
            validateAttributionMonth(reqVO.getAttributionYear(), reqVO.getAttributionMonth(),
                    attribution.getVoucherMakeDate(), attribution.getBizDate());
        }
        // 2. 批量更新归属年月 + 状态
        for (ErpVoucherAttributionDO attribution : attributions) {
            ErpVoucherAttributionDO update = new ErpVoucherAttributionDO()
                    .setId(attribution.getId())
                    .setAttributionYear(reqVO.getAttributionYear())
                    .setAttributionMonth(reqVO.getAttributionMonth())
                    .setAttributionStatus(ErpAttributionStatusEnum.ATTRIBUTED.getStatus());
            attributionMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateVouchers(ErpVoucherAttributionGenerateReqVO reqVO) {
        cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Batch batch = new cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Batch();
        for (Long id : reqVO.getIds()) {
            ErpVoucherAttributionDO a = attributionMapper.selectById(id);
            if (a == null) throw exception(VOUCHER_ATTRIBUTION_NOT_EXISTS);
            cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request r = new cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request();
            r.setBizType(a.getBizType()); r.setBizId(a.getBizId()); r.setVoucherDate(a.getVoucherMakeDate());
            r.setAttributionYear(a.getAttributionYear()); r.setAttributionMonth(a.getAttributionMonth());
            r.setPreviewToken(reqVO.getPreviewTokens() == null ? null : reqVO.getPreviewTokens().get(id)); batch.getItems().add(r);
        }
        return generationService.generate(batch);
    }

    // ==================== 凭证生成-单据来源分页查询 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateVouchersFromBiz(ErpVoucherAttributionGenerateFromBizReqVO reqVO) {
        cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Batch batch = new cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Batch();
        for (ErpVoucherAttributionGenerateFromBizReqVO.BizItem item : reqVO.getItems()) {
            cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request r = new cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request();
            r.setBizType(item.getBizType()); r.setBizId(item.getBizId()); r.setVoucherDate(reqVO.getVoucherMakeDate());
            r.setAttributionYear(reqVO.getAttributionYear()); r.setAttributionMonth(reqVO.getAttributionMonth());r.setPreviewToken(item.getPreviewToken());batch.getItems().add(r);
        }
        return generationService.generate(batch);
    }

    // ==================== 凭证生成-单据来源分页查询（业务单据快照查询） ====================

    @Override
    public PageResult<ErpVoucherAttributionRespVO> searchSourceBizPage(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        Integer sourceBizType = reqVO.getSourceBizType();
        if (sourceBizType == null) {
            return searchAllSourceBiz(reqVO);
        }
        // 路由到对应业务表
        switch (sourceBizType) {
            case 6: case 12: case 18:
                return searchFundSource(reqVO);
            case 2:
                return searchSaleOut(reqVO);
            case 3:
                return searchSaleReturn(reqVO);
            case 4:
                return searchOtherReceivable(reqVO);
            case 8:
                return searchPurchaseIn(reqVO);
            case 9:
                return searchPurchaseReturn(reqVO);
            case 10:
                return searchOtherPayable(reqVO);
            case 16:
                return searchStockOut(reqVO);
            case 17:
                return searchStockIn(reqVO);
            case 21:
                return searchPreReceipt(reqVO);
            case 22:
                return searchPrePayment(reqVO);
            case 23:
                return searchPreReceivable(reqVO);
            // 未接入来源明确说明，不能伪装成没有单据。
            default:
                throw cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.problem("此来源尚未接入凭证生成，请人工处理");
        }
    }

    private PageResult<ErpVoucherAttributionRespVO> searchFundSource(ErpVoucherAttributionSearchSourceBizReqVO req) {
        int type=req.getSourceBizType();
        String bean=type==6?"erpFinanceReceiptMapper":type==12?"erpFinancePaymentMapper":"erpFinanceTransferMapper";
        String time=type==6?"receipt_time":type==12?"payment_time":"transfer_time";
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> q=new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        q.eq("status",20);
        if(req.getBizNo()!=null&&!req.getBizNo().isEmpty())q.like("no",req.getBizNo());
        if(req.getBizDateStartTime()!=null)q.ge(time,req.getBizDateStartTime());
        if(req.getBizDateEndTime()!=null)q.le(time,req.getBizDateEndTime());
        if(req.getPartyName()!=null&&!req.getPartyName().isEmpty()) {
            List<Long> parties=type==6?resolveCustomerIdsByName(req.getPartyName()):type==12?resolveSupplierIdsByName(req.getPartyName()):Collections.emptyList();
            if(parties==null||parties.isEmpty())return PageResult.empty();q.in(type==6?"customer_id":"supplier_id",parties);
        }
        PageResult<Map<String,Object>> page=ruleSourceReader.page(bean,q,req.getPageNo(),req.getPageSize());
        List<ErpVoucherAttributionRespVO> rows=new ArrayList<>();
        for(Map<String,Object> h:page.getList()) {
            String party="";
            if(type==6||type==12) {
                Long partyId=cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.longValue(h,type==6?"customerId":"supplierId");
                if(partyId!=null)party=String.valueOf(ruleSourceReader.one(type==6?"erpCustomerMapper":"erpSupplierMapper",partyId).get("name"));
            }
            rows.add(buildResp(type,(Long)h.get("id"),(String)h.get("no"),(java.time.LocalDateTime)h.get(type==6?"receiptTime":type==12?"paymentTime":"transferTime"),
                (BigDecimal)h.get(type==6?"receiptPrice":type==12?"paymentPrice":"transferPrice"),party,(String)h.get("remark")));
        }
        return new PageResult<>(rows,page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchAllSourceBiz(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int querySize = pageNo * pageSize;
        List<Integer> supportedTypes = java.util.Arrays.asList(6, 12, 18,
                ErpVoucherSourceBizTypeEnum.SALE_OUT.getType(),
                ErpVoucherSourceBizTypeEnum.SALE_RETURN.getType(),
                ErpVoucherSourceBizTypeEnum.OTHER_RECEIVABLE.getType(),
                ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType(),
                ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType(),
                ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType(),
                ErpVoucherSourceBizTypeEnum.OTHER_OUT.getType(),
                ErpVoucherSourceBizTypeEnum.OTHER_IN.getType(),
                ErpVoucherSourceBizTypeEnum.PRE_RECEIPT.getType(),
                ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType(),
                ErpVoucherSourceBizTypeEnum.PRE_RECEIVABLE.getType());

        List<ErpVoucherAttributionRespVO> all = new ArrayList<>();
        long total = 0L;
        for (Integer type : supportedTypes) {
            ErpVoucherAttributionSearchSourceBizReqVO typeReqVO = copySearchReq(reqVO, type, 1, querySize);
            PageResult<ErpVoucherAttributionRespVO> page = searchSourceBizPage(typeReqVO);
            total += page.getTotal();
            if (CollUtil.isNotEmpty(page.getList())) {
                all.addAll(page.getList());
            }
        }
        all.sort(Comparator
                .comparing(ErpVoucherAttributionRespVO::getBizDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(ErpVoucherAttributionRespVO::getBizType,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpVoucherAttributionRespVO::getBizId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        int from = Math.max(0, (pageNo - 1) * pageSize);
        if (from >= all.size()) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        int to = Math.min(all.size(), from + pageSize);
        return new PageResult<>(all.subList(from, to), total);
    }

    private ErpVoucherAttributionSearchSourceBizReqVO copySearchReq(ErpVoucherAttributionSearchSourceBizReqVO source,
                                                                     Integer sourceBizType,
                                                                     Integer pageNo,
                                                                     Integer pageSize) {
        ErpVoucherAttributionSearchSourceBizReqVO target = new ErpVoucherAttributionSearchSourceBizReqVO();
        target.setSourceBizType(sourceBizType);
        target.setBizDateStart(source.getBizDateStart());
        target.setBizDateEnd(source.getBizDateEnd());
        target.setBizNo(source.getBizNo());
        target.setPartyName(source.getPartyName());
        target.setPageNo(pageNo);
        target.setPageSize(pageSize);
        target.setKeyword(source.getKeyword());
        return target;
    }

    // ---------- 单据来源具体查询实现 ----------

    private PageResult<ErpVoucherAttributionRespVO> searchSaleOut(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        // H5: partyName 下推至 SQL，先查匹配的客户 ID 列表，再 IN 过滤
        List<Long> customerIds = resolveCustomerIdsByName(reqVO.getPartyName());
        if (customerIds != null && customerIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpSaleOutDO> wrapper = new LambdaQueryWrapperX<ErpSaleOutDO>()
                .likeIfPresent(ErpSaleOutDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpSaleOutDO::getOutTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpSaleOutDO::getOutTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpSaleOutDO::getCustomerId, customerIds);
        wrapper.eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpSaleOutDO::getTotalPrice)
                .orderByDesc(ErpSaleOutDO::getId);
        PageResult<ErpSaleOutDO> page = saleOutMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                page.getList().stream().map(ErpSaleOutDO::getCustomerId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(customerMap.get(o.getCustomerId()))
                    .map(ErpCustomerDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getOutTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchSaleReturn(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> customerIds = resolveCustomerIdsByName(reqVO.getPartyName());
        if (customerIds != null && customerIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpSaleReturnDO> wrapper = new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .likeIfPresent(ErpSaleReturnDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpSaleReturnDO::getCustomerId, customerIds);
        wrapper.eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpSaleReturnDO::getTotalPrice)
                .orderByDesc(ErpSaleReturnDO::getId);
        PageResult<ErpSaleReturnDO> page = saleReturnMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                page.getList().stream().map(ErpSaleReturnDO::getCustomerId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(customerMap.get(o.getCustomerId()))
                    .map(ErpCustomerDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getReturnTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchPurchaseIn(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> supplierIds = resolveSupplierIdsByName(reqVO.getPartyName());
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpPurchaseInDO> wrapper = new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .likeIfPresent(ErpPurchaseInDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpPurchaseInDO::getInTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpPurchaseInDO::getInTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpPurchaseInDO::getSupplierId, supplierIds);
        wrapper.eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpPurchaseInDO::getTotalPrice)
                .orderByDesc(ErpPurchaseInDO::getId);
        PageResult<ErpPurchaseInDO> page = purchaseInMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                page.getList().stream().map(ErpPurchaseInDO::getSupplierId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(supplierMap.get(o.getSupplierId()))
                    .map(ErpSupplierDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getInTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchPurchaseReturn(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> supplierIds = resolveSupplierIdsByName(reqVO.getPartyName());
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpPurchaseReturnDO> wrapper = new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .likeIfPresent(ErpPurchaseReturnDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpPurchaseReturnDO::getSupplierId, supplierIds);
        wrapper.eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpPurchaseReturnDO::getTotalPrice)
                .orderByDesc(ErpPurchaseReturnDO::getId);
        PageResult<ErpPurchaseReturnDO> page = purchaseReturnMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                page.getList().stream().map(ErpPurchaseReturnDO::getSupplierId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(supplierMap.get(o.getSupplierId()))
                    .map(ErpSupplierDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getReturnTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchOtherReceivable(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        LambdaQueryWrapperX<ErpOtherReceivableDO> wrapper = new LambdaQueryWrapperX<ErpOtherReceivableDO>()
                .likeIfPresent(ErpOtherReceivableDO::getNo, reqVO.getBizNo())
                .likeIfPresent(ErpOtherReceivableDO::getPartyName, reqVO.getPartyName())
                .geIfPresent(ErpOtherReceivableDO::getBizTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpOtherReceivableDO::getBizTime, reqVO.getBizDateEndTime());
        wrapper.eq(ErpOtherReceivableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpOtherReceivableDO::getActualAmount)
                .orderByDesc(ErpOtherReceivableDO::getId);
        PageResult<ErpOtherReceivableDO> page = otherReceivableMapper.selectPage(reqVO, wrapper);
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o ->
                buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getBizTime(),
                        firstNonNull(o.getActualAmount(), o.getTotalAmount()), o.getPartyName(), o.getRemark()))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchOtherPayable(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        LambdaQueryWrapperX<ErpOtherPayableDO> wrapper = new LambdaQueryWrapperX<ErpOtherPayableDO>()
                .likeIfPresent(ErpOtherPayableDO::getNo, reqVO.getBizNo())
                .likeIfPresent(ErpOtherPayableDO::getPartyName, reqVO.getPartyName())
                .geIfPresent(ErpOtherPayableDO::getBizTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpOtherPayableDO::getBizTime, reqVO.getBizDateEndTime());
        wrapper.eq(ErpOtherPayableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpOtherPayableDO::getActualAmount)
                .orderByDesc(ErpOtherPayableDO::getId);
        PageResult<ErpOtherPayableDO> page = otherPayableMapper.selectPage(reqVO, wrapper);
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o ->
                buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getBizTime(),
                        firstNonNull(o.getActualAmount(), o.getTotalAmount()), o.getPartyName(), o.getRemark()))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchFinanceReceipt(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> customerIds = resolveCustomerIdsByName(reqVO.getPartyName());
        if (customerIds != null && customerIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpFinanceReceiptDO> wrapper = new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .likeIfPresent(ErpFinanceReceiptDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpFinanceReceiptDO::getCustomerId, customerIds);
        wrapper.eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .orderByDesc(ErpFinanceReceiptDO::getId);
        PageResult<ErpFinanceReceiptDO> page = financeReceiptMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                page.getList().stream().map(ErpFinanceReceiptDO::getCustomerId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(customerMap.get(o.getCustomerId()))
                    .map(ErpCustomerDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getReceiptTime(),
                    firstNonNull(o.getReceiptPrice(), o.getTotalPrice()), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchFinancePayment(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> supplierIds = resolveSupplierIdsByName(reqVO.getPartyName());
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpFinancePaymentDO> wrapper = new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .likeIfPresent(ErpFinancePaymentDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpFinancePaymentDO::getSupplierId, supplierIds);
        wrapper.eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .orderByDesc(ErpFinancePaymentDO::getId);
        PageResult<ErpFinancePaymentDO> page = financePaymentMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                page.getList().stream().map(ErpFinancePaymentDO::getSupplierId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(supplierMap.get(o.getSupplierId()))
                    .map(ErpSupplierDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getPaymentTime(),
                    firstNonNull(o.getPaymentPrice(), o.getTotalPrice()), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchStockIn(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> supplierIds = resolveSupplierIdsByName(reqVO.getPartyName());
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpStockInDO> wrapper = new LambdaQueryWrapperX<ErpStockInDO>()
                .likeIfPresent(ErpStockInDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpStockInDO::getInTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpStockInDO::getInTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpStockInDO::getSupplierId, supplierIds);
        wrapper.eq(ErpStockInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpStockInDO::getTotalPrice)
                .orderByDesc(ErpStockInDO::getId);
        PageResult<ErpStockInDO> page = stockInMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                page.getList().stream().map(ErpStockInDO::getSupplierId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(supplierMap.get(o.getSupplierId()))
                    .map(ErpSupplierDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getInTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchStockOut(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        List<Long> customerIds = resolveCustomerIdsByName(reqVO.getPartyName());
        if (customerIds != null && customerIds.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpStockOutDO> wrapper = new LambdaQueryWrapperX<ErpStockOutDO>()
                .likeIfPresent(ErpStockOutDO::getNo, reqVO.getBizNo())
                .geIfPresent(ErpStockOutDO::getOutTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpStockOutDO::getOutTime, reqVO.getBizDateEndTime())
                .inIfPresent(ErpStockOutDO::getCustomerId, customerIds);
        wrapper.eq(ErpStockOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpStockOutDO::getTotalPrice)
                .orderByDesc(ErpStockOutDO::getId);
        PageResult<ErpStockOutDO> page = stockOutMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                page.getList().stream().map(ErpStockOutDO::getCustomerId).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()));
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o -> {
            String party = Optional.ofNullable(customerMap.get(o.getCustomerId()))
                    .map(ErpCustomerDO::getName).orElse(null);
            return buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getOutTime(),
                    o.getTotalPrice(), party, o.getRemark());
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchPreReceipt(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        LambdaQueryWrapperX<ErpPreReceiptDO> wrapper = new LambdaQueryWrapperX<ErpPreReceiptDO>()
                .likeIfPresent(ErpPreReceiptDO::getNo, reqVO.getBizNo())
                .likeIfPresent(ErpPreReceiptDO::getPartyName, reqVO.getPartyName())
                .geIfPresent(ErpPreReceiptDO::getBizTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpPreReceiptDO::getBizTime, reqVO.getBizDateEndTime());
        wrapper.eq(ErpPreReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpPreReceiptDO::getActualAmount)
                .orderByDesc(ErpPreReceiptDO::getId);
        PageResult<ErpPreReceiptDO> page = preReceiptMapper.selectPage(reqVO, wrapper);
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o ->
                buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getBizTime(),
                        o.getActualAmount(), o.getPartyName(), o.getRemark()))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchPrePayment(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        LambdaQueryWrapperX<ErpPrePaymentDO> wrapper = new LambdaQueryWrapperX<ErpPrePaymentDO>()
                .likeIfPresent(ErpPrePaymentDO::getNo, reqVO.getBizNo())
                .likeIfPresent(ErpPrePaymentDO::getPartyName, reqVO.getPartyName())
                .geIfPresent(ErpPrePaymentDO::getBizTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpPrePaymentDO::getBizTime, reqVO.getBizDateEndTime());
        wrapper.eq(ErpPrePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpPrePaymentDO::getActualAmount)
                .orderByDesc(ErpPrePaymentDO::getId);
        PageResult<ErpPrePaymentDO> page = prePaymentMapper.selectPage(reqVO, wrapper);
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o ->
                buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getBizTime(),
                        o.getActualAmount(), o.getPartyName(), o.getRemark()))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<ErpVoucherAttributionRespVO> searchPreReceivable(ErpVoucherAttributionSearchSourceBizReqVO reqVO) {
        LambdaQueryWrapperX<ErpPreReceivableDO> wrapper = new LambdaQueryWrapperX<ErpPreReceivableDO>()
                .likeIfPresent(ErpPreReceivableDO::getNo, reqVO.getBizNo())
                .likeIfPresent(ErpPreReceivableDO::getPartyName, reqVO.getPartyName())
                .geIfPresent(ErpPreReceivableDO::getBizTime, reqVO.getBizDateStartTime())
                .leIfPresent(ErpPreReceivableDO::getBizTime, reqVO.getBizDateEndTime());
        wrapper.eq(ErpPreReceivableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .isNotNull(ErpPreReceivableDO::getActualAmount)
                .orderByDesc(ErpPreReceivableDO::getId);
        PageResult<ErpPreReceivableDO> page = preReceivableMapper.selectPage(reqVO, wrapper);
        List<ErpVoucherAttributionRespVO> list = page.getList().stream().map(o ->
                buildResp(reqVO.getSourceBizType(), o.getId(), o.getNo(), o.getBizTime(),
                        o.getActualAmount(), o.getPartyName(), o.getRemark()))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    /**
     * 构造一行 attribution 形态的响应（attributionStatus=10 未归属）
     */
    private ErpVoucherAttributionRespVO buildResp(Integer bizType, Long bizId, String bizNo,
                                                  java.time.LocalDateTime bizDate, BigDecimal bizAmount,
                                                  String transactionParty, String remark) {
        ErpVoucherAttributionRespVO vo = new ErpVoucherAttributionRespVO();
        vo.setBizType(bizType);
        vo.setBizId(bizId);
        vo.setBizNo(bizNo);
        vo.setBizDate(bizDate);
        vo.setBizAmount(bizAmount);
        vo.setTransactionParty(transactionParty);
        vo.setAttributionStatus(ErpAttributionStatusEnum.UNATTRIBUTED.getStatus());
        vo.setRemark(remark);
        cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request r = new cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Request();
        r.setBizType(bizType); r.setBizId(bizId);
        cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Preview p = generationService.previewOne(r);
        vo.setGenerationStatus(p.getStatus());vo.setGenerationIssues(p.getIssues());vo.setVoucherId(p.getVoucherId());
        if ("GENERATED".equals(p.getStatus())) vo.setAttributionStatus(30);
        return vo;
    }

    private List<Long> resolveCustomerIdsByName(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return null;
        }
        return customerService.getCustomerListByNameLike(partyName)
                .stream().map(ErpCustomerDO::getId).collect(Collectors.toList());
    }

    private List<Long> resolveSupplierIdsByName(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return null;
        }
        return supplierService.getSupplierListByNameLike(partyName)
                .stream().map(ErpSupplierDO::getId).collect(Collectors.toList());
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    // ==================== 私有辅助 ====================

    private void deleteOldAttributionVoucherIfNecessary(ErpVoucherAttributionDO attribution) {
        // 普通生成不替换历史凭证。
    }

    private Long createRealVoucherFromAttribution(ErpVoucherAttributionDO attribution, LocalDate voucherDate) {
        throw new IllegalStateException("请使用统一预览生成服务");
    }

    /**
     * 校验：归属年月不能晚于制单月份
     * makeDate 为 null 时按当前日期兜底，避免校验失效。
     * 用 YearMonth 比较，避免 LocalDate 中时分秒导致的边界误判。
     */
    private BigDecimal sumStockRecordAmount(Integer stockBizType, Long bizId) {
        List<ErpStockRecordDO> records = stockRecordMapper.selectListByBiz(stockBizType, bizId);
        if (CollUtil.isEmpty(records)) {
            return BigDecimal.ZERO;
        }
        return records.stream()
                .map(this::getStockRecordAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getStockRecordAmount(ErpStockRecordDO record) {
        BigDecimal amount = record.getTotalPrice();
        if (amount == null && record.getUnitPrice() != null && record.getCount() != null) {
            amount = record.getUnitPrice().multiply(record.getCount());
        }
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }

    private void validateAttributionMonth(Integer attrYear, Integer attrMonth, LocalDate makeDate) {
        validateAttributionMonth(attrYear, attrMonth, makeDate, null);
    }

    private void validateAttributionMonth(Integer attrYear, Integer attrMonth, LocalDate makeDate,
                                          java.time.LocalDateTime bizDate) {
        if (attrYear == null || attrMonth == null) {
            return;
        }
        LocalDate effectiveMakeDate = makeDate != null ? makeDate : LocalDate.now();
        java.time.YearMonth attrYM = java.time.YearMonth.of(attrYear, attrMonth);
        java.time.YearMonth makeYM = java.time.YearMonth.from(effectiveMakeDate);
        if (attrYM.isAfter(makeYM)) {
            throw exception(VOUCHER_ATTRIBUTION_MONTH_INVALID);
        }
        // H2 修复：归属月份不能早于业务发生月份
        if (bizDate != null) {
            java.time.YearMonth bizYM = java.time.YearMonth.from(bizDate.toLocalDate());
            if (attrYM.isBefore(bizYM)) {
                throw exception(VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ);
            }
        }
    }

    /**
     * H3 修复：单据来源类型 sourceBizType（1~20）映射到凭证类型 voucherType（1~11，对应系统开账勾选项）
     * 暂未支持的单据返回 null，跳过 BookOpen 校验
     */
    private Integer mapBizTypeToVoucherType(Integer sourceBizType) {
        if (sourceBizType == null) return null;
        switch (sourceBizType) {
            case 1: case 2: case 3: return 1;   // 销售凭证 / 销售出库 / 销售退货 → SALE
            case 4: return 2;                   // 其他应收 → OTHER_RECEIVABLE
            case 5: return 3;                   // 预收款 → PRE_RECEIPT
            case 6: return 3;                   // 收款凭证（复用预收款）
            case 7: case 8: case 9: return 7;   // 采购凭证 / 采购入库 / 采购退货 → PURCHASE
            case 10: return 8;                  // 其他应付 → OTHER_PAYABLE
            case 11: return 4;                  // 预付款 → PRE_PAYMENT
            case 12: return 8;                  // 付款凭证（复用其他应付）
            case 15: return 6;                  // 调拨出库 → STOCK_MOVE_OUT
            case 16: return 9;                  // 其他出库 → OTHER_OUT
            case 17: return 10;                 // 其他入库 → OTHER_IN
            case 18: return 11;                 // 银行转账 → BANK_TRANSFER
            case 21: return 3;
            case 22: return 4;
            case 23: return 5;
            default: return null;
        }
    }

    private String buildSummary(ErpVoucherAttributionDO attribution) {
        if (attribution.getSummary() != null && !attribution.getSummary().isEmpty()) {
            return attribution.getSummary();
        }
        return "归属调整凭证";
    }

}
