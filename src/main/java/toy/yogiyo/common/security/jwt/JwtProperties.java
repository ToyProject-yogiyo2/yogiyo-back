package toy.yogiyo.common.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtProperties {
    private String secret;
    private Long expired;
    private Long refreshExpired;
}
