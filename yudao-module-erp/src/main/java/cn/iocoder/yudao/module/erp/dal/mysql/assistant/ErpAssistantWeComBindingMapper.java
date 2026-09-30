package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.assistant.ErpAssistantWeComBindingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpAssistantWeComBindingMapper extends BaseMapperX<ErpAssistantWeComBindingDO> {

    default ErpAssistantWeComBindingDO selectByBotAndWeComUser(String botId, String wecomUserId) {
        return selectOne(ErpAssistantWeComBindingDO::getBotId, botId,
                ErpAssistantWeComBindingDO::getWecomUserId, wecomUserId);
    }

    default ErpAssistantWeComBindingDO selectByBotAndUser(String botId, Long userId) {
        return selectOne(ErpAssistantWeComBindingDO::getBotId, botId,
                ErpAssistantWeComBindingDO::getUserId, userId);
    }

}
