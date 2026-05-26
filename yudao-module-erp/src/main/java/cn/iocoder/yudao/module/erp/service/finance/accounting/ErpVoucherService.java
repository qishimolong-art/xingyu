package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 凭证 Service 接口
 *
 * @author Claude
 */
public interface ErpVoucherService {

    /**
     * 创建凭证
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVoucher(@Valid ErpVoucherSaveReqVO createReqVO);

    /**
     * 更新凭证
     *
     * @param updateReqVO 更新信息
     */
    void updateVoucher(@Valid ErpVoucherSaveReqVO updateReqVO);

    /**
     * 删除凭证
     *
     * @param id 编号
     */
    void deleteVoucher(Long id);

    /**
     * 审核凭证
     *
     * @param id 编号
     */
    void auditVoucher(Long id);

    /**
     * 反审核凭证
     *
     * @param id 编号
     */
    void processVoucher(Long id);

    /**
     * 获得凭证
     *
     * @param id 编号
     * @return 凭证
     */
    ErpVoucherDO getVoucher(Long id);

    /**
     * 获得凭证分页
     *
     * @param pageReqVO 分页查询
     * @return 凭证分页
     */
    PageResult<ErpVoucherDO> getVoucherPage(ErpVoucherPageReqVO pageReqVO);

    /**
     * 获得凭证分录列表
     *
     * @param voucherId 凭证编号
     * @return 分录列表
     */
    List<ErpVoucherItemDO> getVoucherItemListByVoucherId(Long voucherId);

    /**
     * 业务单据审核时自动生成凭证（不走 SaveReqVO，绕开前端校验通道）。
     *
     * @param sourceBizType ErpVoucherSourceBizTypeEnum.type，例如 8=采购入库单
     * @param sourceBizId   业务单 ID
     * @param sourceBizNo   业务单号
     * @param sourceBizAmount 业务单据金额
     * @param voucherDate   凭证日期（取业务单据的 inTime/outTime/returnTime 转 LocalDate）
     * @param summary       凭证摘要
     * @param items         分录列表（已组装好，subjectId/code/name 已填充）      
     * @return 凭证 ID
     */
    Long createVoucherFromBiz(Integer sourceBizType, Long sourceBizId, String sourceBizNo,
                              BigDecimal sourceBizAmount, LocalDate voucherDate, String summary,
                              List<ErpVoucherItemDO> items);

}
