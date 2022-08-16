package com.atguigu.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/24 11:47 周日
 * description:
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioPropeties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
