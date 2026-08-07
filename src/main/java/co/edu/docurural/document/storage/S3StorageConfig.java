package co.edu.docurural.document.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "docurural.storage", name = "provider", havingValue = "s3")
    public S3Client s3Client(StorageProperties storageProperties) {
        String region = storageProperties.getS3().getRegion();
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

