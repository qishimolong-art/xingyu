package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;

import javax.validation.Valid;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 产品 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpProductService {

    /**
     * 创建产品
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProduct(@Valid ProductSaveReqVO createReqVO);

    /**
     * 导入产品
     */
    ErpProductImportRespVO importProductList(List<ErpProductImportExcelVO> list);

    /**
     * 解析 CSV 导入文件
     */
    List<ErpProductImportExcelVO> parseCsvImport(Reader reader);

    /**
     * 更新产品
     *
     * @param updateReqVO 更新信息
     */
    void updateProduct(ProductSaveReqVO updateReqVO);

    /**
     * 批量修改产品
     *
     * @param updateReqVO 批量修改信息
     */
    void batchUpdateProduct(ProductBatchUpdateReqVO updateReqVO);

    /**
     * 删除产品
     *
     * @param id 编号
     */
    void deleteProduct(Long id);

    /**
     * 校验产品们的有效性
     *
     * @param ids 编号数组
     * @return 产品列表
     */
    List<ErpProductDO> validProductList(Collection<Long> ids);

    /**
     * 获得产品
     *
     * @param id 编号
     * @return 产品
     */
    ErpProductDO getProduct(Long id);

    /**
     * 获得产品详情（含库存聚合、仓库名、通用件等完整信息）
     *
     * @param id 编号
     * @return 详情 VO
     */
    ErpProductRespVO getProductDetail(Long id);

    /**
     * 获得指定状态的产品 VO 列表
     *
     * @param status 状态
     * @return 产品 VO 列表
     */
    List<ErpProductRespVO> getProductVOListByStatus(Integer status);

    /**
     * 获得产品 VO 列表
     *
     * @param ids 编号数组
     * @return 产品 VO 列表
     */
    List<ErpProductRespVO> getProductVOList(Collection<Long> ids);

    /**
     * 获得产品 VO Map
     *
     * @param ids 编号数组
     * @return 产品 VO Map
     */
    default Map<Long, ErpProductRespVO> getProductVOMap(Collection<Long> ids) {
        return convertMap(getProductVOList(ids), ErpProductRespVO::getId);
    }

    /**
     * 获得产品 VO 分页
     *
     * @param pageReqVO 分页查询
     * @return 产品分页
     */
    PageResult<ErpProductRespVO> getProductVOPage(ErpProductPageReqVO pageReqVO);

    /**
     * 基于产品分类编号，获得产品数量
     *
     * @param categoryId 产品分类编号
     * @return 产品数量
     */
    Long getProductCountByCategoryId(Long categoryId);

    /**
     * 基于产品单位编号，获得产品数量
     *
     * @param unitId 产品单位编号
     * @return 产品数量
     */
    Long getProductCountByUnitId(Long unitId);

    /**
     * 回写产品的最近采购入库价（由采购入库单审批通过时触发）
     *
     * @param productId 产品编号
     * @param lastPurchasePrice 最近采购价
     */
    void updateProductLastPurchasePrice(Long productId, BigDecimal lastPurchasePrice);

    /**
     * 批量修改产品的货架位（shelf 字段）
     *
     * @param productIds 产品编号集合
     * @param shelf 新货架位
     */
    void updateProductsShelf(Collection<Long> productIds, String shelf);

    /**
     * 查询货架位重复的产品 ID 列表
     *
     * @return 产品 ID 列表（shelf 有重复出现的）
     */
    List<Long> findDuplicateShelfProductIds();

    /**
     * 查询空置货架位的产品 ID 列表（shelf 为 null 或空字符串）
     *
     * @return 产品 ID 列表
     */
    List<Long> findEmptyShelfProductIds();

}
