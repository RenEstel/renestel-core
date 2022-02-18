package io.github.renestel.core.web.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Data
public class SslConfig {

    private boolean enabled = false;
    private boolean sniCheck = false;
    private boolean selfSigned = false;

    private String keystoreLocation = "./config/ssl/keystore.jks";
    private String truststoreLocation = "./config/ssl/truststore.jks";

    @ToString.Exclude
    private String keystorePassword = "123456";
    @ToString.Exclude
    private String trustStorePassword = "123456";

    private String keystoreType = "JKS";
    private String trustStoreType = "JKS";
}

