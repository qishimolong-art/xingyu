package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionApplyReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateFromBizReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSearchSourceBizReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 凭证归属（跨月调整）Service 接口
 *
 * @author Claude
 */
public interface ErpVoucherAttributionService {

    /**
     * 创建归属记录
     */
    Long createVoucherAttribution(@Valid ErpVoucherAttributionSaveReqVO createReqVO);

    /**
     * 更新归属记录
     */
    void updateVoucherAttribution(@Valid ErpVoucherAttributionSaveReqVO updateReqVO);

    /**
     * 删除归属记录
     */
    void deleteVoucherAttribution(Long id);

    /**
     * 获得归属记录
     */
    ErpVoucherAttributionDO getVoucherAttribution(Long id);

    /**
     * 校验归属记录存在
     */
    ErpVoucherAttributionDO validateVoucherAttribution(Long id);

    /**
     * 获得归属记录分页
     */
    PageResult<ErpVoucherAttributionDO> getVoucherAttributionPage(ErpVoucherAttributionPageReqVO pageReqVO);

    /**
     * 批量应用归属（设置归属年月）
     */
    void applyAttribution(@Valid ErpVoucherAttributionApplyReqVO reqVO);

    /**
     * 批量生成凭证（生成凭证空壳）
     *
     * @return 生成的凭证 ID 列表（顺序与入参 ids 一致）
     */
    List<Long> generateVouchers(@Valid ErpVoucherAttributionGenerateReqVO reqVO);

    /**
     * 凭证生成-从业务单据快照直接生成凭证
     *
     * 适用场景：「凭证生成」页查询返回的业务单据快照在 attribution 表里还不存在；
     * 本方法事务内先按 bizType+bizId 落 attribution 记录（已存在则复用），再调 {@link #generateVouchers}。
     *
     * @return 生成的凭证 ID 列表（顺序与入参 items 一致）
     */
    List<Long> generateVouchersFromBiz(@Valid ErpVoucherAttributionGenerateFromBizReqVO reqVO);

    /**
     * 凭证生成-按单据来源类型分页查询业务单据
     *
     * 客户需求 5.1：先选单据类型 + 日期 → 查询 → 勾选 → 生成凭证。
     * 按 sourceBizType 路由到不同业务表，统一封装为 ErpVoucherAttributionRespVO（attributionStatus=10 未归属）。
     *
     * 见 刘/财务问题汇总-ds修订版.md 5 节单据类型选项（共 20 种）。
     */
    PageResult<ErpVoucherAttributionRespVO> searchSourceBizPage(@Valid ErpVoucherAttributionSearchSourceBizReqVO reqVO);

}
