package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 凭证字字典 Service 接口
 *
 * @author Claude
 */
public interface ErpVoucherWordService {

    /**
     * 创建凭证字
     */
    Long createVoucherWord(@Valid ErpVoucherWordSaveReqVO createReqVO);

    /**
     * 更新凭证字
     */
    void updateVoucherWord(@Valid ErpVoucherWordSaveReqVO updateReqVO);

    /**
     * 删除凭证字
     */
    void deleteVoucherWord(Long id);

    /**
     * 获得凭证字
     */
    ErpVoucherWordDO getVoucherWord(Long id);

    /**
     * 校验凭证字存在
     */
    ErpVoucherWordDO validateVoucherWord(Long id);

    /**
     * 获得凭证字分页
     */
    PageResult<ErpVoucherWordDO> getVoucherWordPage(ErpVoucherWordPageReqVO pageReqVO);

    /**
     * 获得启用的凭证字列表（下拉用）
     */
    List<ErpVoucherWordDO> getEnabledVoucherWordList();

}
