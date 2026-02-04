# Plan de Refactorización - LeAIrning-Back

## Resumen Ejecutivo

Este documento presenta un análisis completo del backend LeAIrning-Back con propuestas de mejora basadas en:
- **Clean Code**: Código legible, mantenible y bien estructurado
- **DRY** (Don't Repeat Yourself): Eliminación de código duplicado
- **KISS** (Keep It Simple, Stupid): Simplificación de soluciones
- **Java 25**: Aprovechamiento de características modernas del lenguaje
- **Rendimiento**: Optimizaciones de eficiencia

### Hallazgos Principales

| Categoría | Cantidad | Severidad |
|-----------|----------|-----------|
| `RuntimeException` genéricas | 10 | Alta |
| Patrones `orElseThrow()` duplicados | 7 | Media |
| Archivos usando `lombok.val` en lugar de `var` | 19 | Baja |
| Servicios sin logging | 3 de 8 | Alta |
| Validaciones de email duplicadas | 2 | Media |

---

## 1. Clean Code - Excepciones Específicas

### Problema

El código usa `RuntimeException` genéricas que dificultan el manejo diferenciado de errores y no proporcionan contexto semántico.

### Ubicaciones Afectadas

| Archivo | Líneas | Instancias |
|---------|--------|------------|
| `MinioService.java` | 56, 65, 77, 89, 109, 133, 149, 174 | 8 |
| `MinioConfig.java` | 43 | 1 |
| `DocumentsServiceImpl.java` | 98 | 1 |

### Código Actual

```java
// MinioService.java:56
catch (Exception e) {
    throw new RuntimeException("Failed to store file in MinIO", e);
}

// DocumentsServiceImpl.java:98
catch (IOException e) {
    throw new RuntimeException("Failed to read file bytes", e);
}
```

### Solución Propuesta

Crear un paquete `exception/` con excepciones específicas:

```
com.marcos.leairning.exception/
├── StorageException.java                    # Base para errores de almacenamiento
├── StorageOperationException.java           # Errores de operaciones (store, load, delete)
├── StorageBucketInitializationException.java # Errores de inicialización de buckets
├── DocumentProcessingException.java         # Errores de procesamiento de documentos
├── UserNotFoundException.java               # Usuario no encontrado
├── DocumentNotFoundException.java           # Documento no encontrado
├── EmailAlreadyRegisteredException.java     # Email duplicado
├── InvalidCredentialsException.java         # Credenciales inválidas
├── AccountNotVerifiedException.java         # Cuenta no verificada
└── DocumentAccessDeniedException.java       # Acceso denegado a documento
```

### Ejemplo de Implementación

```java
// StorageException.java
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

// UserNotFoundException.java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("Unable to find user with id: " + id);
    }
    public UserNotFoundException(String email) {
        super("Unable to find user with email: " + email);
    }
}
```

### Justificación

- **Claridad**: Cada excepción comunica exactamente qué falló
- **Mantenibilidad**: Facilita el manejo específico en capas superiores
- **Testing**: Permite assertions más precisas en tests
- **HTTP Mapping**: Permite mapear excepciones a códigos HTTP apropiados

### Prioridad: **ALTA**

---

## 2. Clean Code - GlobalExceptionHandler

### Problema

No existe un manejo centralizado de excepciones. Cada controlador debe manejar errores individualmente, lo que lleva a inconsistencias en las respuestas de error.

### Solución Propuesta

Crear `GlobalExceptionHandler` con `@RestControllerAdvice`:

```java
// com/marcos/leairning/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFound(DocumentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageError(StorageException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(DocumentAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(DocumentAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}
```

### Justificación

- **Consistencia**: Todas las respuestas de error siguen el mismo formato (`ProblemDetail` RFC 7807)
- **Centralización**: Un solo lugar para modificar el comportamiento de errores
- **Separación de responsabilidades**: Los controladores se enfocan en la lógica de negocio

### Prioridad: **ALTA**

---

## 3. DRY - Patrón orElseThrow Repetido

### Problema

El patrón `orElseThrow(() -> new IllegalArgumentException(...))` se repite 7 veces en el código.

### Ubicaciones Afectadas

**UsersServiceImpl.java:**
- Líneas 32-34, 46-48, 91-93, 107-109 (4 instancias)

**DocumentsServiceImpl.java:**
- Líneas 66-68, 77-79, 106-108 (3 instancias)

### Código Actual

```java
// UsersServiceImpl.java:32-34
val user = repository.findById(id).orElseThrow(
    () -> new IllegalArgumentException("Unable to find user with id: " + id)
);

// DocumentsServiceImpl.java:66-68
val document = repository.findById(id).orElseThrow(
    () -> new IllegalArgumentException("Unable to find document with id: " + id)
);
```

### Solución Propuesta

Extraer métodos helper en cada servicio:

```java
// UsersServiceImpl.java
private User findUserOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
}

private User findUserByEmailOrThrow(String email) {
    return repository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email));
}

// DocumentsServiceImpl.java
private Document findDocumentOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new DocumentNotFoundException(id));
}
```

### Justificación

- **DRY**: Elimina código duplicado
- **Legibilidad**: `findUserOrThrow(id)` es más claro que el bloque completo
- **Mantenibilidad**: Cambios en la lógica de búsqueda se hacen en un solo lugar
- **Excepciones específicas**: Usa excepciones de dominio en lugar de genéricas

### Prioridad: **MEDIA**

---

## 4. DRY - Validación de Email Duplicada

### Problema

La validación de email ya registrado se repite en dos métodos del mismo servicio.

### Ubicaciones Afectadas

**UsersServiceImpl.java:**
- `save()` - Líneas 55-57
- `saveOauth2User()` - Líneas 73-75

### Código Actual

```java
// save() - Línea 55-56
if (repository.findByEmail(dto.email()).isPresent()) {
    throw new IllegalArgumentException("Email already registered");
}

// saveOauth2User() - Línea 73-74 (idéntico)
if (repository.findByEmail(dto.email()).isPresent()) {
    throw new IllegalArgumentException("Email already registered");
}
```

### Solución Propuesta

```java
private void validateEmailNotRegistered(String email) {
    if (repository.findByEmail(email).isPresent()) {
        throw new EmailAlreadyRegisteredException(email);
    }
}

// Uso en save()
@Override
@Transactional
public UserResponseDTO save(RegisterRequestDTO dto) {
    validateEmailNotRegistered(dto.email());
    // ... resto del método
}

// Uso en saveOauth2User()
@Override
@Transactional
public UserResponseDTO saveOauth2User(Oauth2UserCreateDTO dto) {
    validateEmailNotRegistered(dto.email());
    // ... resto del método
}
```

### Justificación

- **DRY**: Elimina duplicación exacta
- **Single point of change**: Modificar la validación afecta ambos flujos
- **Excepción específica**: `EmailAlreadyRegisteredException` en lugar de `IllegalArgumentException`

### Prioridad: **MEDIA**

---

## 5. DRY - Manejo de Excepciones en MinioService

### Problema

8 bloques `catch(Exception e) { throw new RuntimeException(...) }` casi idénticos en MinioService.

### Código Actual

```java
// Patrón repetido 8 veces
try {
    // operación MinIO
} catch (Exception e) {
    throw new RuntimeException("Failed to [operación] in MinIO", e);
}
```

### Solución Propuesta

Crear una interfaz funcional y método helper:

```java
@FunctionalInterface
private interface MinioOperation<T> {
    T execute() throws Exception;
}

private <T> T executeMinioOperation(MinioOperation<T> operation, String errorMessage) {
    try {
        return operation.execute();
    } catch (Exception e) {
        throw new StorageOperationException(errorMessage, e);
    }
}

// Para operaciones void
private void executeMinioVoidOperation(Runnable operation, String errorMessage) {
    try {
        operation.run();
    } catch (Exception e) {
        throw new StorageOperationException(errorMessage, e);
    }
}
```

### Ejemplo de Uso

```java
// Antes
public String store(byte[] content, Document document) {
    // ... preparación ...
    try {
        client.putObject(PutObjectArgs.builder()
                .bucket(properties.getDocumentsBucket())
                .object(objectPath)
                .stream(inputStream, content.length, -1)
                .contentType(document.getContentType())
                .build());
        return objectPath;
    } catch (Exception e) {
        throw new RuntimeException("Failed to store file in MinIO", e);
    }
}

// Después
public String store(byte[] content, Document document) {
    // ... preparación ...
    return executeMinioOperation(() -> {
        client.putObject(PutObjectArgs.builder()
                .bucket(properties.getDocumentsBucket())
                .object(objectPath)
                .stream(inputStream, content.length, -1)
                .contentType(document.getContentType())
                .build());
        return objectPath;
    }, "Failed to store file in MinIO");
}
```

### Justificación

- **DRY**: Elimina 8 bloques try-catch duplicados
- **Consistencia**: Todas las operaciones manejan errores de la misma forma
- **Excepción específica**: `StorageOperationException` en lugar de `RuntimeException`

### Prioridad: **MEDIA**

---

## 6. Clean Code - Logging Insuficiente

### Problema

3 de 8 servicios no tienen ningún logging, lo que dificulta el debugging y la auditoría.

### Servicios Sin Logging

| Servicio | Operaciones críticas sin log |
|----------|------------------------------|
| `UsersServiceImpl.java` | CRUD de usuarios (get, save, update, delete) |
| `DocumentsServiceImpl.java` | Upload, download, delete de documentos |
| `MinioService.java` | Operaciones de almacenamiento |

### Servicios Con Logging (Referencia)

- `AuthServiceImpl.java` - Usa `@Flogger`
- `EmailServiceImpl.java` - Usa `@Flogger`
- `TokenPairServiceImpl.java` - Usa `@Flogger`

### Solución Propuesta

Agregar `@Flogger` de Lombok y logs en operaciones críticas:

```java
@Flogger
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    @Override
    public UserResponseDTO get(UUID id) {
        log.atFine().log("Fetching user with id: %s", id);
        var user = findUserOrThrow(id);
        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO save(RegisterRequestDTO dto) {
        log.atInfo().log("Registering new user with email: %s", dto.email());
        validateEmailNotRegistered(dto.email());
        var user = mapper.toEntity(dto);
        var savedUser = repository.save(user);
        log.atInfo().log("User registered successfully with id: %s", savedUser.getId());
        return mapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.atInfo().log("Deleting user with id: %s", id);
        var user = findUserOrThrow(id);
        repository.delete(user);
        log.atInfo().log("User deleted successfully: %s", id);
    }
}
```

### Niveles de Log Recomendados

| Nivel | Uso |
|-------|-----|
| `atFine()` | Operaciones de lectura (get, list) |
| `atInfo()` | Operaciones de escritura exitosas (save, update, delete) |
| `atWarning()` | Errores recuperables, operaciones fallidas no críticas |
| `atSevere()` | Errores críticos que requieren atención |

### Justificación

- **Debugging**: Facilita identificar problemas en producción
- **Auditoría**: Registro de operaciones críticas
- **Trazabilidad**: Seguimiento del flujo de datos
- **Consistencia**: Siguiendo el patrón ya establecido en otros servicios

### Prioridad: **ALTA**

---

## 7. SRP - MinioService Tiene Demasiadas Responsabilidades

### Problema

`MinioService` tiene 9 métodos públicos que mezclan dos responsabilidades distintas:
1. Almacenamiento principal de documentos
2. Pipeline de procesamiento RAG

### Métodos Actuales

**Almacenamiento principal:**
- `store()` - Almacenar documento
- `load()` - Cargar bytes
- `loadAsStream()` - Cargar como stream
- `delete()` - Eliminar documento

**Pipeline de procesamiento:**
- `copyTo()` - Copiar entre buckets
- `listPendingFiles()` - Listar archivos pendientes
- `loadFromProcessing()` - Cargar desde bucket de procesamiento
- `markProcessed()` - Marcar como procesado

### Solución Propuesta

Separar en dos servicios especializados:

```
minio/
├── MinioDocumentStorageService.java    # Operaciones CRUD de documentos
├── MinioProcessingPipelineService.java # Operaciones del pipeline RAG
└── MinioProperties.java                # Configuración compartida
```

```java
// MinioDocumentStorageService.java
@Service
@RequiredArgsConstructor
public class MinioDocumentStorageService {
    private final MinioClient client;
    private final MinioProperties properties;

    public String store(byte[] content, Document document) { ... }
    public byte[] load(String path) { ... }
    public InputStream loadAsStream(String path) { ... }
    public void delete(String path) { ... }
}

// MinioProcessingPipelineService.java
@Service
@RequiredArgsConstructor
public class MinioProcessingPipelineService {
    private final MinioClient client;
    private final MinioProperties properties;

    public void copyToPending(String sourcePath) { ... }
    public List<String> listPendingFiles() { ... }
    public byte[] loadFromProcessing(String path) { ... }
    public void markAsProcessed(String path) { ... }
    public void markAsFailed(String path) { ... }
}
```

### Justificación

- **SRP**: Cada servicio tiene una única responsabilidad clara
- **Cohesión**: Métodos relacionados agrupados juntos
- **Testing**: Tests más focalizados por servicio
- **Mantenibilidad**: Cambios en pipeline no afectan almacenamiento principal

### Prioridad: **MEDIA**

---

## 8. KISS - Eliminar @SneakyThrows Innecesario

### Problema

`@SneakyThrows` oculta excepciones checked, pero en este caso es innecesario porque el método interno ya maneja la excepción.

### Ubicación

**DocumentsServiceImpl.java:54**

### Código Actual

```java
@Override
@SneakyThrows  // <- Innecesario
@Transactional
public List<DocumentResponseDTO> upload(List<MultipartFile> files) {
    return files.stream()
            .map(this::uploadDocument)
            .toList();
}
```

### Análisis

El método `uploadDocument()` ya convierte `IOException` a `RuntimeException`:

```java
private DocumentResponseDTO uploadDocument(MultipartFile file) {
    // ...
    try {
        var objectPath = minioService.store(file.getBytes(), document);
        // ...
    } catch (IOException e) {
        throw new RuntimeException("Failed to read file bytes", e);  // Ya convertido
    }
}
```

### Solución

Simplemente eliminar `@SneakyThrows`:

```java
@Override
@Transactional
public List<DocumentResponseDTO> upload(List<MultipartFile> files) {
    return files.stream()
            .map(this::uploadDocument)
            .toList();
}
```

### Justificación

- **KISS**: No agregar complejidad innecesaria
- **Claridad**: El código es más explícito sobre qué excepciones pueden ocurrir
- **Debugging**: Sin `@SneakyThrows`, las excepciones se propagan de forma más predecible

### Prioridad: **BAJA**

---

## 9. Java 25 - Reemplazar val (Lombok) con var (Java Nativo)

### Problema

El código usa `lombok.val` en lugar de `var` nativo de Java. Esto agrega dependencia innecesaria de Lombok para una característica que Java ya proporciona nativamente desde Java 10.

### Archivos Afectados

| Archivo | Ocurrencias |
|---------|-------------|
| `UsersServiceImpl.java` | 8 |
| `DocumentsServiceImpl.java` | 6 |
| `MinioService.java` | 4 |
| `AuthServiceImpl.java` | 7 |
| `JwtService.java` | 10 |
| `Oauth2SuccessHandler.java` | 5 |
| `EmailServiceImpl.java` | 4 |
| `TokenPairServiceImpl.java` | 3 |
| `CurrentUserIdArgumentResolver.java` | 3 |
| `AuthCodeAuthenticationSuccessHandler.java` | 4 |
| Otros | ~10 |
| **Total** | **~64** |

### Cambio Requerido

1. Eliminar `import lombok.val;`
2. Reemplazar `val` por `var`

### Ejemplo

```java
// Antes
import lombok.val;
// ...
val user = repository.findById(id).orElseThrow(...);

// Después (sin import adicional)
var user = repository.findById(id).orElseThrow(...);
```

### Justificación

- **Estándar**: `var` es parte del lenguaje Java desde Java 10
- **Menos dependencias**: No requiere Lombok para esta característica
- **Familiaridad**: Desarrolladores esperan `var`, no `val`
- **IDE Support**: Mejor soporte de IDEs para `var` nativo

### Prioridad: **BAJA**

---

## 10. Java 25 - Stream API en listPendingFiles

### Problema

`MinioService.listPendingFiles()` usa un bucle tradicional con `ArrayList` mutable en lugar de Stream API.

### Código Actual

```java
public List<String> listPendingFiles() {
    var pendingFiles = new ArrayList<String>();
    try {
        var results = client.listObjects(ListObjectsArgs.builder()
                .bucket(properties.getProcessingBucket())
                .prefix(PENDING_PREFIX)
                .build());
        for (var result : results) {
            Item item = result.get();
            if (!item.isDir()) {
                pendingFiles.add(item.objectName());
            }
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to list pending files", e);
    }
    return pendingFiles;
}
```

### Solución Propuesta

```java
public List<String> listPendingFiles() {
    return executeMinioOperation(() -> {
        var results = client.listObjects(ListObjectsArgs.builder()
                .bucket(properties.getProcessingBucket())
                .prefix(PENDING_PREFIX)
                .build());
        return StreamSupport.stream(results.spliterator(), false)
                .map(this::safeGetItem)
                .filter(item -> !item.isDir())
                .map(Item::objectName)
                .toList();
    }, "Failed to list pending files");
}

private Item safeGetItem(Result<Item> result) {
    try {
        return result.get();
    } catch (Exception e) {
        throw new StorageOperationException("Failed to get item from result", e);
    }
}
```

### Justificación

- **Inmutabilidad**: `.toList()` retorna lista inmutable
- **Legibilidad**: Pipeline declarativo más claro que bucle imperativo
- **Consistencia**: Alineado con el uso de streams en otros métodos del proyecto

### Prioridad: **BAJA**

---

## 11. Rendimiento - Consideraciones de Caché

### Observación

El proyecto ya usa Caffeine Cache de forma apropiada con `@Cacheable`, `@CachePut`, y `@CacheEvict`.

### Configuración Actual

```yaml
leairning:
  cache:
    caffeine:
      documents-maximum-size: 1000
      documents-expire-after-access: 60s
      users-maximum-size: 1000
      users-expire-after-access: 60s
```

### Recomendación

Monitorear métricas de caché en producción para ajustar:
- `maximum-size`: Basado en memoria disponible y patrones de acceso
- `expire-after-access`: Basado en frecuencia de cambios de datos

### Prioridad: **BAJA** (ya bien implementado)

---

## Orden de Implementación Recomendado

### Fase 1: Infraestructura de Excepciones (Alta Prioridad)

1. Crear paquete `exception/` con excepciones específicas
2. Crear `GlobalExceptionHandler` con `@RestControllerAdvice`
3. Reemplazar `RuntimeException` en `MinioService`
4. Reemplazar `IllegalArgumentException` en `UsersServiceImpl` y `DocumentsServiceImpl`

### Fase 2: DRY y Logging (Media Prioridad)

5. Extraer métodos helper en `UsersServiceImpl` (`findUserOrThrow`, `validateEmailNotRegistered`)
6. Extraer métodos helper en `DocumentsServiceImpl` (`findDocumentOrThrow`)
7. Crear `executeMinioOperation()` en `MinioService`
8. Agregar `@Flogger` a `UsersServiceImpl`, `DocumentsServiceImpl`, `MinioService`

### Fase 3: Refactorización Estructural (Media Prioridad)

9. Separar `MinioService` en `MinioDocumentStorageService` y `MinioProcessingPipelineService`
10. Eliminar `@SneakyThrows` innecesario

### Fase 4: Modernización Java (Baja Prioridad)

11. Reemplazar `lombok.val` por `var` en todos los archivos
12. Aplicar Stream API en `listPendingFiles()`

---

## Métricas de Éxito

| Métrica | Antes | Después |
|---------|-------|---------|
| Instancias de `RuntimeException` | 10 | 0 |
| Instancias de `IllegalArgumentException` genéricas | 11 | 0 |
| Servicios con logging | 3/8 | 8/8 |
| Uso de `lombok.val` | 19 archivos | 0 archivos |
| Métodos `orElseThrow` duplicados | 7 | 0 |
| Responsabilidades de `MinioService` | 9 métodos mixtos | 2 servicios focalizados |

---

## Archivos Críticos

| Archivo | Razón | Prioridad |
|---------|-------|-----------|
| `src/.../minio/MinioService.java` | 8 RuntimeException, candidato a separación SRP | Alta |
| `src/.../users/UsersServiceImpl.java` | Sin logging, 4 orElseThrow duplicados, 2 validaciones duplicadas | Alta |
| `src/.../documents/DocumentsServiceImpl.java` | Sin logging, 3 orElseThrow duplicados, @SneakyThrows innecesario | Alta |
| `src/.../exception/` (nuevo) | Paquete para excepciones y GlobalExceptionHandler | Alta |
| `src/.../security/auth/AuthServiceImpl.java` | Referencia de logging correcto | Media |

---

## Notas Finales

### Lo que ya está bien implementado

- **Records para DTOs**: `UserResponseDTO`, `LoginRequestDTO`, `TokenPair` ya son records
- **Pattern Matching**: `CurrentUserIdArgumentResolver` y `Oauth2SuccessHandler` ya usan pattern matching
- **Stream API**: La mayoría de operaciones de colecciones usan `.stream().toList()`
- **Caché**: Caffeine bien configurado con anotaciones de Spring

### Consideraciones

- Cada cambio debe ir acompañado de tests actualizados
- Seguir el enfoque TDD: escribir tests primero, implementar después
- Ejecutar `./mvnw test` después de cada fase para verificar que no hay regresiones