package kun.kuntv_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class BackblazeB2Config {

    @Bean
    public Map<String, S3Client> backblazeAccounts(
            @Value("${backblaze.b2.keyId.1}") String keyId1,
            @Value("${backblaze.b2.applicationKey.1}") String applicationKey1,
            @Value("${backblaze.b2.bucketName.1}") String bucketName1,
            @Value("${backblaze.b2.keyId.2}") String keyId2,
            @Value("${backblaze.b2.applicationKey.2}") String applicationKey2,
            @Value("${backblaze.b2.bucketName.2}") String bucketName2) {

        Map<String, S3Client> accounts = new HashMap<>();

        accounts.put(bucketName1, createS3Client(keyId1, applicationKey1));
        accounts.put(bucketName2, createS3Client(keyId2, applicationKey2));

        return accounts;
    }

    private S3Client createS3Client(String keyId, String applicationKey) {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com"))
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    public Map<String, String> keyIdMapping(
            @Value("${backblaze.b2.bucketName.1}") String bucketName1,
            @Value("${backblaze.b2.keyId.1}") String keyId1,
            @Value("${backblaze.b2.bucketName.2}") String bucketName2,
            @Value("${backblaze.b2.keyId.2}") String keyId2) {
        Map<String, String> map = new HashMap<>();
        map.put(bucketName1, keyId1);
        map.put(bucketName2, keyId2);
        return map;
    }

    @Bean
    public Map<String, String> applicationKeyMapping(
            @Value("${backblaze.b2.bucketName.1}") String bucketName1,
            @Value("${backblaze.b2.applicationKey.1}") String applicationKey1,
            @Value("${backblaze.b2.bucketName.2}") String bucketName2,
            @Value("${backblaze.b2.applicationKey.2}") String applicationKey2) {
        Map<String, String> map = new HashMap<>();
        map.put(bucketName1, applicationKey1);
        map.put(bucketName2, applicationKey2);
        return map;
    }
}
