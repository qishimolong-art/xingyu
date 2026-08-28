package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpPrintTemplateMapper extends BaseMapperX<ErpPrintTemplateDO> {

    default ErpPrintTemplateDO selectDefaultByModuleKey(String moduleKey) {
        return selectOne(new LambdaQueryWrapper<ErpPrintTemplateDO>()
                .eq(ErpPrintTemplateDO::getModuleKey, moduleKey)
                .eq(ErpPrintTemplateDO::getDefaulted, true)
                .eq(ErpPrintTemplateDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(ErpPrintTemplateDO::getId)
                .last("LIMIT 1"));
    }

    default List<ErpPrintTemplateDO> selectListByModuleKey(String moduleKey) {
        return selectList(new LambdaQueryWrapper<ErpPrintTemplateDO>()
                .eq(ErpPrintTemplateDO::getModuleKey, moduleKey)
                .eq(ErpPrintTemplateDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(ErpPrintTemplateDO::getDefaulted)
                .orderByDesc(ErpPrintTemplateDO::getUpdateTime)
                .orderByDesc(ErpPrintTemplateDO::getId));
    }

    default void clearDefaultByModuleKey(String moduleKey) {
        update(new ErpPrintTemplateDO().setDefaulted(false), new LambdaUpdateWrapper<ErpPrintTemplateDO>()
                .eq(ErpPrintTemplateDO::getModuleKey, moduleKey)
                .eq(ErpPrintTemplateDO::getDefaulted, true));
    }

}
