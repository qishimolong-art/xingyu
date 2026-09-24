package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUploadReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrBatchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceOcrBatchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceOcrItemMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceOcrBatchStatusEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceOcrItemStatusEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_APP_CODE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_BATCH_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_CONFIRM_INVOICE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_CONFIRM_STATUS_NOT_SUPPORT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_CONFIRM_SUPPLIER_NOT_UNIQUE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_FACTORY_ORDER_NO_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_MATCH_STATUS_NOT_SUPPORT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_NO_CONFIRMED_ITEMS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_NO_MATCHABLE_ITEMS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_RECOGNIZE_STATUS_NOT_SUPPORT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_OCR_UPDATE_FACTORY_ORDER_NO_NOT_SUPPORT;

@Service
@Validated
@Slf4j
public class ErpPurchaseInvoiceOcrServiceImpl implements ErpPurchaseInvoiceOcrService {

    private static final DateTimeFormatter BATCH_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEFAULT_ENDPOINT = "https://dgfp.market.alicloudapi.com/ocrservice/invoice";
    private static final String OCR_ACCEPT = "application/json; charset=utf-8";
    private static final String OCR_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String ALIYUN_SIGNATURE_METHOD = "HmacSHA256";
    private static final String ALIYUN_SIGNATURE_HEADERS = "x-ca-key,x-ca-nonce,x-ca-signature-method,x-ca-timestamp";
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern FACTORY_ORDER_TOKEN_PATTERN = Pattern.compile("^(S[A-Za-z0-9_-]+)");
    private static final Pattern MERGED_FACTORY_ORDER_PATTERN =
            Pattern.compile("^(S(?:\\d{12}|\\d{9}))\\d{10}(?=\\s*(?:\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|$))");
    private static final String INTERNAL_FILE_URL_PREFIX = "/infra/file/";
    private static final List<String> MATCHABLE_ITEM_STATUSES = Arrays.asList(
            ErpPurchaseInvoiceOcrItemStatusEnum.PENDING.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.NO_PURCHASE_IN.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.AMOUNT_DIFF.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.MATCHED.getStatus());
    private static final List<String> ERROR_ITEM_STATUSES = Arrays.asList(
            ErpPurchaseInvoiceOcrItemStatusEnum.OCR_FAILED.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.NO_FACTORY_NO.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.NO_PURCHASE_IN.getStatus(),
            ErpPurchaseInvoiceOcrItemStatusEnum.AMOUNT_DIFF.getStatus());

    @Resource
    private ErpPurchaseInvoiceOcrBatchMapper purchaseInvoiceOcrBatchMapper;
    @Resource
    private ErpPurchaseInvoiceOcrItemMapper purchaseInvoiceOcrItemMapper;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private FileApi fileApi;
    @Resource
    private FileService fileService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${yudao.erp.purchase-invoice-ocr.endpoint:" + DEFAULT_ENDPOINT + "}")
    private String endpoint;
    @Value("${yudao.erp.purchase-invoice-ocr.app-key:}")
    private String appKey;
    @Value("${yudao.erp.purchase-invoice-ocr.app-secret:}")
    private String appSecret;
    @Value("${yudao.erp.purchase-invoice-ocr.app-code:}")
    private String appCode;
    @Value("${yudao.erp.purchase-invoice-ocr.timeout:8000}")
    private int timeout;
    @Value("${yudao.erp.purchase-invoice-ocr.file-url-expiration-seconds:300}")
    private Integer fileUrlExpirationSeconds;
    @Value("${yudao.erp.purchase-invoice-ocr.amount-tolerance:0.01}")
    private BigDecimal amountTolerance;
    @Value("${yudao.erp.purchase-invoice-ocr.use-system-proxy:false}")
    private boolean useSystemProxy;

    @Override
    public Long uploadPurchaseInvoiceOcrBatch(ErpPurchaseInvoiceOcrUploadReqVO uploadReqVO) {
        if (isAutoProcess(uploadReqVO) && !hasInvoiceOcrCredential()) {
            throw exception(PURCHASE_INVOICE_OCR_APP_CODE_NOT_CONFIGURED);
        }
        Long batchId = transactionTemplate.execute(status -> createPurchaseInvoiceOcrBatch(uploadReqVO));
        if (batchId != null && isAutoProcess(uploadReqVO)) {
            autoProcessPurchaseInvoiceOcrBatch(batchId);
        }
        return batchId;
    }

    private Long createPurchaseInvoiceOcrBatch(ErpPurchaseInvoiceOcrUploadReqVO uploadReqVO) {
        ErpPurchaseInvoiceOcrBatchDO batch = ErpPurchaseInvoiceOcrBatchDO.builder()
                .status(ErpPurchaseInvoiceOcrBatchStatusEnum.DRAFT.getStatus())
                .fileCount(uploadReqVO.getFiles().size())
                .generatedInvoiceCount(0)
                .remark(uploadReqVO.getRemark())
                .build();
        purchaseInvoiceOcrBatchMapper.insert(batch);

        batch.setBatchNo(buildBatchNo(batch.getId()));
        purchaseInvoiceOcrBatchMapper.updateById(batch);

        List<ErpPurchaseInvoiceOcrItemDO> items = BeanUtils.toBean(uploadReqVO.getFiles(),
                ErpPurchaseInvoiceOcrItemDO.class, item -> item
                        .setBatchId(batch.getId())
                        .setStatus(ErpPurchaseInvoiceOcrItemStatusEnum.PENDING.getStatus()));
        purchaseInvoiceOcrItemMapper.insertBatch(items);
        return batch.getId();
    }

    private boolean isAutoProcess(ErpPurchaseInvoiceOcrUploadReqVO uploadReqVO) {
        return !Boolean.FALSE.equals(uploadReqVO.getAutoProcess());
    }

    private void autoProcessPurchaseInvoiceOcrBatch(Long batchId) {
        try {
            recognizePurchaseInvoiceOcrBatch(batchId);
        } catch (Exception ex) {
            log.warn("Auto recognize purchase invoice OCR batch failed, batchId: {}", batchId, ex);
            updateAutoProcessFailedBatch(batchId, ErpPurchaseInvoiceOcrBatchStatusEnum.FAILED.getStatus(),
                    "自动识别失败", ex);
            return;
        }
        if (!canAutoMatch(batchId)) {
            updateAutoNoMatchableBatchError(batchId);
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> matchPurchaseInvoiceOcrBatch(batchId));
        } catch (Exception ex) {
            log.warn("Auto match purchase invoice OCR batch failed, batchId: {}", batchId, ex);
            updateAutoProcessFailedBatch(batchId, ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus(),
                    "自动匹配失败", ex);
        }
    }

    private boolean canAutoMatch(Long batchId) {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrBatchMapper.selectById(batchId);
        if (batch == null || !ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus().equals(batch.getStatus())) {
            return false;
        }
        return purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId).stream().anyMatch(this::isMatchableItem);
    }

    private void updateAutoNoMatchableBatchError(Long batchId) {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrBatchMapper.selectById(batchId);
        if (batch == null || !ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus().equals(batch.getStatus())) {
            return;
        }
        String errorMsg = appendErrorMessage(batch.getErrorMsg(), "识别完成，但没有可自动匹配明细，请检查厂家单号和发票金额");
        updateBatchStatus(batchId, batch.getStatus(), errorMsg);
    }

    private void updateAutoProcessFailedBatch(Long batchId, String status, String action, Exception ex) {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrBatchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        String reason = StrUtil.blankToDefault(ex.getMessage(), "处理失败");
        String errorMsg = appendErrorMessage(batch.getErrorMsg(),
                action + "：" + limitText(reason, 1000));
        updateBatchStatus(batchId, status, errorMsg);
    }

    @Override
    public ErpPurchaseInvoiceOcrBatchDO getPurchaseInvoiceOcrBatch(Long id) {
        return purchaseInvoiceOcrBatchMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchaseInvoiceOcrBatchDO> getPurchaseInvoiceOcrBatchPage(
            ErpPurchaseInvoiceOcrBatchPageReqVO pageReqVO) {
        return purchaseInvoiceOcrBatchMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpPurchaseInvoiceOcrBatchDO validatePurchaseInvoiceOcrBatch(Long id) {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrBatchMapper.selectById(id);
        if (batch == null) {
            throw exception(PURCHASE_INVOICE_OCR_BATCH_NOT_EXISTS);
        }
        return batch;
    }

    @Override
    public List<ErpPurchaseInvoiceOcrItemDO> getPurchaseInvoiceOcrItemListByBatchId(Long batchId) {
        return purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId);
    }

    @Override
    public Boolean recognizePurchaseInvoiceOcrBatch(Long batchId) {
        if (!hasInvoiceOcrCredential()) {
            throw exception(PURCHASE_INVOICE_OCR_APP_CODE_NOT_CONFIGURED);
        }

        ErpPurchaseInvoiceOcrBatchDO batch = validatePurchaseInvoiceOcrBatch(batchId);
        validateRecognizable(batch);

        List<ErpPurchaseInvoiceOcrItemDO> items = purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId);
        updateBatchStatus(batchId, ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZING.getStatus(), null);
        if (items.isEmpty()) {
            updateBatchStatus(batchId, ErpPurchaseInvoiceOcrBatchStatusEnum.FAILED.getStatus(), "批次没有可识别文件");
            return true;
        }

        int recognizedCount = 0;
        int failedCount = 0;
        for (ErpPurchaseInvoiceOcrItemDO item : items) {
            try {
                byte[] fileContent = downloadInvoiceFile(item);
                String rawJson = callInvoiceOcr(fileContent);
                OcrParseResult parseResult = parseOcrResponse(rawJson);
                String factoryOrderNo = parseFactoryOrderNo(parseResult.getInvoiceRemark());
                String status = StrUtil.isBlank(factoryOrderNo)
                        ? ErpPurchaseInvoiceOcrItemStatusEnum.NO_FACTORY_NO.getStatus()
                        : ErpPurchaseInvoiceOcrItemStatusEnum.PENDING.getStatus();
                updateRecognizedItem(item.getId(), status, parseResult, factoryOrderNo, rawJson);
                recognizedCount++;
            } catch (Exception ex) {
                failedCount++;
                log.warn("Purchase invoice OCR item recognize failed, batchId: {}, itemId: {}, fileUrl: {}",
                        batchId, item.getId(), item.getFileUrl(), ex);
                updateFailedItem(item.getId(), limitErrorMessage(ex.getMessage()));
            }
        }

        String batchStatus = recognizedCount > 0
                ? ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus()
                : ErpPurchaseInvoiceOcrBatchStatusEnum.FAILED.getStatus();
        String errorMsg = failedCount > 0 ? "识别完成，失败 " + failedCount + " 个文件" : null;
        updateBatchStatus(batchId, batchStatus, errorMsg);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean matchPurchaseInvoiceOcrBatch(Long batchId) {
        ErpPurchaseInvoiceOcrBatchDO batch = validatePurchaseInvoiceOcrBatch(batchId);
        validateMatchable(batch);

        List<ErpPurchaseInvoiceOcrItemDO> items = purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId);
        List<ErpPurchaseInvoiceOcrItemDO> matchableItems = items.stream()
                .filter(this::isMatchableItem)
                .collect(Collectors.toList());
        if (matchableItems.isEmpty()) {
            throw exception(PURCHASE_INVOICE_OCR_NO_MATCHABLE_ITEMS);
        }

        Map<String, List<ErpPurchaseInvoiceOcrItemDO>> invoiceItemMap = matchableItems.stream()
                .collect(Collectors.groupingBy(ErpPurchaseInvoiceOcrItemDO::getFactoryOrderNo,
                        java.util.LinkedHashMap::new, Collectors.toList()));
        Set<String> factoryOrderNos = invoiceItemMap.keySet();
        Map<String, List<ErpPurchaseInDO>> purchaseInMap = purchaseInService
                .getPurchaseInListByFactoryOrderNos(factoryOrderNos).stream()
                .filter(purchaseIn -> ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus()))
                .collect(Collectors.groupingBy(ErpPurchaseInDO::getFactoryOrderNo));

        int exceptionFactoryOrderNoCount = 0;
        for (Map.Entry<String, List<ErpPurchaseInvoiceOcrItemDO>> entry : invoiceItemMap.entrySet()) {
            String factoryOrderNo = entry.getKey();
            List<ErpPurchaseInvoiceOcrItemDO> sameFactoryItems = entry.getValue();
            BigDecimal invoiceTotalAmount = sumInvoiceAmount(sameFactoryItems);
            List<ErpPurchaseInDO> purchaseIns = purchaseInMap.get(factoryOrderNo);
            if (purchaseIns == null || purchaseIns.isEmpty()) {
                exceptionFactoryOrderNoCount++;
                updateMatchedItems(sameFactoryItems, ErpPurchaseInvoiceOcrItemStatusEnum.NO_PURCHASE_IN.getStatus(),
                        null, null, null, null, "未找到已审核的采购入库单");
                continue;
            }

            BigDecimal purchaseInTotalAmount = sumPurchaseInAmount(purchaseIns);
            BigDecimal diffAmount = invoiceTotalAmount.subtract(purchaseInTotalAmount);
            String matchedPurchaseInIds = joinPurchaseInIds(purchaseIns);
            String matchedPurchaseInNos = joinPurchaseInNos(purchaseIns);
            if (diffAmount.abs().compareTo(getAmountTolerance()) <= 0) {
                updateMatchedItems(sameFactoryItems, ErpPurchaseInvoiceOcrItemStatusEnum.MATCHED.getStatus(),
                        matchedPurchaseInIds, matchedPurchaseInNos, purchaseInTotalAmount, BigDecimal.ZERO, null);
            } else {
                exceptionFactoryOrderNoCount++;
                updateMatchedItems(sameFactoryItems, ErpPurchaseInvoiceOcrItemStatusEnum.AMOUNT_DIFF.getStatus(),
                        matchedPurchaseInIds, matchedPurchaseInNos, purchaseInTotalAmount, diffAmount,
                        "发票金额合计与采购入库金额合计不一致");
            }
        }

        String batchStatus = exceptionFactoryOrderNoCount == 0
                ? ErpPurchaseInvoiceOcrBatchStatusEnum.MATCHED.getStatus()
                : ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus();
        String errorMsg = exceptionFactoryOrderNoCount > 0
                ? "匹配完成，异常厂家单号 " + exceptionFactoryOrderNoCount + " 个"
                : null;
        updateBatchStatus(batchId, batchStatus, errorMsg);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmPurchaseInvoiceOcrBatch(Long batchId) {
        ErpPurchaseInvoiceOcrBatchDO batch = validatePurchaseInvoiceOcrBatch(batchId);
        validateConfirmable(batch);

        List<ErpPurchaseInvoiceOcrItemDO> items = purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId);
        List<ErpPurchaseInvoiceOcrItemDO> matchedItems = items.stream()
                .filter(this::isConfirmableItem)
                .collect(Collectors.toList());
        if (matchedItems.isEmpty()) {
            throw exception(PURCHASE_INVOICE_OCR_NO_CONFIRMED_ITEMS);
        }

        Map<String, List<ErpPurchaseInvoiceOcrItemDO>> invoiceItemMap = matchedItems.stream()
                .collect(Collectors.groupingBy(ErpPurchaseInvoiceOcrItemDO::getFactoryOrderNo,
                        java.util.LinkedHashMap::new, Collectors.toList()));
        int generatedInvoiceCount = 0;
        for (Map.Entry<String, List<ErpPurchaseInvoiceOcrItemDO>> entry : invoiceItemMap.entrySet()) {
            String factoryOrderNo = entry.getKey();
            List<ErpPurchaseInvoiceOcrItemDO> sameFactoryItems = entry.getValue();
            List<Long> purchaseInIds = parseMatchedPurchaseInIds(sameFactoryItems);
            List<ErpPurchaseInDO> purchaseIns = validateConfirmPurchaseIns(factoryOrderNo, purchaseInIds);
            List<ErpPurchaseInItemDO> purchaseInItems = purchaseInService.getPurchaseInItemListByInIds(purchaseInIds)
                    .stream()
                    .sorted(Comparator.comparing(ErpPurchaseInItemDO::getInId)
                            .thenComparing(ErpPurchaseInItemDO::getId))
                    .collect(Collectors.toList());
            if (purchaseInItems.isEmpty()) {
                throw exception(PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED, factoryOrderNo);
            }

            Long invoiceId = purchaseInvoiceService.createPurchaseInvoice(
                    buildPurchaseInvoiceSaveReq(batch, factoryOrderNo, sameFactoryItems, purchaseIns, purchaseInItems));
            updateConfirmedItems(sameFactoryItems, invoiceId);
            generatedInvoiceCount++;
        }

        purchaseInvoiceOcrBatchMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrBatchDO>()
                .set(ErpPurchaseInvoiceOcrBatchDO::getStatus, ErpPurchaseInvoiceOcrBatchStatusEnum.CONFIRMED.getStatus())
                .set(ErpPurchaseInvoiceOcrBatchDO::getGeneratedInvoiceCount, generatedInvoiceCount)
                .set(ErpPurchaseInvoiceOcrBatchDO::getErrorMsg, null)
                .eq(ErpPurchaseInvoiceOcrBatchDO::getId, batchId));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePurchaseInvoiceOcrItemFactoryOrderNo(Long itemId,
                                                              ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO updateReqVO) {
        ErpPurchaseInvoiceOcrItemDO item = purchaseInvoiceOcrItemMapper.selectById(itemId);
        if (item == null) {
            throw exception(PURCHASE_INVOICE_OCR_ITEM_NOT_EXISTS);
        }
        validateFactoryOrderNoUpdatable(item);

        String factoryOrderNo = StrUtil.trim(updateReqVO.getFactoryOrderNo());
        if (!factoryOrderNo.startsWith("S")) {
            throw exception(PURCHASE_INVOICE_OCR_FACTORY_ORDER_NO_INVALID);
        }

        purchaseInvoiceOcrItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrItemDO>()
                .set(ErpPurchaseInvoiceOcrItemDO::getFactoryOrderNo, factoryOrderNo)
                .set(ErpPurchaseInvoiceOcrItemDO::getStatus, ErpPurchaseInvoiceOcrItemStatusEnum.PENDING.getStatus())
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInIds, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInNos, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getPurchaseInTotalAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getDiffAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getErrorMsg, null)
                .eq(ErpPurchaseInvoiceOcrItemDO::getId, itemId));
        updateBatchStatus(item.getBatchId(), ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus(), null);
        return matchPurchaseInvoiceOcrBatch(item.getBatchId());
    }

    @Override
    public List<ErpPurchaseInvoiceOcrItemDO> getPurchaseInvoiceOcrErrorItemList(Long batchId) {
        validatePurchaseInvoiceOcrBatch(batchId);
        return purchaseInvoiceOcrItemMapper.selectListByBatchId(batchId).stream()
                .filter(item -> ERROR_ITEM_STATUSES.contains(item.getStatus()) || StrUtil.isNotBlank(item.getErrorMsg()))
                .collect(Collectors.toList());
    }

    private void validateRecognizable(ErpPurchaseInvoiceOcrBatchDO batch) {
        String status = batch.getStatus();
        if (ErpPurchaseInvoiceOcrBatchStatusEnum.DRAFT.getStatus().equals(status)
                || ErpPurchaseInvoiceOcrBatchStatusEnum.FAILED.getStatus().equals(status)
                || ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus().equals(status)) {
            return;
        }
        throw exception(PURCHASE_INVOICE_OCR_RECOGNIZE_STATUS_NOT_SUPPORT);
    }

    private void validateMatchable(ErpPurchaseInvoiceOcrBatchDO batch) {
        String status = batch.getStatus();
        if (ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZED.getStatus().equals(status)
                || ErpPurchaseInvoiceOcrBatchStatusEnum.MATCHED.getStatus().equals(status)) {
            return;
        }
        throw exception(PURCHASE_INVOICE_OCR_MATCH_STATUS_NOT_SUPPORT);
    }

    private void validateConfirmable(ErpPurchaseInvoiceOcrBatchDO batch) {
        if (ErpPurchaseInvoiceOcrBatchStatusEnum.MATCHED.getStatus().equals(batch.getStatus())) {
            return;
        }
        throw exception(PURCHASE_INVOICE_OCR_CONFIRM_STATUS_NOT_SUPPORT);
    }

    private void validateFactoryOrderNoUpdatable(ErpPurchaseInvoiceOcrItemDO item) {
        ErpPurchaseInvoiceOcrBatchDO batch = validatePurchaseInvoiceOcrBatch(item.getBatchId());
        if (ErpPurchaseInvoiceOcrBatchStatusEnum.CONFIRMED.getStatus().equals(batch.getStatus())
                || ErpPurchaseInvoiceOcrBatchStatusEnum.RECOGNIZING.getStatus().equals(batch.getStatus())
                || ErpPurchaseInvoiceOcrItemStatusEnum.CONFIRMED.getStatus().equals(item.getStatus())
                || ErpPurchaseInvoiceOcrItemStatusEnum.IGNORED.getStatus().equals(item.getStatus())
                || ErpPurchaseInvoiceOcrItemStatusEnum.OCR_FAILED.getStatus().equals(item.getStatus())
                || item.getGeneratedInvoiceId() != null
                || item.getTotalAmount() == null) {
            throw exception(PURCHASE_INVOICE_OCR_UPDATE_FACTORY_ORDER_NO_NOT_SUPPORT);
        }
    }

    private boolean isMatchableItem(ErpPurchaseInvoiceOcrItemDO item) {
        return item != null
                && MATCHABLE_ITEM_STATUSES.contains(item.getStatus())
                && StrUtil.isNotBlank(item.getFactoryOrderNo())
                && item.getTotalAmount() != null;
    }

    private boolean isConfirmableItem(ErpPurchaseInvoiceOcrItemDO item) {
        return item != null
                && ErpPurchaseInvoiceOcrItemStatusEnum.MATCHED.getStatus().equals(item.getStatus())
                && item.getGeneratedInvoiceId() == null
                && StrUtil.isNotBlank(item.getFactoryOrderNo());
    }

    private List<Long> parseMatchedPurchaseInIds(Collection<ErpPurchaseInvoiceOcrItemDO> items) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ErpPurchaseInvoiceOcrItemDO item : items) {
            if (StrUtil.isBlank(item.getMatchedPurchaseInIds())) {
                continue;
            }
            for (String idText : item.getMatchedPurchaseInIds().split(",")) {
                String trimmed = StrUtil.trim(idText);
                if (StrUtil.isBlank(trimmed)) {
                    continue;
                }
                ids.add(Long.valueOf(trimmed));
            }
        }
        return new ArrayList<>(ids);
    }

    private List<ErpPurchaseInDO> validateConfirmPurchaseIns(String factoryOrderNo, List<Long> purchaseInIds) {
        if (purchaseInIds.isEmpty()) {
            throw exception(PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED, factoryOrderNo);
        }
        Map<Long, ErpPurchaseInDO> purchaseInMap = purchaseInService.getPurchaseInList(purchaseInIds).stream()
                .collect(Collectors.toMap(ErpPurchaseInDO::getId, purchaseIn -> purchaseIn, (a, b) -> a));
        if (purchaseInMap.size() != purchaseInIds.size()) {
            throw exception(PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED, factoryOrderNo);
        }
        List<ErpPurchaseInvoiceItemDO> approvedInvoiceItems =
                purchaseInvoiceService.getPurchaseInvoiceItemListBySourceInIds(purchaseInIds);
        if (!approvedInvoiceItems.isEmpty()) {
            throw exception(PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED, factoryOrderNo);
        }
        List<ErpPurchaseInDO> purchaseIns = new ArrayList<>();
        for (Long purchaseInId : purchaseInIds) {
            ErpPurchaseInDO purchaseIn = purchaseInMap.get(purchaseInId);
            if (!ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())
                    || Boolean.TRUE.equals(purchaseIn.getHasInvoice())
                    || !factoryOrderNo.equals(purchaseIn.getFactoryOrderNo())) {
                throw exception(PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED, factoryOrderNo);
            }
            purchaseIns.add(purchaseIn);
        }
        Set<Long> supplierIds = purchaseIns.stream()
                .map(ErpPurchaseInDO::getSupplierId)
                .collect(Collectors.toSet());
        if (supplierIds.size() != 1 || supplierIds.contains(null)) {
            throw exception(PURCHASE_INVOICE_OCR_CONFIRM_SUPPLIER_NOT_UNIQUE, factoryOrderNo);
        }
        return purchaseIns;
    }

    private ErpPurchaseInvoiceSaveReqVO buildPurchaseInvoiceSaveReq(ErpPurchaseInvoiceOcrBatchDO batch,
                                                                    String factoryOrderNo,
                                                                    List<ErpPurchaseInvoiceOcrItemDO> ocrItems,
                                                                    List<ErpPurchaseInDO> purchaseIns,
                                                                    List<ErpPurchaseInItemDO> purchaseInItems) {
        LocalDate invoiceDate = ocrItems.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getInvoiceDate)
                .filter(date -> date != null)
                .findFirst()
                .orElse(null);
        String invoiceType = firstNotBlank(ocrItems, ErpPurchaseInvoiceOcrItemDO::getInvoiceType);
        String invoiceNo = joinDistinctInvoiceNos(ocrItems);
        if (invoiceDate == null || StrUtil.isBlank(invoiceType) || StrUtil.isBlank(invoiceNo)) {
            throw exception(PURCHASE_INVOICE_OCR_CONFIRM_INVOICE_FIELD_REQUIRED, factoryOrderNo);
        }

        ErpPurchaseInvoiceSaveReqVO saveReqVO = new ErpPurchaseInvoiceSaveReqVO();
        saveReqVO.setSupplierId(purchaseIns.get(0).getSupplierId());
        saveReqVO.setInvoiceDate(invoiceDate);
        saveReqVO.setInvoiceType(invoiceType);
        saveReqVO.setInvoiceNo(invoiceNo);
        saveReqVO.setInvoiceCount(ocrItems.size());
        saveReqVO.setRemark(limitText(buildConfirmRemark(batch, factoryOrderNo, invoiceNo), 512));
        saveReqVO.setFileUrl(limitText(joinDistinctFileUrls(ocrItems), 512));

        Map<Long, ErpPurchaseInDO> purchaseInMap = purchaseIns.stream()
                .collect(Collectors.toMap(ErpPurchaseInDO::getId, purchaseIn -> purchaseIn, (a, b) -> a));
        saveReqVO.setItems(purchaseInItems.stream()
                .map(item -> buildPurchaseInvoiceItem(item, purchaseInMap.get(item.getInId())))
                .collect(Collectors.toList()));
        return saveReqVO;
    }

    private ErpPurchaseInvoiceSaveReqVO.Item buildPurchaseInvoiceItem(ErpPurchaseInItemDO purchaseInItem,
                                                                      ErpPurchaseInDO purchaseIn) {
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setSourceInId(purchaseInItem.getInId());
        item.setSourceInNo(purchaseIn == null ? null : purchaseIn.getNo());
        item.setSourceInItemId(purchaseInItem.getId());
        item.setProductId(purchaseInItem.getProductId());
        item.setCount(purchaseInItem.getCount());
        item.setProductPrice(purchaseInItem.getProductPrice() == null ? BigDecimal.ZERO : purchaseInItem.getProductPrice());
        item.setTaxPercent(null);
        item.setRemark(purchaseInItem.getRemark());
        return item;
    }

    private String buildConfirmRemark(ErpPurchaseInvoiceOcrBatchDO batch, String factoryOrderNo, String invoiceNo) {
        return "OCR批次：" + batch.getBatchNo() + "；厂家单号：" + factoryOrderNo + "；发票号：" + invoiceNo;
    }

    private String joinDistinctInvoiceNos(Collection<ErpPurchaseInvoiceOcrItemDO> items) {
        return joinDistinctText(items.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getInvoiceNo)
                .collect(Collectors.toList()), 256);
    }

    private String joinDistinctFileUrls(Collection<ErpPurchaseInvoiceOcrItemDO> items) {
        return joinDistinctText(items.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getFileUrl)
                .collect(Collectors.toList()), 512);
    }

    private String joinDistinctText(Collection<String> values, int maxLength) {
        List<String> texts = values.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .distinct()
                .collect(Collectors.toList());
        if (texts.isEmpty()) {
            return null;
        }
        String joined = String.join(",", texts);
        return limitText(joined, maxLength);
    }

    private String firstNotBlank(Collection<ErpPurchaseInvoiceOcrItemDO> items,
                                 java.util.function.Function<ErpPurchaseInvoiceOcrItemDO, String> getter) {
        return items.stream()
                .map(getter)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .findFirst()
                .orElse(null);
    }

    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private void updateConfirmedItems(Collection<ErpPurchaseInvoiceOcrItemDO> items, Long invoiceId) {
        List<Long> itemIds = items.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return;
        }
        purchaseInvoiceOcrItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrItemDO>()
                .set(ErpPurchaseInvoiceOcrItemDO::getStatus, ErpPurchaseInvoiceOcrItemStatusEnum.CONFIRMED.getStatus())
                .set(ErpPurchaseInvoiceOcrItemDO::getGeneratedInvoiceId, invoiceId)
                .set(ErpPurchaseInvoiceOcrItemDO::getErrorMsg, null)
                .in(ErpPurchaseInvoiceOcrItemDO::getId, itemIds));
    }

    private BigDecimal sumInvoiceAmount(Collection<ErpPurchaseInvoiceOcrItemDO> items) {
        return items.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPurchaseInAmount(Collection<ErpPurchaseInDO> purchaseIns) {
        return purchaseIns.stream()
                .map(ErpPurchaseInDO::getTotalPrice)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String joinPurchaseInIds(Collection<ErpPurchaseInDO> purchaseIns) {
        return purchaseIns.stream()
                .map(ErpPurchaseInDO::getId)
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String joinPurchaseInNos(Collection<ErpPurchaseInDO> purchaseIns) {
        return purchaseIns.stream()
                .map(ErpPurchaseInDO::getNo)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private BigDecimal getAmountTolerance() {
        return amountTolerance == null ? new BigDecimal("0.01") : amountTolerance.abs();
    }

    private void updateMatchedItems(Collection<ErpPurchaseInvoiceOcrItemDO> items, String status,
                                    String matchedPurchaseInIds, String matchedPurchaseInNos,
                                    BigDecimal purchaseInTotalAmount, BigDecimal diffAmount,
                                    String errorMsg) {
        List<Long> itemIds = items.stream()
                .map(ErpPurchaseInvoiceOcrItemDO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return;
        }
        purchaseInvoiceOcrItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrItemDO>()
                .set(ErpPurchaseInvoiceOcrItemDO::getStatus, status)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInIds, matchedPurchaseInIds)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInNos, matchedPurchaseInNos)
                .set(ErpPurchaseInvoiceOcrItemDO::getPurchaseInTotalAmount, purchaseInTotalAmount)
                .set(ErpPurchaseInvoiceOcrItemDO::getDiffAmount, diffAmount)
                .set(ErpPurchaseInvoiceOcrItemDO::getErrorMsg, errorMsg)
                .in(ErpPurchaseInvoiceOcrItemDO::getId, itemIds));
    }

    private byte[] downloadInvoiceFile(ErpPurchaseInvoiceOcrItemDO item) {
        byte[] internalContent = readInternalInvoiceFile(item.getFileUrl());
        if (internalContent != null) {
            return internalContent;
        }
        String downloadUrl = resolveDownloadUrl(item.getFileUrl());
        if (StrUtil.isBlank(downloadUrl)) {
            throw new IllegalStateException("发票文件地址为空");
        }
        try (HttpResponse response = prepareInvoiceOcrRequest(HttpRequest.get(downloadUrl))
                .timeout(timeout)
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("HTTP " + response.getStatus());
            }
            byte[] content = response.bodyBytes();
            if (content == null || content.length == 0) {
                throw new IllegalStateException("发票文件内容为空");
            }
            return content;
        } catch (Exception ex) {
            throw new IllegalStateException("下载发票文件失败：" + limitErrorMessage(ex.getMessage()), ex);
        }
    }

    private byte[] readInternalInvoiceFile(String fileUrl) {
        InternalFileLocation location = parseInternalFileLocation(fileUrl);
        if (location == null) {
            return null;
        }
        try {
            byte[] content = fileService.getFileContent(location.getConfigId(), location.getPath());
            if (content == null || content.length == 0) {
                throw new IllegalStateException("发票文件内容为空");
            }
            return content;
        } catch (Exception ex) {
            throw new IllegalStateException("读取系统发票文件失败：" + limitErrorMessage(ex.getMessage()), ex);
        }
    }

    InternalFileLocation parseInternalFileLocation(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return null;
        }
        String urlWithoutQuery = StrUtil.subBefore(fileUrl, "?", false);
        int prefixIndex = urlWithoutQuery.indexOf(INTERNAL_FILE_URL_PREFIX);
        if (prefixIndex < 0) {
            return null;
        }
        String filePath = urlWithoutQuery.substring(prefixIndex + INTERNAL_FILE_URL_PREFIX.length());
        int getIndex = filePath.indexOf("/get/");
        if (getIndex <= 0) {
            return null;
        }
        String configIdText = filePath.substring(0, getIndex);
        String path = filePath.substring(getIndex + "/get/".length());
        if (StrUtil.isBlank(path)) {
            return null;
        }
        try {
            Long configId = Long.valueOf(configIdText);
            return new InternalFileLocation(configId, URLUtil.decode(path, StandardCharsets.UTF_8, false));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveDownloadUrl(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return fileUrl;
        }
        try {
            String presignedUrl = fileApi.presignGetUrl(fileUrl, fileUrlExpirationSeconds);
            if (StrUtil.isNotBlank(presignedUrl)) {
                return presignedUrl;
            }
        } catch (Exception ex) {
            log.warn("Presign purchase invoice OCR file url failed, fileUrl: {}", fileUrl, ex);
        }
        return fileUrl;
    }

    private String callInvoiceOcr(byte[] fileContent) {
        String requestBody = JSONUtil.createObj()
                .set("img", Base64.getEncoder().encodeToString(fileContent))
                .toString();
        HttpRequest request = prepareInvoiceOcrRequest(HttpRequest.post(endpoint))
                .header("Accept", OCR_ACCEPT)
                .header("Content-Type", OCR_CONTENT_TYPE)
                .body(requestBody)
                .timeout(timeout);
        applyInvoiceOcrAuth(request);
        try (HttpResponse response = request.execute()) {
            String body = response.body();
            if (!response.isOk()) {
                throw new IllegalStateException("HTTP " + response.getStatus()
                        + buildInvoiceOcrHttpFailureDetail(body));
            }
            if (StrUtil.isBlank(body)) {
                throw new IllegalStateException("OCR 接口返回为空");
            }
            return body;
        } catch (Exception ex) {
            throw new IllegalStateException("调用发票 OCR 接口失败：" + limitErrorMessage(ex.getMessage()), ex);
        }
    }

    private boolean hasInvoiceOcrCredential() {
        return StrUtil.isNotBlank(appCode) || hasSignatureCredential();
    }

    private boolean hasSignatureCredential() {
        return StrUtil.isAllNotBlank(appKey, appSecret);
    }

    private void applyInvoiceOcrAuth(HttpRequest request) {
        if (StrUtil.isNotBlank(appCode)) {
            request.header("Authorization", "APPCODE " + appCode);
            return;
        }
        if (hasSignatureCredential()) {
            buildAliyunApiGatewaySignatureHeaders(appKey, appSecret, "POST", endpoint, OCR_ACCEPT, OCR_CONTENT_TYPE,
                    String.valueOf(System.currentTimeMillis()), UUID.randomUUID().toString())
                    .forEach(request::header);
        }
    }

    private String buildInvoiceOcrHttpFailureDetail(String body) {
        if (StrUtil.isBlank(body)) {
            return "";
        }
        return "，响应：" + limitErrorMessage(body);
    }

    static Map<String, String> buildAliyunApiGatewaySignatureHeaders(String appKey, String appSecret,
                                                                     String method, String requestUrl,
                                                                     String accept, String contentType,
                                                                     String timestamp, String nonce) {
        Map<String, String> signedHeaders = new LinkedHashMap<>();
        signedHeaders.put("x-ca-key", appKey);
        signedHeaders.put("x-ca-nonce", nonce);
        signedHeaders.put("x-ca-signature-method", ALIYUN_SIGNATURE_METHOD);
        signedHeaders.put("x-ca-timestamp", timestamp);

        String signedHeaderText = signedHeaders.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("\n"));
        String stringToSign = method.toUpperCase(Locale.ROOT) + "\n"
                + StrUtil.nullToEmpty(accept) + "\n"
                + "\n"
                + StrUtil.nullToEmpty(contentType) + "\n"
                + "\n"
                + signedHeaderText + "\n"
                + buildPathAndSortedQuery(requestUrl);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Ca-Key", appKey);
        headers.put("X-Ca-Nonce", nonce);
        headers.put("X-Ca-Signature-Method", ALIYUN_SIGNATURE_METHOD);
        headers.put("X-Ca-Timestamp", timestamp);
        headers.put("X-Ca-Signature-Headers", ALIYUN_SIGNATURE_HEADERS);
        headers.put("X-Ca-Signature", hmacSha256Base64(appSecret, stringToSign));
        return headers;
    }

    private static String buildPathAndSortedQuery(String requestUrl) {
        try {
            URI uri = new URI(requestUrl);
            String path = StrUtil.blankToDefault(uri.getRawPath(), "/");
            String query = uri.getRawQuery();
            if (StrUtil.isBlank(query)) {
                return path;
            }
            return path + "?" + Arrays.stream(query.split("&"))
                    .sorted()
                    .collect(Collectors.joining("&"));
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("发票 OCR 接口地址不合法", ex);
        }
    }

    private static String hmacSha256Base64(String secret, String text) {
        try {
            Mac mac = Mac.getInstance(ALIYUN_SIGNATURE_METHOD);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALIYUN_SIGNATURE_METHOD));
            return Base64.getEncoder().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("生成发票 OCR 签名失败", ex);
        }
    }

    private HttpRequest prepareInvoiceOcrRequest(HttpRequest request) {
        if (!useSystemProxy) {
            request.setProxy(Proxy.NO_PROXY);
        }
        return request;
    }

    private OcrParseResult parseOcrResponse(String rawJson) {
        JSONObject rawObject = JSONUtil.parseObj(rawJson);
        validateOcrResponse(rawObject);

        OcrParseResult result = new OcrParseResult();
        result.setInvoiceNo(toText(findFirstValue(rawObject, "invoiceNo", "invoice_no", "fphm", "发票号码", "发票号")));
        result.setInvoiceDate(parseDate(findFirstValue(rawObject, "invoiceDate", "invoice_date", "kprq", "开票日期", "发票日期")));
        result.setInvoiceType(toText(findFirstValue(rawObject, "invoiceType", "invoice_type", "fplx", "发票类型", "发票种类")));
        result.setTotalAmount(parseAmount(findFirstValue(rawObject, "totalAmount", "total_amount", "invoiceAmount",
                "amountWithTax", "amount_with_tax", "sumAmount", "价税合计", "小写金额", "合计金额", "发票金额", "总金额")));
        result.setTaxExclusiveAmount(parseAmount(findFirstValue(rawObject, "amountWithoutTax", "amount_without_tax",
                "taxExclusiveAmount", "tax_exclusive_amount", "noTaxAmount", "不含税金额", "金额合计")));
        result.setTaxAmount(parseAmount(findFirstValue(rawObject, "taxAmount", "tax_amount", "se", "税额合计", "税额")));
        result.setInvoiceRemark(toText(findFirstValue(rawObject, "remark", "remarks", "invoiceRemark",
                "invoice_remark", "bz", "备注", "备注信息")));

        if (result.getTotalAmount() == null
                && result.getTaxExclusiveAmount() != null
                && result.getTaxAmount() != null) {
            result.setTotalAmount(result.getTaxExclusiveAmount().add(result.getTaxAmount()));
        }
        return result;
    }

    private void validateOcrResponse(JSONObject rawObject) {
        Object success = rawObject.get("success");
        if (Boolean.FALSE.equals(success)) {
            throw new IllegalStateException("OCR 接口返回失败");
        }
        Object errorCode = firstNonNull(rawObject.get("error_code"), rawObject.get("errorCode"));
        if (errorCode == null) {
            return;
        }
        String errorCodeText = String.valueOf(errorCode);
        if (!"0".equals(errorCodeText) && !"200".equals(errorCodeText)) {
            Object errorMessage = firstNonNull(rawObject.get("error_msg"), rawObject.get("errorMsg"), rawObject.get("msg"));
            throw new IllegalStateException("OCR 接口返回失败：" + toText(errorMessage));
        }
    }

    private Object findFirstValue(Object source, String... keys) {
        List<String> normalizedKeys = new ArrayList<>(keys.length);
        for (String key : keys) {
            normalizedKeys.add(normalizeJsonKey(key));
        }
        return findFirstValue(source, normalizedKeys);
    }

    private Object findFirstValue(Object source, List<String> normalizedKeys) {
        if (source instanceof JSONObject) {
            JSONObject object = (JSONObject) source;
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                if (normalizedKeys.contains(normalizeJsonKey(entry.getKey()))) {
                    return entry.getValue();
                }
            }
            for (Object value : object.values()) {
                Object matched = findFirstValue(value, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) source;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && normalizedKeys.contains(normalizeJsonKey(String.valueOf(entry.getKey())))) {
                    return entry.getValue();
                }
            }
            for (Object value : map.values()) {
                Object matched = findFirstValue(value, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof JSONArray) {
            for (Object item : (JSONArray) source) {
                Object matched = findFirstValue(item, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof Iterable) {
            for (Object item : (Iterable<?>) source) {
                Object matched = findFirstValue(item, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        }
        return null;
    }

    private String normalizeJsonKey(String key) {
        return StrUtil.trimToEmpty(key)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase();
    }

    private LocalDate parseDate(Object value) {
        String text = toText(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        text = text.trim();
        if (text.matches("\\d{8}")) {
            return parseDateQuietly(text, DateTimeFormatter.BASIC_ISO_DATE);
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return LocalDate.of(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        if (text.length() >= 10) {
            return parseDateQuietly(text.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return parseDateQuietly(text, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDate parseDateQuietly(String text, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(text, formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        String text = toText(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        text = text.replace(",", "")
                .replace("，", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("元", "")
                .trim();
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group());
    }

    String parseFactoryOrderNo(String invoiceRemark) {
        String remark = StrUtil.trimToEmpty(invoiceRemark);
        if (StrUtil.isBlank(remark)) {
            return null;
        }
        Matcher mergedMatcher = MERGED_FACTORY_ORDER_PATTERN.matcher(remark);
        if (mergedMatcher.find()) {
            return mergedMatcher.group(1);
        }
        String firstToken = remark.split("\\s+")[0];
        Matcher tokenMatcher = FACTORY_ORDER_TOKEN_PATTERN.matcher(firstToken);
        return tokenMatcher.find() ? tokenMatcher.group(1) : null;
    }

    private void updateRecognizedItem(Long itemId, String status, OcrParseResult result,
                                      String factoryOrderNo, String rawJson) {
        purchaseInvoiceOcrItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrItemDO>()
                .set(ErpPurchaseInvoiceOcrItemDO::getStatus, status)
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceNo, result.getInvoiceNo())
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceDate, result.getInvoiceDate())
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceType, result.getInvoiceType())
                .set(ErpPurchaseInvoiceOcrItemDO::getTotalAmount, result.getTotalAmount())
                .set(ErpPurchaseInvoiceOcrItemDO::getTaxExclusiveAmount, result.getTaxExclusiveAmount())
                .set(ErpPurchaseInvoiceOcrItemDO::getTaxAmount, result.getTaxAmount())
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceRemark, result.getInvoiceRemark())
                .set(ErpPurchaseInvoiceOcrItemDO::getParsedFactoryOrderNo, factoryOrderNo)
                .set(ErpPurchaseInvoiceOcrItemDO::getFactoryOrderNo, factoryOrderNo)
                .set(ErpPurchaseInvoiceOcrItemDO::getRawJson, rawJson)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInIds, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInNos, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getPurchaseInTotalAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getDiffAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getGeneratedInvoiceId, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getErrorMsg, null)
                .eq(ErpPurchaseInvoiceOcrItemDO::getId, itemId));
    }

    private void updateFailedItem(Long itemId, String errorMsg) {
        purchaseInvoiceOcrItemMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrItemDO>()
                .set(ErpPurchaseInvoiceOcrItemDO::getStatus, ErpPurchaseInvoiceOcrItemStatusEnum.OCR_FAILED.getStatus())
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceNo, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceDate, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceType, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getTotalAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getTaxExclusiveAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getTaxAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getInvoiceRemark, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getParsedFactoryOrderNo, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getFactoryOrderNo, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getRawJson, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInIds, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getMatchedPurchaseInNos, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getPurchaseInTotalAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getDiffAmount, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getGeneratedInvoiceId, null)
                .set(ErpPurchaseInvoiceOcrItemDO::getErrorMsg, errorMsg)
                .eq(ErpPurchaseInvoiceOcrItemDO::getId, itemId));
    }

    private void updateBatchStatus(Long batchId, String status, String errorMsg) {
        purchaseInvoiceOcrBatchMapper.update(null, new LambdaUpdateWrapper<ErpPurchaseInvoiceOcrBatchDO>()
                .set(ErpPurchaseInvoiceOcrBatchDO::getStatus, status)
                .set(ErpPurchaseInvoiceOcrBatchDO::getErrorMsg, errorMsg)
                .eq(ErpPurchaseInvoiceOcrBatchDO::getId, batchId));
    }

    private String buildBatchNo(Long id) {
        return "PIOCR" + LocalDate.now().format(BATCH_NO_DATE_FORMATTER) + String.format("%08d", id);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StrUtil.isBlank(text) ? null : text;
    }

    private String limitErrorMessage(String message) {
        String text = StrUtil.blankToDefault(message, "识别失败");
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private String appendErrorMessage(String oldMessage, String newMessage) {
        String text = StrUtil.trimToEmpty(newMessage);
        if (StrUtil.isBlank(text)) {
            return oldMessage;
        }
        if (StrUtil.isBlank(oldMessage)) {
            return limitErrorMessage(text);
        }
        if (oldMessage.contains(text)) {
            return oldMessage;
        }
        return limitErrorMessage(oldMessage + "；" + text);
    }

    static class InternalFileLocation {

        private final Long configId;
        private final String path;

        InternalFileLocation(Long configId, String path) {
            this.configId = configId;
            this.path = path;
        }

        public Long getConfigId() {
            return configId;
        }

        public String getPath() {
            return path;
        }

    }

    private static class OcrParseResult {

        private String invoiceNo;
        private LocalDate invoiceDate;
        private String invoiceType;
        private BigDecimal totalAmount;
        private BigDecimal taxExclusiveAmount;
        private BigDecimal taxAmount;
        private String invoiceRemark;

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public void setInvoiceNo(String invoiceNo) {
            this.invoiceNo = invoiceNo;
        }

        public LocalDate getInvoiceDate() {
            return invoiceDate;
        }

        public void setInvoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
        }

        public String getInvoiceType() {
            return invoiceType;
        }

        public void setInvoiceType(String invoiceType) {
            this.invoiceType = invoiceType;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public BigDecimal getTaxExclusiveAmount() {
            return taxExclusiveAmount;
        }

        public void setTaxExclusiveAmount(BigDecimal taxExclusiveAmount) {
            this.taxExclusiveAmount = taxExclusiveAmount;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public void setTaxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
        }

        public String getInvoiceRemark() {
            return invoiceRemark;
        }

        public void setInvoiceRemark(String invoiceRemark) {
            this.invoiceRemark = invoiceRemark;
        }

    }

}
