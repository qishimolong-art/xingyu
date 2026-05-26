package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherWordMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_WORD_NOT_EXISTS;

/**
 * ERP 凭证字字典 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpVoucherWordServiceImpl implements ErpVoucherWordService {

    @Resource
    private ErpVoucherWordMapper voucherWordMapper;

    @Override
    public Long createVoucherWord(ErpVoucherWordSaveReqVO createReqVO) {
        ErpVoucherWordDO voucherWord = BeanUtils.toBean(createReqVO, ErpVoucherWordDO.class);
        voucherWordMapper.insert(voucherWord);
        return voucherWord.getId();
    }

    @Override
    public void updateVoucherWord(ErpVoucherWordSaveReqVO updateReqVO) {
        validateVoucherWord(updateReqVO.getId());
        ErpVoucherWordDO updateObj = BeanUtils.toBean(updateReqVO, ErpVoucherWordDO.class);
        voucherWordMapper.updateById(updateObj);
    }

    @Override
    public void deleteVoucherWord(Long id) {
        validateVoucherWord(id);
        voucherWordMapper.deleteById(id);
    }

    @Override
    public ErpVoucherWordDO getVoucherWord(Long id) {
        return voucherWordMapper.selectById(id);
    }

    @Override
    public ErpVoucherWordDO validateVoucherWord(Long id) {
        ErpVoucherWordDO voucherWord = voucherWordMapper.selectById(id);
        if (voucherWord == null) {
            throw exception(VOUCHER_WORD_NOT_EXISTS);
        }
        return voucherWord;
    }

    @Override
    public PageResult<ErpVoucherWordDO> getVoucherWordPage(ErpVoucherWordPageReqVO pageReqVO) {
        return voucherWordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpVoucherWordDO> getEnabledVoucherWordList() {
        return voucherWordMapper.selectListByEnable(true);
    }

}
