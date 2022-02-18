package io.github.renestel.core.web.rest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.web.server.Ssl;

@Getter
@Setter
public class BaseRestClientProperties {
    private Ssl ssl;
    private Integer maxTotalConnections;
    private Integer maxDefaultPerRouteConnections;
    private Boolean checkCertificateHostname = Boolean.TRUE;
}
