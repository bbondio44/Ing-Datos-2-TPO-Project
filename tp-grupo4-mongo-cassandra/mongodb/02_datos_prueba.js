// =====================================================================
// MongoDB · DATOS DE PRUEBA (GENERADO por generador/generar.py, no editar a mano)
// Uso: mongosh "mongodb://localhost:27017/logistica_docs" mongodb/02_datos_prueba.js
// Re-ejecutable: cada documento se carga con replaceOne(upsert) por _id ->
// vuelve el dataset a su estado inicial sin duplicar ni borrar otros datos.
// Las referencias (id_envio, id_sensor, id_cliente, ...) son los UUID de catalogo_ids.csv.
// FUERA DE CLASE: replaceOne y bulkWrite. Solo en esta carga: reponen el dataset sin duplicar.
// Alternativa vista: updateOne con {upsert: true}, documento por documento.
// "Ahora" de referencia de la demo: 2026-10-22 23:30 UTC
// =====================================================================

function cargar(nombre, docs) {
  const ops = docs.map(d => ({ replaceOne: { filter: { _id: d._id }, replacement: d, upsert: true } }));
  const r = db.getCollection(nombre).bulkWrite(ops, { ordered: true });
  print(nombre.padEnd(20) + ' nuevos: ' + String(r.upsertedCount).padStart(4) +
        '  reemplazados: ' + String(r.matchedCount).padStart(4) +
        '  total en colección: ' + db.getCollection(nombre).countDocuments());
}

// ---------------------------------------------------------------- eventos_logisticos (173)
cargar('eventos_logisticos', [
  {
    _id: "9b3f3921-f819-5319-a88e-7cd64d3d8e1e",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "700ccfa7-2500-5352-a827-713d61fb4a19",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-21T02:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000014"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E009-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e567bbf8-d03",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:00:02.000Z")
  },
  {
    _id: "11603626-7f24-5ca5-9ff6-fa19dedda60f",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "700ccfa7-2500-5352-a827-713d61fb4a19",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T04:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE884XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2bcde90c-dbe",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T04:00:02.000Z")
  },
  {
    _id: "bc1f7db6-cc68-55d0-b56c-a0bd6e8156bb",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: "544cfffd-f74c-592b-b6f5-12dbfcc2466c",
    id_tramo: "700ccfa7-2500-5352-a827-713d61fb4a19",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T05:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.4587, -34.71295]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T06:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "646acb63-4d9",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T05:00:02.000Z")
  },
  {
    _id: "b6baad72-138f-5b58-a14f-9f034147bc5d",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "700ccfa7-2500-5352-a827-713d61fb4a19",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-21T08:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "0369e681-688",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T08:00:02.000Z")
  },
  {
    _id: "b7ad827a-7511-558d-95df-0403fde5be20",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "6cf16c11-01b1-5b46-aaf1-fa7d56d354f1",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T11:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Salida tramo 2 (aereo) con AeroCargo Sudamericana",
    payload: {
      medio_transporte: "AEREO",
      operador: "AeroCargo Sudamericana",
      vehiculo: "LV-CGO"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5ada1893-c40",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T11:00:02.000Z")
  },
  {
    _id: "d5b8efd4-28dc-599b-b19c-4a2567adba36",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: "544cfffd-f74c-592b-b6f5-12dbfcc2466c",
    id_tramo: "6cf16c11-01b1-5b46-aaf1-fa7d56d354f1",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T19:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Aeropuerto de Ezeiza - Miami",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.3638, -4.53025]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T04:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2476ca97-b36",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T19:30:02.000Z")
  },
  {
    _id: "0359fdaf-afc2-5443-a26a-065cc1ab5994",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "6cf16c11-01b1-5b46-aaf1-fa7d56d354f1",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-22T05:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Control aduanero en Miami",
    payload: {
      canal: "ROJO",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen", "Certificado sanitario"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "1e338ae6-7b0",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T05:00:02.000Z")
  },
  {
    _id: "fb327053-d377-564c-81e6-d97189a9dcc7",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "6cf16c11-01b1-5b46-aaf1-fa7d56d354f1",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T06:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "57d4605e-28c",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T06:00:02.000Z")
  },
  {
    _id: "b94c15c4-7cd4-5a94-9d97-714603fb40bb",
    id_envio: "23314655-0f49-57cf-aa62-6b86d1aa705b",
    id_contenedor: null,
    id_tramo: "6cf16c11-01b1-5b46-aaf1-fa7d56d354f1",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T08:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e79ceeeb-214",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T08:00:02.000Z")
  },
  {
    _id: "e3cbce48-e832-55f8-964c-834db381742a",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "f6642cb1-dc38-50d4-9f55-8720afd16144",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T04:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000023"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E018-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "cd2284c9-ae6",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T04:00:02.000Z")
  },
  {
    _id: "4f066536-9a61-521c-830d-7ce227b59f69",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "f6642cb1-dc38-50d4-9f55-8720afd16144",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T06:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE176XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "532994cf-8da",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T06:00:02.000Z")
  },
  {
    _id: "f08526ca-2193-5161-a478-b25878fcbf95",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: "bba9ae70-be3c-57df-9319-4371224c4a5a",
    id_tramo: "f6642cb1-dc38-50d4-9f55-8720afd16144",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T11:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Córdoba - Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-62.41965, -32.18215]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2cb0af2a-d4e",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T11:00:02.000Z")
  },
  {
    _id: "0b2977da-0ed8-572c-9fef-b58a455c8501",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "f6642cb1-dc38-50d4-9f55-8720afd16144",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T18:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2db047f5-8fc",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T18:00:02.000Z")
  },
  {
    _id: "3c028aec-940b-5b9b-9285-a670955355ce",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "34dd81b3-884f-501d-ab93-bf704be12d2b",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T02:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE133XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "21207abc-c1d",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:00:02.000Z")
  },
  {
    _id: "02f896d5-3e28-5276-8cf8-a655506e7ba6",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: "bba9ae70-be3c-57df-9319-4371224c4a5a",
    id_tramo: "34dd81b3-884f-501d-ab93-bf704be12d2b",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T14:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Asunción",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.1132, -29.10395]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T03:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "6bf171e2-cbc",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:30:02.000Z")
  },
  {
    _id: "a222cd89-3118-5ccb-bff1-d25141e28c34",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "34dd81b3-884f-501d-ab93-bf704be12d2b",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-22T04:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Control aduanero en Asunción",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen", "Certificado sanitario"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "0b0178b0-0b3",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T04:00:02.000Z")
  },
  {
    _id: "2a557223-684e-5468-9180-8f037c407d1b",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "34dd81b3-884f-501d-ab93-bf704be12d2b",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T05:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "28184e03-b18",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T05:00:02.000Z")
  },
  {
    _id: "a475ad7e-4ffe-5412-97ef-4c92c6bc1269",
    id_envio: "4770f530-1292-5b45-8bc1-bb7e26559b28",
    id_contenedor: null,
    id_tramo: "34dd81b3-884f-501d-ab93-bf704be12d2b",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T07:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "8f241fcc-a80",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T07:00:02.000Z")
  },
  {
    _id: "b23ee87d-9531-5bfd-a8d1-e16dd284941a",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "521dd8bd-0f98-5e42-b8ff-e4b1723f986c",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T14:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Inicio de carga de 2 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000005", "MSKU0000006"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E003-1", "PR-E003-2"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "72a044d1-8d5",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T14:00:02.000Z")
  },
  {
    _id: "8cfa070a-055c-5738-98eb-7e57c6c327ea",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "521dd8bd-0f98-5e42-b8ff-e4b1723f986c",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T16:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE421XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "d86d9869-833",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T16:00:02.000Z")
  },
  {
    _id: "bf1bf6b3-8f63-575b-b5a6-2223753bc0b9",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: "d37ca4b8-0970-5cec-ac9b-490429b7df61",
    id_tramo: "521dd8bd-0f98-5e42-b8ff-e4b1723f986c",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-19T18:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Santiago",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.75755, -33.1692]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-19T21:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "26f3b40d-f45",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T18:30:02.000Z")
  },
  {
    _id: "3d5f91fd-64ec-5de1-b15e-d134f36e45b6",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "521dd8bd-0f98-5e42-b8ff-e4b1723f986c",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-19T22:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Control aduanero en Santiago",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7e18939d-d95",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T22:00:02.000Z")
  },
  {
    _id: "287a1efa-9472-553a-a80f-0ba59b27a6eb",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "521dd8bd-0f98-5e42-b8ff-e4b1723f986c",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-19T23:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4e8690e0-736",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T23:00:02.000Z")
  },
  {
    _id: "460b05f3-a297-57da-a7c5-fc673d6cc81b",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T07:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE960XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "21ee5418-d6d",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T07:00:02.000Z")
  },
  {
    _id: "577e391c-1927-5d15-a5ef-9a0f9cc049a6",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: "d37ca4b8-0970-5cec-ac9b-490429b7df61",
    id_tramo: "2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T08:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Santiago - Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.141, -33.24805]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T10:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "24a7a081-787",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T08:30:02.000Z")
  },
  {
    _id: "d60a8fd5-00f8-59d1-9429-a4fd581e61ca",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-20T12:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c312f61b-67f",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T12:00:02.000Z")
  },
  {
    _id: "7cc4ebb6-e7f3-5739-be3e-6251978f6fbf",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: null,
    id_tramo: "2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-20T14:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e94c83f2-68a",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T14:00:02.000Z")
  },
  {
    _id: "d54ac45f-f166-5b79-af09-f118174c8d60",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_contenedor: "d37ca4b8-0970-5cec-ac9b-490429b7df61",
    id_tramo: "2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-22T23:15:00.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000005",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "1 lectura(s) de temperatura anómalas (|z| máx 6.73)",
    payload: {
      tipo_regla: "ANOMALIA_TEMPERATURA",
      severidad: "MEDIA",
      sensor: "SEN-005",
      valor_observado: 6.73
    },
    origen: "SISTEMA",
    id_alerta: "9bd9597e-e07e-5762-8ae2-1f9824c2799b",
    id_incidente: null,
    id_correlacion: "79d3e9ce-1e1",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T23:15:02.000Z")
  },
  {
    _id: "1773d245-85cf-565d-8fb5-b45a4ba03800",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "769346ac-d406-51ac-8f14-985679cfaeb5",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T18:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000012"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E007-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "d51705a7-cab",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T18:00:02.000Z")
  },
  {
    _id: "e742ac27-0ca6-5617-a272-544a59f92a6b",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "769346ac-d406-51ac-8f14-985679cfaeb5",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T20:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE755XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "436b347f-529",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T20:00:02.000Z")
  },
  {
    _id: "932d05e4-7c0b-567f-8a52-a46e378c2f44",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: "a4f4c383-f83a-5fe5-9a33-902d50f7f487",
    id_tramo: "769346ac-d406-51ac-8f14-985679cfaeb5",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T01:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Córdoba - Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-62.41965, -32.18215]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T06:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "d2e8dedf-db3",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T01:00:02.000Z")
  },
  {
    _id: "6e47a50a-fb54-5cb7-bef3-1da30963e91c",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "769346ac-d406-51ac-8f14-985679cfaeb5",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T08:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "9133dc5a-925",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T08:00:02.000Z")
  },
  {
    _id: "e02bc9ad-54a7-5d29-9ec3-70f4fa2a70e8",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T16:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE799XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e9d597db-fd8",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T16:00:02.000Z")
  },
  {
    _id: "5e05cac9-3680-5943-93f3-0bc442928612",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: "a4f4c383-f83a-5fe5-9a33-902d50f7f487",
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T04:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Asunción",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.1132, -29.10395]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T17:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "9c08dde2-179",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T04:30:02.000Z")
  },
  {
    _id: "3886d3f5-ae6d-5107-a5b5-4c02f6f9f40e",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-21T18:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Control aduanero en Asunción",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "8fb8ebe9-350",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T18:00:02.000Z")
  },
  {
    _id: "85502a6c-e86f-5980-9b37-6676dae24779",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: "a4f4c383-f83a-5fe5-9a33-902d50f7f487",
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-21T18:00:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000012",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Batería del sensor en 14.8 %",
    payload: {
      tipo_regla: "BATERIA",
      severidad: "MEDIA",
      sensor: "SEN-012",
      valor_observado: 14.8
    },
    origen: "SISTEMA",
    id_alerta: "fc397a0b-b3dd-5556-90dc-9d42e49bd079",
    id_incidente: null,
    id_correlacion: "a822e3be-b3c",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T18:00:07.000Z")
  },
  {
    _id: "cf8c2e97-a75d-5117-a8a5-abc7f622bc3a",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T19:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7a5c2b02-a05",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T19:00:02.000Z")
  },
  {
    _id: "2611acab-2106-5425-839b-63e09c6169be",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_contenedor: null,
    id_tramo: "c09375ab-c708-57d4-b799-197a59043538",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T21:00:00.000Z"),
    ubicacion: {
      nombre: "Asunción",
      pais: "PY",
      geo: {
        type: "Point",
        coordinates: [-57.5759, -25.2637]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "15f4cc0d-c8e",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T21:00:02.000Z")
  },
  {
    _id: "7f5de794-a6a8-5584-8830-583ffbf99a31",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "a03ca0b0-0d03-5875-9291-5e1cd1a2ef87",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T06:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Inicio de carga de 2 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000007", "MSKU0000008"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E004-1", "PR-E004-2"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "0026904f-54f",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T06:00:02.000Z")
  },
  {
    _id: "fca26f94-c5d0-50d5-801c-d0da033ccf7f",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "a03ca0b0-0d03-5875-9291-5e1cd1a2ef87",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T08:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE620XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "1da42ebe-255",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T08:00:02.000Z")
  },
  {
    _id: "cc407038-c930-5bb7-86a5-b2eb57fcd785",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: "5a92f38e-c32b-5035-8b97-1d253d2e58b1",
    id_tramo: "a03ca0b0-0d03-5875-9291-5e1cd1a2ef87",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T12:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.51025, -33.76585]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f5ba63c4-a7d",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T12:00:02.000Z")
  },
  {
    _id: "169aa95b-b1cf-532e-88f3-4d075b05dd8e",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "a03ca0b0-0d03-5875-9291-5e1cd1a2ef87",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T18:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c85d2442-11e",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T18:00:02.000Z")
  },
  {
    _id: "b1d2f9e6-569e-56ec-8f38-91565ca679d5",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T02:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3c86acf9-2ee",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:00:02.000Z")
  },
  {
    _id: "33032c60-7046-5179-add7-20abe3080bd1",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: "5a92f38e-c32b-5035-8b97-1d253d2e58b1",
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T10:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Puerto de Buenos Aires - Montevideo",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.26725, -34.7443]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T18:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c848e516-d73",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T10:00:02.000Z")
  },
  {
    _id: "d70fc6d6-4e1c-5e37-8a1c-2b3301e3f908",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: "5a92f38e-c32b-5035-8b97-1d253d2e58b1",
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-21T12:30:00.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000007",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-56.92264, -34.7933]
      }
    },
    descripcion: "Sensor sin lecturas hace 150 minutos",
    payload: {
      tipo_regla: "INACTIVIDAD",
      severidad: "ALTA",
      sensor: "SEN-007",
      valor_observado: 150.0
    },
    origen: "SISTEMA",
    id_alerta: "fd3e05e0-2fd5-5b37-adbe-c74c571ede3c",
    id_incidente: null,
    id_correlacion: "e8f59989-8d9",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T12:30:02.000Z")
  },
  {
    _id: "1a9f7d57-cdae-57f7-be0a-828adf581e76",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: "5a92f38e-c32b-5035-8b97-1d253d2e58b1",
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "INCIDENTE_DETECTADO",
    fecha: ISODate("2026-10-21T12:30:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000007",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-56.92264, -34.7933]
      }
    },
    descripcion: "Sensor sin lecturas hace 150 minutos",
    payload: {
      tipo_incidente: "ALERTA_INACTIVIDAD",
      severidad: "ALTA",
      origen: "ALERTA",
      id_origen: "fd3e05e0-2fd5-5b37-adbe-c74c571ede3c"
    },
    origen: "SISTEMA",
    id_alerta: null,
    id_incidente: "29ad900d-f0a7-5bdf-a550-c423dd619494",
    id_correlacion: "ac386d4c-672",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T12:30:07.000Z")
  },
  {
    _id: "22ea6d03-e807-500b-a7fe-fb476e0e1ef1",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-21T19:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Control aduanero en Montevideo",
    payload: {
      canal: "ROJO",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "473b414d-353",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T19:00:02.000Z")
  },
  {
    _id: "721e3a0c-e4f4-5ffe-8651-856ada5bff5a",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T20:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f1c32a52-8ae",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T20:00:02.000Z")
  },
  {
    _id: "a5297cd5-d464-5c52-a2d7-1030b4e1f0af",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_contenedor: null,
    id_tramo: "bc56de92-e405-5c2c-b6cf-c189645f32f6",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T22:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "38bbc2cf-895",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T22:00:02.000Z")
  },
  {
    _id: "bab66348-a79c-5ccb-89a9-ddc15087c540",
    id_envio: "653dad7d-e412-585e-9a1b-c1c97c370d0c",
    id_contenedor: null,
    id_tramo: "0ef18837-cea0-5ee7-8133-6c57bb9d2e01",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T11:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000017"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E012-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "88f2966d-e7f",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T11:00:02.000Z")
  },
  {
    _id: "0636846d-a406-5707-ba1f-c66cd55c6d4e",
    id_envio: "653dad7d-e412-585e-9a1b-c1c97c370d0c",
    id_contenedor: null,
    id_tramo: "0ef18837-cea0-5ee7-8133-6c57bb9d2e01",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T13:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE325XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ca7054bc-03d",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T13:00:02.000Z")
  },
  {
    _id: "ea6fa796-b6c0-5290-be1b-670712e01c34",
    id_envio: "653dad7d-e412-585e-9a1b-c1c97c370d0c",
    id_contenedor: "4003fbfb-f791-56dd-9be0-e13e9e7b5e8f",
    id_tramo: "0ef18837-cea0-5ee7-8133-6c57bb9d2e01",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-19T17:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.51025, -33.76585]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-19T21:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "770424a8-4b9",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T17:00:02.000Z")
  },
  {
    _id: "46c2d0da-fb22-57af-8855-3717c3cddde0",
    id_envio: "653dad7d-e412-585e-9a1b-c1c97c370d0c",
    id_contenedor: null,
    id_tramo: "0ef18837-cea0-5ee7-8133-6c57bb9d2e01",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-19T23:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e8435945-4ff",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T23:00:02.000Z")
  },
  {
    _id: "da80fa78-cf3d-53e8-a366-9f46a84c1a36",
    id_envio: "653dad7d-e412-585e-9a1b-c1c97c370d0c",
    id_contenedor: null,
    id_tramo: "0fafb0aa-1426-53ca-be25-f376fda66da7",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T07:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4f70983a-38f",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T07:00:02.000Z")
  },
  {
    _id: "2bda66bb-15dd-5ef5-8853-942c629935dc",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "f4a96996-c5a2-5b37-bc2b-308ff9529662",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T12:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000016"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E011-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2d9286f3-a39",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T12:00:02.000Z")
  },
  {
    _id: "a3cd9663-cf4d-5f6e-ba82-aafbbf0661af",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "f4a96996-c5a2-5b37-bc2b-308ff9529662",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T14:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE633XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f7466a6b-28d",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T14:00:02.000Z")
  },
  {
    _id: "c7062930-40a0-5d10-af19-760a0eaf60ea",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: "8c224184-d7bc-5016-92f6-e72773f8b2c0",
    id_tramo: "f4a96996-c5a2-5b37-bc2b-308ff9529662",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T16:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Santiago",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.75755, -33.1692]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T19:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ca199474-883",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T16:30:02.000Z")
  },
  {
    _id: "8b34e60a-6024-5b95-b479-722f8b03994e",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "f4a96996-c5a2-5b37-bc2b-308ff9529662",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-20T20:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Control aduanero en Santiago",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "816e672f-e68",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T20:00:02.000Z")
  },
  {
    _id: "deadfb9d-c80c-5e70-afed-6c16ec94afce",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "f4a96996-c5a2-5b37-bc2b-308ff9529662",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T21:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "dd48268c-db1",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T21:00:02.000Z")
  },
  {
    _id: "f31b953f-ca85-5bc2-95e4-edd3a6854ac8",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "c647901c-2afe-5ef8-9333-4be47788079c",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T05:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE459XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "874dc2cc-4dc",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T05:00:02.000Z")
  },
  {
    _id: "b4613410-44d8-5613-96ff-e81187d4307c",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: "8c224184-d7bc-5016-92f6-e72773f8b2c0",
    id_tramo: "c647901c-2afe-5ef8-9333-4be47788079c",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T06:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Santiago - Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.141, -33.24805]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T08:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b6b375f9-dd5",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T06:30:02.000Z")
  },
  {
    _id: "3409bc8e-2b21-58fd-abb4-07d6a2717963",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "c647901c-2afe-5ef8-9333-4be47788079c",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T10:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "fe88e0aa-92c",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T10:00:02.000Z")
  },
  {
    _id: "1fdf7aa1-c48e-586d-bb38-380c02425b2d",
    id_envio: "6ee84809-f672-5b8d-87f1-7f395cdaa8ed",
    id_contenedor: null,
    id_tramo: "c647901c-2afe-5ef8-9333-4be47788079c",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T12:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3dea6721-a66",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T12:00:02.000Z")
  },
  {
    _id: "a2d97022-6023-5e84-bd43-923b1dc1eca3",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "2c2bd0d3-af66-5963-8723-cbc7ce49b770",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T13:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000021"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E016-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4ee039e8-471",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T13:00:02.000Z")
  },
  {
    _id: "4d0db7fe-6738-55b0-8f9b-c4f52416a6a0",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "2c2bd0d3-af66-5963-8723-cbc7ce49b770",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T15:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE607XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5f01f0cb-ada",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T15:00:02.000Z")
  },
  {
    _id: "32128b56-06b3-5ed0-a06b-0e5c87f9a6ed",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: "3cb5d737-2d39-56a5-999e-a6fb4d327db4",
    id_tramo: "2c2bd0d3-af66-5963-8723-cbc7ce49b770",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-19T17:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Santiago",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.75755, -33.1692]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-19T20:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "545d4af1-389",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T17:30:02.000Z")
  },
  {
    _id: "65849a03-c106-5e3e-b77c-68a0bf6f3fa7",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "2c2bd0d3-af66-5963-8723-cbc7ce49b770",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-19T21:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Control aduanero en Santiago",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen", "Certificado sanitario"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "74e284cc-09e",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T21:00:02.000Z")
  },
  {
    _id: "5469f62c-0747-57da-8f79-37ca4fc8c17e",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "2c2bd0d3-af66-5963-8723-cbc7ce49b770",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-19T22:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3322bf98-230",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T22:00:02.000Z")
  },
  {
    _id: "78042fc9-1708-5501-b049-3590313e7b21",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "413f0621-7bd7-5ebd-99e9-07f373e65358",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T06:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE603XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b1fb75de-060",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T06:00:02.000Z")
  },
  {
    _id: "012b3ab5-9174-5228-83ec-7b6389bc4a8c",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: "3cb5d737-2d39-56a5-999e-a6fb4d327db4",
    id_tramo: "413f0621-7bd7-5ebd-99e9-07f373e65358",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T07:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Santiago - Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.141, -33.24805]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T09:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "70c0cd44-299",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T07:30:02.000Z")
  },
  {
    _id: "e2cb4e7a-00ec-5091-a2ea-f8be09d21937",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "413f0621-7bd7-5ebd-99e9-07f373e65358",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-20T11:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "afe45b25-9bb",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T11:00:02.000Z")
  },
  {
    _id: "64983c2c-e619-5ff6-bac1-3bee0f5e7ada",
    id_envio: "7b77f577-b7f6-5cd0-b620-0fcfc4a97c53",
    id_contenedor: null,
    id_tramo: "413f0621-7bd7-5ebd-99e9-07f373e65358",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-20T13:00:00.000Z"),
    ubicacion: {
      nombre: "Valparaíso",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-71.6127, -33.0472]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "0dcf80ae-9c0",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T13:00:02.000Z")
  },
  {
    _id: "995c71f8-edcd-5073-95ec-b79e081961d2",
    id_envio: "986c09d3-a6be-5353-a94d-ab683bd9434c",
    id_contenedor: null,
    id_tramo: "beb0be6f-8128-5b77-b89f-05d8c270eef8",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T02:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000015"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E010-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "6a3529a2-357",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T02:00:02.000Z")
  },
  {
    _id: "e7f336a4-b693-58c4-9507-28d6f82a4217",
    id_envio: "986c09d3-a6be-5353-a94d-ab683bd9434c",
    id_contenedor: null,
    id_tramo: "beb0be6f-8128-5b77-b89f-05d8c270eef8",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T04:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE316XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "d908ea5c-667",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T04:00:02.000Z")
  },
  {
    _id: "de73b542-73a2-5aab-a6f4-d4b43bad3623",
    id_envio: "986c09d3-a6be-5353-a94d-ab683bd9434c",
    id_contenedor: "2cb2fb44-c11d-517c-b802-27f09bd1433c",
    id_tramo: "beb0be6f-8128-5b77-b89f-05d8c270eef8",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T08:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.51025, -33.76585]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T12:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "2ceb3452-0b8",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T08:00:02.000Z")
  },
  {
    _id: "7cad6f36-a008-5b25-bb2e-b1117ac55449",
    id_envio: "986c09d3-a6be-5353-a94d-ab683bd9434c",
    id_contenedor: null,
    id_tramo: "beb0be6f-8128-5b77-b89f-05d8c270eef8",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T14:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c8edd596-693",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T14:00:02.000Z")
  },
  {
    _id: "4d1ed835-6068-5d0a-beae-ab74dab97a41",
    id_envio: "986c09d3-a6be-5353-a94d-ab683bd9434c",
    id_contenedor: null,
    id_tramo: "93e65e0a-7c8f-50a1-a85b-7128e7c5dc4a",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T22:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "bf5b4afd-bde",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T22:00:02.000Z")
  },
  {
    _id: "6d800050-4a0f-549f-a673-b046e4b76b67",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "0e0e2068-d4ee-5749-9bc2-3688386e625e",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T16:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000011"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E006-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5ebc967f-dd9",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T16:00:02.000Z")
  },
  {
    _id: "cde518d6-ca7b-5f83-8a1d-67bfc4eca416",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "0e0e2068-d4ee-5749-9bc2-3688386e625e",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T18:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE998XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b409aee3-fe7",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T18:00:02.000Z")
  },
  {
    _id: "84f45d24-b7e6-541e-aa72-15032baf5f87",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: "1f1e7b63-f3fc-5911-9659-81d67085a4a0",
    id_tramo: "0e0e2068-d4ee-5749-9bc2-3688386e625e",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T19:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3758, -34.5956]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T20:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "1acc2bc8-819",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T19:00:02.000Z")
  },
  {
    _id: "3b9da89e-4ce5-5b48-b2c1-9efe05a0e28e",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "0e0e2068-d4ee-5749-9bc2-3688386e625e",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T22:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "244d52e9-bb3",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T22:00:02.000Z")
  },
  {
    _id: "95b7ac5e-43c2-5fb2-91d9-a992f42a6c0d",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "1d914803-2c41-55ef-b2b0-981105b0db81",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T06:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "37992e1f-d0a",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T06:00:02.000Z")
  },
  {
    _id: "764b5053-6810-568d-b1c6-5b786ccabdd1",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: "1f1e7b63-f3fc-5911-9659-81d67085a4a0",
    id_tramo: "1d914803-2c41-55ef-b2b0-981105b0db81",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T14:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Puerto de Buenos Aires - Montevideo",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.26725, -34.7443]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T22:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7d02b106-3c6",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:02.000Z")
  },
  {
    _id: "a3c0264a-09f0-5855-ac9e-98361a3417ee",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "1d914803-2c41-55ef-b2b0-981105b0db81",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-21T23:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Control aduanero en Montevideo",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "1ed42ebd-234",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T23:00:02.000Z")
  },
  {
    _id: "0f39f224-16d8-5237-ba8d-da0118d90dcb",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "1d914803-2c41-55ef-b2b0-981105b0db81",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T00:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "93c2cb1f-fa1",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T00:00:02.000Z")
  },
  {
    _id: "588bff57-de0b-55ee-a6bf-6e4368295805",
    id_envio: "98fc9f7b-15bc-5e20-94c8-b875df1ed8d6",
    id_contenedor: null,
    id_tramo: "1d914803-2c41-55ef-b2b0-981105b0db81",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T02:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5c97a2bf-e77",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T02:00:02.000Z")
  },
  {
    _id: "3122018e-fccc-52b0-a0e2-330d96e3383f",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "a5f8a76a-a5ad-5a0e-a311-f01d71122fbe",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T22:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000013"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E008-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b22f096c-ee8",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T22:00:02.000Z")
  },
  {
    _id: "2e0fc753-62d0-549d-8bb3-904ed3ad4486",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "a5f8a76a-a5ad-5a0e-a311-f01d71122fbe",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T00:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE288XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "125531a6-631",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T00:00:02.000Z")
  },
  {
    _id: "210fa825-9085-53a6-ac1d-adcc7aceffa5",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: "8673db80-6691-5e62-abb9-90bacf34c8d7",
    id_tramo: "a5f8a76a-a5ad-5a0e-a311-f01d71122fbe",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T13:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-63.6137, -33.7466]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T03:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "add7a184-c8c",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T13:30:02.000Z")
  },
  {
    _id: "f758781c-ce11-5db7-bac8-4486a741d465",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "a5f8a76a-a5ad-5a0e-a311-f01d71122fbe",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-21T05:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "36ef9ba8-193",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T05:00:02.000Z")
  },
  {
    _id: "30ceb53f-c8b5-5c7b-a43b-ebfad3c22037",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "e9050c1b-1160-529a-8259-e72989da689a",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T13:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE353XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "30a99b43-8f9",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T13:00:02.000Z")
  },
  {
    _id: "e2a7c561-4e7a-5e7c-8d9c-25612899955e",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: "8673db80-6691-5e62-abb9-90bacf34c8d7",
    id_tramo: "e9050c1b-1160-529a-8259-e72989da689a",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T14:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.4587, -34.71295]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T15:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "46edc5d8-2e0",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:02.000Z")
  },
  {
    _id: "4c7b887e-f3ca-55b8-a371-558e82d1c112",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "e9050c1b-1160-529a-8259-e72989da689a",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T17:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "75556446-d09",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T17:00:02.000Z")
  },
  {
    _id: "96581285-ee01-580c-a204-1da758494360",
    id_envio: "c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4",
    id_contenedor: null,
    id_tramo: "e9050c1b-1160-529a-8259-e72989da689a",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T19:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "906f9604-20a",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T19:00:02.000Z")
  },
  {
    _id: "5bcad753-76a7-507a-9928-e3079419f7c0",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T22:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000025"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E020-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "916034f6-967",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T22:00:02.000Z")
  },
  {
    _id: "5d2caaf4-a49e-595a-a833-d0378710676a",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T00:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE636XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "24eb8da3-eee",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T00:00:02.000Z")
  },
  {
    _id: "8e8fd18e-e1eb-54ab-8b39-513564e5bf79",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: "3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41",
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-21T02:30:00.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000025",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.98966, -33.53108]
      }
    },
    descripcion: "Sensor sin lecturas desde su instalación",
    payload: {
      tipo_regla: "INACTIVIDAD",
      severidad: "ALTA",
      sensor: "SEN-040",
      valor_observado: 150.0
    },
    origen: "SISTEMA",
    id_alerta: "58ede133-d04d-5447-a076-8e1257d78394",
    id_incidente: null,
    id_correlacion: "31495427-e32",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:30:02.000Z")
  },
  {
    _id: "eff0393a-32ea-5246-b167-9d9b64f7c258",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: "3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41",
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "INCIDENTE_DETECTADO",
    fecha: ISODate("2026-10-21T02:30:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000025",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.98966, -33.53108]
      }
    },
    descripcion: "Sensor sin lecturas desde su instalación",
    payload: {
      tipo_incidente: "ALERTA_INACTIVIDAD",
      severidad: "ALTA",
      origen: "ALERTA",
      id_origen: "58ede133-d04d-5447-a076-8e1257d78394"
    },
    origen: "SISTEMA",
    id_alerta: null,
    id_incidente: "8d248830-f083-5b21-a26e-bad0492938ce",
    id_correlacion: "bc7e86ed-439",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:30:07.000Z")
  },
  {
    _id: "0dd11b29-a1e0-5ed1-ba30-353438ca4112",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: "3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41",
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T04:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-59.59315, -33.8832]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T08:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "161200df-ab4",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T04:00:02.000Z")
  },
  {
    _id: "69f1df0b-6b7b-5819-81c8-09494110f27b",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "059c6b5a-2b9c-508f-b9a3-83059e0118c1",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-21T10:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "6db7ccbd-59f",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T10:00:02.000Z")
  },
  {
    _id: "cdbfd781-4db3-5b19-b035-927e770f2afd",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "646fdaea-f447-5aa0-bf65-1c0f07741a3d",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T13:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Salida tramo 2 (aereo) con AeroCargo Sudamericana",
    payload: {
      medio_transporte: "AEREO",
      operador: "AeroCargo Sudamericana",
      vehiculo: "LV-CGO"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "fa56bec3-51f",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T13:00:02.000Z")
  },
  {
    _id: "2761d240-9495-5d5e-8327-40718f2ca458",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: "3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41",
    id_tramo: "646fdaea-f447-5aa0-bf65-1c0f07741a3d",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T21:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Aeropuerto de Ezeiza - Miami",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.3638, -4.53025]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T06:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7576191e-a0e",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T21:30:02.000Z")
  },
  {
    _id: "b422d318-cbb1-51b3-9087-e74c5943af3a",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "646fdaea-f447-5aa0-bf65-1c0f07741a3d",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-22T07:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Control aduanero en Miami",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "decebe20-1ed",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T07:00:02.000Z")
  },
  {
    _id: "f238a19f-ca90-5b9a-92bb-4aa7c109f5c3",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "646fdaea-f447-5aa0-bf65-1c0f07741a3d",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T08:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "afc59738-9f2",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T08:00:02.000Z")
  },
  {
    _id: "9ee4b5b4-e998-5149-9881-b9ee54069289",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_contenedor: null,
    id_tramo: "646fdaea-f447-5aa0-bf65-1c0f07741a3d",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T10:00:00.000Z"),
    ubicacion: {
      nombre: "Miami",
      pais: "US",
      geo: {
        type: "Point",
        coordinates: [-80.1918, 25.7617]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3f697424-806",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T10:00:02.000Z")
  },
  {
    _id: "3173f382-3bb7-5a98-ae32-08fa7ae55f15",
    id_envio: "cbb962f0-a717-5f72-8213-1ea6a59dae3e",
    id_contenedor: null,
    id_tramo: "e8bb3dd9-25b1-5ec1-8fbb-88ab8c584d62",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T08:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000022"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E017-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "24605ba9-cdf",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T08:00:02.000Z")
  },
  {
    _id: "8d6f8dde-c0ab-5e78-b27b-8e63155c3298",
    id_envio: "cbb962f0-a717-5f72-8213-1ea6a59dae3e",
    id_contenedor: null,
    id_tramo: "e8bb3dd9-25b1-5ec1-8fbb-88ab8c584d62",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T10:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE279XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b2128b8a-d72",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T10:00:02.000Z")
  },
  {
    _id: "e55945d4-24fb-53f3-adce-43da7c6a5452",
    id_envio: "cbb962f0-a717-5f72-8213-1ea6a59dae3e",
    id_contenedor: "72234b4d-c7e7-575f-9e9b-dd19389d9909",
    id_tramo: "e8bb3dd9-25b1-5ec1-8fbb-88ab8c584d62",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T11:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3758, -34.5956]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T12:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "10b850e5-a3d",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T11:00:02.000Z")
  },
  {
    _id: "ba5e9d3c-4c43-53f7-883f-3b7dd689c411",
    id_envio: "cbb962f0-a717-5f72-8213-1ea6a59dae3e",
    id_contenedor: null,
    id_tramo: "e8bb3dd9-25b1-5ec1-8fbb-88ab8c584d62",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T14:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "47400b50-dab",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T14:00:02.000Z")
  },
  {
    _id: "36aebcd6-963f-5bb4-b17d-fa6d70287a52",
    id_envio: "cbb962f0-a717-5f72-8213-1ea6a59dae3e",
    id_contenedor: null,
    id_tramo: "44f48daa-e7d0-5756-bb3e-e83ebb7777fd",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T22:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c113d307-e96",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T22:00:02.000Z")
  },
  {
    _id: "d3e6764e-8b13-5b92-8534-f9bfa97427dc",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "dcab0450-00f4-5a7d-a890-312a079cf3f6",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-20T00:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000020"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E015-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "807bec9a-0b0",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T00:00:02.000Z")
  },
  {
    _id: "791b21f4-618f-5b77-9944-d0ff3ffbc6fa",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "dcab0450-00f4-5a7d-a890-312a079cf3f6",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T02:00:00.000Z"),
    ubicacion: {
      nombre: "Rosario",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-60.6505, -32.9442]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE791XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5f088aea-eef",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T02:00:02.000Z")
  },
  {
    _id: "5e641d77-e1c9-54b2-adea-11807616d1d0",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_tramo: "dcab0450-00f4-5a7d-a890-312a079cf3f6",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T07:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Rosario - Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-62.41965, -32.18215]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T12:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "a1ca5499-f28",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T07:00:02.000Z")
  },
  {
    _id: "c2159f9e-9ae3-55e8-838b-b1dc15a234f6",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-20T13:00:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000020",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Vibración 3.4 g supera 2.5 g",
    payload: {
      tipo_regla: "VIBRACION",
      severidad: "MEDIA",
      sensor: "SEN-020",
      valor_observado: 3.4
    },
    origen: "SISTEMA",
    id_alerta: "969a0ff6-56fd-5a5d-953b-ef9d9a5980bd",
    id_incidente: null,
    id_correlacion: "b89fc71b-daf",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T13:00:07.000Z")
  },
  {
    _id: "a1897e5c-abc0-5470-a2ad-d515d76e68ae",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "dcab0450-00f4-5a7d-a890-312a079cf3f6",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T14:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ec5281cb-95c",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T14:00:02.000Z")
  },
  {
    _id: "6d194ecb-dba1-502f-a91b-5e9eb4d7dad7",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T22:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Fletes del Litoral SRL",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Fletes del Litoral SRL",
      vehiculo: "AE399XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "807e741b-4e0",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T22:00:02.000Z")
  },
  {
    _id: "b89632c2-ece4-5935-8d54-c0a5a9f6a4a4",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T04:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Córdoba - Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-66.5173, -32.1548]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T11:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "c9f1f4be-ca5",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T04:30:02.000Z")
  },
  {
    _id: "e410b702-80a1-51b9-a8a6-9f80af7eef57",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T13:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "baab9430-219",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T13:00:02.000Z")
  },
  {
    _id: "652d1df2-d35b-5262-858d-5710aaef3f53",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: null,
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T15:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "66feb792-a6d",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T15:00:02.000Z")
  },
  {
    _id: "bc277088-ff95-595f-bc78-a01ce3373ada",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_tramo: "e93319ba-3433-56af-b122-1925b529c370",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-22T15:00:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000020",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Vibración 3.1 g supera 2.5 g",
    payload: {
      tipo_regla: "VIBRACION",
      severidad: "MEDIA",
      sensor: "SEN-020",
      valor_observado: 3.1
    },
    origen: "SISTEMA",
    id_alerta: "fb83ba84-60da-51d1-9164-16b014dc55e9",
    id_incidente: null,
    id_correlacion: "aa4e6a49-81b",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T15:00:07.000Z")
  },
  {
    _id: "9a1b6add-bc9b-589b-a60c-e7eae35938b6",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "478b706d-5b14-59a7-ac23-1213e30a0763",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T19:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000024"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E019-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "b1cece08-8fb",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T19:00:02.000Z")
  },
  {
    _id: "4a733000-fdc2-5f95-8189-d52ce1de21f2",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "478b706d-5b14-59a7-ac23-1213e30a0763",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T21:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE415XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "9568f729-65f",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T21:00:02.000Z")
  },
  {
    _id: "0845e023-d6b6-5136-9282-bf2196f9690b",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: "5a4a52d9-7346-50a7-9ee8-830e3e31a2ca",
    id_tramo: "478b706d-5b14-59a7-ac23-1213e30a0763",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T10:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-63.6137, -33.7466]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T00:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "94df0125-669",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T10:30:02.000Z")
  },
  {
    _id: "ad0838a0-df9f-5915-8486-9fa760ff1cf5",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "478b706d-5b14-59a7-ac23-1213e30a0763",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-21T02:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "215cd203-32d",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:00:02.000Z")
  },
  {
    _id: "43de6db3-d102-5474-a35e-73cb1189b9bb",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "ca428262-9867-587b-ada6-4622878da9ce",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T10:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE578XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "abddf427-b92",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T10:00:02.000Z")
  },
  {
    _id: "a1bcf730-b210-5928-a18a-e66ce4412c1f",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: "5a4a52d9-7346-50a7-9ee8-830e3e31a2ca",
    id_tramo: "ca428262-9867-587b-ada6-4622878da9ce",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T11:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3758, -34.5956]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T12:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3754e6e4-28e",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T11:00:02.000Z")
  },
  {
    _id: "8ff6fa6b-1ddf-536b-987a-fc776ebcfb8e",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "ca428262-9867-587b-ada6-4622878da9ce",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T14:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "89b7b843-792",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:02.000Z")
  },
  {
    _id: "96e9ec7d-83e9-5b42-abfb-06fe14050a8c",
    id_envio: "d5b901e5-603a-585b-8e8b-fa02b491e825",
    id_contenedor: null,
    id_tramo: "ca428262-9867-587b-ada6-4622878da9ce",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T16:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "5f9093de-a1b",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T16:00:02.000Z")
  },
  {
    _id: "0865cd62-0feb-5e2c-8f12-9a78074ec2f1",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: null,
    id_tramo: "22ff43b2-a1c6-5f83-ac06-0a4011c3d943",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T16:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 2 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000001", "MSKU0000002"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E001-1", "PR-E001-2"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "453fe9b7-2ee",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T16:00:02.000Z")
  },
  {
    _id: "fa927385-48c2-5d5e-bad8-b4a854d7d277",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: null,
    id_tramo: "22ff43b2-a1c6-5f83-ac06-0a4011c3d943",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T18:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE959XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "9df5053b-43b",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T18:00:02.000Z")
  },
  {
    _id: "7f98932e-5b5c-5dc4-ab9c-5baed1ff6c0e",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123",
    id_tramo: "22ff43b2-a1c6-5f83-ac06-0a4011c3d943",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-19T19:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3758, -34.5956]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-19T20:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "71343e6e-b85",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T19:00:02.000Z")
  },
  {
    _id: "4bbdfbaa-d33d-5126-a402-99ad6c5f9420",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: null,
    id_tramo: "22ff43b2-a1c6-5f83-ac06-0a4011c3d943",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-19T22:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "fb6b9fb6-db2",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T22:00:02.000Z")
  },
  {
    _id: "0e0d43bc-cf4b-566b-aa40-896ed9bffeaf",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: null,
    id_tramo: "e8151a73-77d5-56e2-bf25-38268b7bf295",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-20T06:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "195963e2-b8f",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T06:00:02.000Z")
  },
  {
    _id: "d945fdfe-5df2-53ee-a6b3-18f38610f0d7",
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123",
    id_tramo: "e8151a73-77d5-56e2-bf25-38268b7bf295",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-22T23:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Puerto de Buenos Aires - Puerto de Santos",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-52.3518, -29.27415]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-25T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "1e55c3c8-663",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T23:00:02.000Z")
  },
  {
    _id: "bc7ba10d-ce78-5e4f-9e7c-558f26a894b4",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "6b3a18d6-ae1a-52b4-8e34-46c52902dd01",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-21T06:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000018"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E013-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "d0811746-e5e",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T06:00:02.000Z")
  },
  {
    _id: "4a0a8ec6-c060-5a8f-9e5c-2722d05c4bf3",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "6b3a18d6-ae1a-52b4-8e34-46c52902dd01",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T08:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Transportes Ruta 9 S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Transportes Ruta 9 S.A.",
      vehiculo: "AE550XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "be674c9a-216",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T08:00:02.000Z")
  },
  {
    _id: "561c15c4-05fd-57c8-b9f4-802a16e8df13",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: "ddda79aa-4ad6-5abb-b1d1-b61f25812c47",
    id_tramo: "6b3a18d6-ae1a-52b4-8e34-46c52902dd01",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T17:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Córdoba - Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-61.3623, -33.12115]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T02:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f54b10f1-7cf",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T17:00:02.000Z")
  },
  {
    _id: "0318fbf9-8fb5-5760-b489-815e555ec65f",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "6b3a18d6-ae1a-52b4-8e34-46c52902dd01",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-22T04:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "374f7d41-500",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T04:00:02.000Z")
  },
  {
    _id: "d9953e7a-c333-5a23-a063-570bf8c7bd91",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "1fb6bb17-903d-5fee-a675-3eb489b268a3",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-22T07:00:00.000Z"),
    ubicacion: {
      nombre: "Aeropuerto de Ezeiza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.5358, -34.8222]
      }
    },
    descripcion: "Salida tramo 2 (aereo) con AeroCargo Sudamericana",
    payload: {
      medio_transporte: "AEREO",
      operador: "AeroCargo Sudamericana",
      vehiculo: "LV-CGO"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7c742025-018",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T07:00:02.000Z")
  },
  {
    _id: "23eaf9e1-0d3e-5163-afb3-6b23b634c7c2",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: "ddda79aa-4ad6-5abb-b1d1-b61f25812c47",
    id_tramo: "1fb6bb17-903d-5fee-a675-3eb489b268a3",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-22T11:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Aeropuerto de Ezeiza - Callao",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-67.7893, -23.4343]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "a1e2ba23-5be",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T11:30:02.000Z")
  },
  {
    _id: "d3b027a9-c47c-5894-bcc9-ef94c2bb124f",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "1fb6bb17-903d-5fee-a675-3eb489b268a3",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-22T17:00:00.000Z"),
    ubicacion: {
      nombre: "Callao",
      pais: "PE",
      geo: {
        type: "Point",
        coordinates: [-77.0428, -12.0464]
      }
    },
    descripcion: "Control aduanero en Callao",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ba79a40f-89b",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T17:00:02.000Z")
  },
  {
    _id: "30b71666-fde7-5711-8caf-9c1b190f377d",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "1fb6bb17-903d-5fee-a675-3eb489b268a3",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T18:00:00.000Z"),
    ubicacion: {
      nombre: "Callao",
      pais: "PE",
      geo: {
        type: "Point",
        coordinates: [-77.0428, -12.0464]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4057a2b4-394",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T18:00:02.000Z")
  },
  {
    _id: "e6aaac03-2a0d-50d8-8cdc-386f70f6e340",
    id_envio: "dceca613-bcf0-57cf-9929-3d8f07522fe7",
    id_contenedor: null,
    id_tramo: "1fb6bb17-903d-5fee-a675-3eb489b268a3",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T20:00:00.000Z"),
    ubicacion: {
      nombre: "Callao",
      pais: "PE",
      geo: {
        type: "Point",
        coordinates: [-77.0428, -12.0464]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4228cb8e-f9b",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T20:00:02.000Z")
  },
  {
    _id: "67e865d0-b5a2-518d-ad6f-8219df3d454d",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "aeec7c55-cc6e-5ae6-8044-9632f64fac74",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T20:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Inicio de carga de 2 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000003", "MSKU0000004"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E002-1", "PR-E002-2"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ff8111f9-e4e",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T20:00:02.000Z")
  },
  {
    _id: "e4d6390b-e68a-5635-915c-7e40ced57e35",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "aeec7c55-cc6e-5ae6-8044-9632f64fac74",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T22:00:00.000Z"),
    ubicacion: {
      nombre: "Córdoba",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-64.1888, -31.4201]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE780XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "e78388ac-20a",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T22:00:02.000Z")
  },
  {
    _id: "4c7779b3-d2e8-5542-8932-0fb0f1f61d27",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_tramo: "aeec7c55-cc6e-5ae6-8044-9632f64fac74",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T07:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Córdoba - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-61.2794, -33.0038]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "287c030c-502",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T07:00:02.000Z")
  },
  {
    _id: "ef591362-0c73-554f-957f-65658b116043",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "aeec7c55-cc6e-5ae6-8044-9632f64fac74",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T18:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "62ef4f46-43c",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T18:00:02.000Z")
  },
  {
    _id: "acb73994-0cfc-5fb9-8e90-ca497982a3d2",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T02:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "52bbe549-743",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T02:00:02.000Z")
  },
  {
    _id: "ee352f17-e059-5c08-8905-df74000374e0",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "RETRASO",
    fecha: ISODate("2026-10-21T08:00:00.000Z"),
    ubicacion: {
      nombre: "Atlántico Sur",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.81447, -34.09704]
      }
    },
    descripcion: "Demora informada por la naviera",
    payload: {
      motivo: "Congestión en Puerto de Santos",
      demora_estimada_horas: 18,
      nueva_eta: "2026-10-27T06:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: "f6c7d245-ed5f-50e1-809b-8c09122bf160",
    id_correlacion: "3e93c0e7-d4f",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T08:00:02.000Z")
  },
  {
    _id: "4c1256f9-ac74-516e-8e9f-b60c75a0c77a",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: null,
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "INCIDENTE_DETECTADO",
    fecha: ISODate("2026-10-21T08:00:05.000Z"),
    ubicacion: {
      nombre: "Atlántico Sur",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.81447, -34.09704]
      }
    },
    descripcion: "Demora de 18 h en tramo marítimo",
    payload: {
      tipo_incidente: "DEMORA",
      severidad: "ALTA",
      origen: "EVENTO",
      id_origen: "ee352f17-e059-5c08-8905-df74000374e0"
    },
    origen: "SISTEMA",
    id_alerta: null,
    id_incidente: "f6c7d245-ed5f-50e1-809b-8c09122bf160",
    id_correlacion: "d12a9419-d06",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T08:00:07.000Z")
  },
  {
    _id: "ea67b317-0fa5-5a34-bc08-6ac0b2ce2e9a",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-21T14:00:05.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000003",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.25882, -33.60646]
      }
    },
    descripcion: "Temperatura 10.8 °C supera 8 °C en contenedor REEFER",
    payload: {
      tipo_regla: "TEMPERATURA",
      severidad: "ALTA",
      sensor: "SEN-003",
      valor_observado: 10.8
    },
    origen: "SISTEMA",
    id_alerta: "3fa61b6b-b8f6-5faf-9e32-bc7658cd63b5",
    id_incidente: null,
    id_correlacion: "2ef4a600-6f9",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:07.000Z")
  },
  {
    _id: "1447d9ee-098f-567d-ab8b-8ec9431fa1cc",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "INCIDENTE_DETECTADO",
    fecha: ISODate("2026-10-21T14:00:10.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000003",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.25882, -33.60646]
      }
    },
    descripcion: "Temperatura 10.8 °C supera 8 °C en contenedor REEFER",
    payload: {
      tipo_incidente: "ALERTA_TEMPERATURA",
      severidad: "ALTA",
      origen: "ALERTA",
      id_origen: "3fa61b6b-b8f6-5faf-9e32-bc7658cd63b5"
    },
    origen: "SISTEMA",
    id_alerta: null,
    id_incidente: "4288a948-e9e1-51e6-b693-7029d2d32414",
    id_correlacion: "6d00d37a-409",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:12.000Z")
  },
  {
    _id: "1ab4442f-07c6-576f-a901-0f4f007c6bf8",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_tramo: "832d03fd-5634-54bc-bbb2-b831c1d3bb52",
    tipo: "ALERTA_GENERADA",
    fecha: ISODate("2026-10-22T23:15:00.000Z"),
    ubicacion: {
      nombre: "Contenedor MSKU0000003",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-54.18041, -30.88859]
      }
    },
    descripcion: "4 lectura(s) de temperatura anómalas (|z| máx 4.26)",
    payload: {
      tipo_regla: "ANOMALIA_TEMPERATURA",
      severidad: "MEDIA",
      sensor: "SEN-003",
      valor_observado: 4.26
    },
    origen: "SISTEMA",
    id_alerta: "74fc30d0-5fa7-52dd-bb1c-2bd1ba8392b0",
    id_incidente: null,
    id_correlacion: "c3c1a66f-f48",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T23:15:02.000Z")
  },
  {
    _id: "aa8a606a-9ee8-5b25-8e3e-6da3faf02c86",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-19T12:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 2 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000009", "MSKU0000010"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E005-1", "PR-E005-2"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "a7046dbd-7ed",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T12:00:02.000Z")
  },
  {
    _id: "db382064-d9b0-5906-b106-57a24d4ed047",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-19T14:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE745XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f5e22b86-732",
    registrado_por: null,
    creado_en: ISODate("2026-10-19T14:00:02.000Z")
  },
  {
    _id: "766d02b6-8665-5e1f-a65c-b3bc5ce18365",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "DESVIO",
    fecha: ISODate("2026-10-20T03:00:00.000Z"),
    ubicacion: {
      nombre: "Ruta Nacional 7",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-63.41992, -33.77834]
      }
    },
    descripcion: "Desvío de ruta",
    payload: {
      ruta_original: "RN7",
      ruta_alternativa: "RN8",
      km_adicionales: 64,
      motivo: "Corte de ruta"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: "f5b72008-9a09-54bd-8682-60e96cdea169",
    id_correlacion: "6209eaf3-974",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T03:00:02.000Z")
  },
  {
    _id: "a906bd3e-f0ea-58c3-82ec-881377bae0a7",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "INCIDENTE_DETECTADO",
    fecha: ISODate("2026-10-20T03:00:05.000Z"),
    ubicacion: {
      nombre: "Ruta Nacional 7",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-63.41992, -33.77834]
      }
    },
    descripcion: "Desvío por corte en RN7",
    payload: {
      tipo_incidente: "DESVIO",
      severidad: "MEDIA",
      origen: "EVENTO",
      id_origen: "766d02b6-8665-5e1f-a65c-b3bc5ce18365"
    },
    origen: "SISTEMA",
    id_alerta: null,
    id_incidente: "f5b72008-9a09-54bd-8682-60e96cdea169",
    id_correlacion: "52759f0d-da4",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T03:00:07.000Z")
  },
  {
    _id: "5cb0c45b-7713-55b9-8057-c7e2aeed9797",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: "2476f78b-a4fe-5d49-af25-e097bb04de76",
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-20T03:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-63.6137, -33.7466]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-20T17:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "3b9c9878-cce",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T03:30:02.000Z")
  },
  {
    _id: "fecd8952-3271-5504-b147-78f6c2018203",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "881df510-7542-50fd-a2cc-f4bbe1db7dee",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-20T19:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "ba9155d2-5a2",
    registrado_por: null,
    creado_en: ISODate("2026-10-20T19:00:02.000Z")
  },
  {
    _id: "e76b9f24-45dd-5309-8cb4-7e3b5d9ff047",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "46c78512-166d-5584-9358-28d9dd67bb85",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T03:00:00.000Z"),
    ubicacion: {
      nombre: "Mendoza",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-68.8458, -32.8895]
      }
    },
    descripcion: "Salida tramo 2 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE219XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "8ec63527-ec5",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T03:00:02.000Z")
  },
  {
    _id: "27d8862a-e9fd-56d7-a9cd-f648fdd23932",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: "2476f78b-a4fe-5d49-af25-e097bb04de76",
    id_tramo: "46c78512-166d-5584-9358-28d9dd67bb85",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T05:30:00.000Z"),
    ubicacion: {
      nombre: "En ruta Mendoza - Santiago",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-69.75755, -33.1692]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T08:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "6d117e1e-ee3",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T05:30:02.000Z")
  },
  {
    _id: "cbcb98d3-ad13-54c3-855f-54facb669af4",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "46c78512-166d-5584-9358-28d9dd67bb85",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-21T09:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Control aduanero en Santiago",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "733ce2c0-cdc",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T09:00:02.000Z")
  },
  {
    _id: "cba0a7f7-ac4d-54dd-ada5-559acf646f2f",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "46c78512-166d-5584-9358-28d9dd67bb85",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-21T10:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "764ead99-b7e",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T10:00:02.000Z")
  },
  {
    _id: "5f49bbd2-5158-55c1-9451-e2a0c5f3e102",
    id_envio: "ecae19bc-e959-5b5d-bb35-be45c2e6c910",
    id_contenedor: null,
    id_tramo: "46c78512-166d-5584-9358-28d9dd67bb85",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-21T12:00:00.000Z"),
    ubicacion: {
      nombre: "Santiago",
      pais: "CL",
      geo: {
        type: "Point",
        coordinates: [-70.6693, -33.4489]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "7b904b9d-ca5",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T12:00:02.000Z")
  },
  {
    _id: "880c41e8-234c-5f97-9d20-90f69ada5aa8",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "e804aef0-961b-536c-a56e-f53d7501b635",
    tipo: "CARGA_INICIADA",
    fecha: ISODate("2026-10-21T12:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Inicio de carga de 1 contenedor(es)",
    payload: {
      contenedores: ["MSKU0000019"],
      responsable: "Depósito del cliente",
      precintos: ["PR-E014-1"]
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "707f0752-1ce",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T12:00:02.000Z")
  },
  {
    _id: "b541c58b-b1e6-5587-8727-ad4d522227b2",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "e804aef0-961b-536c-a56e-f53d7501b635",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-21T14:00:00.000Z"),
    ubicacion: {
      nombre: "Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3816, -34.6037]
      }
    },
    descripcion: "Salida tramo 1 (terrestre) con Logística Cordillera S.A.",
    payload: {
      medio_transporte: "TERRESTRE",
      operador: "Logística Cordillera S.A.",
      vehiculo: "AE996XY"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "8fc4eaf0-fb3",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T14:00:02.000Z")
  },
  {
    _id: "1f7e71b8-5b37-5b74-8351-6bd7b6e5ceb2",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: "bf05ae76-f8c9-529d-8cdb-52b4cedb42d9",
    id_tramo: "e804aef0-961b-536c-a56e-f53d7501b635",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-21T15:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Buenos Aires - Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.3758, -34.5956]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-21T16:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "bc500e57-535",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T15:00:02.000Z")
  },
  {
    _id: "0d611925-b935-5011-9533-97545cfb937b",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "e804aef0-961b-536c-a56e-f53d7501b635",
    tipo: "TRASBORDO",
    fecha: ISODate("2026-10-21T18:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Trasbordo a tramo 2",
    payload: {
      espera_horas: 8
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "f6afcdde-9cb",
    registrado_por: null,
    creado_en: ISODate("2026-10-21T18:00:02.000Z")
  },
  {
    _id: "38f77a31-7d9a-59e2-95ef-54e36d0b2d08",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "457efe24-5708-582a-b74c-feb8dc5a42f9",
    tipo: "SALIDA",
    fecha: ISODate("2026-10-22T02:00:00.000Z"),
    ubicacion: {
      nombre: "Puerto de Buenos Aires",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-58.37, -34.5875]
      }
    },
    descripcion: "Salida tramo 2 (maritimo) con Naviera Atlántica S.A.",
    payload: {
      medio_transporte: "MARITIMO",
      operador: "Naviera Atlántica S.A.",
      vehiculo: "MV Atlantic Star"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "24f85652-93e",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T02:00:02.000Z")
  },
  {
    _id: "844a50fb-a209-5368-9299-65039cbf5c3e",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: "bf05ae76-f8c9-529d-8cdb-52b4cedb42d9",
    id_tramo: "457efe24-5708-582a-b74c-feb8dc5a42f9",
    tipo: "EN_TRANSITO",
    fecha: ISODate("2026-10-22T10:00:00.000Z"),
    ubicacion: {
      nombre: "En ruta Puerto de Buenos Aires - Montevideo",
      pais: "AR",
      geo: {
        type: "Point",
        coordinates: [-57.26725, -34.7443]
      }
    },
    descripcion: "Reporte de posición del operador",
    payload: {
      avance_pct: 50,
      eta: "2026-10-22T18:00:00+00:00"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "348bff01-d7f",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T10:00:02.000Z")
  },
  {
    _id: "0a81227c-c6d5-5003-99bf-5df504417e89",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "457efe24-5708-582a-b74c-feb8dc5a42f9",
    tipo: "ADUANA",
    fecha: ISODate("2026-10-22T19:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Control aduanero en Montevideo",
    payload: {
      canal: "VERDE",
      documentos: ["Factura comercial", "Packing list", "Certificado de origen"],
      despachante: "Despachante Aduanero Gómez"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "8161a01b-ba9",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T19:00:02.000Z")
  },
  {
    _id: "de5d6d7a-d512-5ed9-b681-b1454975b488",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "457efe24-5708-582a-b74c-feb8dc5a42f9",
    tipo: "DESCARGA",
    fecha: ISODate("2026-10-22T20:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Descarga en destino",
    payload: {
      muelle_o_anden: "A-3"
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "4644315e-c87",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T20:00:02.000Z")
  },
  {
    _id: "52deb1f2-88cc-5009-8cc3-9b1ce4404bb5",
    id_envio: "f6f08a03-2a92-5140-9846-0e2ef076db3d",
    id_contenedor: null,
    id_tramo: "457efe24-5708-582a-b74c-feb8dc5a42f9",
    tipo: "ENTREGADO",
    fecha: ISODate("2026-10-22T22:00:00.000Z"),
    ubicacion: {
      nombre: "Montevideo",
      pais: "UY",
      geo: {
        type: "Point",
        coordinates: [-56.1645, -34.9011]
      }
    },
    descripcion: "Entrega conforme al destinatario",
    payload: {
      receptor: "Recepción destinatario",
      firma_digital: true,
      conforme: true
    },
    origen: "OPERADOR",
    id_alerta: null,
    id_incidente: null,
    id_correlacion: "0fe21787-671",
    registrado_por: null,
    creado_en: ISODate("2026-10-22T22:00:02.000Z")
  }
]);

// ---------------------------------------------------------------- alertas (8)
cargar('alertas', [
  {
    _id: "3fa61b6b-b8f6-5faf-9e32-bc7658cd63b5",
    id_regla: "96196805-40db-51f4-a2b2-0e8cc7ee2a63",
    tipo_regla: "TEMPERATURA",
    id_sensor: "12330766-e3ec-52b9-b9c0-b400c16274f6",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
    id_lectura: "40f75486-acc9-54e4-93d8-f4c2cff39048",
    id_evento: "ea67b317-0fa5-5a34-bc08-6ac0b2ce2e9a",
    id_incidente: "4288a948-e9e1-51e6-b693-7029d2d32414",
    operador: ">",
    umbral: 8.0,
    valor_observado: 10.8,
    severidad: "ALTA",
    estado: "RECONOCIDA",
    activa: true,
    mensaje: "Temperatura 10.8 °C supera 8 °C en contenedor REEFER",
    detalle: {
      valor_maximo: 11.6,
      lecturas: [
        {
          id_lectura: "40f75486-acc9-54e4-93d8-f4c2cff39048",
          fecha_hora: ISODate("2026-10-21T14:00:00.000Z"),
          temperatura: 10.8
        },
        {
          id_lectura: "4a3a0cfa-21f0-5ca2-babf-517b0942738a",
          fecha_hora: ISODate("2026-10-21T15:00:00.000Z"),
          temperatura: 11.4
        },
        {
          id_lectura: "8ffaa3bd-b059-5f7f-97fe-1a4e42b2915b",
          fecha_hora: ISODate("2026-10-21T16:00:00.000Z"),
          temperatura: 11.6
        },
        {
          id_lectura: "67caf0e4-be1e-528a-80ea-77ff3e451eb6",
          fecha_hora: ISODate("2026-10-21T17:00:00.000Z"),
          temperatura: 10.9
        }
      ]
    },
    fecha: ISODate("2026-10-21T14:00:05.000Z"),
    fecha_lectura: ISODate("2026-10-21T14:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-21T17:00:00.000Z"),
    ocurrencias: NumberInt(4),
    clave_dedup: "96196805-40db-51f4-a2b2-0e8cc7ee2a63|12330766-e3ec-52b9-b9c0-b400c16274f6",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-21T14:00:05.000Z"),
        id_usuario: null,
        comentario: null
      },
      {
        estado: "RECONOCIDA",
        fecha: ISODate("2026-10-21T14:20:05.000Z"),
        id_usuario: "6810f791-1878-5de4-b0f6-7e63d996d4ac",
        comentario: "Se avisó a FrioTech (P02) para revisar el equipo de frío"
      }
    ]
  },
  {
    _id: "fc397a0b-b3dd-5556-90dc-9d42e49bd079",
    id_regla: "e175309e-cb9c-542e-91b0-946800a453f3",
    tipo_regla: "BATERIA",
    id_sensor: "5f9d963d-5c94-598d-9f99-83906ed03b7b",
    id_contenedor: "a4f4c383-f83a-5fe5-9a33-902d50f7f487",
    id_envio: "5bcda798-982d-5078-a904-acb7769b79dd",
    id_cliente: "23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9",
    id_lectura: "eae4bf77-9784-561b-98fe-af8e975d9897",
    id_evento: "85502a6c-e86f-5980-9b37-6676dae24779",
    id_incidente: null,
    operador: "<",
    umbral: 15.0,
    valor_observado: 14.8,
    severidad: "MEDIA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "Batería del sensor en 14.8 %",
    detalle: {
      bateria_actual: 8.4
    },
    fecha: ISODate("2026-10-21T18:00:05.000Z"),
    fecha_lectura: ISODate("2026-10-21T18:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-22T23:00:00.000Z"),
    ocurrencias: NumberInt(30),
    clave_dedup: "e175309e-cb9c-542e-91b0-946800a453f3|5f9d963d-5c94-598d-9f99-83906ed03b7b",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-21T18:00:05.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  },
  {
    _id: "fd3e05e0-2fd5-5b37-adbe-c74c571ede3c",
    id_regla: "f686b8bb-fc4e-531e-9635-1966bb5ed92f",
    tipo_regla: "INACTIVIDAD",
    id_sensor: "ee32d9a8-5a2d-5bb2-b703-370676b20446",
    id_contenedor: "5a92f38e-c32b-5035-8b97-1d253d2e58b1",
    id_envio: "6378a987-ec3f-504e-80db-0a38c77ee116",
    id_cliente: "07b5388d-ba2c-57c2-adbd-21e43c3f9c4c",
    id_lectura: null,
    id_evento: "d70fc6d6-4e1c-5e37-8a1c-2b3301e3f908",
    id_incidente: "29ad900d-f0a7-5bdf-a550-c423dd619494",
    operador: ">",
    umbral: 120.0,
    valor_observado: 150.0,
    severidad: "ALTA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "Sensor sin lecturas hace 150 minutos",
    detalle: {
      ultima_lectura: ISODate("2026-10-21T10:00:00.000Z"),
      minutos_sin_datos_al_cierre: 2250
    },
    fecha: ISODate("2026-10-21T12:30:00.000Z"),
    fecha_lectura: ISODate("2026-10-21T10:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-22T23:30:00.000Z"),
    ocurrencias: NumberInt(36),
    clave_dedup: "f686b8bb-fc4e-531e-9635-1966bb5ed92f|ee32d9a8-5a2d-5bb2-b703-370676b20446",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-21T12:30:00.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  },
  {
    _id: "58ede133-d04d-5447-a076-8e1257d78394",
    id_regla: "f686b8bb-fc4e-531e-9635-1966bb5ed92f",
    tipo_regla: "INACTIVIDAD",
    id_sensor: "1a41b97f-b75c-5365-8111-d65ac5ec9d46",
    id_contenedor: "3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41",
    id_envio: "c231e5a7-7434-5d4b-b8db-405b7f42be44",
    id_cliente: "07b5388d-ba2c-57c2-adbd-21e43c3f9c4c",
    id_lectura: null,
    id_evento: "8e8fd18e-e1eb-54ab-8b39-513564e5bf79",
    id_incidente: "8d248830-f083-5b21-a26e-bad0492938ce",
    operador: ">",
    umbral: 120.0,
    valor_observado: 150.0,
    severidad: "ALTA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "Sensor sin lecturas desde su instalación",
    detalle: {
      ultima_lectura: null,
      asignado_desde: ISODate("2026-10-21T00:00:00.000Z")
    },
    fecha: ISODate("2026-10-21T02:30:00.000Z"),
    fecha_lectura: null,
    ultima_ocurrencia: ISODate("2026-10-22T23:30:00.000Z"),
    ocurrencias: NumberInt(46),
    clave_dedup: "f686b8bb-fc4e-531e-9635-1966bb5ed92f|1a41b97f-b75c-5365-8111-d65ac5ec9d46",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-21T02:30:00.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  },
  {
    _id: "969a0ff6-56fd-5a5d-953b-ef9d9a5980bd",
    id_regla: "9c1403ef-fefe-5226-a415-d021f0f00376",
    tipo_regla: "VIBRACION",
    id_sensor: "099eb63e-59f7-5b79-82f9-ecd7cc14a31a",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_cliente: "23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9",
    id_lectura: "91188e38-cd40-5f89-a9da-36f20352a5aa",
    id_evento: "c2159f9e-9ae3-55e8-838b-b1dc15a234f6",
    id_incidente: null,
    operador: ">",
    umbral: 2.5,
    valor_observado: 3.4,
    severidad: "MEDIA",
    estado: "CERRADA",
    activa: false,
    mensaje: "Vibración 3.4 g supera 2.5 g",
    detalle: null,
    fecha: ISODate("2026-10-20T13:00:05.000Z"),
    fecha_lectura: ISODate("2026-10-20T13:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-20T13:00:00.000Z"),
    ocurrencias: NumberInt(1),
    clave_dedup: "9c1403ef-fefe-5226-a415-d021f0f00376|099eb63e-59f7-5b79-82f9-ecd7cc14a31a",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-20T13:00:05.000Z"),
        id_usuario: null,
        comentario: null
      },
      {
        estado: "RECONOCIDA",
        fecha: ISODate("2026-10-20T13:40:05.000Z"),
        id_usuario: "ce4c9cee-103c-5a27-99a4-0d7cffa87635",
        comentario: "Golpe durante maniobra de carga"
      },
      {
        estado: "CERRADA",
        fecha: ISODate("2026-10-20T15:00:05.000Z"),
        id_usuario: "6810f791-1878-5de4-b0f6-7e63d996d4ac",
        comentario: "Inspección sin daños en la mercadería"
      }
    ]
  },
  {
    _id: "fb83ba84-60da-51d1-9164-16b014dc55e9",
    id_regla: "9c1403ef-fefe-5226-a415-d021f0f00376",
    tipo_regla: "VIBRACION",
    id_sensor: "099eb63e-59f7-5b79-82f9-ecd7cc14a31a",
    id_contenedor: "efca5f90-e9bc-58ef-9e6d-87b0d85e4af0",
    id_envio: "cdea56b0-4ef2-5338-8342-9a3444d56919",
    id_cliente: "23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9",
    id_lectura: "7151c492-cc8d-5db1-bc4d-bdff308f9f21",
    id_evento: "bc277088-ff95-595f-bc78-a01ce3373ada",
    id_incidente: null,
    operador: ">",
    umbral: 2.5,
    valor_observado: 3.1,
    severidad: "MEDIA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "Vibración 3.1 g supera 2.5 g",
    detalle: null,
    fecha: ISODate("2026-10-22T15:00:05.000Z"),
    fecha_lectura: ISODate("2026-10-22T15:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-22T15:00:00.000Z"),
    ocurrencias: NumberInt(1),
    clave_dedup: "9c1403ef-fefe-5226-a415-d021f0f00376|099eb63e-59f7-5b79-82f9-ecd7cc14a31a",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-22T15:00:05.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  },
  {
    _id: "74fc30d0-5fa7-52dd-bb1c-2bd1ba8392b0",
    id_regla: "810b94b9-8c35-5cce-bd94-ce9c0a9ca625",
    tipo_regla: "ANOMALIA_TEMPERATURA",
    id_sensor: "12330766-e3ec-52b9-b9c0-b400c16274f6",
    id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
    id_lectura: "8ffaa3bd-b059-5f7f-97fe-1a4e42b2915b",
    id_evento: "1ab4442f-07c6-576f-a901-0f4f007c6bf8",
    id_incidente: null,
    operador: ">",
    umbral: 3.0,
    valor_observado: 4.26,
    severidad: "MEDIA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "4 lectura(s) de temperatura anómalas (|z| máx 4.26)",
    detalle: {
      metodo: "z-score sobre la ventana 2026-10-20/22",
      limite_z: 3,
      lecturas: [
        {
          id_lectura: "40f75486-acc9-54e4-93d8-f4c2cff39048",
          fecha_hora: ISODate("2026-10-21T14:00:00.000Z"),
          temperatura: 10.8,
          z: 3.74
        },
        {
          id_lectura: "4a3a0cfa-21f0-5ca2-babf-517b0942738a",
          fecha_hora: ISODate("2026-10-21T15:00:00.000Z"),
          temperatura: 11.4,
          z: 4.13
        },
        {
          id_lectura: "8ffaa3bd-b059-5f7f-97fe-1a4e42b2915b",
          fecha_hora: ISODate("2026-10-21T16:00:00.000Z"),
          temperatura: 11.6,
          z: 4.26
        },
        {
          id_lectura: "67caf0e4-be1e-528a-80ea-77ff3e451eb6",
          fecha_hora: ISODate("2026-10-21T17:00:00.000Z"),
          temperatura: 10.9,
          z: 3.81
        }
      ]
    },
    fecha: ISODate("2026-10-22T23:15:00.000Z"),
    fecha_lectura: ISODate("2026-10-21T16:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-22T23:15:00.000Z"),
    ocurrencias: NumberInt(4),
    clave_dedup: "810b94b9-8c35-5cce-bd94-ce9c0a9ca625|12330766-e3ec-52b9-b9c0-b400c16274f6",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-22T23:15:00.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  },
  {
    _id: "9bd9597e-e07e-5762-8ae2-1f9824c2799b",
    id_regla: "810b94b9-8c35-5cce-bd94-ce9c0a9ca625",
    tipo_regla: "ANOMALIA_TEMPERATURA",
    id_sensor: "94858666-0f1b-5d7a-8f8d-0118791421e9",
    id_contenedor: "d37ca4b8-0970-5cec-ac9b-490429b7df61",
    id_envio: "506de4f8-5386-58b4-ad0d-2f851434aec6",
    id_cliente: "e32b1f7c-90fe-5eca-935c-8062fca267ae",
    id_lectura: "641a042a-4c55-5b51-8fec-2a750f571dc4",
    id_evento: "d54ac45f-f166-5b79-af09-f118174c8d60",
    id_incidente: null,
    operador: ">",
    umbral: 3.0,
    valor_observado: 6.73,
    severidad: "MEDIA",
    estado: "ABIERTA",
    activa: true,
    mensaje: "1 lectura(s) de temperatura anómalas (|z| máx 6.73)",
    detalle: {
      metodo: "z-score sobre la ventana 2026-10-20/22",
      limite_z: 3,
      lecturas: [
        {
          id_lectura: "641a042a-4c55-5b51-8fec-2a750f571dc4",
          fecha_hora: ISODate("2026-10-22T09:00:00.000Z"),
          temperatura: 7.9,
          z: 6.73
        }
      ]
    },
    fecha: ISODate("2026-10-22T23:15:00.000Z"),
    fecha_lectura: ISODate("2026-10-22T09:00:00.000Z"),
    ultima_ocurrencia: ISODate("2026-10-22T23:15:00.000Z"),
    ocurrencias: NumberInt(1),
    clave_dedup: "810b94b9-8c35-5cce-bd94-ce9c0a9ca625|94858666-0f1b-5d7a-8f8d-0118791421e9",
    historial_estados: [
      {
        estado: "ABIERTA",
        fecha: ISODate("2026-10-22T23:15:00.000Z"),
        id_usuario: null,
        comentario: null
      }
    ]
  }
]);

// ---------------------------------------------------------------- conversaciones (4)
cargar('conversaciones', [
  {
    _id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    tipo: "PRIVADA",
    asunto: "Demora del envío E002",
    nombre_grupo: null,
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    participantes: [
      {
        tipo: "CLIENTE",
        id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
        id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
        nombre: "Lácteos Pampeanos S.A."
      },
      {
        tipo: "OPERADOR",
        id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
        id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
        nombre: "Naviera Atlántica S.A."
      }
    ],
    ids_participantes: ["CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    creada_por: "a841cc7a-599e-59a5-9a94-aca73ccae354",
    fecha_creacion: ISODate("2026-10-21T08:30:00.000Z"),
    estado: "ABIERTA",
    cantidad_mensajes: NumberInt(6),
    ultimo_mensaje: {
      fecha: ISODate("2026-10-21T22:30:00.000Z"),
      remitente: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771",
      extracto: "Les avisamos apenas tengamos turno de atraque."
    }
  },
  {
    _id: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    tipo: "GRUPAL",
    asunto: "Excursión de temperatura contenedor MSKU0000003",
    nombre_grupo: "GT Cadena de frío E002",
    id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92",
    participantes: [
      {
        tipo: "INTERNO",
        id_entidad: "b1052e98-971b-52d6-9913-aed4e195c326",
        id_usuario: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
        nombre: "Grupo 4 Logística (plataforma)"
      },
      {
        tipo: "CLIENTE",
        id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
        id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
        nombre: "Lácteos Pampeanos S.A."
      },
      {
        tipo: "OPERADOR",
        id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
        id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
        nombre: "Naviera Atlántica S.A."
      },
      {
        tipo: "PROVEEDOR",
        id_entidad: "ec12ca06-a78a-5605-ab40-31988a417f61",
        id_usuario: "ed4790a5-f2d1-5b87-aa7a-fdfac52d73a3",
        nombre: "FrioTech Mantenimiento Reefer"
      }
    ],
    ids_participantes: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771", "PROVEEDOR:ec12ca06-a78a-5605-ab40-31988a417f61"],
    creada_por: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
    fecha_creacion: ISODate("2026-10-21T14:25:00.000Z"),
    estado: "ABIERTA",
    cantidad_mensajes: NumberInt(6),
    ultimo_mensaje: {
      fecha: ISODate("2026-10-21T18:40:00.000Z"),
      remitente: "INTERNO:b1052e98-971b-52d6-9913-aed4e195c326",
      extracto: "Registramos el incidente asociado y dejamos la alerta como r"
    }
  },
  {
    _id: "6ca73e3c-c0d2-5e52-8656-21f724089883",
    tipo: "PRIVADA",
    asunto: "Documentación aduanera E001",
    nombre_grupo: null,
    id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add",
    participantes: [
      {
        tipo: "CLIENTE",
        id_entidad: "aef2750c-1592-5ed4-b7a7-ab5771efc165",
        id_usuario: "f68c3c80-1d10-5742-a526-db58c06e1790",
        nombre: "Frigorífico del Sur S.A."
      },
      {
        tipo: "PROVEEDOR",
        id_entidad: "6c866005-e083-5a0a-b855-6f8e23aca2c3",
        id_usuario: "91b073d7-1124-59c9-912f-a7fc845c3aac",
        nombre: "Despachante Aduanero Gómez"
      }
    ],
    ids_participantes: ["CLIENTE:aef2750c-1592-5ed4-b7a7-ab5771efc165", "PROVEEDOR:6c866005-e083-5a0a-b855-6f8e23aca2c3"],
    creada_por: "f68c3c80-1d10-5742-a526-db58c06e1790",
    fecha_creacion: ISODate("2026-10-20T10:00:00.000Z"),
    estado: "ABIERTA",
    cantidad_mensajes: NumberInt(4),
    ultimo_mensaje: {
      fecha: ISODate("2026-10-20T18:00:00.000Z"),
      remitente: "PROVEEDOR:6c866005-e083-5a0a-b855-6f8e23aca2c3",
      extracto: "Recibido, queda todo presentado."
    }
  },
  {
    _id: "1ea916d1-c2c8-5c83-97b8-7a77730245dc",
    tipo: "GRUPAL",
    asunto: "Mantenimiento de sensores IoT",
    nombre_grupo: "GT Mantenimiento IoT",
    id_envio: null,
    participantes: [
      {
        tipo: "INTERNO",
        id_entidad: "b1052e98-971b-52d6-9913-aed4e195c326",
        id_usuario: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
        nombre: "Grupo 4 Logística (plataforma)"
      },
      {
        tipo: "OPERADOR",
        id_entidad: "5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd",
        id_usuario: "ce4c9cee-103c-5a27-99a4-0d7cffa87635",
        nombre: "Transportes Ruta 9 S.A."
      },
      {
        tipo: "PROVEEDOR",
        id_entidad: "899c5257-09ac-51ef-958e-a36a42ba6dc5",
        id_usuario: "527a32d4-0d2d-565f-a516-8c29040deac3",
        nombre: "IoT Sensores Patagonia"
      }
    ],
    ids_participantes: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "OPERADOR:5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd", "PROVEEDOR:899c5257-09ac-51ef-958e-a36a42ba6dc5"],
    creada_por: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
    fecha_creacion: ISODate("2026-10-22T09:00:00.000Z"),
    estado: "ABIERTA",
    cantidad_mensajes: NumberInt(3),
    ultimo_mensaje: {
      fecha: ISODate("2026-10-22T11:00:00.000Z"),
      remitente: "OPERADOR:5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd",
      extracto: "Transportes Ruta 9: podemos coordinar la visita mañana a las"
    }
  }
]);

// ---------------------------------------------------------------- mensajes (19)
cargar('mensajes', [
  {
    _id: "b84f68b0-cc26-51ce-9eec-6c2ea9f012db",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
      id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
      nombre: "Lácteos Pampeanos S.A."
    },
    destinatario: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771"
    },
    fecha_envio: ISODate("2026-10-21T08:30:00.000Z"),
    contenido: "Buenos días, nos llegó el aviso de demora del E002. ¿Cuál es la nueva fecha estimada?",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    adjunto: null
  },
  {
    _id: "dcd34e15-0d4e-5579-a19a-00973b775b06",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
      id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
      nombre: "Naviera Atlántica S.A."
    },
    destinatario: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b"
    },
    fecha_envio: ISODate("2026-10-21T08:55:00.000Z"),
    contenido: "Hola Sofía, por congestión en Santos estimamos 18 horas más. Nueva ETA: 27/10 06:00.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b"],
    adjunto: null
  },
  {
    _id: "c2d2213f-6fe2-51c4-9010-9bbab43f389b",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
      id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
      nombre: "Lácteos Pampeanos S.A."
    },
    destinatario: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771"
    },
    fecha_envio: ISODate("2026-10-21T09:10:00.000Z"),
    contenido: "¿La cadena de frío está garantizada durante la espera?",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    adjunto: null
  },
  {
    _id: "7c814cc9-1581-51af-a0ff-c473287a14b4",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
      id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
      nombre: "Naviera Atlántica S.A."
    },
    destinatario: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b"
    },
    fecha_envio: ISODate("2026-10-21T09:40:00.000Z"),
    contenido: "Sí, el contenedor sigue conectado a la toma reefer del buque.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b"],
    adjunto: null
  },
  {
    _id: "450afc08-05dc-509e-a717-c6eed4c4f38d",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
      id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
      nombre: "Lácteos Pampeanos S.A."
    },
    destinatario: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771"
    },
    fecha_envio: ISODate("2026-10-21T10:00:00.000Z"),
    contenido: "Perfecto, quedamos atentos a cualquier novedad.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    adjunto: null
  },
  {
    _id: "3e4a750d-5862-5385-9856-56d439a16510",
    id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427",
    remitente: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
      id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
      nombre: "Naviera Atlántica S.A."
    },
    destinatario: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b"
    },
    fecha_envio: ISODate("2026-10-21T22:30:00.000Z"),
    contenido: "Les avisamos apenas tengamos turno de atraque.",
    tipo: "TEXTO",
    estado: "ENVIADO",
    leido_por: [],
    adjunto: null
  },
  {
    _id: "64501967-6baf-5d96-8055-00c1a25e6985",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "INTERNO",
      id_entidad: "b1052e98-971b-52d6-9913-aed4e195c326",
      id_usuario: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
      nombre: "Grupo 4 Logística (plataforma)"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T14:25:00.000Z"),
    contenido: "Se detectó temperatura de 10.8 °C en MSKU0000003 (alerta REG-01). Abrimos este grupo de trabajo.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771", "PROVEEDOR:ec12ca06-a78a-5605-ab40-31988a417f61"],
    adjunto: null
  },
  {
    _id: "1a52c709-4c78-5c0e-af00-6347896f4b52",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "PROVEEDOR",
      id_entidad: "ec12ca06-a78a-5605-ab40-31988a417f61",
      id_usuario: "ed4790a5-f2d1-5b87-aa7a-fdfac52d73a3",
      nombre: "FrioTech Mantenimiento Reefer"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T14:55:00.000Z"),
    contenido: "FrioTech: revisamos la telemetría, parece una falla del compresor. Coordinamos con la naviera.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    adjunto: null
  },
  {
    _id: "34d881bf-44b7-5ca2-b612-e4318ee5fcd0",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "OPERADOR",
      id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771",
      id_usuario: "c5064550-9836-5ea7-907a-6a42e69a8c58",
      nombre: "Naviera Atlántica S.A."
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T15:20:00.000Z"),
    contenido: "Naviera: personal a bordo reinició el equipo de frío, informamos en 1 hora.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "PROVEEDOR:ec12ca06-a78a-5605-ab40-31988a417f61"],
    adjunto: null
  },
  {
    _id: "1daa00d2-6b8c-5404-999d-22dde4c25fd2",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b",
      id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354",
      nombre: "Lácteos Pampeanos S.A."
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T15:45:00.000Z"),
    contenido: "Necesitamos saber si la mercadería sigue apta. Adjuntamos certificado sanitario de origen.",
    tipo: "ARCHIVO",
    estado: "LEIDO",
    leido_por: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771", "PROVEEDOR:ec12ca06-a78a-5605-ab40-31988a417f61"],
    adjunto: {
      nombre: "certificado_sanitario_E002.pdf",
      tamano_kb: 182,
      url: "s3://grupo4-docs/E002/certificado_sanitario.pdf"
    }
  },
  {
    _id: "029c2261-5745-5ce7-bd12-ea18c6aaaf8a",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "PROVEEDOR",
      id_entidad: "ec12ca06-a78a-5605-ab40-31988a417f61",
      id_usuario: "ed4790a5-f2d1-5b87-aa7a-fdfac52d73a3",
      nombre: "FrioTech Mantenimiento Reefer"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T18:25:00.000Z"),
    contenido: "Temperatura nuevamente en 4.6 °C desde las 18:00. Recomendamos inspección en destino.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"],
    adjunto: null
  },
  {
    _id: "e3c8ea3b-4bb7-5fec-8b03-5e02bd16bbc9",
    id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e",
    remitente: {
      tipo: "INTERNO",
      id_entidad: "b1052e98-971b-52d6-9913-aed4e195c326",
      id_usuario: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
      nombre: "Grupo 4 Logística (plataforma)"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-21T18:40:00.000Z"),
    contenido: "Registramos el incidente asociado y dejamos la alerta como reconocida.",
    tipo: "TEXTO",
    estado: "ENVIADO",
    leido_por: [],
    adjunto: null
  },
  {
    _id: "5769958f-4943-5dab-8dba-f587306af137",
    id_conversacion: "6ca73e3c-c0d2-5e52-8656-21f724089883",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "aef2750c-1592-5ed4-b7a7-ab5771efc165",
      id_usuario: "f68c3c80-1d10-5742-a526-db58c06e1790",
      nombre: "Frigorífico del Sur S.A."
    },
    destinatario: {
      tipo: "PROVEEDOR",
      id_entidad: "6c866005-e083-5a0a-b855-6f8e23aca2c3"
    },
    fecha_envio: ISODate("2026-10-20T10:00:00.000Z"),
    contenido: "Ana, ¿ya está lista la documentación para el despacho del E001 en Santos?",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["PROVEEDOR:6c866005-e083-5a0a-b855-6f8e23aca2c3"],
    adjunto: null
  },
  {
    _id: "fb65c86a-8f98-58f1-ba3d-a6cc172339b4",
    id_conversacion: "6ca73e3c-c0d2-5e52-8656-21f724089883",
    remitente: {
      tipo: "PROVEEDOR",
      id_entidad: "6c866005-e083-5a0a-b855-6f8e23aca2c3",
      id_usuario: "91b073d7-1124-59c9-912f-a7fc845c3aac",
      nombre: "Despachante Aduanero Gómez"
    },
    destinatario: {
      tipo: "CLIENTE",
      id_entidad: "aef2750c-1592-5ed4-b7a7-ab5771efc165"
    },
    fecha_envio: ISODate("2026-10-20T10:45:00.000Z"),
    contenido: "Sí, factura, packing list y certificado sanitario cargados. Falta el certificado de origen.",
    tipo: "ARCHIVO",
    estado: "LEIDO",
    leido_por: ["CLIENTE:aef2750c-1592-5ed4-b7a7-ab5771efc165"],
    adjunto: {
      nombre: "certificado_sanitario_E002.pdf",
      tamano_kb: 182,
      url: "s3://grupo4-docs/E002/certificado_sanitario.pdf"
    }
  },
  {
    _id: "03d113f7-fc61-5678-b4ba-c5edb5bf2fba",
    id_conversacion: "6ca73e3c-c0d2-5e52-8656-21f724089883",
    remitente: {
      tipo: "CLIENTE",
      id_entidad: "aef2750c-1592-5ed4-b7a7-ab5771efc165",
      id_usuario: "f68c3c80-1d10-5742-a526-db58c06e1790",
      nombre: "Frigorífico del Sur S.A."
    },
    destinatario: {
      tipo: "PROVEEDOR",
      id_entidad: "6c866005-e083-5a0a-b855-6f8e23aca2c3"
    },
    fecha_envio: ISODate("2026-10-20T11:00:00.000Z"),
    contenido: "Lo enviamos hoy antes de las 18.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["PROVEEDOR:6c866005-e083-5a0a-b855-6f8e23aca2c3"],
    adjunto: null
  },
  {
    _id: "0aef37d6-71a1-5b49-a2ee-67c66c612af3",
    id_conversacion: "6ca73e3c-c0d2-5e52-8656-21f724089883",
    remitente: {
      tipo: "PROVEEDOR",
      id_entidad: "6c866005-e083-5a0a-b855-6f8e23aca2c3",
      id_usuario: "91b073d7-1124-59c9-912f-a7fc845c3aac",
      nombre: "Despachante Aduanero Gómez"
    },
    destinatario: {
      tipo: "CLIENTE",
      id_entidad: "aef2750c-1592-5ed4-b7a7-ab5771efc165"
    },
    fecha_envio: ISODate("2026-10-20T18:00:00.000Z"),
    contenido: "Recibido, queda todo presentado.",
    tipo: "TEXTO",
    estado: "ENVIADO",
    leido_por: [],
    adjunto: null
  },
  {
    _id: "41cba1e2-62d2-542e-939b-d5daa745c577",
    id_conversacion: "1ea916d1-c2c8-5c83-97b8-7a77730245dc",
    remitente: {
      tipo: "INTERNO",
      id_entidad: "b1052e98-971b-52d6-9913-aed4e195c326",
      id_usuario: "aa621106-4bb0-59d5-aafd-48ca46e584cf",
      nombre: "Grupo 4 Logística (plataforma)"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-22T09:00:00.000Z"),
    contenido: "El sensor SEN-012 tiene batería baja y SEN-007 dejó de reportar. ¿Pueden coordinar el reemplazo?",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["OPERADOR:5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd", "PROVEEDOR:899c5257-09ac-51ef-958e-a36a42ba6dc5"],
    adjunto: null
  },
  {
    _id: "a1cea642-d277-5151-a076-21626598bf6f",
    id_conversacion: "1ea916d1-c2c8-5c83-97b8-7a77730245dc",
    remitente: {
      tipo: "PROVEEDOR",
      id_entidad: "899c5257-09ac-51ef-958e-a36a42ba6dc5",
      id_usuario: "527a32d4-0d2d-565f-a516-8c29040deac3",
      nombre: "IoT Sensores Patagonia"
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-22T09:50:00.000Z"),
    contenido: "IoT Sensores: tenemos repuestos, necesitamos acceso al contenedor en destino.",
    tipo: "TEXTO",
    estado: "LEIDO",
    leido_por: ["INTERNO:b1052e98-971b-52d6-9913-aed4e195c326", "OPERADOR:5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd"],
    adjunto: null
  },
  {
    _id: "ca588638-77ef-5322-a660-5773561ecf0b",
    id_conversacion: "1ea916d1-c2c8-5c83-97b8-7a77730245dc",
    remitente: {
      tipo: "OPERADOR",
      id_entidad: "5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd",
      id_usuario: "ce4c9cee-103c-5a27-99a4-0d7cffa87635",
      nombre: "Transportes Ruta 9 S.A."
    },
    destinatario: null,
    fecha_envio: ISODate("2026-10-22T11:00:00.000Z"),
    contenido: "Transportes Ruta 9: podemos coordinar la visita mañana a las 10.",
    tipo: "TEXTO",
    estado: "ENVIADO",
    leido_por: [],
    adjunto: null
  }
]);

// ---------------------------------------------------------------- reportes_generados (3)
cargar('reportes_generados', [
  {
    _id: "44586de9-83d5-5dca-bc79-006b44e3a324",
    id_solicitud: "7ef4e17a-4ba4-591f-a639-75738dd3b8a5",
    id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
    solicitado_por: "a841cc7a-599e-59a5-9a94-aca73ccae354",
    tipo: "ALERTAS_POR_ESTADO",
    parametros: {
      id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
      desde: "2026-10-20",
      hasta: "2026-10-22"
    },
    formato: "JSON",
    estado: "GENERADO",
    resultado: {
      por_estado: {
        RECONOCIDA: 1,
        ABIERTA: 1
      },
      total: 2
    },
    ubicacion: null,
    generado_en: ISODate("2026-10-22T23:40:00.000Z")
  },
  {
    _id: "04240426-0b1f-5371-83d5-2e7a3bbd9be8",
    id_solicitud: "f4142d5a-d2d1-594c-ac93-b63220780bf2",
    id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
    solicitado_por: "a841cc7a-599e-59a5-9a94-aca73ccae354",
    tipo: "TELEMETRIA_CONTENEDOR_DIA",
    parametros: {
      id_contenedor: "96f3d043-00e2-5c9e-8a9d-d0810f2f5995",
      dia: "2026-10-21"
    },
    formato: "JSON",
    estado: "GENERADO",
    resultado: {
      sensores: [
        {
          sensor: "SEN-003",
          lecturas: 24,
          temp_min: 4.15,
          temp_max: 11.6,
          temp_promedio: 5.82
        },
        {
          sensor: "SEN-028",
          lecturas: 24,
          temp_min: 4.04,
          temp_max: 5.16,
          temp_promedio: 4.61
        }
      ]
    },
    ubicacion: null,
    generado_en: ISODate("2026-10-22T23:41:00.000Z")
  },
  {
    _id: "e7405043-eeae-5701-a89b-f7da2dec0d0b",
    id_solicitud: "f107a396-d1c6-51cb-a696-c7962e5a457a",
    id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b",
    solicitado_por: "6810f791-1878-5de4-b0f6-7e63d996d4ac",
    tipo: "HISTORIAL_EVENTOS_ENVIO",
    parametros: {
      id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92"
    },
    formato: "CSV",
    estado: "GENERADO",
    resultado: null,
    ubicacion: "s3://grupo4-reportes/SOL-003/historial_E002.csv",
    generado_en: ISODate("2026-10-22T23:42:00.000Z")
  }
]);

print('Datos de prueba de MongoDB cargados.');
