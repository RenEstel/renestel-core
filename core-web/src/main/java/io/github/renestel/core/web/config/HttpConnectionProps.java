package io.github.renestel.core.web.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HttpConnectionProps {
    private String baseUrl;
    private boolean debug;
    private int connectionTimeout = 2;
    private int readTimeout = 2;
    private int writeTimeout = 2;
    private SslConfig ssl;
}
