package ar.edu.uade.logistica.integration.neo4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traduce un evento del outbox a sentencias Cypher. Todas usan MERGE: si el nodo o la relación ya existe
 * lo reutiliza, si no lo crea. Por eso reprocesar un evento no duplica, y tampoco importa si una relación
 * llega antes que sus nodos: MERGE crea el nodo con su id y el evento del nodo completa las propiedades.
 *
 * FUERA DE CLASE: los parámetros $nombre. Se usan porque el driver separa los valores de la consulta.
 */
final class ProyeccionGrafo {
    private ProyeccionGrafo() { }

    record Sentencia(String cypher, Map<String, Object> parametros) { }

    static List<Sentencia> sentencias(String agregado, String tipoEvento, Map<String, Object> p) {
        boolean baja = tipoEvento.endsWith("_DELETE");
        return switch (agregado) {
            case "cliente" -> baja ? borrar("Cliente", "id_cliente", p) : cliente(p);
            case "operador_logistico" -> baja ? borrar("Operador", "id_operador", p) : operador(p);
            case "ubicacion" -> baja ? borrar("Ubicacion", "id_ubicacion", p) : ubicacion(p);
            case "vehiculo" -> baja ? borrar("Vehiculo", "id_vehiculo", p) : vehiculo(p);
            case "contenedor" -> baja ? borrar("Contenedor", "id_contenedor", p) : contenedor(p);
            case "envio" -> baja ? borrar("Envio", "id_envio", p) : envio(p);
            case "tramo" -> baja ? borrar("Tramo", "id_tramo", p) : tramo(p);
            case "incidente" -> baja ? borrar("Incidente", "id_incidente", p) : incidente(p);
            case "envio_contenedor" -> baja ? desasociar(p) : asociar(p);
            // Proveedores y sensores no forman parte de la red del grafo.
            default -> List.of();
        };
    }

    private static List<Sentencia> cliente(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (c:Cliente {id_cliente: $id_cliente})
                SET c.razon_social = $razon_social, c.pais = $pais, c.estado = $estado
                """, p));
    }

    private static List<Sentencia> operador(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (o:Operador {id_operador: $id_operador})
                SET o.razon_social = $razon_social, o.pais = $pais, o.estado = $estado
                """, p));
    }

    private static List<Sentencia> ubicacion(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (u:Ubicacion {id_ubicacion: $id_ubicacion})
                SET u.codigo = $codigo, u.nombre = $nombre, u.pais = $pais, u.tipo = $tipo
                """, p));
    }

    private static List<Sentencia> vehiculo(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (v:Vehiculo {id_vehiculo: $id_vehiculo})
                SET v.tipo = $tipo, v.identificacion = $identificacion, v.estado = $estado
                MERGE (o:Operador {id_operador: $id_operador})
                MERGE (v)-[:PERTENECE_A]->(o)
                """, p));
    }

    private static List<Sentencia> contenedor(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (k:Contenedor {id_contenedor: $id_contenedor})
                SET k.codigo_internacional = $codigo_internacional, k.tipo = $tipo, k.estado = $estado
                """, p));
    }

    private static List<Sentencia> envio(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (e:Envio {id_envio: $id_envio})
                SET e.origen = $origen, e.destino = $destino, e.estado = $estado, e.volumen_m3 = $volumen_m3
                MERGE (c:Cliente {id_cliente: $id_cliente})
                MERGE (c)-[:SOLICITA]->(e)
                """, p));
    }

    private static List<Sentencia> tramo(Map<String, Object> p) {
        List<Sentencia> sentencias = new ArrayList<>();
        sentencias.add(new Sentencia("""
                MERGE (t:Tramo {id_tramo: $id_tramo})
                SET t.origen = $origen, t.destino = $destino, t.medio_transporte = $medio_transporte,
                    t.estado = $estado, t.numero_tramo = $numero_tramo
                MERGE (e:Envio {id_envio: $id_envio})
                MERGE (e)-[r:COMPUESTO_POR]->(t)
                SET r.orden = $numero_tramo
                """, p));
        if (p.get("id_ubicacion_origen") != null) sentencias.add(new Sentencia("""
                MERGE (t:Tramo {id_tramo: $id_tramo})
                MERGE (u:Ubicacion {id_ubicacion: $id_ubicacion_origen})
                MERGE (t)-[:ORIGEN]->(u)
                """, p));
        if (p.get("id_ubicacion_destino") != null) sentencias.add(new Sentencia("""
                MERGE (t:Tramo {id_tramo: $id_tramo})
                MERGE (u:Ubicacion {id_ubicacion: $id_ubicacion_destino})
                MERGE (t)-[:DESTINO]->(u)
                """, p));
        if (p.get("id_vehiculo") != null) sentencias.add(new Sentencia("""
                MERGE (t:Tramo {id_tramo: $id_tramo})
                MERGE (v:Vehiculo {id_vehiculo: $id_vehiculo})
                MERGE (t)-[:EJECUTADO_POR]->(v)
                """, p));
        // Un tramo puede tener operador sin vehículo cargado: la relación directa mantiene la red completa.
        if (p.get("id_operador") != null) sentencias.add(new Sentencia("""
                MERGE (t:Tramo {id_tramo: $id_tramo})
                MERGE (o:Operador {id_operador: $id_operador})
                MERGE (t)-[:OPERADO_POR]->(o)
                """, p));
        return sentencias;
    }

    private static List<Sentencia> incidente(Map<String, Object> p) {
        List<Sentencia> sentencias = new ArrayList<>();
        sentencias.add(new Sentencia("""
                MERGE (i:Incidente {id_incidente: $id_incidente})
                SET i.tipo = $tipo, i.severidad = $severidad, i.estado = $estado, i.descripcion = $descripcion
                MERGE (e:Envio {id_envio: $id_envio})
                MERGE (e)-[:REGISTRA]->(i)
                """, p));
        if (p.get("id_tramo") != null) sentencias.add(new Sentencia("""
                MERGE (i:Incidente {id_incidente: $id_incidente})
                MERGE (t:Tramo {id_tramo: $id_tramo})
                MERGE (t)-[:REGISTRA]->(i)
                """, p));
        return sentencias;
    }

    private static List<Sentencia> asociar(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MERGE (e:Envio {id_envio: $id_envio})
                MERGE (k:Contenedor {id_contenedor: $id_contenedor})
                MERGE (e)-[a:ASIGNADO_A {fecha_asociacion: $fecha_asociacion}]->(k)
                SET a.fecha_desasociacion = $fecha_desasociacion
                """, p));
    }

    private static List<Sentencia> desasociar(Map<String, Object> p) {
        return List.of(new Sentencia("""
                MATCH (:Envio {id_envio: $id_envio})-[a:ASIGNADO_A {fecha_asociacion: $fecha_asociacion}]->
                      (:Contenedor {id_contenedor: $id_contenedor})
                DELETE a
                """, p));
    }

    private static List<Sentencia> borrar(String etiqueta, String id, Map<String, Object> p) {
        return List.of(new Sentencia(
                "MATCH (n:" + etiqueta + " {" + id + ": $" + id + "}) DETACH DELETE n", p));
    }
}
