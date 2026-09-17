package cn.iocoder.yudao.module.erp.controller.admin.report.vo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** 避免全局时间戳反序列化器把日期字符串解析为 epoch 0。 */
public class ErpOpeningDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    @Override public LocalDateTime deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        if(parser.currentToken()!=JsonToken.VALUE_STRING)
            throw InvalidFormatException.from(parser,"切换时间必须为 yyyy-MM-dd HH:mm:ss 字符串",parser.getText(),LocalDateTime.class);
        try { return LocalDateTime.parse(parser.getText(),FORMAT); }
        catch(RuntimeException ex) { throw InvalidFormatException.from(parser,"切换时间格式或日期无效",parser.getText(),LocalDateTime.class); }
    }
}
