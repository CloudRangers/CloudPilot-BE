package com.cloudrangers.cloudpilot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateConfig {

    /**
     * vCenter처럼 사설망/자가 서명 인증서를 쓰는 서버에 붙기 위한
     * "모든 인증서 신뢰" RestTemplate.
     *
     * ⚠️ 운영에서는 쓰면 안 되고, 지금 과제 개발용으로만 사용!
     */
    @Bean
    public RestTemplate restTemplate() throws Exception {
        // 1) 모든 인증서를 신뢰하는 TrustManager 정의
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        // 2) SSLContext에 적용
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        // 3) 전역 기본 소켓 팩토리/호스트네임 검증기 설정
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        // 4) 기본 HttpURLConnection 기반 RestTemplate 사용
        return new RestTemplate();
    }
}
