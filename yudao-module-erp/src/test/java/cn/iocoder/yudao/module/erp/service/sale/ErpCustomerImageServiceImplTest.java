package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerImageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMAGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ErpCustomerImageServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerImageServiceImpl customerImageService;

    @Mock
    private ErpCustomerImageMapper imageMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createImage ====================

    @Test
    public void testCreateImage_success() {
        // 准备参数
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setImageType("门头照");
        reqVO.setImageName("test.jpg");
        reqVO.setImageUrl("http://example.com/test.jpg");
        reqVO.setDefaulted(false);
        reqVO.setSort(0);

        // mock：insert 回填 id
        when(imageMapper.insert(ArgumentMatchers.<ErpCustomerImageDO>any())).thenAnswer(invocation -> {
            ErpCustomerImageDO image = invocation.getArgument(0);
            image.setId(100L);
            return 1;
        });

        // 执行
        Long resultId = customerImageService.createImage(reqVO);

        // 断言
        assertEquals(100L, resultId);
        verify(customerService).validateCustomer(eq(1L));
        verify(imageMapper).insert(ArgumentMatchers.<ErpCustomerImageDO>argThat(image ->
                image.getCustomerId().equals(1L)
                        && "http://example.com/test.jpg".equals(image.getImageUrl())
                        && "门头照".equals(image.getImageType())));
    }

    @Test
    public void testCreateImage_customerNotExists() {
        // 准备参数
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setCustomerId(999L);
        reqVO.setImageUrl("http://example.com/test.jpg");

        // mock：客户校验抛异常
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(999L));

        // 执行 & 断言
        assertServiceException(() -> customerImageService.createImage(reqVO), CUSTOMER_NOT_EXISTS);
        verify(imageMapper, never()).insert(ArgumentMatchers.<ErpCustomerImageDO>any());
    }

    // ==================== updateImage ====================

    @Test
    public void testUpdateImage_success() {
        // 准备参数
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setImageUrl("http://example.com/new.jpg");

        // mock：存在校验通过
        when(imageMapper.selectById(eq(10L))).thenReturn(new ErpCustomerImageDO().setId(10L));

        // 执行
        customerImageService.updateImage(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(1L));
        verify(imageMapper).updateById(ArgumentMatchers.<ErpCustomerImageDO>argThat(image ->
                image.getId().equals(10L)
                        && "http://example.com/new.jpg".equals(image.getImageUrl())));
    }

    @Test
    public void testUpdateImage_notExists() {
        // 准备参数
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setId(999L);
        reqVO.setCustomerId(1L);
        reqVO.setImageUrl("http://example.com/new.jpg");

        // mock：不存在
        when(imageMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerImageService.updateImage(reqVO), CUSTOMER_IMAGE_NOT_EXISTS);
        verify(imageMapper, never()).updateById(ArgumentMatchers.<ErpCustomerImageDO>any());
    }

    // ==================== deleteImage ====================

    @Test
    public void testDeleteImage_success() {
        // mock：存在
        when(imageMapper.selectById(eq(20L))).thenReturn(new ErpCustomerImageDO().setId(20L));

        // 执行
        customerImageService.deleteImage(20L);

        // 断言
        verify(imageMapper).deleteById(eq(20L));
    }

    @Test
    public void testDeleteImage_notExists() {
        // mock：不存在
        when(imageMapper.selectById(eq(999L))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerImageService.deleteImage(999L), CUSTOMER_IMAGE_NOT_EXISTS);
        verify(imageMapper, never()).deleteById(any());
    }

    // ==================== getImageListByCustomerId ====================

    @Test
    public void testGetImageListByCustomerId_success() {
        // 准备数据
        Long customerId = 5L;
        ErpCustomerImageDO image1 = new ErpCustomerImageDO().setId(1L).setCustomerId(customerId).setImageUrl("a.jpg");
        ErpCustomerImageDO image2 = new ErpCustomerImageDO().setId(2L).setCustomerId(customerId).setImageUrl("b.jpg");
        when(imageMapper.selectListByCustomerId(eq(customerId))).thenReturn(Arrays.asList(image1, image2));

        // 执行
        List<ErpCustomerImageDO> result = customerImageService.getImageListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        verify(customerService).validateCustomer(eq(customerId));
        verify(imageMapper).selectListByCustomerId(eq(customerId));
    }

}
