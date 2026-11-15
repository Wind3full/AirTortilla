-- Esquema relacional de AirTortilla (MySQL 8.4).
-- Compose crea previamente la base de datos "airtortilla".
USE airtortilla;

CREATE TABLE IF NOT EXISTS paises (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    codigo_iso VARCHAR(2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_paises_nombre UNIQUE (nombre),
    CONSTRAINT uk_paises_codigo_iso UNIQUE (codigo_iso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    destino_id BIGINT NOT NULL,
    origen VARCHAR(100) NOT NULL,
    nombre_pasajero VARCHAR(150) NOT NULL,
    fecha_reserva DATE NOT NULL,
    localizador VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reservas_localizador UNIQUE (localizador),
    CONSTRAINT fk_reservas_destino FOREIGN KEY (destino_id) REFERENCES paises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_role CHECK (role IN ('EMPLEADO', 'ADMINISTRADOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    registro_afectado VARCHAR(120) NOT NULL,
    fecha TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
