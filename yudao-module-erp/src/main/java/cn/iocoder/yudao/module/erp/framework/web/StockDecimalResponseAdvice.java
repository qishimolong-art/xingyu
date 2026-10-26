package cn.iocoder.yudao.module.erp.framework.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/** Explicit opt-in leaves default JSON and unrelated callers unchanged. */
@ControllerAdvice(basePackages = "cn.iocoder.yudao.module.erp.controller.admin")
@Order(Ordered.LOWEST_PRECEDENCE) // 原始响应记录完成后，再转换金额等小数的传输格式
public class StockDecimalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper mapper;

    public StockDecimalResponseAdvice(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(MethodParameter parameter, Class<? extends HttpMessageConverter<?>> converter) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converter);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter parameter, MediaType media,
                                  Class<? extends HttpMessageConverter<?>> converter,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String mode = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("decimalMode");
        return "string".equals(mode) ? stringify(mapper.valueToTree(body)) : body;
    }

    static JsonNode stringify(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isFloatingPointNumber()) {
            return TextNode.valueOf(node.decimalValue().toPlainString());
        }
        if (node instanceof ObjectNode object) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.forEach(name -> object.set(name, stringify(node.get(name))));
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < node.size(); i++) {
                array.set(i, stringify(node.get(i)));
            }
        }
        return node;
    }
}
