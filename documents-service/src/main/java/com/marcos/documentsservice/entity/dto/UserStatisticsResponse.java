package com.marcos.documentsservice.entity.dto;

public record UserStatisticsResponse(
        long totalDocuments,
        long storageUsed
) {
}