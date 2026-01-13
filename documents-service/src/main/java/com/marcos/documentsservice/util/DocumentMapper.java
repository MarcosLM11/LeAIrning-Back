package com.marcos.documentsservice.util;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.dto.DocumentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {

    DocumentDTO toDTO(Document document);

    Document toEntity(DocumentDTO documentDTO);
}