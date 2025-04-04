package kr.cocoh.api.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${ncloud.access-key}")
    private String accessKey;

    @Value("${ncloud.secret-key}")
    private String secretKey;

    @Value("${ncloud.endpoint}")
    private String endpoint;

    @Value("${ncloud.region}")
    private String region;

    @Value("${ncloud.bucket-name}")
    private String bucketName;

    @Bean
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
    
    // 저장 경로 상수 정의
    public static final class StoragePaths {
        public static final String PROFILES = "profiles";
        public static final String SALONS = "salons";
        public static final String ADS = "ads";
        public static final String TEMP = "temp";
        
        private StoragePaths() {
            // 인스턴스화 방지
        }
    }
}