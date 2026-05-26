package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenVoucherConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_PERIOD_INVALID;

/**
 * ERP 系统开账 Service 实现类
 *
 * @author Claude
 */
@Service
@Slf4j
@Validated
public class ErpBookOpenServiceImpl implements ErpBookOpenService {

    @Resource
    private ErpBookOpenMapper bookOpenMapper;
    @Resource
    private ErpBookOpenVoucherConfigMapper voucherConfigMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    @Lazy
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    @Lazy
    private ErpVoucherService voucherService;
    @Resource
    private ErpVoucherMapper voucherMapper;

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Resource
    private ErpPreReceiptMapper preReceiptMapper;
    @Resource
    private ErpPrePaymentMapper prePaymentMapper;
    @Resource
    private ErpPreReceivableMapper preReceivableMapper;
    @Resource
    private ErpOtherPayableMapper otherPayableMapper;
    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;

    @Resource
    @Lazy
    private ErpSupplierService supplierService;
    @Resource
    @Lazy
    private ErpCustomerService customerService;
    @Resource
    @Lazy
    private ErpStockService stockService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBookOpen(ErpBookOpenSaveReqVO createReqVO) {
        // 1.0 校验期间合法性（S5 修复：period > 12 时 YearMonth.of 抛 DateTimeException）
        validatePeriod(createReqVO.getFiscalYear(), createReqVO.getPeriod());
        // 1.1 校验同 (chainName, fiscalYear, period) 不重复
        validateBookOpenDuplicate(createReqVO.getChainName(), createReqVO.getFiscalYear(),
                createReqVO.getPeriod(), null);
        // 1.2 生成开账编号
        String no = noRedisDAO.generate(ErpNoRedisDAO.BOOK_OPEN_NO_PREFIX);
        if (bookOpenMapper.selectByNo(no) != null) {
            throw exception(BOOK_OPEN_NO_EXISTS);
        }

        // 2. 插入主表（默认 opened=true，operate* 信息从登录用户取）
        ErpBookOpenDO bookOpen = BeanUtils.toBean(createReqVO, ErpBookOpenDO.class).setNo(no);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        bookOpen.setOpened(true).setOperateTime(LocalDateTime.now()).setOperatorUserId(loginUserId);
        if (loginUserId != null) {
            AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
            if (user != null) {
                bookOpen.setOperator(user.getNickname());
            }
        }
        // S6 修复：唯一索引兜底，捕获并发场景下绕过内存校验的重复插入
        try {
            bookOpenMapper.insert(bookOpen);
        } catch (DuplicateKeyException ex) {
            throw exception(BOOK_OPEN_DUPLICATE);
        }

        // 3. 自动插入 11 条凭证类型默认勾选 enabled=true
        List<ErpBookOpenVoucherConfigDO> configs = new ArrayList<>();
        ErpVoucherTypeEnum[] voucherTypes = ErpVoucherTypeEnum.values();
        for (int i = 0; i < voucherTypes.length; i++) {
            configs.add(ErpBookOpenVoucherConfigDO.builder()
                    .bookOpenId(bookOpen.getId())
                    .voucherType(voucherTypes[i].getType())
                    .enabled(true)
                    .sort(i + 1)
                    .build());
        }
        voucherConfigMapper.insertBatch(configs);

        // 4. 扫描该期间已审核业务单据，批量补生成凭证（失败不阻塞主流程）
        try {
            scanAndGenerateVouchersAfterCreate(bookOpen);
        } catch (Exception ex) {
            log.error("[createBookOpen][bookOpenId={} 期间扫描生成凭证失败]", bookOpen.getId(), ex);
        }

        return bookOpen.getId();
    }

    @Override
    public void updateBookOpen(ErpBookOpenSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateBookOpen(updateReqVO.getId());
        // 1.1 校验期间合法性（S5 修复）
        validatePeriod(updateReqVO.getFiscalYear(), updateReqVO.getPeriod());
        // 2. 校验三元组不重复（排除自己）
        validateBookOpenDuplicate(updateReqVO.getChainName(), updateReqVO.getFiscalYear(),
                updateReqVO.getPeriod(), updateReqVO.getId());
        // 3. 更新基础信息
        ErpBookOpenDO updateObj = BeanUtils.toBean(updateReqVO, ErpBookOpenDO.class);
        bookOpenMapper.updateById(updateObj);
    }

    private void validateBookOpenDuplicate(String chainName, Integer fiscalYear, Integer period, Long excludeId) {
        ErpBookOpenDO exist = bookOpenMapper.selectByChainNameAndYearAndPeriod(chainName, fiscalYear, period);
        if (exist == null) {
            return;
        }
        if (excludeId != null && exist.getId().equals(excludeId)) {
            return;
        }
        throw exception(BOOK_OPEN_DUPLICATE);
    }

    /**
     * 校验会计年/会计期合法性（S5 修复）。
     * 必须显式校验，否则 period > 12 时 {@link YearMonth#of(int, int)} 会直接抛 DateTimeException。
     */
    private void validatePeriod(Integer fiscalYear, Integer period) {
        if (fiscalYear == null || period == null) {
            throw exception(BOOK_OPEN_PERIOD_INVALID);
        }
        if (fiscalYear < 1900 || fiscalYear > 9999) {
            throw exception(BOOK_OPEN_PERIOD_INVALID);
        }
        if (period < 1 || period > 12) {
            throw exception(BOOK_OPEN_PERIOD_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookOpen(Long id) {
        // 1. 校验存在
        validateBookOpen(id);
        // 2. 级联删除凭证勾选
        voucherConfigMapper.deleteByBookOpenId(id);
        // 3. 删除主表
        bookOpenMapper.deleteById(id);
    }

    @Override
    public ErpBookOpenDO getBookOpen(Long id) {
        return bookOpenMapper.selectById(id);
    }

    @Override
    public ErpBookOpenDO validateBookOpen(Long id) {
        ErpBookOpenDO bookOpen = bookOpenMapper.selectById(id);
        if (bookOpen == null) {
            throw exception(BOOK_OPEN_NOT_EXISTS);
        }
        return bookOpen;
    }

    @Override
    public PageResult<ErpBookOpenDO> getBookOpenPage(ErpBookOpenPageReqVO pageReqVO) {
        return bookOpenMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpBookOpenVoucherConfigDO> getBookOpenVoucherConfigList(Long bookOpenId) {
        validateBookOpen(bookOpenId);
        return voucherConfigMapper.selectListByBookOpenId(bookOpenId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookOpenVoucherConfigs(ErpBookOpenVoucherConfigSaveReqVO reqVO) {
        // 1. 校验主表存在
        validateBookOpen(reqVO.getBookOpenId());
        // 2. 先删后插（事务内）
        voucherConfigMapper.deleteByBookOpenId(reqVO.getBookOpenId());
        List<ErpBookOpenVoucherConfigDO> configs = new ArrayList<>();
        List<ErpBookOpenVoucherConfigSaveReqVO.Item> items = reqVO.getItems();
        for (int i = 0; i < items.size(); i++) {
            ErpBookOpenVoucherConfigSaveReqVO.Item item = items.get(i);
            configs.add(ErpBookOpenVoucherConfigDO.builder()
                    .bookOpenId(reqVO.getBookOpenId())
                    .voucherType(item.getVoucherType())
                    .enabled(item.getEnabled())
                    .sort(item.getSort() != null ? item.getSort() : i + 1)
                    .build());
        }
        voucherConfigMapper.insertBatch(configs);
    }

    @Override
    public boolean isVoucherTypeEnabled(LocalDate bizDate, Integer voucherType) {
        if (bizDate == null || voucherType == null) {
            return false;
        }
        ErpBookOpenDO bookOpen = bookOpenMapper.selectByYearAndPeriod(bizDate.getYear(), bizDate.getMonthValue());
        if (bookOpen == null) {
            log.warn("[isVoucherTypeEnabled][未找到开账记录 fiscalYear={} period={}]",
                    bizDate.getYear(), bizDate.getMonthValue());
            return false;
        }
        if (!Boolean.TRUE.equals(bookOpen.getOpened())) {
            log.warn("[isVoucherTypeEnabled][开账记录未启用 bookOpenId={}]", bookOpen.getId());
            return false;
        }
        ErpBookOpenVoucherConfigDO config = voucherConfigMapper
                .selectByBookOpenIdAndVoucherType(bookOpen.getId(), voucherType);
        if (config == null) {
            log.warn("[isVoucherTypeEnabled][未配置凭证类型 bookOpenId={} voucherType={}]",
                    bookOpen.getId(), voucherType);
            return false;
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            log.warn("[isVoucherTypeEnabled][凭证类型未启用 bookOpenId={} voucherType={}]",
                    bookOpen.getId(), voucherType);
            return false;
        }
        return true;
    }

    /**
     * 新增系统开账后，扫描该期间已审核且尚未生成凭证的业务单据，按勾选的凭证类型批量补生成凭证。
     * 单条凭证生成失败不影响其他单据；主流程已提交，本方法异常会被外层 try-catch 吞掉只打日志。
     */
    private void scanAndGenerateVouchersAfterCreate(ErpBookOpenDO bookOpen) {
        if (bookOpen.getFiscalYear() == null || bookOpen.getPeriod() == null) {
            return;
        }
        // 1. 计算期间起止
        YearMonth ym = YearMonth.of(bookOpen.getFiscalYear(), bookOpen.getPeriod());
        LocalDateTime periodStart = ym.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        // 2. 查启用的凭证类型集合
        List<ErpBookOpenVoucherConfigDO> configs = voucherConfigMapper.selectListByBookOpenId(bookOpen.getId());
        if (configs == null || configs.isEmpty()) {
            return;
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (ErpBookOpenVoucherConfigDO cfg : configs) {
            if (!Boolean.TRUE.equals(cfg.getEnabled())) {
                continue;
            }
            Integer vt = cfg.getVoucherType();
            try {
                if (ErpVoucherTypeEnum.SALE.getType().equals(vt)) {
                    scanSaleOut(periodStart, periodEnd, txTemplate);
                    scanSaleReturn(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.OTHER_RECEIVABLE.getType().equals(vt)) {
                    scanOtherReceivable(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.PRE_RECEIPT.getType().equals(vt)) {
                    scanPreReceipt(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.PRE_PAYMENT.getType().equals(vt)) {
                    scanPrePayment(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.PRE_RECEIVABLE.getType().equals(vt)) {
                    scanPreReceivable(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.PURCHASE.getType().equals(vt)) {
                    scanPurchaseIn(periodStart, periodEnd, txTemplate);
                    scanPurchaseReturn(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.OTHER_PAYABLE.getType().equals(vt)) {
                    scanOtherPayable(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.OTHER_OUT.getType().equals(vt)) {
                    scanStockOut(periodStart, periodEnd, txTemplate);
                } else if (ErpVoucherTypeEnum.OTHER_IN.getType().equals(vt)) {
                    scanStockIn(periodStart, periodEnd, txTemplate);
                }
                // VoucherType 6 调拨出库 / 11 银行转账 暂不实现
            } catch (Exception ex) {
                log.error("[scanAndGenerateVouchersAfterCreate][voucherType={} 扫描失败]", vt, ex);
            }
        }
    }

    private boolean voucherExists(Integer sourceBizType, Long sourceBizId) {
        List<ErpVoucherDO> existed = voucherMapper.selectListByBiz(sourceBizType, sourceBizId);
        return existed != null && !existed.isEmpty();
    }

    private void scanPurchaseIn(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.PURCHASE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType();
        List<ErpPurchaseInDO> list = purchaseInMapper.selectList(new LambdaQueryWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpPurchaseInDO::getInTime, periodStart)
                .le(ErpPurchaseInDO::getInTime, periodEnd));
        for (ErpPurchaseInDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    ErpSupplierDO supplier = doc.getSupplierId() != null ? supplierService.getSupplier(doc.getSupplierId()) : null;
                    String supplierName = supplier != null ? supplier.getName() : "";
                    List<ErpVoucherItemDO> items = autoVoucherBuilder.buildPurchaseInItems(doc, supplierName);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getInTime().toLocalDate(), "采购入库 - " + supplierName, items);
                } catch (Exception ex) {
                    log.error("[scanPurchaseIn][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanPurchaseReturn(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.PURCHASE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType();
        List<ErpPurchaseReturnDO> list = purchaseReturnMapper.selectList(new LambdaQueryWrapper<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpPurchaseReturnDO::getReturnTime, periodStart)
                .le(ErpPurchaseReturnDO::getReturnTime, periodEnd));
        for (ErpPurchaseReturnDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    ErpSupplierDO supplier = doc.getSupplierId() != null ? supplierService.getSupplier(doc.getSupplierId()) : null;
                    String supplierName = supplier != null ? supplier.getName() : "";
                    List<ErpVoucherItemDO> items = autoVoucherBuilder.buildPurchaseReturnItems(doc, supplierName);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getReturnTime().toLocalDate(), "采购退货 - " + supplierName, items);
                } catch (Exception ex) {
                    log.error("[scanPurchaseReturn][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanSaleOut(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.SALE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.SALE_OUT.getType();
        List<ErpSaleOutDO> list = saleOutMapper.selectList(new LambdaQueryWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpSaleOutDO::getOutTime, periodStart)
                .le(ErpSaleOutDO::getOutTime, periodEnd));
        for (ErpSaleOutDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpSaleOutItemDO> items = saleOutItemMapper.selectListByOutId(doc.getId());
                    BigDecimal sumCost = sumStockCost(items, ErpSaleOutItemDO::getProductId,
                            ErpSaleOutItemDO::getWarehouseId, ErpSaleOutItemDO::getCount);
                    ErpCustomerDO customer = doc.getCustomerId() != null ? customerService.getCustomer(doc.getCustomerId()) : null;
                    String customerName = customer != null ? customer.getName() : "";
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildSaleOutItems(doc, customerName, sumCost);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getOutTime().toLocalDate(), "销售出库 - " + customerName, vItems);
                } catch (Exception ex) {
                    log.error("[scanSaleOut][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanSaleReturn(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.SALE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.SALE_RETURN.getType();
        List<ErpSaleReturnDO> list = saleReturnMapper.selectList(new LambdaQueryWrapper<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpSaleReturnDO::getReturnTime, periodStart)
                .le(ErpSaleReturnDO::getReturnTime, periodEnd));
        for (ErpSaleReturnDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpSaleReturnItemDO> items = saleReturnItemMapper.selectListByReturnId(doc.getId());
                    BigDecimal sumCost = sumStockCost(items, ErpSaleReturnItemDO::getProductId,
                            ErpSaleReturnItemDO::getWarehouseId, ErpSaleReturnItemDO::getCount);
                    ErpCustomerDO customer = doc.getCustomerId() != null ? customerService.getCustomer(doc.getCustomerId()) : null;
                    String customerName = customer != null ? customer.getName() : "";
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildSaleReturnItems(doc, customerName, sumCost);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getReturnTime().toLocalDate(), "销售退货 - " + customerName, vItems);
                } catch (Exception ex) {
                    log.error("[scanSaleReturn][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanStockIn(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.OTHER_IN.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.OTHER_IN.getType();
        List<ErpStockInDO> list = stockInMapper.selectList(new LambdaQueryWrapper<ErpStockInDO>()
                .eq(ErpStockInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpStockInDO::getInTime, periodStart)
                .le(ErpStockInDO::getInTime, periodEnd));
        for (ErpStockInDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    BigDecimal sumCost = doc.getTotalPrice() != null ? doc.getTotalPrice() : BigDecimal.ZERO;
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildStockInItems(doc, sumCost);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getInTime().toLocalDate(), "其他入库 - " + doc.getNo(), vItems);
                } catch (Exception ex) {
                    log.error("[scanStockIn][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanStockOut(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.OTHER_OUT.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.OTHER_OUT.getType();
        List<ErpStockOutDO> list = stockOutMapper.selectList(new LambdaQueryWrapper<ErpStockOutDO>()
                .eq(ErpStockOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpStockOutDO::getOutTime, periodStart)
                .le(ErpStockOutDO::getOutTime, periodEnd));
        for (ErpStockOutDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpStockOutItemDO> items = stockOutItemMapper.selectListByOutId(doc.getId());
                    BigDecimal sumCost = sumStockCost(items, ErpStockOutItemDO::getProductId,
                            ErpStockOutItemDO::getWarehouseId, ErpStockOutItemDO::getCount);
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildStockOutItems(doc, sumCost);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getTotalPrice(),
                            doc.getOutTime().toLocalDate(), "其他出库 - " + doc.getNo(), vItems);
                } catch (Exception ex) {
                    log.error("[scanStockOut][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanOtherReceivable(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.OTHER_RECEIVABLE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.OTHER_RECEIVABLE.getType();
        List<ErpOtherReceivableDO> list = otherReceivableMapper.selectList(new LambdaQueryWrapper<ErpOtherReceivableDO>()
                .eq(ErpOtherReceivableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpOtherReceivableDO::getBizTime, periodStart)
                .le(ErpOtherReceivableDO::getBizTime, periodEnd));
        for (ErpOtherReceivableDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildOtherReceivableItems(doc);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(),
                            firstNonNull(doc.getActualAmount(), doc.getTotalAmount()),
                            doc.getBizTime().toLocalDate(), "其他应收 - " + nullToEmpty(doc.getPartyName()), vItems);
                } catch (Exception ex) {
                    log.error("[scanOtherReceivable][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanOtherPayable(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.OTHER_PAYABLE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType();
        List<ErpOtherPayableDO> list = otherPayableMapper.selectList(new LambdaQueryWrapper<ErpOtherPayableDO>()
                .eq(ErpOtherPayableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpOtherPayableDO::getBizTime, periodStart)
                .le(ErpOtherPayableDO::getBizTime, periodEnd));
        for (ErpOtherPayableDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildOtherPayableItems(doc);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(),
                            firstNonNull(doc.getActualAmount(), doc.getTotalAmount()),
                            doc.getBizTime().toLocalDate(), "其他应付 - " + nullToEmpty(doc.getPartyName()), vItems);
                } catch (Exception ex) {
                    log.error("[scanOtherPayable][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanPreReceipt(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.PRE_RECEIPT.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.PRE_RECEIPT.getType();
        List<ErpPreReceiptDO> list = preReceiptMapper.selectList(new LambdaQueryWrapper<ErpPreReceiptDO>()
                .eq(ErpPreReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpPreReceiptDO::getBizTime, periodStart)
                .le(ErpPreReceiptDO::getBizTime, periodEnd));
        for (ErpPreReceiptDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildPreReceiptItems(doc);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getActualAmount(),
                            doc.getBizTime().toLocalDate(), "预收款 - " + nullToEmpty(doc.getPartyName()), vItems);
                } catch (Exception ex) {
                    log.error("[scanPreReceipt][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanPrePayment(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.PRE_PAYMENT.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType();
        List<ErpPrePaymentDO> list = prePaymentMapper.selectList(new LambdaQueryWrapper<ErpPrePaymentDO>()
                .eq(ErpPrePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpPrePaymentDO::getBizTime, periodStart)
                .le(ErpPrePaymentDO::getBizTime, periodEnd));
        for (ErpPrePaymentDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildPrePaymentItems(doc);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getActualAmount(),
                            doc.getBizTime().toLocalDate(), "预付款 - " + nullToEmpty(doc.getPartyName()), vItems);
                } catch (Exception ex) {
                    log.error("[scanPrePayment][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void scanPreReceivable(LocalDateTime periodStart, LocalDateTime periodEnd, TransactionTemplate tx) {
        Integer voucherType = ErpVoucherTypeEnum.PRE_RECEIVABLE.getType();
        Integer sourceBizType = ErpVoucherSourceBizTypeEnum.PRE_RECEIVABLE.getType();
        List<ErpPreReceivableDO> list = preReceivableMapper.selectList(new LambdaQueryWrapper<ErpPreReceivableDO>()
                .eq(ErpPreReceivableDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .ge(ErpPreReceivableDO::getBizTime, periodStart)
                .le(ErpPreReceivableDO::getBizTime, periodEnd));
        for (ErpPreReceivableDO doc : list) {
            if (voucherExists(sourceBizType, doc.getId())) {
                continue;
            }
            tx.execute(status -> {
                try {
                    List<ErpVoucherItemDO> vItems = autoVoucherBuilder.buildPreReceivableItems(doc);
                    voucherService.createVoucherFromBiz(sourceBizType, doc.getId(), doc.getNo(), doc.getActualAmount(),
                            doc.getBizTime().toLocalDate(), "预收账款 - " + nullToEmpty(doc.getPartyName()), vItems);
                } catch (Exception ex) {
                    log.error("[scanPreReceivable][bizId={} 生成凭证失败]", doc.getId(), ex);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    /**
     * 通用：扣库存前累加成本快照（productId × warehouseId 取 stock.costPrice，乘以 count 求和）。
     * 用于销售出库 / 销售退货 / 其他出库。
     */
    private <T> BigDecimal sumStockCost(List<T> items,
                                        java.util.function.Function<T, Long> productIdGetter,
                                        java.util.function.Function<T, Long> warehouseIdGetter,
                                        java.util.function.Function<T, BigDecimal> countGetter) {
        BigDecimal sum = BigDecimal.ZERO;
        if (items == null) {
            return sum;
        }
        for (T item : items) {
            ErpStockDO stock = stockService.getStock(productIdGetter.apply(item), warehouseIdGetter.apply(item));
            BigDecimal cost = (stock != null && stock.getCostPrice() != null) ? stock.getCostPrice() : BigDecimal.ZERO;
            BigDecimal count = countGetter.apply(item);
            if (count == null) {
                count = BigDecimal.ZERO;
            }
            sum = sum.add(cost.multiply(count));
        }
        return sum;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

}
