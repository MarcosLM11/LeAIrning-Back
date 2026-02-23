<p align="center">
  <h1 align="center">LeAIrning Backend</h1>
  <p align="center">
    <strong>Monolito Spring Boot con IA generativa, gestión documental y pipeline RAG</strong>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot 4.0.2"/>
    <img src="https://img.shields.io/badge/Spring%20AI-2.0.0--M2-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring AI"/>
    <img src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
    <img src="https://img.shields.io/badge/Qdrant-Vector%20DB-DC382D?style=flat-square" alt="Qdrant"/>
    <img src="https://img.shields.io/badge/MinIO-S3%20Storage-C72E49?style=flat-square&logo=minio&logoColor=white" alt="MinIO"/>
  </p>
</p>

---

## Tabla de Contenidos

- [Descripcion](#descripcion)
- [Arquitectura](#arquitectura)
- [Estructura de Paquetes](#estructura-de-paquetes)
- [Dependencias Principales](#dependencias-principales)
- [Configuracion](#configuracion)
- [Base de Datos y Migraciones](#base-de-datos-y-migraciones)
- [Seguridad](#seguridad)
- [Pipeline RAG](#pipeline-rag)
- [Modulo de IA](#modulo-de-ia)
- [Cache](#cache)
- [Email](#email)
- [Logging](#logging)
- [Testing](#testing)
- [Build y Ejecucion](#build-y-ejecucion)
- [API Endpoints](#api-endpoints)

---

## Descripcion

El backend de LeAIrning es un **monolito modular** construido con Spring Boot 4.0.2 y Java 25. Encapsula toda la lógica de negocio: gestión de usuarios, almacenamiento de documentos, procesamiento RAG, chat con IA, generación de quizzes y un sistema de seguridad multi-capa con OAuth2 y JWT.

Cada dominio de negocio está organizado en su propio paquete siguiendo una **arquitectura por capas** (Controller → Service → Repository), con DTOs mapeados por MapStruct y excepciones de dominio específicas.

El front de la aplicación se encuentra en [LeAIrningFront](https://github.com/MarcosLM11/LeAIrning-Front).

### Funcionalidades principales

| Funcionalidad | Descripción |
|---|---|
| **Gestión de usuarios** | CRUD completo con registro, verificación por email, login tradicional y OAuth2 (Google, GitHub) |
| **Gestión de documentos** | Subida multipart, almacenamiento en MinIO (S3-compatible), listado y eliminación por usuario |
| **Pipeline RAG** | Procesamiento de documentos: lectura (Tika) → chunking (Tokenizer) → embeddings (Ollama) → indexación (Qdrant) |
| **Chat con IA** | Conversaciones contextualizadas con RAG: recuperación de fragmentos relevantes y generación de respuestas con Ollama |
| **Generación de quizzes** | Creación automática de cuestionarios (multiple choice, verdadero/falso) a partir del contenido documental |
| **Seguridad multi-capa** | JWT (HS512) con refresh token rotation, OAuth2, auth code exchange, rate limiting, protección brute-force |
| **Email asíncrono** | Verificación de cuenta y bienvenida mediante plantillas Thymeleaf con envío `@Async` |
| **Caché** | Caffeine para usuarios, documentos, rate-limit buckets y auth codes temporales |
| **Observabilidad** | Logging estructurado (Flogger + Logstash JSON), request logging filter, OpenTelemetry |

---

## Arquitectura

### Capas del monolito

```
┌─────────────────────────────────────────────────────────┐
│                    Controllers (REST)                    │
│  AuthController · UsersController · DocumentsController  │
│  ChatAIController · ConversationController · Quizz...    │
├─────────────────────────────────────────────────────────┤
│                      Services                            │
│  AuthService · UsersService · DocumentsService           │
│  ChatService · ConversationService · QuizzService        │
│  JwtService · TokenPairService · EmailService            │
├─────────────────────────────────────────────────────────┤
│                    Repositories (JPA)                     │
│  UsersRepository · DocumentsRepository                   │
│  ConversationRepository · QuizzRepository                │
├─────────────────────────────────────────────────────────┤
│                   Infraestructura                         │
│  PostgreSQL · MinIO · Qdrant · Ollama · Kafka · Caffeine │
└─────────────────────────────────────────────────────────┘
```

### Patrones clave

| Patrón | Implementación |
|---|---|
| **Layered Architecture** | Controller → Service → Repository en cada módulo |
| **DTO Pattern** | MapStruct para mapeo Entity ↔ DTO |
| **Configuration Properties** | `@ConfigurationProperties` para binding tipado (`JwtProperties`, `MinioProperties`, etc.) |
| **Global Exception Handling** | `@ControllerAdvice` con respuestas `ProblemDetail` (RFC 9457) |
| **Async Processing** | `@Async` para emails y pipeline de documentos |
| **Spring Cloud Function** | Pipeline composable para procesamiento de documentos |
| **Auditing** | Entidades base con `@CreatedDate`, `@LastModifiedDate` vía JPA Auditing |

---

## Estructura de Paquetes

```
com.marcos.leairning/
│
├── LeAIrningBackApplication.java       # Punto de entrada
│
├── ai/
│   ├── chat/                           # Chat conversacional con RAG
│   │   ├── config/                     # ChatAIConfig, post-procesadores
│   │   │   ├── ChatAIConfig            #   Configuración Spring AI advisor
│   │   │   ├── CompressionDocumentPostProcessor
│   │   │   └── TranslateResponsePostProcessor
│   │   ├── controller/
│   │   │   ├── ChatAIController        #   POST /api/v1/chat
│   │   │   └── ConversationController  #   CRUD /api/v1/conversations
│   │   ├── dto/                        #   ChatRequestDTO, ChatResponseDTO, etc.
│   │   ├── model/                      #   Conversation (JPA entity)
│   │   ├── repository/                 #   ConversationRepository
│   │   ├── service/                    #   ChatService, ConversationService
│   │   │   └── ollama/                 #   ChatServiceOllamaImpl
│   │   └── util/                       #   ConversationMapper
│   │
│   └── quizz/                          # Generación de quizzes por IA
│       ├── QuizzController             #   POST /api/v1/quizzes
│       ├── QuizzService                #   Lógica de generación
│       ├── QuizzEntity                 #   JPA entity
│       ├── QuizzRepository
│       └── models/                     #   Quizz, Question, QuestionType, GeneratedQuizz
│
├── cache/
│   ├── CacheConfig                     # Beans Caffeine por nombre
│   ├── CaffeineCacheProperties         # Configuración de tamaños y TTL
│   └── RateLimitKeyProvider            # Clave por IP para rate limiting
│
├── documents/
│   ├── Document                        # JPA entity (fichero + metadatos)
│   ├── DocumentResponseDTO
│   ├── DocumentsMapper                 # MapStruct
│   ├── DocumentsController             # CRUD + upload multipart
│   ├── DocumentsService / Impl
│   └── DocumentsRepository
│
├── email/
│   ├── EmailService                    # Interfaz
│   └── EmailServiceImpl                # @Async, Spring Mail + Thymeleaf
│
├── exception/
│   ├── GlobalExceptionHandler          # @ControllerAdvice → ProblemDetail
│   ├── AccountLockedException
│   ├── AccountNotVerifiedException
│   ├── DocumentNotFoundException
│   ├── DocumentAccessDeniedException
│   ├── DocumentProcessingException
│   ├── ConversationNotFoundException
│   ├── QuizzNotFoundException
│   ├── EmailAlreadyRegisteredException
│   ├── InvalidCredentialsException
│   ├── InvalidVerificationTokenException
│   ├── StorageException
│   ├── StorageOperationException
│   ├── StorageBucketInitializationException
│   ├── VectorStoreException
│   ├── DocumentReaderException
│   └── UserNotFoundException
│
├── logging/
│   ├── LoggingConfiguration            # Setup global
│   └── RequestLoggingFilter            # Log de peticiones HTTP
│
├── minio/
│   ├── MinioConfig                     # Bean MinioClient
│   ├── MinioProperties                 # @ConfigurationProperties
│   ├── MinioDocumentStorageService     # Operaciones de documentos
│   └── MinioProcessingPipelineService  # Operaciones del pipeline
│
├── pipeline/
│   ├── DocumentProcessingPipeline      # Spring Cloud Function pipeline
│   ├── DocumentContext                 # Contexto de ejecución
│   ├── FileSupplierConfig              # Supplier de documentos
│   └── PipelineProperties              # chunk-size, min-chunk-length
│
├── security/
│   ├── AbstractSecurityConfiguration   # Base: CSRF off, stateless sessions
│   ├── DefaultWebSecurityConfiguration # Catch-all chain + PasswordEncoder
│   │
│   ├── annotations/
│   │   ├── @BusinessAuthorityOnly      # Acceso negocio
│   │   └── @RefreshTokenAuthorityOnly  # Acceso refresh
│   │
│   ├── auth/
│   │   ├── AuthController              # POST /auth/login, /auth/register, /auth/verify
│   │   ├── AuthService / Impl          # Lógica de autenticación
│   │   ├── LoginAttemptService / Impl  # Protección brute-force
│   │   ├── LogoutController            # POST /auth/logout
│   │   ├── LoginRequestDTO, RegisterRequestDTO, AuthCodeResponse
│   │   ├── AuthProperties              # Frontend URL, etc.
│   │   └── LoginLockoutProperties      # Max intentos, duración lockout
│   │
│   ├── code/
│   │   ├── AuthCodeExchangeController  # POST /auth/token (exchange code)
│   │   └── AuthCodeAuthenticationSuccessHandler
│   │
│   ├── jwt/
│   │   ├── JwtService                  # Generación/validación HS512
│   │   ├── JwtProperties               # TTL access/refresh
│   │   ├── JwtSecretProperties         # Secret + algorithm
│   │   ├── JwtSecurityConfiguration    # Filter chain JWT
│   │   ├── RevokedTokenService         # Revocación de tokens
│   │   └── RevokedTokenValidator       # Validador de tokens revocados
│   │
│   ├── oauth2/
│   │   ├── Oauth2SuccessHandler        # Handler post-OAuth2
│   │   ├── Oauth2SecurityConfiguration # Filter chain OAuth2
│   │   ├── Oauth2UserCreateDTO
│   │   └── GitHubEmailService          # Fetch email de GitHub API
│   │
│   ├── refreshtoken/
│   │   └── RefreshTokenController      # POST /auth/refresh (rotation)
│   │
│   └── token/
│       ├── TokenPair                   # Access + Refresh tokens
│       └── TokenPairService / Impl     # Generación y cache de pares
│
├── users/
│   ├── User                            # JPA entity (UUID, email, name, password, provider)
│   ├── UserResponseDTO, UserCreateDTO, UserUpdateDTO
│   ├── UsersMapper                     # MapStruct
│   ├── UsersController                 # CRUD /api/v1/users
│   ├── UsersService / Impl
│   └── UsersRepository
│
└── util/
    ├── jpa/
    │   ├── AbstractJpaAuditableEntity           # Base con audit fields
    │   └── AbstractJpaVersionedAuditableEntity   # + @Version
    ├── logging/
    │   └── LoggingUtils                          # Config Flogger
    ├── template/
    │   └── TemplateService                       # Procesamiento Thymeleaf
    └── web/
        ├── @CurrentUserId                        # Anotación custom
        ├── CurrentUserIdArgumentResolver          # Resolver del JWT
        └── WebConfig                              # WebMvcConfigurer
```

---

## Dependencias Principales

### Framework y Runtime

| Dependencia | Versión | Propósito |
|---|---|---|
| Spring Boot | 4.0.2 | Framework base con auto-configuración |
| Spring Web | 4.0.2 | REST controllers y MVC |
| Spring Data JPA | 4.0.2 | Acceso a datos con Hibernate |
| Spring Security | 7.1.0-M1 | Autenticación y autorización |
| Spring AI (BOM) | 2.0.0-M2 | Integración con modelos de IA |
| Spring Cloud (BOM) | 2025.1.1 | Cloud Function para pipeline |

### IA y Procesamiento

| Dependencia | Versión | Propósito |
|---|---|---|
| spring-ai-ollama | 2.0.0-M2 | Chat y embeddings con modelos locales |
| spring-ai-qdrant | 2.0.0-M2 | Vector store para RAG |
| spring-ai-tika-document-reader | 2.0.0-M2 | Lectura de documentos |
| tika-langdetect-optimaize | 3.0.0 | Detección de idioma |

### Infraestructura

| Dependencia | Versión | Propósito |
|---|---|---|
| postgresql (driver) | Runtime | Conexión a PostgreSQL 18 |
| flyway-database-postgresql | Latest | Migraciones de esquema |
| minio | 8.6.0 | Cliente S3-compatible |
| caffeine | Latest | Caché en memoria |
| bucket4j-spring-boot-starter | 0.14.0-RC1 | Rate limiting |

### Seguridad

| Dependencia | Versión | Propósito |
|---|---|---|
| spring-security-oauth2-authorization-server | 7.1.0-M1 | OAuth2 server |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.5 | JWT HS512 |

### Utilidades

| Dependencia | Versión | Propósito |
|---|---|---|
| mapstruct | 1.6.3 | Mapeo automático Entity ↔ DTO |
| lombok | Latest | Reducción de boilerplate |
| flogger + flogger-slf4j-backend | 0.9 | Logging fluido |
| spring-boot-starter-opentelemetry | Latest | Trazas distribuidas |

### Testing

| Dependencia | Versión | Propósito |
|---|---|---|
| spring-boot-starter-test | 4.0.2 | JUnit 5, Mockito, AssertJ |
| spring-boot-testcontainers | 4.0.2 | Integración TestContainers |
| testcontainers-postgresql | 1.21.4+ | Container PostgreSQL |
| testcontainers-qdrant | 1.21.4+ | Container Qdrant |
| testcontainers-ollama | 1.21.4+ | Container Ollama |
| testcontainers-minio | 1.21.4+ | Container MinIO |

---

## Configuracion

La configuración reside en `src/main/resources/application.yaml`:

### Propiedades principales

```yaml
# JWT
leairning.jwt.access-token-ttl: 30m
leairning.jwt.refresh-token-ttl: 7d
leairning.secret.jwt.algorithm: HS512

# MinIO
leairning.storage.minio.endpoint: http://localhost:9000
leairning.storage.minio.buckets.documents: leairning-documents
leairning.storage.minio.buckets.processing: leairning-processing
leairning.storage.minio.auto-create-buckets: true

# Pipeline RAG
leairning.pipeline.chunk-size: 500
leairning.pipeline.keep-separator: false
leairning.pipeline.min-chunk-length: 10

# Cache Caffeine
leairning.cache.caffeine.documents: 1000 items, 60s TTL
leairning.cache.caffeine.users: 1000 items, 60s TTL
leairning.cache.caffeine.rate-limit: 100000 items, 1h TTL

# Seguridad
leairning.auth.frontend-url: http://localhost:4200
leairning.auth.login-lockout.max-attempts: 5
leairning.auth.login-lockout.lockout-duration: 15m

# IA (Ollama)
spring.ai.ollama.base-url: http://localhost:11434
spring.ai.ollama.chat.options.model: granite3.3:8b
spring.ai.ollama.embedding.options.model: granite-embedding:278m

# Vector Store (Qdrant)
spring.ai.vectorstore.qdrant.host: localhost
spring.ai.vectorstore.qdrant.port: 6334
spring.ai.vectorstore.qdrant.collection-name: documents

# Base de datos
spring.datasource.url: jdbc:postgresql://localhost:5432/leairningdb

# Email
spring.mail.host: smtp.gmail.com
spring.mail.port: 587

# Ficheros
spring.servlet.multipart.max-file-size: 50MB
```

---

## Base de Datos y Migraciones

### Esquema gestionado por Flyway

Las migraciones residen en `src/main/resources/db/migration/`:

| Versión | Fichero | Descripción |
|---|---|---|
| V1 | `create_users_table.sql` | Tabla `users` con UUID, email, nombre, password, verificado |
| V2 | `create_documents_table.sql` | Tabla `documents` con FK a users, metadatos de fichero |
| V3 | `add_verified_to_users.sql` | Columna `verified` en users |
| V4 | `create_conversations_tables.sql` | Tablas de conversación y mensajes |
| V5 | `create_spring_ai_chat_memory_table.sql` | Tabla de memoria de chat Spring AI |
| V6 | `recreate_spring_ai_chat_memory_table.sql` | Corrección de esquema de memoria |
| V7 | `remove_duplicate_chat_messages.sql` | Limpieza de datos duplicados |
| V8 | `add_provider_to_users.sql` | Columna `provider` para OAuth2 (local/google/github) |
| V9 | `create_quizz_table.sql` | Tabla de almacenamiento de quizzes |

### Entidades JPA

Las entidades heredan de clases base auditables:

```
AbstractJpaAuditableEntity (UUID id, createdDate, lastModifiedDate)
    │
    ├── User (email, name, password, verified, provider)
    ├── Document (fileName, contentType, size, userId)
    ├── Conversation (title, userId)
    └── QuizzEntity (content, userId, documentId)

AbstractJpaVersionedAuditableEntity extends AbstractJpaAuditableEntity
    └── + @Version para optimistic locking
```

---

## Seguridad

### Arquitectura de filter chains

El sistema de seguridad utiliza múltiples `SecurityFilterChain` con orden de prioridad:

```
Request
  │
  ├──► JwtSecurityConfiguration      (Orden 1: /api/**)
  │    JWT Bearer token validation
  │
  ├──► Oauth2SecurityConfiguration   (Orden 2: /oauth2/**)
  │    OAuth2 login flow (Google, GitHub)
  │
  └──► DefaultWebSecurityConfiguration (Orden 3: catch-all)
       Endpoints públicos (/auth/login, /auth/register, etc.)
```

### Flujo de autenticación completo

```
1. REGISTRO
   POST /auth/register → Crear user (unverified) → Email verificación async
   User verifica email → POST /auth/verify → Email bienvenida async → Auth code

2. LOGIN TRADICIONAL
   POST /auth/login → Validar credenciales → Generar JWT pair
   → Almacenar en Caffeine cache como auth code (60s TTL)
   → Devolver auth code al cliente
   → POST /auth/token → Intercambiar code por tokens

3. OAUTH2 (Google/GitHub)
   GET /oauth2/authorization/{provider} → Redirect a provider
   → Callback → Crear/actualizar user → Generar JWT pair
   → Redirect a frontend con auth code

4. REFRESH
   POST /auth/refresh → Validar refresh token → Rotar stateless
   → Nuevo par de tokens (sin DB)

5. LOGOUT
   POST /auth/logout → Revocar tokens actuales
```

### Protección contra ataques

- **Brute-force**: `LoginAttemptService` bloquea tras 5 intentos fallidos (15min)
- **Rate limiting**: Bucket4j con claves por IP
- **Token revocation**: `RevokedTokenService` + `RevokedTokenValidator`
- **CSRF**: Deshabilitado (API stateless con JWT)
- **Sessions**: Stateless (`STATELESS` session creation policy)

---

## Pipeline RAG

Implementado con **Spring Cloud Function** como una composición de funciones:

```java
// Composición declarativa del pipeline
supplier → reader → splitter → embedder → storer
```

### Componentes del pipeline

| Componente | Clase | Función |
|---|---|---|
| **Supplier** | `FileSupplierConfig` | Obtiene el documento desde MinIO |
| **Reader** | Spring AI Tika | Extrae texto del documento |
| **Splitter** | Spring AI Tokenizer | Divide en chunks (500 tokens) |
| **Embedder** | Ollama `granite-embedding:278m` | Genera embeddings vectoriales |
| **Storer** | Qdrant Vector Store | Almacena vectores con metadatos |

### Post-procesadores de chat

| Post-procesador | Función |
|---|---|
| `CompressionDocumentPostProcessor` | Comprime documentos largos recuperados |
| `TranslateResponsePostProcessor` | Traduce respuestas al idioma del usuario |

---

## Modulo de IA

### Chat con RAG

El módulo `ai/chat/` proporciona chat conversacional contextualizado:

- **ChatAIController**: Recibe preguntas del usuario y devuelve respuestas generadas
- **ChatServiceOllamaImpl**: Implementación con Ollama como LLM local
  - Modelo de chat: `granite3.3:8b` (contexto: 4096 tokens)
  - Modelo de embeddings: `granite-embedding:278m`
- **ConversationService**: Gestión de historial de conversaciones (CRUD)
- **Spring AI Advisor**: Configura el RAG advisor con Qdrant como retriever

### Generación de Quizzes

El módulo `ai/quizz/` genera cuestionarios automáticos:

- **QuizzService**: Envía prompt al LLM con contenido documental
- **QuizzEntity**: Persiste quizzes generados en PostgreSQL
- Modelos: `Quizz`, `Question`, `QuestionType` (multiple choice, true/false, etc.)

---

## Cache

Caché en memoria con **Caffeine**, configurada por nombre:

| Cache | Max Items | TTL | Uso |
|---|---|---|---|
| `documents` | 1,000 | 60s | Documentos por usuario |
| `users` | 1,000 | 60s | Datos de usuario |
| `rate-limit` | 100,000 | 1h | Buckets de rate limiting por IP |
| Auth codes | - | 60s | Códigos de intercambio temporales |

---

## Email

Sistema de email asíncrono (`@Async`) con plantillas Thymeleaf:

| Plantilla | Evento | Contenido |
|---|---|---|
| `verification-email.html` | Registro | Link de verificación |
| `welcome-email.html` | Verificación exitosa | Bienvenida al usuario |

Cada plantilla tiene versión HTML y texto plano. El envío es a través de Spring Mail (SMTP Gmail, puerto 587).

---

## Logging

### Configuración

- **Framework**: Flogger con backend SLF4J
- **Formato**: Texto plano (consola) + JSON estructurado (Logstash) en fichero
- **Rotación**: 10MB por fichero, 30 días retención, 500MB máximo total
- **Filtro HTTP**: `RequestLoggingFilter` registra cada petición/respuesta

### Convención de logs

```java
// Siempre usar placeholders, nunca concatenación
logger.info("{} documents found for user {}", count, userId);
```

---

## Testing

### Estructura de tests (36 clases)

```
src/test/java/com/marcos/leairning/
├── AbstractRepositoryTest              # Base para tests de repositorio
├── DemoApplicationTests                # @SpringBootTest (requiere Docker)
│
├── users/
│   ├── UsersControllerTest
│   ├── UsersServiceImplTest
│   ├── UsersRepositoryTest
│   └── UsersMapperTest
│
├── documents/
│   ├── DocumentsControllerTest
│   ├── DocumentsServiceImplTest
│   ├── DocumentsRepositoryTest
│   └── DocumentsMapperTest
│
├── ai/chat/
│   ├── ChatAIControllerTest
│   ├── ConversationControllerTest
│   ├── ConversationServiceImplTest
│   ├── ConversationRepositoryTest
│   ├── ChatServiceOllamaImplTest
│   └── ConversationMapperTest
│
├── ai/quizz/
│   ├── QuizzControllerTest
│   ├── QuizzServiceTest
│   └── QuizzRepositoryTest
│
├── security/
│   ├── auth/
│   │   ├── AuthControllerTest
│   │   ├── AuthServiceImplTest
│   │   ├── LoginAttemptServiceImplTest
│   │   └── LogoutControllerTest
│   ├── jwt/
│   │   ├── JwtServiceTest
│   │   └── RevokedTokenServiceTest
│   ├── code/
│   │   └── AuthCodeExchangeControllerTest
│   ├── refreshtoken/
│   │   └── RefreshTokenControllerTest
│   └── token/
│       └── TokenPairServiceImplTest
│
├── cache/
│   └── RateLimitKeyProviderTest
├── email/
│   └── EmailServiceImplTest
├── exception/
│   └── GlobalExceptionHandlerTest
├── logging/
│   └── RequestLoggingFilterTest
├── minio/
│   └── MinioDocumentStorageServiceTest
├── pipeline/
│   └── DocumentProcessingPipelineTest
└── util/
    ├── CurrentUserIdArgumentResolverTest
    └── TemplateServiceTest
```

### Estrategia de testing

| Tipo | Herramientas | Características |
|---|---|---|
| **Unit** | Mockito, JUnit 5 | < 10ms, sin dependencias externas |
| **Integration** | TestContainers | PostgreSQL, MinIO, Qdrant, Ollama en containers |
| **Repository** | `AbstractRepositoryTest` | Base class con setup común |

---

## Build y Ejecucion

### Prerequisitos

- Java 25
- Docker & Docker Compose (para infraestructura)
- Ollama con modelos `granite3.3:8b` y `granite-embedding:278m`

### Comandos

```bash
# Levantar infraestructura
docker-compose up -d

# Compilar y ejecutar tests
./mvnw clean package

# Solo tests
./mvnw test

# Test específico
./mvnw test -Dtest=UsersServiceImplTest

# Test de un método específico
./mvnw test -Dtest=UsersServiceImplTest#shouldCreateUser

# Ejecutar la aplicación
./mvnw spring-boot:run
```

La aplicación se inicia en **http://localhost:8080**.

---

## API Endpoints

### Autenticación

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/login` | Login con email/password |
| POST | `/auth/register` | Registro de nuevo usuario |
| POST | `/auth/verify` | Verificación de email |
| POST | `/auth/token` | Intercambio de auth code por tokens |
| POST | `/auth/refresh` | Rotación de refresh token |
| POST | `/auth/logout` | Cierre de sesión |
| GET | `/oauth2/authorization/google` | Login con Google |
| GET | `/oauth2/authorization/github` | Login con GitHub |

### Usuarios

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/users/me` | Obtener usuario actual |
| PUT | `/api/v1/users/me` | Actualizar usuario actual |

### Documentos

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/documents` | Listar documentos del usuario |
| POST | `/api/v1/documents` | Subir documento (multipart) |
| GET | `/api/v1/documents/{id}` | Obtener documento |
| DELETE | `/api/v1/documents/{id}` | Eliminar documento |

### Chat (IA)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/chat` | Enviar mensaje al chat RAG |
| GET | `/api/v1/conversations` | Listar conversaciones |
| GET | `/api/v1/conversations/{id}` | Obtener conversación |
| DELETE | `/api/v1/conversations/{id}` | Eliminar conversación |

### Quizzes

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/quizzes` | Generar quiz a partir de documento |
| GET | `/api/v1/quizzes` | Listar quizzes del usuario |
| GET | `/api/v1/quizzes/{id}` | Obtener quiz |

> Todos los endpoints bajo `/api/v1/` requieren autenticación JWT. El ID del usuario se extrae automáticamente del token con `@CurrentUserId`.

---

<p align="center">
  Java 25 · Spring Boot 4.0.2 · Spring AI 2.0.0-M2
</p>
