package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenVoucherConfigMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
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





    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBookOpen(ErpBookOpenSaveReqVO createReqVO) {
        // 1.0 年度开账：前端只传会计年度，月份/开始日期由后端统一归一化
        validateFiscalYear(createReqVO.getFiscalYear());
        normalizeAnnualPeriod(createReqVO);
        // 1.1 校验同一租户同一年度不重复
        validateBookOpenDuplicate(createReqVO.getFiscalYear(), null);
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

        // 开账仅启用期间与凭证类型，由财务在凭证生成页统一处理。

        return bookOpen.getId();
    }

    @Override
    public void updateBookOpen(ErpBookOpenSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateBookOpen(updateReqVO.getId());
        // 1.1 年度开账归一化并校验年度合法性
        validateFiscalYear(updateReqVO.getFiscalYear());
        normalizeAnnualPeriod(updateReqVO);
        // 2. 校验年度不重复（排除自己）
        validateBookOpenDuplicate(updateReqVO.getFiscalYear(), updateReqVO.getId());
        // 3. 更新基础信息
        ErpBookOpenDO updateObj = BeanUtils.toBean(updateReqVO, ErpBookOpenDO.class);
        bookOpenMapper.updateById(updateObj);
    }

    private void validateBookOpenDuplicate(Integer fiscalYear, Long excludeId) {
        List<ErpBookOpenDO> exists = bookOpenMapper.selectListByYear(fiscalYear);
        if (exists == null || exists.isEmpty()) {
            return;
        }
        for (ErpBookOpenDO exist : exists) {
            if (excludeId != null && excludeId.equals(exist.getId())) {
                continue;
            }
            throw exception(BOOK_OPEN_DUPLICATE);
        }
    }

    /**
     * 年度开账保留 period/startDate 字段兼容旧表结构，新数据统一存 1 月与当年 1 月 1 日。
     */
    private void normalizeAnnualPeriod(ErpBookOpenSaveReqVO reqVO) {
        if (reqVO.getFiscalYear() == null) {
            throw exception(BOOK_OPEN_PERIOD_INVALID);
        }
        reqVO.setPeriod(1);
        reqVO.setStartDate(LocalDate.of(reqVO.getFiscalYear(), 1, 1));
    }

    /**
     * 校验会计年度合法性。
     */
    private void validateFiscalYear(Integer fiscalYear) {
        if (fiscalYear == null) {
            throw exception(BOOK_OPEN_PERIOD_INVALID);
        }
        if (fiscalYear < 1900 || fiscalYear > 9999) {
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
        ErpBookOpenDO bookOpen = bookOpenMapper.selectLatestByYear(bizDate.getYear());
        if (bookOpen == null) {
            log.warn("[isVoucherTypeEnabled][未找到年度开账记录 fiscalYear={}]", bizDate.getYear());
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
     * 新增系统开账后，扫描该年度已审核且尚未生成凭证的业务单据，按勾选的凭证类型批量补生成凭证。
     * 单条凭证生成失败不影响其他单据；主流程已提交，本方法异常会被外层 try-catch 吞掉只打日志。
     */
}
