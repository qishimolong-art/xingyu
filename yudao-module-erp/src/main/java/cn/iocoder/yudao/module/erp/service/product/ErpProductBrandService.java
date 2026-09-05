package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;

import javax.validation.Valid;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

/**
 * ERP 配件品牌 Service 接口
 */
public interface ErpProductBrandService {

    Long createProductBrand(@Valid ErpProductBrandSaveReqVO createReqVO);

    void updateProductBrand(@Valid ErpProductBrandSaveReqVO updateReqVO);

    void deleteProductBrand(Long id);

    void deleteProductBrandList(Collection<Long> ids);

    ErpProductBrandImportRespVO importProductBrandList(List<ErpProductBrandImportExcelVO> list);

    List<ErpProductBrandImportExcelVO> parseCsvImport(Reader reader);

    ErpProductBrandDO getProductBrand(Long id);

    PageResult<ErpProductBrandDO> getProductBrandPage(ErpProductBrandPageReqVO pageReqVO);

    List<ErpProductBrandDO> getProductBrandListByStatus(Integer status);

    List<ErpProductBrandDO> getProductBrandListByStatusForCurrentUser(Integer status);

    void validateEnabledProductBrand(String name);

}
