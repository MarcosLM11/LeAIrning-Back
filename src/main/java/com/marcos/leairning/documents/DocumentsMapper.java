package com.marcos.leairning.documents;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.web.multipart.MultipartFile;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentsMapper {

    Document toEntity(MultipartFile file);

    DocumentResponseDTO toDTO(Document document);
}
