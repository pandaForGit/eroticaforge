package com.eroticaforge.dataanalysis.corpus.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * 兼容模型将 intensity 输出为数字或字符串（如 "3" 或 "1-5"）。
 *
 * @author EroticaForge
 */
public class FlexibleIntensityDeserializer extends JsonDeserializer<Integer> {

    private static final int DEFAULT = 3;

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return DEFAULT;
        }
        if (node.isInt()) {
            return clamp(node.intValue());
        }
        if (node.isTextual()) {
            String s = node.asText().trim().replaceAll("[^0-9]", "");
            if (s.isEmpty()) {
                return DEFAULT;
            }
            try {
                return clamp(Integer.parseInt(s));
            } catch (NumberFormatException ex) {
                return DEFAULT;
            }
        }
        return DEFAULT;
    }

    private static int clamp(int v) {
        if (v < 1) {
            return 1;
        }
        if (v > 5) {
            return 5;
        }
        return v;
    }
}
