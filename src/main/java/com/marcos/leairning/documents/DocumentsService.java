package com.marcos.leairning.documents;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentsService {

    void upload(List<MultipartFile> files);
}
