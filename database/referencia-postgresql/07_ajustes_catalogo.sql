-- Ajustes sobre el catalogo compartido. Va despues de 06_datos_catalogo.sql y se puede repetir.
BEGIN;
SET LOCAL search_path=tp_logistica,public;

-- Para bases cargadas antes de que la columna estuviera en 00_esquema.sql.
ALTER TABLE regla_alerta ADD COLUMN IF NOT EXISTS tipo_contenedor varchar(60);

-- REG-01 y REG-02 son de cadena de frio: solo aplican a contenedores refrigerados.
UPDATE regla_alerta SET tipo_contenedor='REEFER'
 WHERE id_regla IN ('96196805-40db-51f4-a2b2-0e8cc7ee2a63','ba9fb76f-fec9-5f6e-b9b5-df61a2035234');

-- Sin cuenta corriente, facturar_reporte y facturar_envio rechazan al cliente.
INSERT INTO cuenta_corriente(id_cliente,naturaleza)
SELECT c.id_cliente,'COBRAR' FROM cliente c
 WHERE NOT EXISTS (SELECT 1 FROM cuenta_corriente k WHERE k.id_cliente=c.id_cliente);
COMMIT;
