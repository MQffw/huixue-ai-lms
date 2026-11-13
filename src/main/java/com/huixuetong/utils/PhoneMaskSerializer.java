package com.huixuetong.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 手机号脱敏序列化器
 * 只作用于 JSON 输出（返回给前端时脱敏），不影响实体 getter，
 * 因此 MyBatis 写库拿到的仍然是真实号码。
 */
public class PhoneMaskSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.length() < 7) {
            gen.writeString(value);
            return;
        }
        gen.writeString(value.substring(0, 3) + "****" + value.substring(value.length() - 4));
    }
}
