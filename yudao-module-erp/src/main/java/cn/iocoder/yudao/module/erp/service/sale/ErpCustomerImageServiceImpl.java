package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerImageMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMAGE_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerImageServiceImpl implements ErpCustomerImageService {

    @Resource
    private ErpCustomerImageMapper imageMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createImage(ErpCustomerImageSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerImageDO image = BeanUtils.toBean(createReqVO, ErpCustomerImageDO.class);
        imageMapper.insert(image);
        return image.getId();
    }

    @Override
    public void updateImage(ErpCustomerImageSaveReqVO updateReqVO) {
        ErpCustomerImageDO existing = validateImageExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            throw exception(CUSTOMER_IMAGE_NOT_EXISTS);
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        imageMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerImageDO.class));
    }

    @Override
    public void deleteImage(Long id) {
        validateImageExists(id);
        imageMapper.deleteById(id);
    }

    private ErpCustomerImageDO validateImageExists(Long id) {
        ErpCustomerImageDO existing = imageMapper.selectById(id);
        if (existing == null) throw exception(CUSTOMER_IMAGE_NOT_EXISTS);
        validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    private void validateVisibleCustomer(Long customerId) {
        if (customerId == null || customerService.getCustomer(customerId) == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerImageDO getImage(Long id) {
        ErpCustomerImageDO existing = imageMapper.selectById(id);
        if (existing != null) validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    @Override
    public PageResult<ErpCustomerImageDO> getImagePage(ErpCustomerImagePageReqVO pageReqVO) {
        validateVisibleCustomer(pageReqVO.getCustomerId());
        if (pageReqVO.getPageSize() <= 0 || pageReqVO.getPageSize() > 100) pageReqVO.setPageSize(100);
        return imageMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerImageDO> getImageListByCustomerId(Long customerId) {
        validateVisibleCustomer(customerId);
        return imageMapper.selectListByCustomerId(customerId);
    }

}
