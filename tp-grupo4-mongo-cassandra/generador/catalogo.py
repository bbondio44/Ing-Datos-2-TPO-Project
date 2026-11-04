"""Catálogo maestro del dataset de prueba (entidades que viven en PostgreSQL).

Cassandra y MongoDB solo REFERENCIAN estas entidades por UUID. Los UUID salen de
uuid5(codigo), así Persona 1 (PostgreSQL) y Persona 3 (Neo4j) pueden cargar exactamente
los mismos identificadores desde catalogo_ids.csv (contrato común, Sprint 0).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone

from ids import id_estable

UTC = timezone.utc


def t(texto: str) -> datetime:
    return datetime.fromisoformat(texto).replace(tzinfo=UTC)


# Ventana de telemetría y "ahora" de referencia de la demo (todo en UTC, decisión D01)
INICIO_TELEMETRIA = t("2026-10-20T00:00:00")
FIN_TELEMETRIA = t("2026-10-22T23:00:00")
PASO = timedelta(hours=1)
AHORA_REFERENCIA = t("2026-10-22T23:30:00")


@dataclass
class Lugar:
    nombre: str
    pais: str
    lat: float
    lon: float


LUGARES = {
    "BUE": Lugar("Buenos Aires", "AR", -34.6037, -58.3816),
    "PBA": Lugar("Puerto de Buenos Aires", "AR", -34.5875, -58.3700),
    "ROS": Lugar("Rosario", "AR", -32.9442, -60.6505),
    "COR": Lugar("Córdoba", "AR", -31.4201, -64.1888),
    "MZA": Lugar("Mendoza", "AR", -32.8895, -68.8458),
    "EZE": Lugar("Aeropuerto de Ezeiza", "AR", -34.8222, -58.5358),
    "SSZ": Lugar("Puerto de Santos", "BR", -23.9608, -46.3336),
    "MVD": Lugar("Montevideo", "UY", -34.9011, -56.1645),
    "SCL": Lugar("Santiago", "CL", -33.4489, -70.6693),
    "VAP": Lugar("Valparaíso", "CL", -33.0472, -71.6127),
    "ASU": Lugar("Asunción", "PY", -25.2637, -57.5759),
    "CLL": Lugar("Callao", "PE", -12.0464, -77.0428),
    "MIA": Lugar("Miami", "US", 25.7617, -80.1918),
}

CLIENTES = [
    ("C01", "Frigorífico del Sur S.A.", "AR"),
    ("C02", "Lácteos Pampeanos S.A.", "AR"),
    ("C03", "Vinos de Cuyo S.R.L.", "AR"),
    ("C04", "AgroExport Litoral S.A.", "AR"),
    ("C05", "Farmacéutica Andina S.A.", "CL"),
    ("C06", "Textil Oriental S.A.", "UY"),
    ("C07", "Autopartes Mercosur S.A.", "BR"),
    ("C08", "Frutas del Valle S.A.", "AR"),
]
PROVEEDORES = [
    ("P01", "Seguros Carga Segura"), ("P02", "FrioTech Mantenimiento Reefer"),
    ("P03", "Despachante Aduanero Gómez"), ("P04", "Embalajes Industriales SRL"),
    ("P05", "IoT Sensores Patagonia"), ("P06", "Depósito Fiscal Dock Sud"),
    ("P07", "Combustibles del Plata"), ("P08", "Consultora Logística Norte"),
]
OPERADORES = [
    ("O01", "Naviera Atlántica S.A."), ("O02", "Transportes Ruta 9 S.A."),
    ("O03", "AeroCargo Sudamericana"), ("O04", "Logística Cordillera S.A."),
    ("O05", "Fletes del Litoral SRL"),
]
EMPRESA = ("EMP", "Grupo 4 Logística (plataforma)")

# (codigo envío, cliente, origen, puerto/escala, destino, medio tramo 2, hora salida T1 relativa al 2026-10-19 00:00, prioridad)
ENVIOS_DEF = [
    ("E001", "C01", "BUE", "PBA", "SSZ", "MARITIMO", 6, "ALTA"),
    ("E002", "C02", "COR", "PBA", "SSZ", "MARITIMO", 10, "ALTA"),
    ("E003", "C03", "MZA", "SCL", "VAP", "TERRESTRE", 4, "MEDIA"),
    ("E004", "C04", "ROS", "PBA", "MVD", "MARITIMO", 20, "MEDIA"),
    ("E005", "C05", "BUE", "MZA", "SCL", "TERRESTRE", 2, "ALTA"),
    ("E006", "C06", "BUE", "PBA", "MVD", "MARITIMO", 30, "BAJA"),
    ("E007", "C07", "COR", "ROS", "ASU", "TERRESTRE", 8, "MEDIA"),
    ("E008", "C08", "MZA", "BUE", "EZE", "TERRESTRE", 12, "ALTA"),
    ("E009", "C01", "BUE", "EZE", "MIA", "AEREO", 40, "ALTA"),
    ("E010", "C02", "ROS", "PBA", "SSZ", "MARITIMO", 16, "MEDIA"),
    ("E011", "C03", "MZA", "SCL", "VAP", "TERRESTRE", 26, "BAJA"),
    ("E012", "C04", "ROS", "PBA", "SSZ", "MARITIMO", 1, "MEDIA"),
    ("E013", "C05", "COR", "EZE", "CLL", "AEREO", 44, "ALTA"),
    ("E014", "C06", "BUE", "PBA", "MVD", "MARITIMO", 50, "BAJA"),
    ("E015", "C07", "ROS", "COR", "MZA", "TERRESTRE", 14, "MEDIA"),
    ("E016", "C08", "MZA", "SCL", "VAP", "TERRESTRE", 3, "MEDIA"),
    ("E017", "C01", "BUE", "PBA", "SSZ", "MARITIMO", 22, "ALTA"),
    ("E018", "C02", "COR", "ROS", "ASU", "TERRESTRE", 18, "BAJA"),
    ("E019", "C03", "MZA", "BUE", "PBA", "TERRESTRE", 9, "MEDIA"),
    ("E020", "C04", "ROS", "EZE", "MIA", "AEREO", 36, "MEDIA"),
]

VELOCIDAD_KMH = {"TERRESTRE": 45.0, "MARITIMO": 16.0, "AEREO": 650.0}  # promedio con paradas
OPERADOR_POR_MEDIO = {"TERRESTRE": ["O02", "O04", "O05"], "MARITIMO": ["O01"], "AEREO": ["O03"]}


@dataclass
class Tramo:
    id: str
    codigo: str
    orden: int
    medio: str
    origen: Lugar
    destino: Lugar
    salida: datetime
    llegada_estimada: datetime
    id_operador: str
    codigo_operador: str


@dataclass
class Envio:
    id: str
    codigo: str
    id_cliente: str
    codigo_cliente: str
    origen: Lugar
    destino: Lugar
    prioridad: str
    tramos: list[Tramo] = field(default_factory=list)
    contenedores: list[str] = field(default_factory=list)   # códigos

    @property
    def llegada_final(self) -> datetime:
        return self.tramos[-1].llegada_estimada


@dataclass
class Contenedor:
    id: str
    codigo: str
    tipo: str
    capacidad_m3: float
    codigo_envio: str
    sensores: list[str] = field(default_factory=list)


@dataclass
class Sensor:
    id: str
    codigo: str
    tipo: str
    fabricante: str
    codigo_contenedor: str


@dataclass
class Regla:
    id: str
    codigo: str
    tipo_regla: str
    operador: str
    umbral: float
    severidad: str
    descripcion: str


def _distancia_km(a: Lugar, b: Lugar) -> float:
    from math import asin, cos, radians, sin, sqrt

    dlat, dlon = radians(b.lat - a.lat), radians(b.lon - a.lon)
    h = sin(dlat / 2) ** 2 + cos(radians(a.lat)) * cos(radians(b.lat)) * sin(dlon / 2) ** 2
    return 2 * 6371 * asin(sqrt(h))


class Catalogo:
    def __init__(self) -> None:
        self.clientes = {c: (id_estable("cliente", c), n, p) for c, n, p in CLIENTES}
        self.proveedores = {c: (id_estable("proveedor", c), n) for c, n in PROVEEDORES}
        self.operadores = {c: (id_estable("operador", c), n) for c, n in OPERADORES}
        self.empresa = (id_estable("empresa", EMPRESA[0]), EMPRESA[1])
        self.envios: dict[str, Envio] = {}
        self.contenedores: dict[str, Contenedor] = {}
        self.sensores: dict[str, Sensor] = {}
        self._armar_envios()
        self._armar_contenedores_y_sensores()
        self.reglas = self._armar_reglas()
        self.usuarios = self._armar_usuarios()

    # ------------------------------------------------------------------
    def _armar_envios(self) -> None:
        base = t("2026-10-19T12:00:00")
        for i, (cod, cli, o, escala, d, medio2, h, prioridad) in enumerate(ENVIOS_DEF):
            origen, intermedio, destino = LUGARES[o], LUGARES[escala], LUGARES[d]
            env = Envio(id_estable("envio", cod), cod, self.clientes[cli][0], cli, origen, destino, prioridad)
            salida = base + timedelta(hours=h)
            for orden, (medio, a, b) in enumerate([("TERRESTRE", origen, intermedio), (medio2, intermedio, destino)], 1):
                horas = max(2.0, _distancia_km(a, b) * 1.25 / VELOCIDAD_KMH[medio])
                if medio == "AEREO":
                    horas += 3  # carga/descarga en aeropuerto
                llegada = salida + timedelta(hours=round(horas))
                ops = OPERADOR_POR_MEDIO[medio]
                cod_op = ops[i % len(ops)]
                env.tramos.append(Tramo(
                    id_estable("tramo", f"{cod}-{orden}"), f"{cod}-T{orden}", orden, medio, a, b,
                    salida, llegada, self.operadores[cod_op][0], cod_op,
                ))
                salida = llegada + timedelta(hours=10 if medio2 != "AEREO" else 5)  # espera en puerto/escala
            self.envios[cod] = env

    def _armar_contenedores_y_sensores(self) -> None:
        envios = list(self.envios.values())
        n = 0
        for idx, env in enumerate(envios):
            cantidad = 2 if idx < 5 else 1          # E001..E005 llevan 2 contenedores
            for _ in range(cantidad):
                n += 1
                codigo = f"MSKU{n:07d}"
                tipo = "REEFER" if n <= 8 else ("DRY" if n % 3 else "OPEN_TOP")
                cont = Contenedor(id_estable("contenedor", codigo), codigo, tipo,
                                  67.7 if tipo == "REEFER" else 33.2, env.codigo)
                env.contenedores.append(codigo)
                self.contenedores[codigo] = cont
        conts = list(self.contenedores.values())
        # SEN-001..025: multiparámetro (con GPS), uno por contenedor
        for i, cont in enumerate(conts, 1):
            self._sensor(f"SEN-{i:03d}", "MULTIPARAMETRO", "Teltonika", cont)
        # SEN-026..039: temperatura/humedad (sin GPS) en contenedores 1..14
        for i, cont in enumerate(conts[:14], 26):
            self._sensor(f"SEN-{i:03d}", "TEMPERATURA_HUMEDAD", "Sensitech", cont)
        # SEN-040: instalado en el contenedor 25 pero nunca reportó (caso límite)
        self._sensor("SEN-040", "MULTIPARAMETRO", "Teltonika", conts[24])

    def _sensor(self, codigo: str, tipo: str, fabricante: str, cont: Contenedor) -> None:
        s = Sensor(id_estable("sensor", codigo), codigo, tipo, fabricante, cont.codigo)
        cont.sensores.append(codigo)
        self.sensores[codigo] = s

    def _armar_reglas(self) -> dict[str, Regla]:
        defs = [
            ("REG-01", "TEMPERATURA", ">", 8.0, "ALTA", "Cadena de frío: temperatura mayor a 8 °C (contenedores REEFER)"),
            ("REG-02", "TEMPERATURA", "<", 2.0, "MEDIA", "Cadena de frío: temperatura menor a 2 °C (contenedores REEFER)"),
            ("REG-03", "BATERIA", "<", 15.0, "MEDIA", "Batería del sensor por debajo del 15 %"),
            ("REG-04", "INACTIVIDAD", ">", 120.0, "ALTA", "Sensor sin reportar más de 120 minutos"),
            ("REG-05", "VIBRACION", ">", 2.5, "MEDIA", "Vibración mayor a 2.5 g (posible golpe)"),
            ("REG-06", "HUMEDAD", ">", 90.0, "BAJA", "Humedad relativa mayor a 90 %"),
            ("REG-07", "ANOMALIA_TEMPERATURA", ">", 3.0, "MEDIA", "Anomalía estadística: |z| > 3 respecto del histórico del sensor"),
        ]
        return {c: Regla(id_estable("regla_alerta", c), c, *resto) for c, *resto in defs}

    def _armar_usuarios(self) -> dict[str, dict]:
        defs = [
            ("U-ADMIN", "Admin Plataforma", "ADMIN", "INTERNO", self.empresa[0]),
            ("U-OPS", "Lucía Operaciones", "OPERACIONES", "INTERNO", self.empresa[0]),
            ("U-C01", "Martín (Frigorífico del Sur)", "CLIENTE", "CLIENTE", self.clientes["C01"][0]),
            ("U-C02", "Sofía (Lácteos Pampeanos)", "CLIENTE", "CLIENTE", self.clientes["C02"][0]),
            ("U-O01", "Diego (Naviera Atlántica)", "OPERADOR", "OPERADOR", self.operadores["O01"][0]),
            ("U-O02", "Carla (Transportes Ruta 9)", "OPERADOR", "OPERADOR", self.operadores["O02"][0]),
            ("U-P02", "Julián (FrioTech)", "PROVEEDOR", "PROVEEDOR", self.proveedores["P02"][0]),
            ("U-P03", "Ana (Despachante Gómez)", "PROVEEDOR", "PROVEEDOR", self.proveedores["P03"][0]),
            ("U-P05", "Bruno (IoT Sensores)", "PROVEEDOR", "PROVEEDOR", self.proveedores["P05"][0]),
        ]
        return {c: {"id": id_estable("usuario", c), "codigo": c, "nombre": n, "rol": r,
                    "tipo_entidad": te, "id_entidad": ie} for c, n, r, te, ie in defs}

    # ------------------------------------------------------------------ consultas útiles
    def contenedor_de(self, cod_sensor: str) -> Contenedor:
        return self.contenedores[self.sensores[cod_sensor].codigo_contenedor]

    def envio_de_contenedor(self, cod_cont: str) -> Envio:
        return self.envios[self.contenedores[cod_cont].codigo_envio]

    def posicion(self, cod_cont: str, instante: datetime) -> tuple[float, float]:
        """Posición teórica del contenedor recorriendo los tramos de su envío."""
        env = self.envio_de_contenedor(cod_cont)
        if instante <= env.tramos[0].salida:
            return env.origen.lat, env.origen.lon
        for tramo in env.tramos:
            if tramo.salida <= instante <= tramo.llegada_estimada:
                f = (instante - tramo.salida) / (tramo.llegada_estimada - tramo.salida)
                return (tramo.origen.lat + (tramo.destino.lat - tramo.origen.lat) * f,
                        tramo.origen.lon + (tramo.destino.lon - tramo.origen.lon) * f)
            if instante < tramo.salida:
                return tramo.origen.lat, tramo.origen.lon   # esperando en escala
        return env.destino.lat, env.destino.lon

    def estado_envio(self, env: Envio, instante: datetime) -> str:
        if instante >= env.llegada_final:
            return "ENTREGADO"
        if instante < env.tramos[0].salida:
            return "PENDIENTE"
        return "EN_TRANSITO"

    def filas_csv(self) -> list[dict]:
        filas = []
        add = lambda tipo, cod, uid, desc, extra="": filas.append(
            {"entidad": tipo, "codigo": cod, "uuid": uid, "descripcion": desc, "referencias": extra})
        add("empresa", EMPRESA[0], self.empresa[0], self.empresa[1])
        for c, (uid, n, p) in self.clientes.items():
            add("cliente", c, uid, n, f"pais={p}")
        for c, (uid, n) in self.proveedores.items():
            add("proveedor", c, uid, n)
        for c, (uid, n) in self.operadores.items():
            add("operador_logistico", c, uid, n)
        for u in self.usuarios.values():
            add("usuario", u["codigo"], u["id"], u["nombre"], f"rol={u['rol']};{u['tipo_entidad']}={u['id_entidad']}")
        for env in self.envios.values():
            add("envio", env.codigo, env.id, f"{env.origen.nombre} -> {env.destino.nombre}",
                f"cliente={env.codigo_cliente};prioridad={env.prioridad}")
            for tr in env.tramos:
                add("tramo", tr.codigo, tr.id, f"{tr.medio} {tr.origen.nombre} -> {tr.destino.nombre}",
                    f"envio={env.codigo};orden={tr.orden};operador={tr.codigo_operador};"
                    f"salida={tr.salida:%Y-%m-%dT%H:%MZ};llegada_estimada={tr.llegada_estimada:%Y-%m-%dT%H:%MZ}")
        for cont in self.contenedores.values():
            add("contenedor", cont.codigo, cont.id, f"{cont.tipo} {cont.capacidad_m3} m3", f"envio={cont.codigo_envio}")
        for s in self.sensores.values():
            add("sensor", s.codigo, s.id, f"{s.tipo} ({s.fabricante})", f"contenedor={s.codigo_contenedor}")
        for r in self.reglas.values():
            add("regla_alerta", r.codigo, r.id, r.descripcion,
                f"tipo={r.tipo_regla};operador={r.operador};umbral={r.umbral};severidad={r.severidad}")
        return filas
