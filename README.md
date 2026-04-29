# FlowPolicy Backend

> Sistema de Gestión y Automatización de Trámites basado en Políticas de Negocio — Backend

API REST + WebSocket del sistema **FlowPolicy**, construido con Spring Boot 3 y MongoDB. Gestiona políticas BPMN, usuarios, formularios dinámicos, trámites, ejecuciones y monitoreo en tiempo real.

---

## Stack

| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java (JDK) | 21 | Lenguaje principal |
| Spring Boot | 3.5.4 | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data MongoDB | 4.x | Persistencia de datos |
| Spring WebSocket | 6.x | Comunicación en tiempo real |
| JWT (jjwt) | 0.11.5+ | Tokens de autenticación |
| MongoDB | Atlas / Local | Base de datos principal |
| Lombok | latest | Reducción de boilerplate |
| Maven | 3.9+ | Gestión de dependencias |

---

## Requisitos previos

```bash
java -version      # Java 21+
mvn -version       # Maven 3.9+
```

---

## Instalación y ejecución local

```bash
cd fp_backend
mvn spring-boot:run
# Servidor en http://localhost:8080
```

### Variables en `application.properties`

```properties
spring.data.mongodb.uri=mongodb+srv://...
jwt.secret=<clave_secreta_min_32_chars>
jwt.expiration=86400000
ia.service.url=http://localhost:5000
```

---

## Endpoints principales

### Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Inicio de sesión → devuelve JWT |
| POST | `/api/v1/auth/registro` | Registro de empresa + gestor inicial |

### Usuarios

| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | `/api/v1/usuarios` | GESTOR_SISTEMA |
| POST | `/api/v1/usuarios` | GESTOR_SISTEMA |
| PUT | `/api/v1/usuarios/{id}` | GESTOR_SISTEMA |
| PUT | `/api/v1/usuarios/{id}/toggle` | GESTOR_SISTEMA |

### Departamentos

| Método | Endpoint | Rol |
|--------|----------|-----|
| GET | `/api/v1/departamentos` | GESTOR_SISTEMA, ADMINISTRADOR_AREA |
| POST | `/api/v1/departamentos` | GESTOR_SISTEMA |
| PUT | `/api/v1/departamentos/{id}` | GESTOR_SISTEMA |
| DELETE | `/api/v1/departamentos/{id}` | GESTOR_SISTEMA |

### Políticas de negocio

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/politicas` | Lista paginada |
| POST | `/api/v1/politicas` | Crear política |
| PUT | `/api/v1/politicas/{id}` | Editar metadatos |
| PUT | `/api/v1/politicas/{id}/diagrama` | Guardar nodos + transiciones del diagrama |
| PUT | `/api/v1/politicas/{id}/activar` | Activar política |
| PUT | `/api/v1/politicas/{id}/desactivar` | Desactivar política |
| DELETE | `/api/v1/politicas/{id}` | Eliminar (soft delete) |

### Nodos del diagrama

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/nodos/politica/{politicaId}` | Nodos de una política |
| GET | `/api/v1/nodos/elemento/{elementId}/politica/{politicaId}` | Nodo por elementId BPMN |
| PUT | `/api/v1/nodos/{elementId}/config` | Configurar nombre, departamento y formulario |
| PUT | `/api/v1/nodos/{elementId}/tipo-flujo` | Actualizar tipo de flujo |

### Formularios dinámicos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/formularios` | Listar (filtrable por departamento) |
| POST | `/api/v1/formularios` | Crear formulario con campos |
| PUT | `/api/v1/formularios/{id}` | Actualizar |
| DELETE | `/api/v1/formularios/{id}` | Eliminar (soft delete) |

### Trámites

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/tramites` | Lista (filtrable por política) |
| POST | `/api/v1/tramites` | Crear trámite e iniciar ejecución |
| GET | `/api/v1/tramites/{id}` | Detalle del trámite |

### Ejecuciones (Funcionario)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/ejecuciones/mis-tareas` | Bandeja del funcionario |
| GET | `/api/v1/ejecuciones/historial` | Historial de tareas completadas |
| PUT | `/api/v1/ejecuciones/{id}/completar` | Completar tarea con respuestas |
| PUT | `/api/v1/ejecuciones/{id}/rechazar` | Rechazar tarea con motivo |

### Monitoreo en tiempo real

| Tipo | Endpoint | Descripción |
|------|----------|-------------|
| GET | `/api/v1/monitoreo/{politicaId}` | Estado actual de todos los nodos |
| WS | `ws://localhost:8080/ws-monitor` | Conexión WebSocket STOMP |
| — | `/topic/monitor/{politicaId}` | Topic de actualizaciones push |

### Inteligencia Artificial

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/ia/preguntar` | Chat NLP sobre el estado del sistema (Java nativo) |
| POST | `/api/v1/ia/generar-diagrama` | Genera diagrama BPMN desde texto (delega a Python) |
| POST | `/api/v1/ia/generar-formulario` | Genera campos de formulario con IA (delega a Python) |
| GET | `/api/v1/ia/analizar-cuellos/{politicaId}` | Análisis de cuellos de botella (Java nativo) |

---

## Colecciones MongoDB

| Colección | Descripción |
|-----------|-------------|
| `empresas` | Empresa registrada |
| `usuarios` | Usuarios del sistema (todos los roles) |
| `departamentos` | Departamentos de la empresa |
| `politicas` | Políticas de negocio (BPMN) |
| `nodos` | Nodos del diagrama (INICIO, PROCESO, DECISION, FIN) |
| `transiciones` | Conexiones entre nodos |
| `formularios` | Formularios con campos dinámicos (7 tipos) |
| `tramites` | Instancias de proceso en ejecución |
| `ejecuciones_nodo` | Estado de cada tarea por trámite |
| `notificaciones` | Notificaciones WebSocket |

> MongoDB es schemaless. Las colecciones se crean automáticamente al guardar el primer documento.

---

## Roles del sistema

| Rol | Descripción |
|-----|-------------|
| `GESTOR_SISTEMA` | Control total: usuarios, políticas, formularios, trámites, IA |
| `ADMINISTRADOR_AREA` | Gestiona formularios y monitoreo de su departamento |
| `FUNCIONARIO` | Ejecuta tareas asignadas en su bandeja |

---

## Estructura del proyecto

```
src/main/java/com/flowpolicy/
├── auth/            ← Autenticación JWT
├── usuario/         ← Gestión de usuarios y roles
├── departamento/    ← Gestión de departamentos
├── politica/        ← Políticas de negocio
├── nodo/            ← Nodos del diagrama BPMN
├── transicion/      ← Transiciones entre nodos
├── formulario/      ← Formularios dinámicos
├── tramite/         ← Trámites (instancias de proceso)
├── ejecucion/       ← Ejecuciones de nodo por trámite
├── monitor/         ← Monitoreo en tiempo real (WebSocket)
├── ia/              ← Servicios de IA (diagrama, formulario, cuellos)
├── notificacion/    ← Notificaciones WebSocket
├── security/        ← JWT filter, UserDetailsService
└── common/          ← ApiResponse, excepciones globales
```

---

## Licencia

Proyecto académico — Ingeniería de Software I
