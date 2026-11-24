-- Actualización de las versiones anteriores de FleetRoute. No borra registros.
BEGIN;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS es_principal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS puede_ver_reportes BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ubicaciones ADD COLUMN IF NOT EXISTS pais VARCHAR(80) NOT NULL DEFAULT 'Nicaragua';
ALTER TABLE ubicaciones ADD COLUMN IF NOT EXISTS latitud DOUBLE PRECISION;
ALTER TABLE ubicaciones ADD COLUMN IF NOT EXISTS longitud DOUBLE PRECISION;
ALTER TABLE ubicaciones ADD COLUMN IF NOT EXISTS categoria VARCHAR(20) NOT NULL DEFAULT 'Otro';
ALTER TABLE ubicaciones ADD COLUMN IF NOT EXISTS color VARCHAR(7) NOT NULL DEFAULT '#0891B2';
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS moneda VARCHAR(3) NOT NULL DEFAULT 'NIO';
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS pais VARCHAR(80) NOT NULL DEFAULT 'Nicaragua';
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS zona_horaria VARCHAR(60) NOT NULL DEFAULT 'America/Managua';
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS direccion VARCHAR(200);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'ubicaciones'::regclass
               AND conname = 'uq_ubicacion' AND pg_get_constraintdef(oid) NOT LIKE '%pais%') THEN
        ALTER TABLE ubicaciones DROP CONSTRAINT uq_ubicacion;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'ubicaciones'::regclass
                   AND conname = 'uq_ubicacion') THEN
        ALTER TABLE ubicaciones ADD CONSTRAINT uq_ubicacion UNIQUE (nombre, departamento, pais);
    END IF;
    IF to_regclass('public.paises_habilitados') IS NULL THEN
        CREATE TABLE paises_habilitados (
            id_pais_habilitado SERIAL PRIMARY KEY,
            nombre VARCHAR(80) NOT NULL UNIQUE,
            fecha_activacion TIMESTAMP NOT NULL DEFAULT NOW()
        );
        INSERT INTO paises_habilitados(nombre)
        SELECT DISTINCT pais FROM ubicaciones WHERE pais IS NOT NULL AND trim(pais) <> '';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS rutas (
    id_ruta SERIAL PRIMARY KEY,
    id_origen INT NOT NULL REFERENCES ubicaciones(id_ubicacion) ON DELETE RESTRICT ON UPDATE CASCADE,
    id_destino INT NOT NULL REFERENCES ubicaciones(id_ubicacion) ON DELETE RESTRICT ON UPDATE CASCADE,
    tiempo_estimado_horas NUMERIC(6,2) NOT NULL CHECK (tiempo_estimado_horas > 0),
    precio NUMERIC(12,2) CHECK (precio IS NULL OR precio > 0),
    CONSTRAINT uq_ruta_origen_destino UNIQUE (id_origen, id_destino),
    CONSTRAINT ck_ruta_distinta CHECK (id_origen <> id_destino)
);
ALTER TABLE rutas ADD COLUMN IF NOT EXISTS precio NUMERIC(12,2) CHECK (precio IS NULL OR precio > 0);

CREATE TABLE IF NOT EXISTS alertas_operador (
    id_alerta SERIAL PRIMARY KEY,
    id_viaje INT NOT NULL REFERENCES viajes(id_viaje) ON DELETE CASCADE ON UPDATE CASCADE,
    id_operador INT NOT NULL REFERENCES operadores(id_operador) ON DELETE RESTRICT ON UPDATE CASCADE,
    id_unidad INT NOT NULL REFERENCES unidades(id_unidad) ON DELETE RESTRICT ON UPDATE CASCADE,
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    ubicacion VARCHAR(200),
    mensaje VARCHAR(300),
    fecha_hora TIMESTAMP NOT NULL DEFAULT NOW(),
    atendida BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_alertas_fecha ON alertas_operador(fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_alertas_atendida ON alertas_operador(atendida);

COMMIT;
