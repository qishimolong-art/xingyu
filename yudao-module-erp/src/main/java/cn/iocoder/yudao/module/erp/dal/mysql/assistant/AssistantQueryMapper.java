package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface AssistantQueryMapper {
    @SelectProvider(type = AssistantSql.class, method = "select")
    @Options(timeout = 10)
    List<Map<String, Object>> select(Map<String, Object> context);

}
