package kun.kuntv_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;
@Configuration
public class BackblazeB2Config {

    @Value("${backblaze.b2.keyId}")
    private String keyId;

    @Value("${backblaze.b2.applicationKey}")
    private String applicationKey;

    @Value("${backblaze.b2.bucketName}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(keyId, applicationKey);
        return S3Client.builder()
                .credentialsProvider(() -> credentials)
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com")) // Endpoint di Backblaze
                .region(Region.US_EAST_1) // Imposta la regione corretta // Imposta la regione corretta di Backblaze B2
                .build();
    }
}
