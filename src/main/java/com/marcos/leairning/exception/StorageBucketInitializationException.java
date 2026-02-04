package com.marcos.leairning.exception;

public class StorageBucketInitializationException extends StorageException {

    public StorageBucketInitializationException(String bucketName, Throwable cause) {
        super("Failed to create or check bucket: " + bucketName, cause);
    }
}