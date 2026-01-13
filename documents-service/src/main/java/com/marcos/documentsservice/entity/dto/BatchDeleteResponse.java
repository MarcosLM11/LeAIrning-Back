package com.marcos.documentsservice.entity.dto;

public record BatchDeleteResponse(
        int deleted,
        int failed
) {
}