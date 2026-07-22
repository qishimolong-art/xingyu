package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerCreditStatusBO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 客户 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpCustomerService {

    /**
     * 创建客户
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCustomer(@Valid ErpCustomerSaveReqVO createReqVO);

    /**
     * 更新客户
     *
     * @param updateReqVO 更新信息
     */
    void updateCustomer(@Valid ErpCustomerSaveReqVO updateReqVO);

    ErpCustomerDeptDistributionRespVO getCustomerDeptDistribution(Long id);

    void updateCustomerDeptDistribution(@Valid ErpCustomerDeptDistributionSaveReqVO reqVO);

    /**
     * 删除客户
     *
     * @param id 编号
     */
    void deleteCustomer(Long id);

    /**
     * 批量删除客户
     *
     * @param ids 编号列表
     */
    void deleteCustomerList(List<Long> ids);

    /**
     * 获得客户
     *
     * @param id 编号
     * @return 客户
     */
    ErpCustomerDO getCustomer(Long id);

    /**
     * 校验客户
     *
     * @param id 编号
     * @return 客户
     */
    ErpCustomerDO validateCustomer(Long id);

    ErpCustomerDO validateCustomerForSale(Long id);

    /**
     * 校验系统自动生成销售单据时使用的客户。
     *
     * <p>该校验不使用当前操作人的客户数据范围，而是按来源销售业务的实际所属部门校验客户分配关系。</p>
     *
     * @param id 客户编号
     * @param saleDeptId 销售业务所属部门编号
     * @return 客户
     */
    ErpCustomerDO validateCustomerForGeneratedSale(Long id, Long saleDeptId);

    List<Long> getCustomerSaleDeptIds(Long customerId);

    void validateCustomerSaleDept(Long customerId, Long deptId);

    ErpCustomerCreditStatusBO getCustomerCreditStatus(Long customerId);

    Map<Long, ErpCustomerCreditStatusBO> getCustomerCreditStatusMap(Collection<Long> customerIds);

    /**
     * 获得客户列表
     *
     * @param ids 编号列表
     * @return 客户列表
     */
    List<ErpCustomerDO> getCustomerList(Collection<Long> ids);

    /**
     * 获得客户 Map
     *
     * @param ids 编号列表
     * @return 客户 Map
     */
    default Map<Long, ErpCustomerDO> getCustomerMap(Collection<Long> ids) {
        return convertMap(getCustomerList(ids), ErpCustomerDO::getId);
    }

    /**
     * 获得客户分页
     *
     * @param pageReqVO 分页查询
     * @return 客户分页
     */
    PageResult<ErpCustomerDO> getCustomerPage(ErpCustomerPageReqVO pageReqVO);

    /**
     * 获得指定状态的客户列表
     *
     * @param status 状态
     * @return 客户列表
     */
    List<ErpCustomerDO> getCustomerListByStatus(Integer status);

    /**
     * 按名称模糊匹配客户列表
     *
     * @param name 名称片段
     * @return 客户列表
     */
    List<ErpCustomerDO> getCustomerListByNameLike(String name);

    Map<Long, List<Long>> getCustomerDeptMap(Collection<Long> customerIds);

    /**
     * 导入客户列表
     *
     * @param list 导入的客户列表
     */
    void importCustomerList(@Valid List<ErpCustomerImportExcelVO> list);

    /**
     * 批量编辑客户
     *
     * @param reqVO 批量编辑参数
     */
    void batchUpdateCustomer(@Valid ErpCustomerBatchUpdateReqVO reqVO);

    /**
     * 批量停用客户
     *
     * @param ids 客户编号列表
     */
    void batchDisableCustomer(List<Long> ids);

    /**
     * 还原停用客户
     *
     * @param ids 客户编号列表
     */
    void restoreCustomer(List<Long> ids);

    /**
     * 合并客户。
     *
     * @param sourceId 被合并客户编号
     * @param keepId 保留客户编号
     */
    void mergeCustomer(Long sourceId, Long keepId);

}
