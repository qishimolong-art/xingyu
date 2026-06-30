package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinBatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinRespVO;

import javax.validation.Valid;

public interface ErpRecycleBinService {

    PageResult<ErpRecycleBinRespVO> getRecycleBinPage(@Valid ErpRecycleBinPageReqVO pageReqVO);

    void restore(@Valid ErpRecycleBinBatchReqVO reqVO);

    void clear(@Valid ErpRecycleBinBatchReqVO reqVO);

}
