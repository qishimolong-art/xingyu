package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpPriceSystemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 价格体系 Service 接口
 *
 * @author Claude
 */
public interface ErpPriceSystemService {

    /**
     * 创建价格体系
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPriceSystem(@Valid ErpPriceSystemSaveReqVO createReqVO);

    /**
     * 更新价格体系
     *
     * @param updateReqVO 更新信息
     */
    void updatePriceSystem(@Valid ErpPriceSystemSaveReqVO updateReqVO);

    /**
     * 批量删除价格体系
     *
     * @param ids 编号数组
     */
    void deletePriceSystem(List<Long> ids);

    /**
     * 获得价格体系
     *
     * @param id 编号
     * @return 价格体系
     */
    ErpPriceSystemDO getPriceSystem(Long id);

    /**
     * 校验价格体系是否存在
     *
     * @param id 编号
     * @return 价格体系
     */
    ErpPriceSystemDO validatePriceSystem(Long id);

    /**
     * 获得价格体系分页
     *
     * @param pageReqVO 分页查询
     * @return 价格体系分页
     */
    PageResult<ErpPriceSystemDO> getPriceSystemPage(ErpPriceSystemPageReqVO pageReqVO);

    /**
     * 获得价格体系列表
     *
     * @param ids 编号数组
     * @return 价格体系列表
     */
    List<ErpPriceSystemDO> getPriceSystemList(Collection<Long> ids);

    /**
     * 获得指定状态的价格体系列表，用于下拉选项
     *
     * @param status 状态
     * @return 价格体系列表
     */
    List<ErpPriceSystemDO> getPriceSystemListByStatus(Integer status);

    /**
     * 获得价格体系 Map
     *
     * @param ids 编号数组
     * @return 价格体系 Map
     */
    default Map<Long, ErpPriceSystemDO> getPriceSystemMap(Collection<Long> ids) {
        return convertMap(getPriceSystemList(ids), ErpPriceSystemDO::getId);
    }

}
