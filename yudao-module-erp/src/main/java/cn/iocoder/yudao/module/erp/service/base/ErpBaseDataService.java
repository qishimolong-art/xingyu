package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 基础数据 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpBaseDataService {

    /**
     * 创建基础数据
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBaseData(@Valid ErpBaseDataSaveReqVO createReqVO);

    /**
     * 更新基础数据
     *
     * @param updateReqVO 更新信息
     */
    void updateBaseData(@Valid ErpBaseDataSaveReqVO updateReqVO);

    /**
     * 删除基础数据
     *
     * @param id 编号
     */
    void deleteBaseData(Long id);

    /**
     * 获得基础数据
     *
     * @param id 编号
     * @return 基础数据
     */
    ErpBaseDataDO getBaseData(Long id);

    /**
     * 获得基础数据分页
     *
     * @param pageReqVO 分页查询
     * @return 基础数据分页
     */
    PageResult<ErpBaseDataDO> getBaseDataPage(ErpBaseDataPageReqVO pageReqVO);

    /**
     * 按类型获取启用的基础数据列表
     *
     * @param type 数据类型
     * @return 基础数据列表
     */
    List<ErpBaseDataDO> getBaseDataSimpleListByType(String type);

}
