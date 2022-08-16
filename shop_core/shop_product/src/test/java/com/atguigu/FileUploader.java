package com.atguigu;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import io.minio.PutObjectOptions;
import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.errors.MinioException;

public class FileUploader {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException, XmlPullParserException {
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
            MinioClient minioClient = new MinioClient("http://106.52.255.217:9000", "minioadmin", "minioadmin");

            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists("jqw0212");
            if (isExist) {
                System.out.println("Bucket already exists.");
            } else {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket("jqw0212");
            }

            // 使用putObject上传一个文件到存储桶中。
            //参数: 1.文件的桶  2.文件上传成功后的文件名  3.文件流  4.文件参数设置
            FileInputStream inputStream = new FileInputStream("F:\\JavaStudy\\老师视频资料\\商城-张强\\day03\\资料\\图片资源\\三星手机\\梵梦紫2.jpg");
            // 1. inputstream在不被阻塞的情况下, 一次可以读取到的数据长度  2. -为自动检测
            PutObjectOptions putObjectOptions = new PutObjectOptions(inputStream.available(), -1);
            putObjectOptions.setContentType("image/jpeg");
            minioClient.putObject("jqw0212", "new1.jpg",inputStream,putObjectOptions );
            System.out.println("上传成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
        }
    }
}