-- =====================================================================
--  FleetRoute Pro — Esquema de Base de Datos
--  Motor: PostgreSQL 14+
--  Codificación: UTF-8
--  Proyecto Final PAE — UAM
--
--  Este script:
--    1. Elimina y recrea las tablas (solo para desarrollo).
--    2. Crea catálogos, maestros, tablas operativas y restricciones.
--    3. Crea triggers para la fecha límite y la numeración de eventos.
--    4. Inserta catálogos y datos de ejemplo.
--
--  IMPORTANTE:
--    - El script NO crea la base de datos. Créala antes:
--        createdb -U postgres fleetroute_pro
--        psql -U postgres -d fleetroute_pro -v ON_ERROR_STOP=1 -f database/fleetroute_pro.sql
--    - Contiene DROP TABLE: úsalo en desarrollo o haz respaldo antes.
-- =====================================================================

SET client_encoding = 'UTF8';
SET TIME ZONE 'America/Managua';

BEGIN;

-- Necesaria para los hashes BCrypt de los usuarios de ejemplo.
-- Si tu usuario no puede crear extensiones, ejecútala como superusuario.
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ---------------------------------------------------------------------
-- 0. Limpieza (útil en desarrollo; NO ejecutar en producción)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS registro_jornada        CASCADE;
DROP TABLE IF EXISTS viajes                  CASCADE;
DROP TABLE IF EXISTS unidad_operador         CASCADE;   -- esquema anterior
DROP TABLE IF EXISTS cargas                  CASCADE;
DROP TABLE IF EXISTS usuarios                CASCADE;
DROP TABLE IF EXISTS operadores              CASCADE;
DROP TABLE IF EXISTS unidades                CASCADE;
DROP TABLE IF EXISTS rutas                   CASCADE;
DROP TABLE IF EXISTS ubicaciones             CASCADE;
DROP TABLE IF EXISTS clientes                CASCADE;
DROP TABLE IF EXISTS empresa                 CASCADE;
DROP TABLE IF EXISTS cat_tipo_evento         CASCADE;
DROP TABLE IF EXISTS cat_estado_viaje        CASCADE;
DROP TABLE IF EXISTS cat_estado_carga        CASCADE;
DROP TABLE IF EXISTS cat_estado_operador     CASCADE;
DROP TABLE IF EXISTS cat_estado_unidad       CASCADE;

DROP FUNCTION IF EXISTS fn_validar_fecha_limite_carga() CASCADE;
DROP FUNCTION IF EXISTS fn_asignar_num_evento() CASCADE;


-- =====================================================================
-- 1. CATÁLOGOS (tablas de apoyo)
-- =====================================================================

CREATE TABLE cat_estado_unidad (
    id_estado_unidad SMALLSERIAL PRIMARY KEY,
    nombre           VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE cat_estado_operador (
    id_estado_operador SMALLSERIAL PRIMARY KEY,
    nombre             VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE cat_estado_carga (
    id_estado_carga SMALLSERIAL PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE cat_estado_viaje (
    id_estado_viaje SMALLSERIAL PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE cat_tipo_evento (
    id_tipo_evento SMALLSERIAL PRIMARY KEY,
    nombre         VARCHAR(30) NOT NULL UNIQUE
);


-- =====================================================================
-- 2. MAESTROS Y ACCESO
-- =====================================================================

-- Perfil de la empresa transportista. La aplicación exige una sola fila,
-- creada por el asistente de primera configuración; no se siembra aquí.
CREATE TABLE empresa (
    id_empresa          SERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    ruc                 VARCHAR(20),
    telefono            VARCHAR(30),
    email               VARCHAR(120),
    direccion           VARCHAR(200),
    fecha_configuracion TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE clientes (
    id_cliente     SERIAL PRIMARY KEY,
    nombre_empresa VARCHAR(150) NOT NULL UNIQUE,
    contacto       VARCHAR(120),
    telefono       VARCHAR(30),
    email          VARCHAR(120),
    direccion      VARCHAR(200),
    activo         BOOLEAN   NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ubicaciones (
    id_ubicacion SERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    departamento VARCHAR(80)  NOT NULL,
    pais         VARCHAR(80)  NOT NULL DEFAULT 'Nicaragua',
    latitud      DOUBLE PRECISION,
    longitud     DOUBLE PRECISION,

    CONSTRAINT uq_ubicacion UNIQUE (nombre, departamento)
);

CREATE TABLE rutas (
    id_ruta               SERIAL PRIMARY KEY,
    id_origen             INT NOT NULL,
    id_destino            INT NOT NULL,
    tiempo_estimado_horas NUMERIC(6,2) NOT NULL,

    CONSTRAINT fk_ruta_origen
        FOREIGN KEY (id_origen) REFERENCES ubicaciones (id_ubicacion)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ruta_destino
        FOREIGN KEY (id_destino) REFERENCES ubicaciones (id_ubicacion)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_ruta_origen_destino UNIQUE (id_origen, id_destino),
    CONSTRAINT ck_ruta_tiempo CHECK (tiempo_estimado_horas > 0),
    CONSTRAINT ck_ruta_distinta CHECK (id_origen <> id_destino)
);

CREATE TABLE unidades (
    id_unidad            SERIAL PRIMARY KEY,
    numero_unidad        VARCHAR(20)   NOT NULL UNIQUE,
    placas               VARCHAR(15)   NOT NULL UNIQUE,
    modelo               VARCHAR(80)   NOT NULL,
    capacidad_toneladas  NUMERIC(8,2),
    id_estado_unidad     SMALLINT      NOT NULL,
    fecha_registro       TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_unidad_estado
        FOREIGN KEY (id_estado_unidad) REFERENCES cat_estado_unidad (id_estado_unidad)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_unidad_capacidad
        CHECK (capacidad_toneladas IS NULL OR capacidad_toneladas > 0)
);

CREATE TABLE operadores (
    id_operador           SERIAL PRIMARY KEY,
    cedula                VARCHAR(20)  NOT NULL UNIQUE,
    nombre                VARCHAR(150) NOT NULL,
    telefono              VARCHAR(30),
    horas_max_conduccion  NUMERIC(5,2) NOT NULL,
    horas_min_descanso    NUMERIC(5,2) NOT NULL,
    id_estado_operador    SMALLINT     NOT NULL,
    fecha_ingreso         DATE,

    CONSTRAINT fk_operador_estado
        FOREIGN KEY (id_estado_operador) REFERENCES cat_estado_operador (id_estado_operador)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_operador_max_conduccion
        CHECK (horas_max_conduccion > 0 AND horas_max_conduccion <= 24),
    CONSTRAINT ck_operador_min_descanso
        CHECK (horas_min_descanso > 0 AND horas_min_descanso <= 24)
);

CREATE TABLE usuarios (
    id_usuario     SERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    id_operador    INT          UNIQUE,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    es_principal   BOOLEAN      NOT NULL DEFAULT FALSE,
    puede_ver_reportes BOOLEAN  NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usuario_operador
        FOREIGN KEY (id_operador) REFERENCES operadores (id_operador)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_usuario_rol
        CHECK (rol IN ('ADMIN', 'OPERADOR')),
    CONSTRAINT ck_usuario_rol_operador
        CHECK (
            (rol = 'ADMIN'    AND id_operador IS NULL)
            OR
            (rol = 'OPERADOR' AND id_operador IS NOT NULL)
        )
);


-- =====================================================================
-- 3. TRANSACCIONALES
-- =====================================================================

CREATE TABLE cargas (
    id_carga             SERIAL PRIMARY KEY,
    numero_carga         VARCHAR(30)   NOT NULL UNIQUE,
    descripcion          VARCHAR(250),
    peso_toneladas       NUMERIC(8,2)  NOT NULL,
    id_cliente           INT,                       -- opcional
    fecha_limite_cliente TIMESTAMP     NOT NULL,    -- SLA con el cliente
    id_estado_carga      SMALLINT      NOT NULL,
    fecha_creacion       TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_carga_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_carga_estado
        FOREIGN KEY (id_estado_carga) REFERENCES cat_estado_carga (id_estado_carga)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_carga_peso
        CHECK (peso_toneladas > 0)
);


CREATE TABLE viajes (
    id_viaje          SERIAL PRIMARY KEY,
    id_unidad         INT           NOT NULL,
    id_operador       INT           NOT NULL,
    id_carga          INT           NOT NULL,
    origen            INT           NOT NULL,       -- FK a ubicaciones
    destino           INT           NOT NULL,       -- FK a ubicaciones
    id_viaje_previo   INT           UNIQUE,         -- encadenamiento lineal
    tiempo_estimado   NUMERIC(6,2)  NOT NULL,       -- horas de manejo
    cobro_viaje       NUMERIC(12,2) NOT NULL,
    fecha_inicio      TIMESTAMP,
    fecha_fin         TIMESTAMP,
    id_estado_viaje   SMALLINT      NOT NULL,
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_viaje_unidad
        FOREIGN KEY (id_unidad) REFERENCES unidades (id_unidad)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_operador
        FOREIGN KEY (id_operador) REFERENCES operadores (id_operador)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_carga
        FOREIGN KEY (id_carga) REFERENCES cargas (id_carga)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_origen
        FOREIGN KEY (origen) REFERENCES ubicaciones (id_ubicacion)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_destino
        FOREIGN KEY (destino) REFERENCES ubicaciones (id_ubicacion)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_estado
        FOREIGN KEY (id_estado_viaje) REFERENCES cat_estado_viaje (id_estado_viaje)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_viaje_previo
        FOREIGN KEY (id_viaje_previo) REFERENCES viajes (id_viaje)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT ck_viaje_tiempo
        CHECK (tiempo_estimado > 0),
    CONSTRAINT ck_viaje_cobro
        CHECK (cobro_viaje > 0),
    CONSTRAINT ck_viaje_ruta
        CHECK (origen <> destino),
    CONSTRAINT ck_viaje_no_self
        CHECK (id_viaje_previo IS NULL OR id_viaje_previo <> id_viaje),
    CONSTRAINT ck_viaje_fechas
        CHECK (fecha_fin IS NULL OR (fecha_inicio IS NOT NULL AND fecha_fin >= fecha_inicio))
);


-- =====================================================================
-- 4. REGISTRO_JORNADA (bitácora de eventos del viaje)
-- =====================================================================

CREATE TABLE registro_jornada (
    id_registro    SERIAL PRIMARY KEY,
    id_viaje       INT          NOT NULL,
    num_evento     INT          NOT NULL,   -- consecutivo por viaje (trigger)
    id_tipo_evento SMALLINT     NOT NULL,
    fecha_hora     TIMESTAMP    NOT NULL DEFAULT NOW(),
    observacion    VARCHAR(200),

    CONSTRAINT fk_rj_viaje
        FOREIGN KEY (id_viaje) REFERENCES viajes (id_viaje)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rj_tipo
        FOREIGN KEY (id_tipo_evento) REFERENCES cat_tipo_evento (id_tipo_evento)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_rj_viaje_num
        UNIQUE (id_viaje, num_evento),
    CONSTRAINT ck_rj_num_evento
        CHECK (num_evento > 0)
);


-- =====================================================================
-- 5. TRIGGERS
-- =====================================================================

-- 5.1 La fecha límite del cliente no puede ser anterior a la fecha actual.
--     Se usa la fecha de Nicaragua para no depender de la zona del servidor.
CREATE OR REPLACE FUNCTION fn_validar_fecha_limite_carga()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_hoy DATE := (CURRENT_TIMESTAMP AT TIME ZONE 'America/Managua')::date;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.fecha_limite_cliente::date < v_hoy THEN
            RAISE EXCEPTION
                'La fecha límite del cliente (%) no puede ser anterior a la fecha actual (%)',
                NEW.fecha_limite_cliente, v_hoy;
        END IF;
    ELSIF NEW.fecha_limite_cliente IS DISTINCT FROM OLD.fecha_limite_cliente THEN
        IF NEW.fecha_limite_cliente::date < v_hoy THEN
            RAISE EXCEPTION
                'La fecha límite del cliente (%) no puede ser anterior a la fecha actual (%)',
                NEW.fecha_limite_cliente, v_hoy;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_carga_fecha_limite
BEFORE INSERT OR UPDATE ON cargas
FOR EACH ROW EXECUTE FUNCTION fn_validar_fecha_limite_carga();


-- 5.2 Numera los eventos por viaje de forma segura.
--     Bloquea la fila del viaje para serializar inserciones concurrentes.
CREATE OR REPLACE FUNCTION fn_asignar_num_evento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM 1
       FROM viajes
      WHERE id_viaje = NEW.id_viaje
      FOR UPDATE;

    SELECT COALESCE(MAX(num_evento), 0) + 1
      INTO NEW.num_evento
      FROM registro_jornada
     WHERE id_viaje = NEW.id_viaje;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_rj_num_evento
BEFORE INSERT ON registro_jornada
FOR EACH ROW EXECUTE FUNCTION fn_asignar_num_evento();


-- =====================================================================
-- 6. REGLAS DE APLICACIÓN (no expresables como CHECK)
-- =====================================================================
--  * Alerta de fatiga cuando la conducción acumulada supera el 80 %
--    de operadores.horas_max_conduccion (RF-08).
--  * Bloquear REANUDAR si el descanso acumulado es menor que
--    operadores.horas_min_descanso (RF-07).
--  * No asignar unidades ni operadores que no estén DISPONIBLE (RF-04).
--  * Al crear un viaje, pasar unidad y operador a EN_RUTA; al finalizar
--    sin viaje encadenado, devolverlos a DISPONIBLE (RF-04/RF-09).
--  * Validar que viaje_n+1.origen = viaje_n.destino, con la misma
--    unidad y operador (RF-05); validar también ciclos en id_viaje_previo.
--  * El operador solo puede ver sus propios viajes; el administrador ve
--    el monitor completo (RF-11).


-- =====================================================================
-- 7. ÍNDICES RECOMENDADOS
-- =====================================================================

-- FKs y filtros de cargas
CREATE INDEX idx_cargas_id_cliente    ON cargas (id_cliente);
CREATE INDEX idx_cargas_estado_limite ON cargas (id_estado_carga, fecha_limite_cliente);

-- FKs y filtros de viajes
CREATE INDEX idx_viajes_id_unidad   ON viajes (id_unidad);
CREATE INDEX idx_viajes_id_operador ON viajes (id_operador);
CREATE INDEX idx_viajes_id_carga    ON viajes (id_carga);
CREATE INDEX idx_viajes_origen      ON viajes (origen);
CREATE INDEX idx_viajes_destino     ON viajes (destino);
CREATE INDEX idx_viajes_estado_fecha ON viajes (id_estado_viaje, fecha_inicio);
-- (id_viaje_previo ya está cubierto por UNIQUE)

-- Estados para el monitor de flota
CREATE INDEX idx_unidades_estado   ON unidades (id_estado_unidad);
CREATE INDEX idx_operadores_estado ON operadores (id_estado_operador);

-- Reportes de jornada
CREATE INDEX idx_rj_id_tipo ON registro_jornada (id_tipo_evento, fecha_hora);
-- (uq_rj_viaje_num ya cubre la búsqueda por id_viaje)


-- =====================================================================
-- 8. SEED — Catálogos
-- =====================================================================

INSERT INTO cat_estado_unidad (nombre) VALUES
    ('DISPONIBLE'),
    ('EN_RUTA'),
    ('MANTENIMIENTO');

INSERT INTO cat_estado_operador (nombre) VALUES
    ('DISPONIBLE'),
    ('EN_RUTA'),
    ('DESCANSO'),
    ('INACTIVO');

INSERT INTO cat_estado_carga (nombre) VALUES
    ('PENDIENTE'),
    ('ASIGNADA'),
    ('EN_TRANSITO'),
    ('ENTREGADA');

INSERT INTO cat_estado_viaje (nombre) VALUES
    ('PROGRAMADO'),
    ('EN_CURSO'),
    ('PAUSADO'),
    ('FINALIZADO'),
    ('CANCELADO');

INSERT INTO cat_tipo_evento (nombre) VALUES
    ('INICIO_TRAYECTO'),
    ('PAUSA_DESCANSO'),
    ('REANUDAR'),
    ('CONFIRMAR_ENTREGA');


-- =====================================================================
-- 9. SEED — Datos de ejemplo (puedes eliminarlos en producción)
-- =====================================================================

INSERT INTO ubicaciones (nombre, departamento) VALUES
    ('Managua',    'Managua'),
    ('León',       'León'),
    ('Granada',    'Granada'),
    ('Masaya',     'Masaya'),
    ('Estelí',     'Estelí'),
    ('Matagalpa',  'Matagalpa'),
    ('Chinandega', 'Chinandega'),
    ('Rivas',      'Rivas');

INSERT INTO clientes (nombre_empresa, contacto, telefono, email)
VALUES ('Cliente Demo', 'Contacto Demo', '0000-0000', 'demo@fleetroute.local');

INSERT INTO unidades (
    numero_unidad, placas, modelo, capacidad_toneladas, id_estado_unidad
)
VALUES (
    'U-001', 'M 000001', 'Unidad Demo', 30.00,
    (SELECT id_estado_unidad FROM cat_estado_unidad WHERE nombre = 'DISPONIBLE')
);

INSERT INTO operadores (
    cedula, nombre, telefono, horas_max_conduccion, horas_min_descanso, id_estado_operador
)
VALUES (
    '001-000000-0001A', 'Operador Demo', '0000-0000', 8.00, 8.00,
    (SELECT id_estado_operador FROM cat_estado_operador WHERE nombre = 'DISPONIBLE')
);

-- Usuarios de demostración. Cambia estas contraseñas en producción.
-- El hash se genera con BCrypt (pgcrypto); la aplicación debe verificar
-- con un algoritmo BCrypt compatible. El primer administrador es el principal:
-- solo él puede gestionar cuentas y permisos en el panel administrativo.
INSERT INTO usuarios (username, password_hash, rol, es_principal, puede_ver_reportes)
VALUES ('admin', crypt('Admin123!', gen_salt('bf', 10)), 'ADMIN', TRUE, TRUE);

INSERT INTO usuarios (username, password_hash, rol, id_operador)
VALUES (
    'operador',
    crypt('Operador123!', gen_salt('bf', 10)),
    'OPERADOR',
    (SELECT id_operador FROM operadores WHERE cedula = '001-000000-0001A')
);


-- =====================================================================
-- 10. COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================================

COMMENT ON TABLE  empresa          IS 'Perfil de la empresa transportista; una fila creada en el primer arranque.';
COMMENT ON TABLE  clientes         IS 'Empresas clientes que contratan fletes (tabla de apoyo opcional).';
COMMENT ON TABLE  ubicaciones      IS 'Catálogo maestro de puntos geográficos para origen y destino.';
COMMENT ON TABLE  unidades         IS 'Camiones / tractocamiones de la flota.';
COMMENT ON TABLE  operadores       IS 'Choferes con sus reglas de fatiga según contrato.';
COMMENT ON TABLE  usuarios         IS 'Credenciales de acceso por rol; password_hash en BCrypt.';
COMMENT ON TABLE  cargas           IS 'Envíos contratados por un cliente, con SLA (fecha límite).';
COMMENT ON TABLE  viajes           IS 'Ejecución de una carga por una unidad y un operador.';
COMMENT ON TABLE  registro_jornada IS 'Bitácora de eventos del viaje para el motor de fatiga.';

COMMENT ON COLUMN unidades.modelo              IS 'Obligatorio según RF-01.';
COMMENT ON COLUMN unidades.capacidad_toneladas IS 'Opcional; si se informa, se valida contra el peso de la carga.';
COMMENT ON COLUMN operadores.nombre            IS 'Nombre completo del operador (campo nombre del PDF).';
COMMENT ON COLUMN operadores.horas_max_conduccion IS 'Tope de conducción antes de descanso obligatorio.';
COMMENT ON COLUMN operadores.horas_min_descanso   IS 'Descanso acumulado mínimo requerido antes de reanudar; debe ser > 0.';
COMMENT ON COLUMN usuarios.password_hash       IS 'Hash BCrypt; nunca almacenar la contraseña en texto plano.';
COMMENT ON COLUMN usuarios.id_operador         IS 'Obligatorio para rol OPERADOR; nulo para ADMIN.';
COMMENT ON COLUMN cargas.id_cliente            IS 'Opcional; permite asociar la carga a un cliente.';
COMMENT ON COLUMN cargas.fecha_limite_cliente  IS 'SLA pactado con el cliente; no puede ser anterior a la fecha actual.';
COMMENT ON COLUMN viajes.origen                IS 'FK a ubicaciones; punto de origen del viaje.';
COMMENT ON COLUMN viajes.destino               IS 'FK a ubicaciones; punto de destino del viaje.';
COMMENT ON COLUMN viajes.tiempo_estimado       IS 'Horas de manejo estimadas (distinto del SLA del cliente).';
COMMENT ON COLUMN viajes.cobro_viaje           IS 'Tarifa cobrada por el flete; debe ser mayor a cero.';
COMMENT ON COLUMN viajes.id_viaje_previo       IS 'Encadenamiento lineal: viaje anterior de la misma unidad/operador.';
COMMENT ON COLUMN registro_jornada.num_evento  IS 'Consecutivo por viaje; lo asigna automáticamente trg_rj_num_evento.';
COMMENT ON COLUMN registro_jornada.id_registro IS 'Identificador único del evento (id_registro del PDF).';


-- =====================================================================
-- 11. VERIFICACIÓN RÁPIDA (opcional)
-- =====================================================================
-- SELECT table_name FROM information_schema.tables
--  WHERE table_schema = 'public' ORDER BY table_name;
--
-- SELECT conname, contype FROM pg_constraint
--  WHERE conrelid = 'viajes'::regclass ORDER BY contype;
--
-- SELECT tgname FROM pg_trigger
--  WHERE tgrelid IN ('cargas'::regclass, 'registro_jornada'::regclass)
--    AND NOT tgisinternal;
--
-- SELECT username, rol, activo FROM usuarios ORDER BY id_usuario;

COMMIT;
