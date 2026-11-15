-- Importación manual alternativa. Ejecútala solo con las tablas paises y
-- reservas vacías. LOCAL hace que el cliente MySQL lea los ficheros del equipo.
USE airtortilla;

LOAD DATA LOCAL INFILE 'res/Paises.csv'
INTO TABLE paises
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@nombre, @codigo_iso)
SET nombre = TRIM(TRAILING '\r' FROM TRIM(@nombre)),
    codigo_iso = UPPER(TRIM(TRAILING '\r' FROM TRIM(@codigo_iso)));

CREATE TEMPORARY TABLE import_reservas (
    destino VARCHAR(100) NOT NULL,
    origen VARCHAR(100) NOT NULL,
    nombre_pasajero VARCHAR(150) NOT NULL,
    fecha_reserva VARCHAR(20) NOT NULL,
    localizador VARCHAR(30) NOT NULL
);

LOAD DATA LOCAL INFILE 'res/Reservas.csv'
INTO TABLE import_reservas
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(destino, origen, nombre_pasajero, fecha_reserva, localizador);

INSERT INTO reservas (destino_id, origen, nombre_pasajero, fecha_reserva, localizador)
SELECT COALESCE(p.id, 0),
       TRIM(TRAILING '\r' FROM TRIM(r.origen)),
       TRIM(TRAILING '\r' FROM TRIM(r.nombre_pasajero)),
       STR_TO_DATE(TRIM(TRAILING '\r' FROM TRIM(r.fecha_reserva)), '%Y-%m-%d'),
       TRIM(TRAILING '\r' FROM TRIM(r.localizador))
FROM import_reservas r
LEFT JOIN paises p ON p.nombre = TRIM(TRAILING '\r' FROM TRIM(r.destino));

DROP TEMPORARY TABLE import_reservas;
