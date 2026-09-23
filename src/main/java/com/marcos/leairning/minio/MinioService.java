package com.marcos.leairning.minio;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioService {
    private static final String BUCKET_NAME = "leairning";
    private final S3Template s3Template;

    public void upload(String key, InputStream inputStream) {
        s3Template.upload(BUCKET_NAME,key,inputStream);
    }

    public Resource download(String key) {
        return s3Template.download(BUCKET_NAME,key);
    }

    public void delete(String key) {
        s3Template.deleteObject(BUCKET_NAME,key);
    }
}