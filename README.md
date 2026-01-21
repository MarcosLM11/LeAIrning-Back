# LeAIrning-Back

Plataforma de microservicios para gestión documental con capacidades RAG (Retrieval-Augmented Generation). Permite a los usuarios subir documentos e interactuar con ellos mediante chat potenciado por IA.

## Arquitectura General

```mermaid
graph TB
    subgraph Cliente
        UI[Frontend App]
    end

    subgraph API Layer
        GW[API Gateway<br/>:8080]
    end

    subgraph Discovery & Config
        EUR[Discovery Service<br/>Eureka :8761]
        CFG[Config Server<br/>:8888]
    end

    subgraph Business Services
        AUTH[Auth Service<br/>:8083]
        USR[Users Service<br/>:8081]
        DOC[Documents Service<br/>:8082]
        CHAT[Chat Service<br/>:8086]
        NOTIF[Notification Service<br/>:8084]
    end

    subgraph Infrastructure
        PG[(PostgreSQL<br/>:5432)]
        QDR[(Qdrant<br/>:6333/6334)]
        MINIO[(MinIO<br/>:9000)]
        KAFKA[(Kafka<br/>:9092)]
        OLLAMA[Ollama<br/>:11434]
    end

    UI --> GW
    GW --> AUTH
    GW --> USR
    GW --> DOC
    GW --> CHAT

    AUTH --> PG
    USR --> PG
    DOC --> PG
    DOC --> QDR
    DOC --> MINIO
    DOC --> OLLAMA
    CHAT --> QDR
    CHAT --> OLLAMA

    USR -.-> KAFKA
    KAFKA -.-> NOTIF

    AUTH -.-> EUR
    USR -.-> EUR
    DOC -.-> EUR
    CHAT -.-> EUR
    GW -.-> EUR

    CFG -.-> EUR
    AUTH -.-> CFG
    USR -.-> CFG
    DOC -.-> CFG
    CHAT -.-> CFG
```

## Servicios

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| **discovery-service** | 8761 | Eureka Server para service discovery |
| **config-server** | 8888 | Spring Cloud Config Server (configuración centralizada) |
| **api-gateway** | 8080 | Spring Cloud Gateway WebFlux (routing y validación JWT) |
| **auth-service** | 8083 | Autenticación y generación de tokens JWT |
| **users-service** | 8081 | CRUD de usuarios con JPA auditing y migraciones Flyway |
| **documents-service** | 8082 | Gestión documental + pipeline RAG |
| **chat-service** | 8086 | Chat con IA usando RAG sobre documentos |
| **notification-service** | 8084 | Notificaciones por email vía Kafka |

### Orden de Inicio

```mermaid
graph LR
    A[1. discovery-service] --> B[2. config-server]
    B --> C[3. Otros servicios]
```

## Pipeline RAG

El procesamiento de documentos utiliza Spring Cloud Function con streams reactivos:

```mermaid
graph LR
    subgraph Upload
        A[Document Upload] --> B[MinIO Storage]
    end

    subgraph Processing Pipeline
        B --> C[fileSupplier]
        C --> D[documentReader]
        D --> E[splitter]
        E --> F[titleDeterminer]
        F --> G[vectorStoreConsumer]
    end

    subgraph Storage
        G --> H[(Qdrant<br/>Vector Store)]
        B --> I[(MinIO<br/>leairning-documents)]
    end

    subgraph Chat
        J[User Query] --> K[Chat Service]
        K --> H
        H --> L[Relevant Chunks]
        L --> M[Ollama LLM]
        M --> N[AI Response]
    end
```

### Almacenamiento MinIO

- **Bucket documentos**: `leairning-documents` con path `{userId}/{year}/{month}/{uuid}.{ext}`
- **Bucket procesamiento**: `leairning-processing` con prefijos `pending/`, `processed/`, `failed/`

## Infraestructura

| Componente | Puerto | Propósito |
|------------|--------|-----------|
| **PostgreSQL 16** | 5432 | Bases de datos: `usersdb`, `documentsdb`, `authdb` |
| **Qdrant** | 6333/6334 | Vector database para embeddings |
| **MinIO** | 9000/9001 | Object storage S3-compatible |
| **Kafka** | 9092 | Message broker (KRaft mode) |
| **Ollama** | 11434 | LLM local (`llama3.2`, `nomic-embed-text`) |

## Stack Tecnológico

- **Java 25** con Spring Boot 4.0.1 (3.5.x para servicios con Spring AI)
- **Spring AI 1.1.2** para procesamiento documental y embeddings
- **Spring Cloud** (Gateway, Config, Eureka, OpenFeign)
- **MapStruct** para mapeo de DTOs
- **Flyway** para migraciones de base de datos
- **TestContainers** para tests de integración

## Inicio Rápido

### Prerrequisitos

- Java 25
- Docker y Docker Compose
- Ollama instalado localmente

### 1. Iniciar infraestructura

```bash
docker-compose up -d
```

### 2. Iniciar Ollama (separado)

```bash
ollama serve
# En otra terminal, descargar modelos necesarios
ollama pull llama3.2
ollama pull nomic-embed-text
```

### 3. Iniciar servicios

Desde el directorio de cada servicio:

```bash
# 1. Discovery Service (primero)
cd discovery-service && ./mvnw spring-boot:run

# 2. Config Server (segundo)
cd config-server && ./mvnw spring-boot:run

# 3. Resto de servicios (en cualquier orden)
cd auth-service && ./mvnw spring-boot:run
cd users-service && ./mvnw spring-boot:run
cd documents-service && ./mvnw spring-boot:run
cd chat-service && ./mvnw spring-boot:run
```

### 4. Verificar servicios

- Eureka Dashboard: http://localhost:8761
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin123)
- API Gateway: http://localhost:8080

## Comunicación entre Servicios

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Users as Users Service

    Client->>Gateway: POST /api/1.0/auth/login
    Gateway->>Auth: Forward request
    Auth->>Users: GET /internal/users/by-username/{username}
    Users-->>Auth: User details
    Auth-->>Gateway: JWT Token
    Gateway-->>Client: JWT Token

    Client->>Gateway: GET /api/1.0/users (+ JWT)
    Gateway->>Gateway: Validate JWT
    Gateway->>Users: Forward + X-User-Id header
    Users-->>Gateway: User data
    Gateway-->>Client: Response
```

## Convenciones de API

- **Base path**: `/api/1.0/{resource}`
- **Autenticación**: Header `Authorization: Bearer {jwt}`
- **Contexto de usuario**: Headers `X-User-Id` y `X-User-Role` (propagados por Gateway)
- **Uploads**: Multipart form data, máximo 50MB
- **Paginación**: Spring Data Pageable estándar
- **Errores**: `ProblemDetail` (RFC 7807)

## Configuración Centralizada

Los archivos de configuración están en `config-repo/`:

```
config-repo/
├── {service-name}.yml           # Configuración base
├── {service-name}-dev.yml       # Perfil desarrollo
└── {service-name}-prod.yml      # Perfil producción
```

Para encriptar valores sensibles:

```bash
curl -X POST http://configadmin:configsecret@localhost:8888/encrypt -d "valor-secreto"
```

## Tests

Cada servicio incluye tests unitarios y de integración:

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar una clase de test específica
./mvnw test -Dtest=DocumentServiceTest

# Ejecutar un método específico
./mvnw test -Dtest=DocumentServiceTest#testUploadDocument
```

Los tests de integración usan TestContainers para PostgreSQL, MinIO, Qdrant y Ollama.

## Mejoras Futuras

### Corto Plazo
- [ ] Completar implementación de notification-service
- [ ] Añadir soporte para más formatos de documento (DOCX, XLSX)
- [ ] Implementar rate limiting en API Gateway
- [ ] Añadir caché de respuestas con Redis

### Medio Plazo
- [ ] Implementar circuit breaker con Resilience4j
- [ ] Añadir observabilidad completa (Prometheus + Grafana)
- [ ] Distributed tracing con Zipkin/Jaeger
- [ ] Soporte multi-tenant

### Largo Plazo
- [ ] Migración a Kubernetes
- [ ] Soporte para múltiples proveedores LLM (OpenAI, Anthropic)
- [ ] Sistema de plugins para procesadores de documentos
- [ ] Búsqueda semántica avanzada con filtros

## Documentación Técnica

Documentos de diseño detallados en `/docs/`:

| Documento | Descripción |
|-----------|-------------|
| `api-gateway/0001-api-gateway-design.md` | Arquitectura del Gateway y routing |
| `auth-service/0001-auth-security-design.md` | Diseño de seguridad JWT |
| `config-server/0001-config-server-design.md` | Configuración centralizada |
| `documents-service/0001-documents-service-design.md` | Arquitectura y API specs |
| `documents-service/0002-rag-processing-pipeline-design.md` | Pipeline RAG reactivo |
| `documents-service/0003-minio-storage-migration.md` | Abstracción de storage |
| `notification-service/0001-notification-service-design.md` | Sistema de notificaciones |
| `logging/0001-logging-design.md` | Logging estructurado |

## Autor

**Marcos López Marín**

## Licencia

Este proyecto es privado y de uso personal/educativo.