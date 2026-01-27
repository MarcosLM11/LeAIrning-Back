package com.marcos.leairning.documents;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentsMapper {

    DocumentResponseDTO toDTO(Document document);
}
