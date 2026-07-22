package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 供应商 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpSupplierService {

    /**
     * 创建供应商
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSupplier(@Valid ErpSupplierSaveReqVO createReqVO);

    /**
     * 更新供应商
     *
     * @param updateReqVO 更新信息
     */
    void updateSupplier(@Valid ErpSupplierSaveReqVO updateReqVO);

    ErpSupplierDeptDistributionRespVO getSupplierDeptDistribution(Long id);

    void updateSupplierDeptDistribution(@Valid ErpSupplierDeptDistributionSaveReqVO reqVO);

    /**
     * 批量编辑供应商
     *
     * @param reqVO 批量编辑参数
     */
    void batchUpdateSupplier(@Valid ErpSupplierBatchUpdateReqVO reqVO);

    /**
     * 批量停用供应商
     *
     * @param ids 供应商编号列表
     */
    void batchDisableSupplier(List<Long> ids);

    /**
     * 还原停用供应商
     *
     * @param ids 供应商编号列表
     */
    void restoreSupplier(List<Long> ids);

    /**
     * 合并供应商。
     *
     * @param sourceId 被合并供应商编号
     * @param keepId 保留供应商编号
     */
    void mergeSupplier(Long sourceId, Long keepId);

    /**
     * 删除供应商
     *
     * @param id 编号
     */
    void deleteSupplier(Long id);

    /**
     * 批量删除供应商
     *
     * @param ids 编号列表
     */
    void deleteSupplierList(List<Long> ids);

    /**
     * 获得供应商
     *
     * @param id 编号
     * @return 供应商
     */
    ErpSupplierDO getSupplier(Long id);

    /**
     * 校验供应商
     *
     * @param id 编号
     * @return 供应商
     */
    ErpSupplierDO validateSupplier(Long id);

    /**
     * 获得供应商列表
     *
     * @param ids 编号列表
     * @return 供应商列表
     */
    List<ErpSupplierDO> getSupplierList(Collection<Long> ids);

    /**
     * 获得供应商 Map
     *
     * @param ids 编号列表
     * @return 供应商 Map
     */
    default Map<Long, ErpSupplierDO> getSupplierMap(Collection<Long> ids) {
        return convertMap(getSupplierList(ids), ErpSupplierDO::getId);
    }

    /**
     * 获得供应商分页
     *
     * @param pageReqVO 分页查询
     * @return 供应商分页
     */
    PageResult<ErpSupplierDO> getSupplierPage(ErpSupplierPageReqVO pageReqVO);

    /**
     * 更新供应商开启状态
     *
     * @param id     编号
     * @param status 状态
     */
    void updateSupplierStatus(Long id, Integer status);

    /**
     * 获得指定状态的供应商列表
     *
     * @param status 状态
     * @return 供应商列表
     */
    List<ErpSupplierDO> getSupplierListByStatus(Integer status);

    /**
     * 按名称模糊匹配供应商列表
     *
     * @param name 名称片段
     * @return 供应商列表
     */
    List<ErpSupplierDO> getSupplierListByNameLike(String name);

    /**
     * 获得供应商适用部门 Map。
     *
     * @param supplierIds 供应商编号集合
     * @return 供应商编号与适用部门编号集合的映射
     */
    Map<Long, List<Long>> getSupplierDeptMap(Collection<Long> supplierIds);

    /**
     * 导入供应商列表
     *
     * @param list 导入的供应商列表
     */
    void importSupplierList(@Valid List<ErpSupplierImportExcelVO> list);

}
