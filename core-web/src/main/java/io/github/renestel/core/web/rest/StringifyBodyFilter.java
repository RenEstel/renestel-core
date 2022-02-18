package io.github.renestel.core.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.zalando.logbook.BodyFilter;

import javax.annotation.Nullable;

public class StringifyBodyFilter implements BodyFilter {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final int maxSize;

    public StringifyBodyFilter(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public String filter(@Nullable String contentType, String body) {

        if (!StringUtils.isEmpty(body) &&
                contentType != null &&
                contentType.toLowerCase().contains("json")
        ) {

            try {
                var json = mapper.writeValueAsString(body);
                return json.length() <= maxSize
                        ? json
                        : json.substring(0, maxSize) + "...\"";
            } catch (JsonProcessingException ex) {
                return "error parse as json string...";
            }

        }

        return body;
    }
}
