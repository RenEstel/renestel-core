package io.github.renestel.core.web.rest;

import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.BodyFilters;
import org.zalando.logbook.CorrelationId;
import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.HeaderFilters;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.QueryFilter;
import org.zalando.logbook.QueryFilters;
import org.zalando.logbook.json.JsonBodyFilters;

import java.util.Set;

@Configuration
public class ZalandoConfig {

    public static final String REPLACEMENT_STRING = "XXX";
    public static final int REPLACEMENT_INT = -1;

    private final Set<String> defaultFilters = Set.of("accessToken", "access_token", "authToken", "auth_token", "token", "key", "phone", "email", "refresh_token", "epkId");

    @Bean
    public HttpLogWriter createHttpLogWriter() {
        return new CustomHttpLogWriter();
    }

    @Bean
    public CorrelationId createCorrelationId(Tracer tracer) {
        return new CustomCorrelationId(tracer);
    }

    @Bean
    @ConditionalOnProperty(value = "logbook.query.filters")
    public QueryFilter queryFilter(@Value("${logbook.query.filters}") String[] filters) {
        var filter = QueryFilters.defaultValue();

        for (var item : filters) {
            filter = QueryFilter.merge(
                    filter,
                    QueryFilters.replaceQuery(item, REPLACEMENT_STRING)
            );
        }

        return filter;
    }

    @Bean
    public BodyFilter bodyFilter(
            @Value("${logbook.body.filters.string:}") String[] stringFilter,
            @Value("${logbook.body.filters.number:}") String[] numberFilter,
            @Value("${logbook.body.max-size}") int maxSize
    ) {
        var filter = BodyFilters.defaultValue();
        filter = BodyFilter.merge(filter, BodyFilters.replaceFormUrlEncodedProperty(defaultFilters, REPLACEMENT_STRING));

        for (var item : stringFilter) {
            filter = BodyFilter.merge(
                    filter,
                    JsonBodyFilters.replaceJsonStringProperty(x -> x.contains(item), REPLACEMENT_STRING)
            );
        }

        for (var item : numberFilter) {
            filter = BodyFilter.merge(
                    filter,
                    JsonBodyFilters.replaceJsonNumberProperty(x -> x.contains(item), REPLACEMENT_INT)
            );
        }

        return maxSize > 0
                ? BodyFilter.merge(filter, new StringifyBodyFilter(maxSize))
                : filter;
    }


    @Bean
    public HeaderFilter headerFilter(@Value("${logbook.headers.enabled}") boolean enabled) {
        return enabled
                ? HeaderFilters.defaultValue()
                : HeaderFilters.removeHeaders(s -> true);
    }
}
