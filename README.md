# FleetRoute Pro

![Java](https://img.shields.io/badge/Java-25.0.2%20Temurin-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21%2B-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-336791?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?logo=apachemaven&logoColor=white)

> **Sistema de Despacho de Cargas, Control de Jornadas y Encadenamiento de Rutas**

Proyecto final de la asignatura **PAE — Programación de Aplicaciones de Escritorio** · 2026

---

## Información importante del entorno

- **JDK:** el proyecto se desarrolla con **Eclipse Temurin 25.0.2**, cumpliendo con el requisito de Java 17 o superior.
- **Base de datos:** se utiliza **PostgreSQL 14+** como sistema gestor de base de datos; el esquema tiene **13 tablas**.
- **Script de base de datos:** el repositorio incluye [`database/fleetroute_pro.sql`](../../Qoder/2026-11-04/91b5298e/database/fleetroute_pro.sql), con la creación de tablas, restricciones, triggers, índices, catálogos y datos de ejemplo.
- **Extensión:** el script crea `pgcrypto` para generar los hashes BCrypt de los usuarios de demostración.
- **Conexión a la base de datos:** se realiza mediante un archivo **`.env`**. Cada integrante mantiene su propio archivo `.env` en su equipo con sus credenciales locales de PostgreSQL (usuario, contraseña, puerto, etc.). Este archivo **no se sube al repositorio**.
- **Archivo de ejemplo:** se incluye **`example.env`** como plantilla. Cualquier persona que descargue el proyecto debe copiarlo, renombrarlo a `.env` y colocar en él sus propias credenciales.

---

## Tabla de contenido

- [Descripción general](#descripción-general)
- [Integrantes](#integrantes)
- [Stack tecnológico](#stack-tecnológico)
- [Funcionalidades por rol](#funcionalidades-por-rol)
- [Requerimientos funcionales](#requerimientos-funcionales)
- [Requerimientos no funcionales](#requerimientos-no-funcionales)
- [Reglas de negocio](#reglas-de-negocio)
- [Arquitectura del proyecto](#arquitectura-del-proyecto)
- [Modelo de datos](#modelo-de-datos)
- [Requisitos previos](#requisitos-previos)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Datos de ejemplo](#datos-de-ejemplo)
- [Variables de entorno](#variables-de-entorno)
- [Flujo de uso](#flujo-de-uso)
- [Solución de problemas](#solución-de-problemas)
- [Documentación](#documentación)

---

## Descripción general

**FleetRoute Pro** es una aplicación de escritorio orientada a empresas de transporte de carga pesada. Permite a la administración planificar, despachar y encadenar viajes en carretera, optimizando los tiempos de traslado, supervisando el cumplimiento de las horas de descanso contractuales de los choferes y controlando las fechas límite de entrega pactadas con los clientes.

> **Alcance:** este software **no es un inventario**. No gestiona almacenes ni stock de repuestos. Su enfoque es 100 % operativo: control de logística de viajes, horas de fatiga, conductores y cumplimiento de plazos de flete.

---

## Integrantes

| # | Estudiante |
|---|------------|
| 1 | Diego Marcello Chamendy Chow |
| 2 | Carlos Eduardo Rodríguez Flores |
| 3 | Jose Salgado |
| 4 | Roberto Fabio Tercero |

---

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje base | Java (Eclipse Temurin 25.0.2) |
| Interfaz gráfica | JavaFX + FXML |
| Acceso a datos | JDBC · Patrón DAO |
| Base de datos | PostgreSQL |
| Gestión de dependencias | Maven |
| Configuración | Archivo `.env` (variables de conexión) |
| Seguridad | Contraseñas con hash BCrypt y acceso por rol |
| Arquitectura | Capas: Vista → Servicios → DAO → Modelo |

---

## Funcionalidades por rol

Ambos roles inician sesión con credenciales almacenadas en la base de datos; las contraseñas se guardan con hash BCrypt (RF-11 / RNF-04).

### Módulo Administrador (Despacho)

- **Planificador de asignaciones:** vinculación de unidad, operador y carga en un mismo viaje.
- **Pre-asignación de viajes:** encolamiento de la siguiente carga validando la consistencia geográfica de la ruta.
- **Monitor de flota en vivo:** tablero semaforizado con el estado de unidades, operadores y viajes activos.
- **Control financiero:** registro y consulta de la tarifa cobrada por cada flete, por operador y por unidad.
- **Gestión de unidades, operadores y cargas:** alta, consulta, actualización y baja.

### Módulo Operador (Cabina)

- **Interfaz tipo tableta:** botones grandes, alto contraste y pantalla simplificada para cabina.
- **Cronómetro digital:** horas conducidas y descanso acumulado en tiempo real.
- **Controles rápidos:** `INICIAR TRAYECTO`, `PAUSA`, `REANUDAR` y `CONFIRMAR ENTREGA`.
- **Panel de siguiente asignación:** vista previa de la carga encadenada que se activará al finalizar el viaje actual.

---

## Requerimientos funcionales

| Código | Requerimiento | Prioridad |
|--------|---------------|-----------|
| RF-01 | **Gestión de unidades (camiones):** registrar, consultar, actualizar y dar de baja unidades con número de unidad, placas, modelo y estado operativo. | Alta |
| RF-02 | **Gestión de operadores y reglas de descanso:** registrar operadores con sus datos personales, horas máximas de conducción y horas mínimas de descanso. | Alta |
| RF-03 | **Gestión de cargas y fecha límite (SLA):** registrar cargas con descripción, peso, número de carga y fecha límite pactada con el cliente. | Alta |
| RF-04 | **Asignación de viajes:** crear un viaje vinculando unidad disponible, operador y carga, definiendo origen, destino, tiempo estimado y cobro. | Alta |
| RF-05 | **Encadenamiento y validación geográfica:** pre-asignar el siguiente viaje validando que el origen del nuevo viaje coincida con el destino del actual. | Alta |
| RF-06 | **Registro de eventos de jornada:** iniciar trayecto, pausa de descanso, reanudar manejo y confirmar entrega, con timestamp real. | Alta |
| RF-07 | **Cálculo acumulativo de horas:** sumar horas efectivas de manejo y descansos fragmentados para validar las reglas contractuales. | Alta |
| RF-08 | **Alertas de fatiga y semáforo SLA:** alertas visuales de estado (a tiempo, descanso pendiente, retraso/SLA). | Media |
| RF-09 | **Transición automática de viajes encadenados:** al confirmar una entrega, el siguiente viaje se activa automáticamente en la pantalla del operador. | Alta |
| RF-10 | **Control financiero por viaje:** registrar el cobro de cada viaje y consultar el total facturado por operador y por unidad. | Media |
| RF-11 | **Autenticación y control de acceso por rol:** Administrador (oficina) u Operador (cabina), con credenciales validadas contra la base de datos. | Alta |
| RF-12 | **Monitor de flota en vivo:** tablero en tiempo real con estados semaforizados, filtrable por estado, unidad u operador. | Media |

---

## Requerimientos no funcionales

| Código | Descripción |
|--------|-------------|
| RNF-01 | **Rendimiento:** las operaciones principales deben responder en menos de 2 segundos en condiciones normales. |
| RNF-02 | **Usabilidad:** la interfaz del operador debe ser tipo tableta, con botones grandes y un máximo de 3 pasos por evento. |
| RNF-03 | **Compatibilidad:** ejecución sobre Java 17 o superior, JavaFX y PostgreSQL en una red LAN local. |
| RNF-04 | **Seguridad:** contraseñas almacenadas con hash, expiración de sesión por inactividad y acceso restringido por rol. |
| RNF-05 | **Integridad de datos:** claves foráneas, restricciones `NOT NULL` y validaciones `CHECK` en el modelo relacional. |
| RNF-06 | **Mantenibilidad:** arquitectura en capas (Vista JavaFX + Servicios + DAO + Modelo) con separación de responsabilidades. |
| RNF-07 | **Portabilidad:** ejecución en Windows, Linux y macOS sobre la JVM. |
| RNF-08 | **Disponibilidad y persistencia:** cada evento de jornada se registra de forma transaccional en PostgreSQL. |
| RNF-09 | **Escalabilidad:** índices en `id_operador`, `id_viaje` y `estado` para sostener el crecimiento del histórico. |
| RNF-10 | **Documentación:** modelo relacional, manual de usuario por rol y manual técnico de instalación y despliegue. |

---

## Reglas de negocio

1. **Descansos flexibles por contrato.** Cada operador tiene un límite de horas continuas de manejo y un tiempo total de descanso obligatorio. El chofer puede fragmentar sus descansos y el sistema acumula los tiempos para validar el cumplimiento.
2. **Encadenamiento estricto de rutas.** El administrador puede pre-asignar el siguiente viaje mientras la unidad sigue en ruta. El sistema exige que `origen_viaje_n+1 == destino_viaje_n`; si no se cumple, se bloquea la pre-asignación.
3. **Doble métrica de tiempos (manejo vs. SLA cliente).** El sistema separa el tiempo estimado de trayecto de la fecha límite contractual de la carga. Los retrasos se miden contra la fecha límite del cliente.
4. **Transición automática de despacho.** Al confirmar la entrega, la siguiente carga encadenada se activa automáticamente en la pantalla del operador, sin aprobaciones manuales.

Además:

- Una unidad en estado **En Ruta** no puede asignarse a un nuevo viaje.
- Solo se asignan unidades y operadores en estado **Disponible**.
- Al crear un viaje, la unidad y el operador pasan automáticamente a estado **En Ruta**.
- La fecha límite del cliente no puede ser anterior a la fecha actual; se valida con el trigger `trg_carga_fecha_limite`.
- La alerta de descanso se activa al superar el **80 %** de las horas máximas de conducción; esta regla y el bloqueo de reanudación se implementan en la capa de servicios.
- La alerta de SLA se activa cuando la proyección de llegada supera la fecha límite del cliente.

### Restricciones aplicadas en PostgreSQL

- `unidades.modelo` es obligatorio; `capacidad_toneladas` es opcional.
- `operadores.horas_max_conduccion` y `horas_min_descanso` deben estar en el rango `(0, 24]`.
- `cargas.peso_toneladas > 0`, `viajes.tiempo_estimado > 0` y `viajes.cobro_viaje > 0`.
- `viajes.origen` y `viajes.destino` deben ser distintos y referencian `ubicaciones`.
- `viajes.fecha_fin` requiere `fecha_inicio`; `id_viaje_previo` no puede apuntar al mismo viaje.
- `registro_jornada` tiene `UNIQUE (id_viaje, num_evento)` y el trigger `trg_rj_num_evento` asigna el consecutivo.
- `usuarios` solo admite roles `ADMIN` u `OPERADOR`; un operador debe tener `id_operador` y su contraseña se guarda con hash BCrypt.
- La asignación de unidad y operador se valida por estado en los servicios de despacho; no hay una tabla de autorización previa.

---

## Arquitectura del proyecto

El código sigue una arquitectura en capas con el patrón **DAO**, separando la interfaz, la lógica de negocio y el acceso a datos:

| Capa | Responsabilidad |
|------|-----------------|
| **Vista (JavaFX + FXML)** | Pantallas del administrador y del operador, controles y estilos. |
| **Controladores** | Manejo de eventos de la interfaz y comunicación con los servicios. |
| **Servicios** | Reglas de negocio: encadenamiento, cálculo de jornadas, alertas de fatiga y SLA. |
| **DAO** | Operaciones CRUD y consultas contra PostgreSQL (`ViajeDAO`, `OperadorDAO`, `CargaDAO`, `UsuarioDAO`, `ClienteDAO`, `UbicacionDAO`, entre otros). |
| **Modelo** | Clases de dominio: `Unidad`, `Operador`, `Carga`, `Viaje`, `RegistroJornada`, `Usuario`, `Cliente`, `Ubicacion`. |
| **Configuración** | Conexión JDBC (Singleton) y carga de variables desde el archivo `.env`. |

### Estructura del repositorio

```text
FleetRoute-Pro/
├── database/
│   └── fleetroute_pro.sql      # Esquema, catálogos, triggers y datos de ejemplo
├── src/
│   └── main/
│       ├── java/
│       │   ├── config/         # Conexión JDBC (Singleton) y lectura del .env
│       │   ├── model/          # Clases de dominio
│       │   ├── dao/            # Acceso a datos (DAO)
│       │   ├── service/        # Lógica y reglas de negocio
│       │   ├── controller/     # Controladores de las vistas
│       │   └── view/           # Clases de apoyo de la interfaz
│       └── resources/
│           ├── fxml/           # Vistas FXML
│           └── styles/         # Hojas de estilo CSS
├── example.env                 # Plantilla de variables de entorno
├── .env                        # Credenciales locales (NO se sube al repositorio)
├── .gitignore
├── pom.xml
└── README.md
```

---

## Modelo de datos

La base de datos `fleetroute_pro` tiene **13 tablas**: 5 catálogos, 4 maestros, 1 de acceso, 2 operativas y 1 bitácora. El esquema completo, las restricciones, los índices, los triggers y los datos de ejemplo están en [`database/fleetroute_pro.sql`](../../Qoder/2026-11-04/91b5298e/database/fleetroute_pro.sql).

| Grupo | Tablas |
|-------|--------|
| Catálogos | `cat_estado_unidad`, `cat_estado_operador`, `cat_estado_carga`, `cat_estado_viaje`, `cat_tipo_evento` |
| Maestros | `clientes`, `ubicaciones`, `unidades`, `operadores` |
| Acceso | `usuarios` |
| Operativas | `cargas`, `viajes` |
| Bitácora | `registro_jornada` |

### Tablas principales

| Tabla | Campos principales | Función |
|-------|--------------------|---------|
| `clientes` | `id_cliente`, `nombre_empresa`, `contacto`, `telefono`, `email`, `direccion`, `activo`, `fecha_registro` | Catálogo opcional de clientes para asociar cargas. |
| `ubicaciones` | `id_ubicacion`, `nombre`, `departamento`, `pais` | Catálogo maestro de orígenes y destinos. |
| `unidades` | `id_unidad`, `numero_unidad`, `placas`, `modelo`, `capacidad_toneladas`, `id_estado_unidad`, `fecha_registro` | Camiones y estado operativo; capacidad opcional. |
| `operadores` | `id_operador`, `cedula`, `nombre`, `telefono`, `horas_max_conduccion`, `horas_min_descanso`, `id_estado_operador`, `fecha_ingreso` | Choferes y reglas de descanso. |
| `usuarios` | `id_usuario`, `username`, `password_hash`, `rol`, `id_operador`, `activo`, `fecha_creacion` | Acceso al sistema con rol `ADMIN` u `OPERADOR`. |
| `cargas` | `id_carga`, `numero_carga`, `descripcion`, `peso_toneladas`, `id_cliente`, `fecha_limite_cliente`, `id_estado_carga`, `fecha_creacion` | Envíos y SLA con el cliente. |
| `viajes` | `id_viaje`, `id_unidad`, `id_operador`, `id_carga`, `origen`, `destino`, `id_viaje_previo`, `tiempo_estimado`, `cobro_viaje`, `fecha_inicio`, `fecha_fin`, `id_estado_viaje`, `fecha_creacion` | Ruta, cobro y encadenamiento. |
| `registro_jornada` | `id_registro`, `id_viaje`, `num_evento`, `id_tipo_evento`, `fecha_hora`, `observacion` | Bitácora de eventos de la jornada. |

### Catálogos de estado

| Catálogo | Valores |
|----------|---------|
| `cat_estado_unidad` | `DISPONIBLE`, `EN_RUTA`, `MANTENIMIENTO` |
| `cat_estado_operador` | `DISPONIBLE`, `EN_RUTA`, `DESCANSO`, `INACTIVO` |
| `cat_estado_carga` | `PENDIENTE`, `ASIGNADA`, `EN_TRANSITO`, `ENTREGADA` |
| `cat_estado_viaje` | `PROGRAMADO`, `EN_CURSO`, `PAUSADO`, `FINALIZADO`, `CANCELADO` |
| `cat_tipo_evento` | `INICIO_TRAYECTO`, `PAUSA_DESCANSO`, `REANUDAR`, `CONFIRMAR_ENTREGA` |

> Los campos `id_estado_*` y `id_tipo_evento` son claves foráneas a estos catálogos; equivalen a los campos `estado` y `tipo_evento` del PDF, pero evitan texto libre.
>
> La interfaz puede mostrar estos valores en formato legible (por ejemplo, `EN_RUTA` como “En Ruta”). No hardcodees los IDs: consulta el catálogo por `nombre`.

### Relaciones principales

- `cargas.id_cliente` → `clientes.id_cliente` (opcional).
- `viajes.id_unidad` → `unidades.id_unidad`.
- `viajes.id_operador` → `operadores.id_operador`.
- `viajes.id_carga` → `cargas.id_carga`.
- `viajes.origen` y `viajes.destino` → `ubicaciones.id_ubicacion`.
- `viajes.id_viaje_previo` → `viajes.id_viaje` (auto-referencia para el encadenamiento).
- `registro_jornada.id_viaje` → `viajes.id_viaje`.
- `registro_jornada.id_tipo_evento` → `cat_tipo_evento.id_tipo_evento`.
- `usuarios.id_operador` → `operadores.id_operador` (único y obligatorio para el rol `OPERADOR`).
- Las columnas `id_estado_*` referencian sus catálogos correspondientes.

### Triggers y automatizaciones

- **`trg_carga_fecha_limite`:** rechaza una `fecha_limite_cliente` anterior a la fecha actual al insertar o modificar una carga.
- **`trg_rj_num_evento`:** asigna `num_evento` de forma consecutiva por viaje. El aplicativo no necesita enviar ese valor; el trigger bloquea la fila del viaje para evitar duplicados en inserciones concurrentes.

> `viajes.origen` y `viajes.destino` almacenan el ID de la ubicación, no texto libre. Esto permite validar el encadenamiento comparando IDs.

---

## Requisitos previos

| Herramienta | Versión mínima | Notas |
|-------------|----------------|-------|
| JDK Eclipse Temurin | **25.0.2** | Debe estar configurado como SDK del proyecto. |
| Maven | 3.9 o superior | Gestiona JavaFX y el driver JDBC de PostgreSQL. |
| PostgreSQL | 14 o superior | Servicio activo en `localhost:5432` (o el puerto configurado); probado con PostgreSQL 18.4. |
| Git | Cualquier versión reciente | Para clonar el repositorio. |
| IDE (opcional) | IntelliJ IDEA / Eclipse | Recomendado para desarrollo y depuración. |

Las dependencias de JavaFX y del driver JDBC se descargan automáticamente mediante Maven; no es necesario instalar el SDK de JavaFX por separado.

> El script crea la extensión `pgcrypto` para generar los hashes BCrypt de los usuarios de ejemplo. El usuario de PostgreSQL debe tener permiso para crear extensiones; si no, un superusuario debe ejecutar `CREATE EXTENSION pgcrypto;`.

Descargas:

- Eclipse Temurin: <https://adoptium.net/>
- PostgreSQL: <https://www.postgresql.org/download/>

---

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd FleetRoute-Pro
```

### 2. Crear la base de datos

El script `database/fleetroute_pro.sql` crea las tablas, restricciones, índices, triggers, catálogos y datos de ejemplo. **Incluye `DROP TABLE`**, así que úsalo en desarrollo o haz un respaldo antes.

**Opción A — Línea de comandos (`psql`):**

```bash
psql -U postgres -c "CREATE DATABASE fleetroute_pro;"
psql -U postgres -d fleetroute_pro -v ON_ERROR_STOP=1 -f database/fleetroute_pro.sql
```

**Opción B — pgAdmin:**

1. Crear una base de datos llamada `fleetroute_pro`.
2. Abrir la base de datos y ejecutar el contenido de `database/fleetroute_pro.sql` desde el Query Tool.

> El script no crea la base de datos; solo el esquema y los datos. En Windows, si `psql` no se reconoce, agrega la carpeta `bin` de PostgreSQL al `PATH` o ejecuta los comandos desde la ruta de instalación.
>
> Si el usuario de PostgreSQL no puede crear la extensión `pgcrypto`, ejecuta `CREATE EXTENSION pgcrypto;` como superusuario antes de volver a correr el script.

### 3. Configurar las credenciales

Copia el archivo de ejemplo y renómbralo a `.env`:

**Windows (CMD):**

```bat
copy example.env .env
```

**Linux / macOS:**

```bash
cp example.env .env
```

Edita `.env` con los datos de **tu** instancia local de PostgreSQL:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fleetroute_pro
DB_USER=postgres
DB_PASSWORD=tu_contrasena
```

> Cada integrante debe crear su propio `.env` con sus credenciales. El archivo `.env` está incluido en `.gitignore` y **nunca debe subirse al repositorio**.

### 4. Compilar y ejecutar

```bash
mvn clean javafx:run
```

Para verificar únicamente la compilación:

```bash
mvn clean compile
```

**Desde IntelliJ IDEA:**

1. Abrir la carpeta del proyecto (IntelliJ detecta el `pom.xml`).
2. Configurar el SDK del proyecto en **File → Project Structure → Project** y seleccionar **Temurin 25.0.2**.
3. Verificar que el archivo `.env` esté en la raíz del proyecto.
4. Ejecutar la clase principal o la meta de Maven `javafx:run`.

---

## Datos de ejemplo

El script inserta los catálogos y un conjunto mínimo de datos para probar el sistema:

| Dato | Valor |
|------|-------|
| Ubicaciones | Managua, León, Granada, Masaya, Estelí, Matagalpa, Chinandega y Rivas |
| Cliente | `Cliente Demo` |
| Unidad | `U-001` (`M 000001`), estado `DISPONIBLE` |
| Operador | `Operador Demo`, cédula `001-000000-0001A`, estado `DISPONIBLE` |
| Usuario administrador | `admin` / `Admin123!` |
| Usuario operador | `operador` / `Operador123!` |

> Las contraseñas de ejemplo se almacenan con hash BCrypt. **Cámbialas antes de usar el sistema fuera del entorno académico.** Si quieres un esquema vacío, elimina la sección 9 del script, pero conserva al menos un usuario `ADMIN` (o vuelve a insertarlo) para poder iniciar sesión.
>
> El script no crea viajes de prueba: crea la unidad y el operador para que el administrador registre el primer viaje desde la aplicación.
>
> Las ubicaciones y el cliente son catálogos de apoyo. Si el módulo Administrador no incluye su mantenimiento, puedes agregar más registros con SQL o ampliar la interfaz; los viajes siempre deben usar ubicaciones existentes.

---

## Variables de entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_HOST` | Host donde se ejecuta PostgreSQL. | `localhost` |
| `DB_PORT` | Puerto del servicio PostgreSQL. | `5432` |
| `DB_NAME` | Nombre de la base de datos. | `fleetroute_pro` |
| `DB_USER` | Usuario de PostgreSQL. | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario. | `tu_contrasena` |

Contenido de la plantilla `example.env`:

```env
# Configuracion de conexion a PostgreSQL
# Copia este archivo como .env y coloca tus credenciales locales.
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fleetroute_pro
DB_USER=postgres
DB_PASSWORD=tu_contrasena
```

> El esquema usa `TIMESTAMP` en hora local de Nicaragua (`America/Managua`) y el script configura esa zona. Configura la sesión JDBC con la misma zona horaria para que `NOW()` y las fechas sean consistentes.

---

## Flujo de uso

### Administrador

1. Iniciar sesión con credenciales de administrador (demo: `admin` / `Admin123!`).
2. Registrar o actualizar unidades, operadores y cargas.
3. Crear un viaje asignando una unidad disponible, un operador y una carga.
4. Pre-asignar el siguiente viaje validando que el origen coincida con el destino actual.
5. Supervisar el monitor de flota en vivo y las alertas de fatiga/SLA.
6. Consultar el cobro de cada viaje y el total facturado por operador y por unidad.

### Operador

1. Iniciar sesión con credenciales de operador (demo: `operador` / `Operador123!`) y visualizar el viaje asignado.
2. Presionar `INICIAR TRAYECTO` para comenzar el registro de la jornada.
3. Registrar pausas de descanso y reanudar el manejo; el sistema acumula los tiempos.
4. Presionar `CONFIRMAR ENTREGA` al finalizar el viaje.
5. Si existe un viaje encadenado, se activa automáticamente en la cabina; si no, la unidad y el operador vuelven a estado **Disponible**.

---

## Solución de problemas

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| `JavaFX runtime components are missing` | La aplicación se ejecutó sin el módulo de JavaFX. | Ejecutar con `mvn clean javafx:run` o configurar el module-path en el IDE. |
| `Connection refused` | El servicio de PostgreSQL no está activo o el puerto es incorrecto. | Iniciar el servicio y revisar `DB_HOST` y `DB_PORT` en `.env`. |
| `FATAL: password authentication failed` | Credenciales incorrectas. | Verificar `DB_USER` y `DB_PASSWORD` en el `.env` local. |
| `database "fleetroute_pro" does not exist` | No se creó la base de datos o no se ejecutó el script. | Ejecutar el paso 2 de la instalación. |
| No se encuentra el archivo `.env` | No se copió la plantilla. | Copiar `example.env` a `.env` en la raíz del proyecto. |
| Acentos o caracteres extraños en la interfaz | Codificación incorrecta. | Configurar la codificación del proyecto en UTF-8. |
| `ERROR: La fecha límite del cliente ... no puede ser anterior...` | Se intentó guardar una carga con fecha pasada. | Usar la fecha actual o una futura. |
| `violates check constraint "ck_viaje_cobro"` | Cobro del viaje en cero o nulo. | Enviar un `cobro_viaje` mayor a cero. |
| `violates check constraint "ck_operador_min_descanso"` | Descanso mínimo en cero. | Usar un valor mayor a cero (máximo 24). |
| `null value in column "modelo"` | La unidad no tiene modelo. | Completar el campo `modelo`; es obligatorio. |
| `violates foreign key constraint "fk_viaje_origen"` o `fk_viaje_destino` | El origen o destino no existe en `ubicaciones`. | Crear la ubicación o seleccionar una existente. |
| `duplicate key value violates unique constraint "uq_rj_viaje_num"` | El trigger no está instalado o se insertaron números de evento repetidos. | Volver a ejecutar el script para crear `trg_rj_num_evento`; no enviar `num_evento`. |
| `permission denied to create extension "pgcrypto"` | El usuario de PostgreSQL no puede crear extensiones. | Ejecutar `CREATE EXTENSION pgcrypto;` como superusuario o pedir permisos. |
| `could not open extension control file ... pgcrypto` | Falta el paquete contrib de PostgreSQL. | Instalar `postgresql-contrib` o generar los hashes con la aplicación. |

---

## Documentación

- **Documento de Requerimientos — FleetRoute Pro v1.0:** objetivo general, requerimientos funcionales y no funcionales, reglas de negocio y modelo relacional.
- **README.md:** guía de instalación, configuración y uso del sistema.
- **`database/fleetroute_pro.sql`:** 13 tablas, catálogos, restricciones, índices, triggers y datos de ejemplo.

---

## Licencia

Proyecto académico desarrollado para la asignatura **PAE — Programación de Aplicaciones de Escritorio**. Su uso está destinado a fines educativos.
