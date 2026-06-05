package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

/**
 * ERP 客户 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCustomerServiceImpl implements ErpCustomerService {

    private static final String FIELD_PERMISSION_MODULE = "erp_customer";

    @Resource
    private ErpCustomerMapper customerMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Override
    public Long createCustomer(ErpCustomerSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        // 插入
        ErpCustomerDO customer = BeanUtils.toBean(createReqVO, ErpCustomerDO.class);
        // 自动生成编码
        if (!StringUtils.hasText(customer.getCode())) {
            customer.setCode(noRedisDAO.generate(ErpNoRedisDAO.CUSTOMER_NO_PREFIX));
        }
        if (!StringUtils.hasText(customer.getMemberCode())) {
            customer.setMemberCode(noRedisDAO.generate(ErpNoRedisDAO.MEMBER_NO_PREFIX));
        }
        if (!StringUtils.hasText(customer.getPlatformCode())) {
            customer.setPlatformCode(noRedisDAO.generate(ErpNoRedisDAO.PLATFORM_NO_PREFIX));
        }
        // sort 默认值
        if (customer.getSort() == null) {
            customer.setSort(0);
        }
        customerMapper.insert(customer);
        // 返回
        return customer.getId();
    }

    @Override
    public void updateCustomer(ErpCustomerSaveReqVO updateReqVO) {
        // 校验存在
        ErpCustomerDO existing = validateCustomerExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, existing);
        // 更新
        ErpCustomerDO updateObj = BeanUtils.toBean(updateReqVO, ErpCustomerDO.class);
        customerMapper.updateById(updateObj);
    }

    @Override
    public void deleteCustomer(Long id) {
        // 校验存在
        validateCustomerExists(id);
        // 删除
        customerMapper.deleteById(id);
    }

    private ErpCustomerDO validateCustomerExists(Long id) {
        ErpCustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    @Override
    public ErpCustomerDO getCustomer(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public ErpCustomerDO validateCustomer(Long id) {
        ErpCustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(customer.getStatus())) {
            throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
        }
        return customer;
    }

    @Override
    public List<ErpCustomerDO> getCustomerList(Collection<Long> ids) {
        return customerMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpCustomerDO> getCustomerPage(ErpCustomerPageReqVO pageReqVO) {
        return customerMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByStatus(Integer status) {
        return customerMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByNameLike(String name) {
        if (name == null || name.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return customerMapper.selectListByNameLike(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importCustomerList(List<ErpCustomerImportExcelVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ErpCustomerImportExcelVO importVO : list) {
            if (importVO == null || !StringUtils.hasText(importVO.getName())) {
                continue;
            }
            ErpCustomerDO customer = BeanUtils.toBean(importVO, ErpCustomerDO.class);
            if (customer.getStatus() == null) {
                customer.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (customer.getSort() == null) {
                customer.setSort(0);
            }
            fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, customer);
            customerMapper.insert(customer);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCustomer(ErpCustomerBatchUpdateReqVO reqVO) {
        LambdaUpdateWrapper<ErpCustomerDO> wrapper = new LambdaUpdateWrapper<ErpCustomerDO>()
                .in(ErpCustomerDO::getId, reqVO.getIds());
        boolean hasUpdate = false;
        if (reqVO.getSaleUserId() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "saleUserId")) {
            wrapper.set(ErpCustomerDO::getSaleUserId, reqVO.getSaleUserId());
            hasUpdate = true;
        }
        if (reqVO.getDeveloperUserId() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "developerUserId")) {
            wrapper.set(ErpCustomerDO::getDeveloperUserId, reqVO.getDeveloperUserId());
            hasUpdate = true;
        }
        if (reqVO.getDeptId() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "deptId")) {
            wrapper.set(ErpCustomerDO::getDeptId, reqVO.getDeptId());
            hasUpdate = true;
        }
        if (reqVO.getStatus() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "status")) {
            wrapper.set(ErpCustomerDO::getStatus, reqVO.getStatus());
            hasUpdate = true;
        }
        if (reqVO.getPriceLevel() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "priceLevel")) {
            wrapper.set(ErpCustomerDO::getPriceLevel, reqVO.getPriceLevel());
            hasUpdate = true;
        }
        if (reqVO.getRouteId() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "routeId")) {
            wrapper.set(ErpCustomerDO::getRouteId, reqVO.getRouteId());
            hasUpdate = true;
        }
        if (reqVO.getFreightExplainId() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "freightExplainId")) {
            wrapper.set(ErpCustomerDO::getFreightExplainId, reqVO.getFreightExplainId());
            hasUpdate = true;
        }
        if (reqVO.getRemark() != null && !fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "remark")) {
            wrapper.set(ErpCustomerDO::getRemark, reqVO.getRemark());
            hasUpdate = true;
        }
        if (!hasUpdate) {
            return;
        }
        customerMapper.update(null, wrapper);
    }

}
