package com.atguigu.utils;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/24 12:00 周日
 * description:
 */
@EnableConfigurationProperties(MinioPropeties.class)
@Configuration
public class MinioUploader {
    @Autowired
    private MinioPropeties minioPropeties;

    @Autowired
    private MinioClient minioClient;

    @Bean
    public MinioClient minioClient() throws Exception {
// 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
        MinioClient minioClient = new MinioClient(minioPropeties.getEndpoint(), minioPropeties.getAccessKey(), minioPropeties.getSecretKey());

        // 检查存储桶是否已经存在
        boolean isExist = minioClient.bucketExists(minioPropeties.getBucketName());
        if (isExist) {
            System.out.println("Bucket already exists.");
        } else {
            // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
            minioClient.makeBucket(minioPropeties.getBucketName());
            //minioClient.setBucketPolicy(minioPropeties.getBucketName(),"");
        }
        return minioClient;
    }

    public String uploadFile(MultipartFile file) throws Exception {

        String fileName = UUID.randomUUID().toString() + file.getOriginalFilename();
        // 使用putObject上传一个文件到存储桶中。
        //参数: 1.文件的桶  2.文件上传成功后的文件名  3.文件流  4.文件参数设置
        InputStream inputStream = file.getInputStream();
        // 1. inputstream在不被阻塞的情况下, 一次可以读取到的数据长度  2. -为自动检测
        PutObjectOptions putObjectOptions = new PutObjectOptions(inputStream.available(), -1);
        putObjectOptions.setContentType(file.getContentType());
        minioClient.putObject(minioPropeties.getBucketName(), fileName, inputStream, putObjectOptions);
        System.out.println("上传成功");
        String retUrl = minioPropeties.getEndpoint() + "/" + minioPropeties.getBucketName() + "/" + fileName;
        return retUrl;
    }
}

