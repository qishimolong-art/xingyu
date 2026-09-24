package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission.ErpSaleDirectForbiddenDeptRespVO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售直接开单部门权限 Service 接口
 */
public interface ErpSaleDirectDeptPermissionService {

    List<ErpSaleDirectForbiddenDeptRespVO> getForbiddenDeptList();

    void updateForbiddenDeptList(@Valid Collection<Long> deptIds);

    void validateSaleDocumentDeptAllowed(Long deptId);

}
