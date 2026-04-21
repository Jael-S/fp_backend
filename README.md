# fp_backend

> Sistema de Gestión y Automatización de Trámites basado en Políticas de Negocio — Backend

Backend del sistema **FlowPolicy**, construido con Spring Boot 3 y MongoDB. Expone una API REST + WebSockets para gestionar políticas de negocio, trámites, usuarios, mapa de cobertura y notificaciones en tiempo real.

---

## Stack

| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java (JDK) | 26 | Lenguaje principal |
| Spring Boot | 3.x | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data MongoDB | 4.x | Persistencia de datos |
| Spring WebSocket | 6.x | Comunicación en tiempo real |
| JWT (jjwt) | 0.11.5+ | Tokens de autenticación |
| MongoDB | 8.2.6 | Base de datos principal |
| Firebase Admin SDK | 9.2.0 | Push notifications |
| Azure Blob Storage | 12.25.1 | Almacenamiento de archivos |
| Lombok | latest | Reducción de boilerplate |
| Springdoc OpenAPI | 2.3.0+ | Documentación Swagger |
| Maven | 3.8+ | Gestión de dependencias |

---

## Requisitos previos

Asegúrate de tener instalado en tu máquina:

```bash
java -version      # Java 26+
mvn -version       # Maven 3.8+
mongod --version   # MongoDB 8.2.6+
git --version      # Git (cualquier versión reciente)
```

---

## Instalación y ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/fp_backend.git
cd fp_backend
```

### 2. Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto (nunca se sube al repositorio):

```env
MONGODB_URI=mongodb://localhost:27017/flowpolicy_db
JWT_SECRET=tu-secreto-muy-largo-y-seguro-minimo-32-caracteres
JWT_EXPIRATION=86400000
IA_SERVICE_URL=http://localhost:8001
FIREBASE_CREDENTIALS=
AZURE_STORAGE_CONNECTION_STRING=
```

> Para desarrollo local, solo `MONGODB_URI` y `JWT_SECRET` son obligatorios. El resto puede dejarse vacío hasta que se necesite.

### 3. Iniciar MongoDB local

```bash
# Windows (si se instaló como servicio)
net start MongoDB
```

### 4. Compilar y ejecutar

```bash
# Compilar
mvn clean compile

# Ejecutar en modo desarrollo
mvn spring-boot:run

# El servidor inicia en: http://localhost:8080
```

### 5. Verificar que funciona

```bash
curl http://localhost:8080/actuator/health
# Respuesta esperada: {"status":"UP"}
```

---

## Documentación de la API

Una vez el servidor esté corriendo, accede a Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Swagger muestra todos los endpoints disponibles, permite probarlos directamente y documenta los DTOs de request/response.

---

## Estructura del proyecto

```
src/
└── main/
    ├── java/com/flowpolicy/
    │   ├── FlowPolicyApplication.java     ← Entry point
    │   ├── config/                        ← Configuraciones globales
    │   │   ├── SecurityConfig.java
    │   │   ├── WebSocketConfig.java
    │   │   └── CorsConfig.java
    │   ├── common/                        ← Código compartido
    │   │   ├── dto/
    │   │   │   ├── ApiResponse.java
    │   │   │   └── PageResponse.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── UnauthorizedException.java
    │   │   └── utils/
    │   │       └── JwtUtil.java
    │   ├── security/
    │   │   ├── JwtFilter.java
    │   │   └── UserDetailsServiceImpl.java
    │   │
    │   ├── auth/                          ← Módulo: Autenticación (CU1)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   └── service/
    │   ├── usuario/                       ← Módulo: Usuarios (CU2)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── departamento/                  ← Módulo: Departamentos (CU3)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── politica/                      ← Módulo: Políticas de negocio (CU4)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── formulario/                    ← Módulo: Formularios dinámicos (CU5)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── diagrama/                      ← Módulo: Diagrama de actividades (CU6)
    │   │   ├── nodo/
    │   │   │   ├── controller/
    │   │   │   ├── dto/
    │   │   │   ├── model/
    │   │   │   ├── repository/
    │   │   │   └── service/
    │   │   └── transicion/
    │   │       ├── controller/
    │   │       ├── dto/
    │   │       ├── model/
    │   │       ├── repository/
    │   │       └── service/
    │   ├── tramite/                       ← Módulo: Trámites (CU7)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── ejecucion/                     ← Módulo: Ejecuciones de nodo (CU8)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── monitor/                       ← Módulo: Monitor tiempo real (CU9)
    │   │   ├── controller/
    │   │   └── service/
    │   ├── notificacion/                  ← Módulo: Notificaciones (CU10)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── ia/                            ← Módulo: Inteligencia Artificial (CU11)
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   └── service/
    │   └── ubicacion/                     ← Módulo: Mapa de cobertura (CU12)
    │       ├── controller/
    │       ├── dto/
    │       ├── model/
    │       ├── repository/
    │       └── service/
    │
    └── resources/
        └── application.properties
```

Cada módulo es completamente autónomo y contiene sus propias capas:

```
[modulo]/
├── controller/    → Endpoints REST (@RestController)
├── dto/           → Objetos de transferencia (request/response)
├── model/         → Documentos MongoDB (@Document)
├── repository/    → Interfaces MongoRepository
└── service/       → Lógica de negocio (@Service)
```

---

## Base de datos

El proyecto usa **MongoDB** con las siguientes colecciones principales:

| Colección | Descripción |
|-----------|-------------|
| `usuarios` | Todos los usuarios del sistema (Gestor, Admin de Área, Operador) |
| `departamentos` | Áreas o departamentos de la organización |
| `politicas` | Políticas de negocio (definición del proceso workflow) |
| `nodos` | Nodos del diagrama de actividades |
| `transiciones` | Flechas entre nodos del diagrama |
| `formularios` | Formularios dinámicos asociados a cada nodo |
| `tramites` | Instancias de procesos en ejecución |
| `ejecuciones_nodo` | Registro de cada paso ejecutado en un trámite |
| `notificaciones` | Notificaciones web y push |
| `analisis_ia` | Resultados de análisis del microservicio IA |
| `ubicaciones` | Puntos del mapa de cobertura del servicio |

> **No se requieren migraciones.** MongoDB es schemaless. Spring Data MongoDB crea las colecciones automáticamente al guardar el primer documento.

---

## Roles del sistema

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| `GESTOR_SISTEMA` | Administrador total de la plataforma | Todo el sistema |
| `ADMINISTRADOR_AREA` | Responsable de un departamento específico | Su departamento |
| `OPERADOR` | Empleado que ejecuta las tareas asignadas | Sus actividades |

---

## Variables de entorno

| Variable | Descripción | Requerida en local |
|----------|-------------|-------------------|
| `MONGODB_URI` | URI de conexión a MongoDB | ✅ Sí |
| `JWT_SECRET` | Secreto para firmar tokens JWT (mín. 32 chars) | ✅ Sí |
| `JWT_EXPIRATION` | Expiración del JWT en milisegundos (default: 86400000 = 24h) | No |
| `IA_SERVICE_URL` | URL del microservicio Python | No |
| `FIREBASE_CREDENTIALS` | JSON de credenciales Firebase en base64 | No |
| `AZURE_STORAGE_CONNECTION_STRING` | Conexión a Azure Blob Storage | No |

---

## Despliegue en Azure

El backend se despliega en **Azure App Service** (Java 26, Linux):

```bash
# Build
mvn clean package -DskipTests

# Deploy con Azure CLI
az webapp deploy \
  --resource-group rg-flowpolicy \
  --name fp-backend \
  --src-path target/flowpolicy-0.0.1-SNAPSHOT.jar \
  --type jar
```

**Servicios Azure utilizados:**
- Azure App Service → API principal
- Azure Container Apps → Microservicio IA (Python/FastAPI)
- Azure Blob Storage → Archivos adjuntos de formularios
- MongoDB Atlas → Base de datos en la nube

---

## Convención de commits

Este proyecto sigue [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(auth): implementar autenticación JWT
fix(tramite): corregir motor de workflow en transiciones paralelas
docs(readme): actualizar instrucciones de instalación
refactor(usuario): simplificar validación de roles
test(auth): agregar pruebas unitarias para login
chore(deps): actualizar dependencias de seguridad
```

---

## Licencia

Proyecto académico — Universidad Autónoma Gabriel René Moreno
Materia: Ingeniería de Software I — Ing. Martínez Canedo