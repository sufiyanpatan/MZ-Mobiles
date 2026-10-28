package mz.mobile.bussiness.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final RestClient client;
    private final String fromNumber;

    public TwilioSmsSender(@Value("${app.sms.twilio.account-sid}") String accountSid,
                           @Value("${app.sms.twilio.auth-token}") String authToken,
                           @Value("${app.sms.twilio.from-number}") String fromNumber
    ) {

        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio credentials are missing. Check your environment variables.");
        }
        this.fromNumber = fromNumber;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));     // never hang a login request
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .defaultHeaders(h -> h.setBasicAuth(accountSid, authToken))
                .build();
    }

    @Override
    public void sendOtp(String mobile, String code, int validMinutes) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", mobile);                    // already +91XXXXXXXXXX
        form.add("From", fromNumber);
        form.add("Body", "Your Second Hand Mobile code is " + code
                + ". Valid for " + validMinutes + " minutes. Do not share it with anyone.");

        try {
            client.post()
                    .uri("/Messages.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Twilio's error body holds a code and a message (it never contains your OTP)
            log.error("Twilio rejected SMS for {}: HTTP {} {}",
                    mask(mobile), e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException("SMS provider failure");
        }  catch (RestClientException e) {
            // never log the OTP, the request body, or the full number
            log.error("Twilio SMS failed for {}: {}", mask(mobile), e.getClass().getSimpleName());
            throw new IllegalStateException("SMS provider failure");
        }
    }

    private String mask(String mobile) {
        return mobile.length() < 4 ? "****" : "*******" + mobile.substring(mobile.length() - 4);
    }
}
