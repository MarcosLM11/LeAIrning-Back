package com.marcos.documentsservice;

import com.marcos.documentsservice.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class})
@SpringBootTest
class DocumentsServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
