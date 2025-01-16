package com.springboot.vitalorganize.config;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Configuration
@ConfigurationProperties(prefix = "paypal")
public class PayPalConfig {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Bean
    public APIContext getAPIContext() throws PayPalRESTException {
        Map<String, String> sdkConfig = new HashMap<>();
        sdkConfig.put("mode", mode);

        OAuthTokenCredential authTokenCredential = new OAuthTokenCredential(clientId, clientSecret, sdkConfig);
        String accessToken = authTokenCredential.getAccessToken();

        APIContext apiContext = new APIContext(accessToken);
        apiContext.setConfigurationMap(sdkConfig);

        return apiContext;

    }


}
