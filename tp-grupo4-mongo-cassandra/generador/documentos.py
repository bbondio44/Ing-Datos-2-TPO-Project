"""Documentos de MongoDB coherentes con la telemetría simulada y el catálogo."""

from __future__ import annotations

from datetime import datetime, timedelta

from catalogo import AHORA_REFERENCIA, Catalogo, Envio, Tramo, t
from ids import id_derivado, id_estable
from telemetria import (
    GOLPES,
    LECTURA_TARDIA,
    SENSOR_BATERIA,
    SENSOR_EXCURSION,
    SENSOR_INACTIVO,
    SENSOR_NUNCA,
    SENSOR_VIBRACION,
    ULTIMA_DE_INACTIVO,
    Lectura,
    anomalias,
)

S = timedelta(seconds=1)
M = timedelta(minutes=1)
H = timedelta(hours=1)
ASIGNACION_SEN040 = t("2026-10-21T00:00:00")   # alta del sensor en el contenedor (PostgreSQL)


def _geo(lat: float, lon: float) -> dict:
    return {"type": "Point", "coordinates": [round(lon, 5), round(lat, 5)]}


def _ubic(nombre: str, pais: str, lat: float, lon: float) -> dict:
    return {"nombre": nombre, "pais": pais, "geo": _geo(lat, lon)}


class Documentos:
    def __init__(self, cat: Catalogo, lecturas: list[Lectura]):
        self.cat = cat
        self.lecturas = lecturas
        self.eventos: list[dict] = []
        self.alertas: list[dict] = []
        self.conversaciones: list[dict] = []
        self.mensajes: list[dict] = []
        self.reportes: list[dict] = []
        self.incidentes: list[dict] = []   # filas para PostgreSQL (Persona 1)
        self._eventos_base()
        self._eventos_especiales()
        self._alertas()
        self._mensajeria()
        self._reportes()
        self.eventos.sort(key=lambda e: (e["id_envio"], e["fecha"]))

    # ================================================================== eventos
    def _evento(self, env: Envio, tipo: str, fecha: datetime, ubic: dict | None, descripcion: str,
                payload: dict, origen: str = "OPERADOR", tramo: Tramo | None = None,
                cont: str | None = None, **extra) -> dict:
        ev = {
            "_id": id_derivado("evento", env.codigo, tipo, fecha.isoformat(), descripcion),
            "id_envio": env.id,
            "id_contenedor": self.cat.contenedores[cont].id if cont else None,
            "id_tramo": tramo.id if tramo else None,
            "tipo": tipo,
            "fecha": fecha,
            "ubicacion": ubic,
            "descripcion": descripcion,
            "payload": payload,
            "origen": origen,
            "id_alerta": None,
            "id_incidente": None,
            "id_correlacion": id_derivado("corr", env.codigo, tipo, fecha.isoformat())[:12],
            "registrado_por": None,
            "creado_en": fecha + 2 * S,
        }
        ev.update(extra)
        if fecha <= AHORA_REFERENCIA:
            self.eventos.append(ev)
        return ev

    def _eventos_base(self) -> None:
        for env in self.cat.envios.values():
            cont0 = env.contenedores[0]
            ultimo = len(env.tramos)
            for tr in env.tramos:
                o, d = tr.origen, tr.destino
                op_nombre = self.cat.operadores[tr.codigo_operador][1]
                if tr.orden == 1:
                    self._evento(env, "CARGA_INICIADA", tr.salida - 2 * H, _ubic(o.nombre, o.pais, o.lat, o.lon),
                                 f"Inicio de carga de {len(env.contenedores)} contenedor(es)",
                                 {"contenedores": env.contenedores, "responsable": "Depósito del cliente",
                                  "precintos": [f"PR-{env.codigo}-{i}" for i in range(1, len(env.contenedores) + 1)]},
                                 tramo=tr)
                self._evento(env, "SALIDA", tr.salida, _ubic(o.nombre, o.pais, o.lat, o.lon),
                             f"Salida tramo {tr.orden} ({tr.medio.lower()}) con {op_nombre}",
                             {"medio_transporte": tr.medio, "operador": op_nombre,
                              "vehiculo": {"TERRESTRE": f"AE{int(tr.id[:4], 16) % 900 + 100}XY",
                                           "MARITIMO": "MV Atlantic Star", "AEREO": "LV-CGO"}[tr.medio]},
                             tramo=tr)
                medio_t = tr.salida + (tr.llegada_estimada - tr.salida) / 2
                lat, lon = self.cat.posicion(cont0, medio_t)
                self._evento(env, "EN_TRANSITO", medio_t, _ubic(f"En ruta {o.nombre} - {d.nombre}", o.pais, lat, lon),
                             "Reporte de posición del operador",
                             {"avance_pct": 50, "eta": tr.llegada_estimada.isoformat()}, tramo=tr, cont=cont0)
                if o.pais != d.pais:
                    self._evento(env, "ADUANA", tr.llegada_estimada + H, _ubic(d.nombre, d.pais, d.lat, d.lon),
                                 f"Control aduanero en {d.nombre}",
                                 {"canal": "ROJO" if env.codigo in ("E004", "E009") else "VERDE",
                                  "documentos": ["Factura comercial", "Packing list", "Certificado de origen"] +
                                                (["Certificado sanitario"] if env.codigo_cliente in ("C01", "C02", "C08") else []),
                                  "despachante": self.cat.proveedores["P03"][1]},
                                 tramo=tr)
                if tr.orden < ultimo:
                    self._evento(env, "TRASBORDO", tr.llegada_estimada + 2 * H, _ubic(d.nombre, d.pais, d.lat, d.lon),
                                 f"Trasbordo a tramo {tr.orden + 1}", {"espera_horas": 8}, tramo=tr)
                else:
                    self._evento(env, "DESCARGA", tr.llegada_estimada + 2 * H, _ubic(d.nombre, d.pais, d.lat, d.lon),
                                 "Descarga en destino", {"muelle_o_anden": "A-3"}, tramo=tr)
                    self._evento(env, "ENTREGADO", tr.llegada_estimada + 4 * H, _ubic(d.nombre, d.pais, d.lat, d.lon),
                                 "Entrega conforme al destinatario",
                                 {"receptor": "Recepción destinatario", "firma_digital": True, "conforme": True},
                                 tramo=tr)

    def _incidente(self, env: Envio, tramo: Tramo | None, tipo: str, severidad: str, fecha: datetime,
                   descripcion: str, origen: str, id_origen: str, ubic: dict | None, cont: str | None = None) -> str:
        id_inc = id_derivado("incidente", origen, id_origen)
        self.incidentes.append({"id_incidente": id_inc, "envio": env.codigo, "id_envio": env.id,
                                "id_tramo": tramo.id if tramo else "", "tipo": tipo, "fecha": fecha.isoformat(),
                                "severidad": severidad, "estado": "ABIERTO", "descripcion": descripcion,
                                "origen": origen, "id_origen": id_origen})
        self._evento(env, "INCIDENTE_DETECTADO", fecha + 5 * S, ubic, descripcion,
                     {"tipo_incidente": tipo, "severidad": severidad, "origen": origen, "id_origen": id_origen},
                     origen="SISTEMA", tramo=tramo, cont=cont, id_incidente=id_inc)
        return id_inc

    def _eventos_especiales(self) -> None:
        # E002: retraso por congestión portuaria -> incidente
        env = self.cat.envios["E002"]
        tr = env.tramos[1]
        f = t("2026-10-21T08:00:00")
        lat, lon = self.cat.posicion(env.contenedores[0], f)
        ub = _ubic("Atlántico Sur", "AR", lat, lon)
        ev = self._evento(env, "RETRASO", f, ub, "Demora informada por la naviera",
                          {"motivo": "Congestión en Puerto de Santos", "demora_estimada_horas": 18,
                           "nueva_eta": (tr.llegada_estimada + 18 * H).isoformat()}, tramo=tr)
        ev["id_incidente"] = self._incidente(env, tr, "DEMORA", "ALTA", f, "Demora de 18 h en tramo marítimo",
                                             "EVENTO", ev["_id"], ub)
        # E005: desvío por corte de ruta -> incidente
        env = self.cat.envios["E005"]
        tr = env.tramos[0]
        f = t("2026-10-20T03:00:00")
        lat, lon = self.cat.posicion(env.contenedores[0], f)
        ub = _ubic("Ruta Nacional 7", "AR", lat, lon)
        ev = self._evento(env, "DESVIO", f, ub, "Desvío de ruta",
                          {"ruta_original": "RN7", "ruta_alternativa": "RN8", "km_adicionales": 64,
                           "motivo": "Corte de ruta"}, tramo=tr)
        ev["id_incidente"] = self._incidente(env, tr, "DESVIO", "MEDIA", f, "Desvío por corte en RN7",
                                             "EVENTO", ev["_id"], ub)

    # ================================================================== alertas
    def _tramo_en(self, env: Envio, f: datetime) -> Tramo:
        for tr in env.tramos:
            if f <= tr.llegada_estimada:
                return tr
        return env.tramos[-1]

    def _alerta(self, cod_regla: str, cod_sensor: str, fecha: datetime, valor: float | None, ocurrencias: int,
                ultima: datetime, mensaje: str, historial: list[tuple[str, datetime, str | None, str | None]],
                fecha_lectura: datetime | None = None, detalle: dict | None = None,
                id_lectura: str | None = None) -> dict:
        regla = self.cat.reglas[cod_regla]
        sensor = self.cat.sensores[cod_sensor]
        cont = self.cat.contenedores[sensor.codigo_contenedor]
        env = self.cat.envios[cont.codigo_envio]
        estado = historial[-1][0]
        al = {
            "_id": id_derivado("alerta", regla.codigo, cod_sensor, fecha.isoformat()),
            "id_regla": regla.id,
            "tipo_regla": regla.tipo_regla,
            "id_sensor": sensor.id,
            "id_contenedor": cont.id,
            "id_envio": env.id,
            "id_cliente": env.id_cliente,
            "id_lectura": id_lectura,
            "id_evento": None,
            "id_incidente": None,
            "operador": regla.operador,
            "umbral": regla.umbral,
            "valor_observado": valor,
            "severidad": regla.severidad,
            "estado": estado,
            "activa": estado != "CERRADA",
            "mensaje": mensaje,
            "detalle": detalle,
            "fecha": fecha,
            "fecha_lectura": fecha_lectura,
            "ultima_ocurrencia": ultima,
            "ocurrencias": ocurrencias,
            "clave_dedup": f"{regla.id}|{sensor.id}",
            "historial_estados": [{"estado": e, "fecha": f, "id_usuario": u, "comentario": c}
                                  for e, f, u, c in historial],
        }
        tramo = self._tramo_en(env, fecha)
        lat, lon = self.cat.posicion(cont.codigo, fecha)
        ub = _ubic(f"Contenedor {cont.codigo}", tramo.origen.pais, lat, lon)
        ev = self._evento(env, "ALERTA_GENERADA", fecha, ub, mensaje,
                          {"tipo_regla": regla.tipo_regla, "severidad": regla.severidad, "sensor": cod_sensor,
                           "valor_observado": valor}, origen="SISTEMA", tramo=tramo, cont=cont.codigo,
                          id_alerta=al["_id"])
        al["id_evento"] = ev["_id"]
        if regla.severidad in ("ALTA", "CRITICA"):
            al["id_incidente"] = self._incidente(env, tramo, f"ALERTA_{regla.tipo_regla}", regla.severidad, fecha,
                                                 mensaje, "ALERTA", al["_id"], ub, cont=cont.codigo)
        self.alertas.append(al)
        return al

    def _alertas(self) -> None:
        usr = self.cat.usuarios
        L = self.lecturas
        # 1) Temperatura > 8 °C en REEFER (SEN-003): 4 lecturas -> 1 alerta con 4 ocurrencias (D04)
        exc = [l for l in L if l.cod_sensor == SENSOR_EXCURSION and l.temperatura > 8]
        f0 = exc[0].fecha_hora + 5 * S
        self._alerta("REG-01", SENSOR_EXCURSION, f0, exc[0].temperatura, len(exc), exc[-1].fecha_hora,
                     f"Temperatura {exc[0].temperatura} °C supera 8 °C en contenedor REEFER",
                     [("ABIERTA", f0, None, None),
                      ("RECONOCIDA", f0 + 20 * M, usr["U-OPS"]["id"], "Se avisó a FrioTech (P02) para revisar el equipo de frío")],
                     fecha_lectura=exc[0].fecha_hora, id_lectura=exc[0].id_lectura,
                     detalle={"valor_maximo": max(l.temperatura for l in exc),
                              "lecturas": [{"id_lectura": l.id_lectura, "fecha_hora": l.fecha_hora, "temperatura": l.temperatura} for l in exc]})
        # 2) Batería < 15 % (SEN-012)
        baj = [l for l in L if l.cod_sensor == SENSOR_BATERIA and l.bateria < 15]
        f0 = baj[0].fecha_hora + 5 * S
        self._alerta("REG-03", SENSOR_BATERIA, f0, baj[0].bateria, len(baj), baj[-1].fecha_hora,
                     f"Batería del sensor en {baj[0].bateria} %",
                     [("ABIERTA", f0, None, None)], fecha_lectura=baj[0].fecha_hora, id_lectura=baj[0].id_lectura,
                     detalle={"bateria_actual": baj[-1].bateria})
        # 3) Inactividad (SEN-007): proceso periódico cada hora a los :30
        f0 = ULTIMA_DE_INACTIVO + 150 * M
        chequeos = int((t("2026-10-22T23:30:00") - f0) / H) + 1
        self._alerta("REG-04", SENSOR_INACTIVO, f0, 150.0, chequeos, t("2026-10-22T23:30:00"),
                     "Sensor sin lecturas hace 150 minutos",
                     [("ABIERTA", f0, None, None)], fecha_lectura=ULTIMA_DE_INACTIVO,
                     detalle={"ultima_lectura": ULTIMA_DE_INACTIVO, "minutos_sin_datos_al_cierre":
                              int((t("2026-10-22T23:30:00") - ULTIMA_DE_INACTIVO) / M)})
        # 4) Inactividad (SEN-040): nunca reportó desde su asignación
        f0 = ASIGNACION_SEN040 + 150 * M
        chequeos = int((t("2026-10-22T23:30:00") - f0) / H) + 1
        self._alerta("REG-04", SENSOR_NUNCA, f0, 150.0, chequeos, t("2026-10-22T23:30:00"),
                     "Sensor sin lecturas desde su instalación",
                     [("ABIERTA", f0, None, None)],
                     detalle={"ultima_lectura": None, "asignado_desde": ASIGNACION_SEN040})
        # 5) Vibración: primer golpe cerrado, segundo golpe reabre (índice único parcial)
        g1, g2 = sorted(GOLPES)
        l1 = next(l for l in L if l.cod_sensor == SENSOR_VIBRACION and l.fecha_hora == g1)
        f1 = g1 + 5 * S
        self._alerta("REG-05", SENSOR_VIBRACION, f1, GOLPES[g1], 1, g1, f"Vibración {GOLPES[g1]} g supera 2.5 g",
                     [("ABIERTA", f1, None, None),
                      ("RECONOCIDA", f1 + 40 * M, usr["U-O02"]["id"], "Golpe durante maniobra de carga"),
                      ("CERRADA", f1 + 2 * H, usr["U-OPS"]["id"], "Inspección sin daños en la mercadería")],
                     fecha_lectura=g1, id_lectura=l1.id_lectura)
        l2 = next(l for l in L if l.cod_sensor == SENSOR_VIBRACION and l.fecha_hora == g2)
        f2 = g2 + 5 * S
        self._alerta("REG-05", SENSOR_VIBRACION, f2, GOLPES[g2], 1, g2, f"Vibración {GOLPES[g2]} g supera 2.5 g",
                     [("ABIERTA", f2, None, None)], fecha_lectura=g2, id_lectura=l2.id_lectura)
        # 6) Anomalías de temperatura (R32): proceso nocturno |z| > 3 sobre el histórico
        corrida = t("2026-10-22T23:15:00")
        for cod, fuera in anomalias(L).items():
            peor = max(fuera, key=lambda x: abs(x[1]))
            self._alerta("REG-07", cod, corrida, round(abs(peor[1]), 2), len(fuera), corrida,
                         f"{len(fuera)} lectura(s) de temperatura anómalas (|z| máx {abs(peor[1]):.2f})",
                         [("ABIERTA", corrida, None, None)], fecha_lectura=peor[0].fecha_hora,
                         id_lectura=peor[0].id_lectura,
                         detalle={"metodo": "z-score sobre la ventana 2026-10-20/22", "limite_z": 3,
                                  "lecturas": [{"id_lectura": l.id_lectura, "fecha_hora": l.fecha_hora,
                                                "temperatura": l.temperatura, "z": round(z, 2)} for l, z in fuera]})

    # ================================================================== mensajería
    def _p(self, tipo: str, cod: str) -> dict:
        if tipo == "CLIENTE":
            uid, nombre, _ = self.cat.clientes[cod]
        elif tipo == "PROVEEDOR":
            uid, nombre = self.cat.proveedores[cod]
        elif tipo == "OPERADOR":
            uid, nombre = self.cat.operadores[cod]
        else:
            uid, nombre = self.cat.empresa
        usuario = next((u["id"] for u in self.cat.usuarios.values()
                        if u["tipo_entidad"] == tipo and u["id_entidad"] == uid), None)
        return {"tipo": tipo, "id_entidad": uid, "id_usuario": usuario, "nombre": nombre}

    def _conversacion(self, codigo: str, tipo: str, asunto: str, participantes: list[dict], creada_por: dict,
                      fecha: datetime, mensajes: list[tuple[int, str, timedelta]], nombre_grupo: str | None = None,
                      envio: str | None = None) -> None:
        cid = id_estable("conversacion", codigo)
        claves = [f"{p['tipo']}:{p['id_entidad']}" for p in participantes]
        conv = {"_id": cid, "tipo": tipo, "asunto": asunto, "nombre_grupo": nombre_grupo,
                "id_envio": self.cat.envios[envio].id if envio else None,
                "participantes": participantes, "ids_participantes": claves,
                "creada_por": creada_por["id_usuario"], "fecha_creacion": fecha, "estado": "ABIERTA",
                "cantidad_mensajes": len(mensajes), "ultimo_mensaje": None}
        for n, (idx, texto, delta) in enumerate(mensajes, 1):
            rem = participantes[idx]
            f = fecha + delta
            otros = [c for c in claves if c != claves[idx]]
            # Los mensajes anteriores al último ya fueron leídos por todos; el último sigue sin leer.
            leido = otros if n < len(mensajes) else []
            msg = {"_id": id_derivado("mensaje", codigo, str(n)), "id_conversacion": cid,
                   "remitente": rem,
                   "destinatario": ({k: participantes[1 - idx][k] for k in ("tipo", "id_entidad")}
                                    if tipo == "PRIVADA" else None),
                   "fecha_envio": f, "contenido": texto, "tipo": "TEXTO",
                   "estado": "LEIDO" if leido else "ENVIADO", "leido_por": leido, "adjunto": None}
            if "certificado" in texto.lower():
                msg["tipo"] = "ARCHIVO"
                msg["adjunto"] = {"nombre": "certificado_sanitario_E002.pdf", "tamano_kb": 182,
                                  "url": "s3://grupo4-docs/E002/certificado_sanitario.pdf"}
            self.mensajes.append(msg)
            conv["ultimo_mensaje"] = {"fecha": f, "remitente": f"{rem['tipo']}:{rem['id_entidad']}",
                                      "extracto": texto[:60]}
        self.conversaciones.append(conv)

    def _mensajeria(self) -> None:
        c02, o01, p02, emp = self._p("CLIENTE", "C02"), self._p("OPERADOR", "O01"), self._p("PROVEEDOR", "P02"), self._p("INTERNO", "EMP")
        c01, p03, o02, p05 = self._p("CLIENTE", "C01"), self._p("PROVEEDOR", "P03"), self._p("OPERADOR", "O02"), self._p("PROVEEDOR", "P05")
        self._conversacion("CONV-001", "PRIVADA", "Demora del envío E002", [c02, o01], c02, t("2026-10-21T08:30:00"), [
            (0, "Buenos días, nos llegó el aviso de demora del E002. ¿Cuál es la nueva fecha estimada?", 0 * M),
            (1, "Hola Sofía, por congestión en Santos estimamos 18 horas más. Nueva ETA: 27/10 06:00.", 25 * M),
            (0, "¿La cadena de frío está garantizada durante la espera?", 40 * M),
            (1, "Sí, el contenedor sigue conectado a la toma reefer del buque.", 70 * M),
            (0, "Perfecto, quedamos atentos a cualquier novedad.", 90 * M),
            (1, "Les avisamos apenas tengamos turno de atraque.", 14 * H),
        ], envio="E002")
        self._conversacion("CONV-002", "GRUPAL", "Excursión de temperatura contenedor MSKU0000003",
                           [emp, c02, o01, p02], emp, t("2026-10-21T14:25:00"), [
            (0, "Se detectó temperatura de 10.8 °C en MSKU0000003 (alerta REG-01). Abrimos este grupo de trabajo.", 0 * M),
            (3, "FrioTech: revisamos la telemetría, parece una falla del compresor. Coordinamos con la naviera.", 30 * M),
            (2, "Naviera: personal a bordo reinició el equipo de frío, informamos en 1 hora.", 55 * M),
            (1, "Necesitamos saber si la mercadería sigue apta. Adjuntamos certificado sanitario de origen.", 80 * M),
            (3, "Temperatura nuevamente en 4.6 °C desde las 18:00. Recomendamos inspección en destino.", 4 * H),
            (0, "Registramos el incidente asociado y dejamos la alerta como reconocida.", 4 * H + 15 * M),
        ], nombre_grupo="GT Cadena de frío E002", envio="E002")
        self._conversacion("CONV-003", "PRIVADA", "Documentación aduanera E001", [c01, p03], c01, t("2026-10-20T10:00:00"), [
            (0, "Ana, ¿ya está lista la documentación para el despacho del E001 en Santos?", 0 * M),
            (1, "Sí, factura, packing list y certificado sanitario cargados. Falta el certificado de origen.", 45 * M),
            (0, "Lo enviamos hoy antes de las 18.", 60 * M),
            (1, "Recibido, queda todo presentado.", 8 * H),
        ], envio="E001")
        self._conversacion("CONV-004", "GRUPAL", "Mantenimiento de sensores IoT", [emp, o02, p05], emp, t("2026-10-22T09:00:00"), [
            (0, "El sensor SEN-012 tiene batería baja y SEN-007 dejó de reportar. ¿Pueden coordinar el reemplazo?", 0 * M),
            (2, "IoT Sensores: tenemos repuestos, necesitamos acceso al contenedor en destino.", 50 * M),
            (1, "Transportes Ruta 9: podemos coordinar la visita mañana a las 10.", 2 * H),
        ], nombre_grupo="GT Mantenimiento IoT")

    # ================================================================== reportes
    def _reportes(self) -> None:
        c02 = self.cat.clientes["C02"][0]
        alertas_c02 = [a for a in self.alertas if a["id_cliente"] == c02]
        por_estado: dict[str, int] = {}
        for a in alertas_c02:
            por_estado[a["estado"]] = por_estado.get(a["estado"], 0) + 1
        self.reportes.append({
            "_id": id_estable("reporte", "RPT-001"), "id_solicitud": id_estable("solicitud_reporte", "SOL-001"),
            "id_cliente": c02, "solicitado_por": self.cat.usuarios["U-C02"]["id"],
            "tipo": "ALERTAS_POR_ESTADO", "parametros": {"id_cliente": c02, "desde": "2026-10-20", "hasta": "2026-10-22"},
            "formato": "JSON", "estado": "GENERADO", "resultado": {"por_estado": por_estado, "total": len(alertas_c02)},
            "ubicacion": None, "generado_en": t("2026-10-22T23:40:00")})
        cont = self.cat.contenedores["MSKU0000003"]
        sensores = []
        for cod in cont.sensores:
            vals = [l.temperatura for l in self.lecturas
                    if l.cod_sensor == cod and l.dia == "2026-10-21" and l.temperatura is not None]
            if vals:
                sensores.append({"sensor": cod, "lecturas": len(vals), "temp_min": min(vals), "temp_max": max(vals),
                                 "temp_promedio": round(sum(vals) / len(vals), 2)})
        self.reportes.append({
            "_id": id_estable("reporte", "RPT-002"), "id_solicitud": id_estable("solicitud_reporte", "SOL-002"),
            "id_cliente": c02, "solicitado_por": self.cat.usuarios["U-C02"]["id"],
            "tipo": "TELEMETRIA_CONTENEDOR_DIA", "parametros": {"id_contenedor": cont.id, "dia": "2026-10-21"},
            "formato": "JSON", "estado": "GENERADO", "resultado": {"sensores": sensores},
            "ubicacion": None, "generado_en": t("2026-10-22T23:41:00")})
        e002 = self.cat.envios["E002"]
        hist = sorted((e for e in self.eventos if e["id_envio"] == e002.id), key=lambda e: e["fecha"])
        self.reportes.append({
            "_id": id_estable("reporte", "RPT-003"), "id_solicitud": id_estable("solicitud_reporte", "SOL-003"),
            "id_cliente": c02, "solicitado_por": self.cat.usuarios["U-OPS"]["id"],
            "tipo": "HISTORIAL_EVENTOS_ENVIO", "parametros": {"id_envio": e002.id},
            "formato": "CSV", "estado": "GENERADO", "resultado": None,
            "ubicacion": "s3://grupo4-reportes/SOL-003/historial_E002.csv", "generado_en": t("2026-10-22T23:42:00")})
