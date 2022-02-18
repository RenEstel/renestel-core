package io.github.renestel.core.web.rest;

import io.opentracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.zalando.logbook.CorrelationId;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;

import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class CustomCorrelationId implements CorrelationId {
    private final Tracer tracer;

    private String getMessageId(HttpHeaders headers) {
        if (headers != null) {
            var msgHeader = headers.get("renestel-message-id");
            if (msgHeader != null)
                return msgHeader.get(0);
        }

        return null;
    }

    private String getCorrelationId() {
        var span = tracer.activeSpan();
        if (span != null) {
            return span.context().toSpanId();
        }

        var random = ThreadLocalRandom.current();
        return Long.toHexString(random.nextLong() | -9223372036854775808L);
    }

    @Override
    public String generate(HttpRequest request) {
        String msgId = getMessageId(request.getHeaders());
        var id = getCorrelationId();

        return msgId == null
                ? id
                : String.format("%s:%s", msgId, id);
    }
}
