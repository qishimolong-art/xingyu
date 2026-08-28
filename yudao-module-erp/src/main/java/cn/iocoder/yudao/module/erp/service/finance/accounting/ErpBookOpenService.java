package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 系统开账 Service 接口
 *
 * @author Claude
 */
public interface ErpBookOpenService {

    /**
     * 创建开账记录（同步初始化 11 条凭证类型勾选默认 enabled=true）
     */
    Long createBookOpen(@Valid ErpBookOpenSaveReqVO createReqVO);

    /**
     * 更新开账主表（年度开账：期间固定为 1 月，期初日期固定为当年 1 月 1 日）
     */
    void updateBookOpen(@Valid ErpBookOpenSaveReqVO updateReqVO);

    /**
     * 删除开账记录（级联删除凭证勾选配置）
     */
    void deleteBookOpen(Long id);

    /**
     * 获得开账记录（含凭证勾选配置）
     */
    ErpBookOpenDO getBookOpen(Long id);

    /**
     * 校验开账记录存在
     */
    ErpBookOpenDO validateBookOpen(Long id);

    /**
     * 分页获得开账记录
     */
    PageResult<ErpBookOpenDO> getBookOpenPage(ErpBookOpenPageReqVO pageReqVO);

    /**
     * 获得指定开账记录的凭证类型勾选列表
     */
    List<ErpBookOpenVoucherConfigDO> getBookOpenVoucherConfigList(Long bookOpenId);

    /**
     * 批量更新指定开账记录的凭证类型勾选（先按 bookOpenId 删旧再插新）
     */
    void updateBookOpenVoucherConfigs(@Valid ErpBookOpenVoucherConfigSaveReqVO reqVO);

    /**
     * 判断指定日期所属年度、指定凭证类型是否已开账并启用。
     *
     * 用于业务单据审核时判断是否触发自动生成凭证。
     * 静默策略：未开账或未启用直接返回 false（log warn，不抛异常）。
     *
     * @param bizDate     业务发生日期
     * @param voucherType ErpVoucherTypeEnum.type
     */
    boolean isVoucherTypeEnabled(LocalDate bizDate, Integer voucherType);

}
