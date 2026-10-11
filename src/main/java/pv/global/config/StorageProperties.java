package pv.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @NotBlank String bucket,
        @DefaultValue("ap-northeast-2") String region,
        String endpoint,
        String publicEndpoint,
        @DefaultValue("false") boolean pathStyle,
        @NotNull @DefaultValue("50MB") DataSize maxSize,
        @NotNull @DefaultValue("10m") Duration uploadUrlTtl,
        @NotNull @DefaultValue("10m") Duration downloadUrlTtl,
        @NotEmpty Set<String> allowedContentTypes
        ) {
}
