// Gera app/src/main/java/com/example/pocmapa/data/RoadRoutes.kt com trajetos que seguem
// as ruas de Araraquara-SP, calculados pelo roteador público OSRM (dados do OpenStreetMap).
//
// Uso (na pasta poc-e5-mapa/lucas):  node tools/gerar-rotas.mjs
//
// A POC não chama o OSRM em tempo de execução: os trajetos ficam gravados no código e o
// simulador os percorre como uma sequência pré-determinada. Geração de rotas no app é a Entrega 6.

import { writeFileSync } from "node:fs";

const OSRM = "https://router.project-osrm.org";

// Pontos de referência [lat, lon]. Cada rota é fechada (volta ao início) para o loop da simulação.
const DISTRIBUTION_CENTER = [-21.7980, -48.1830];

const routes = {
    // Fiorino: CD -> entrega -> CD
    fiorinoOut: [DISTRIBUTION_CENTER, [-21.7900, -48.1900], [-21.7820, -48.1760]],
    fiorinoBack: [[-21.7820, -48.1760], DISTRIBUTION_CENTER],
    // Sprinter: circuito com duas paradas
    sprinterLeg1: [[-21.8030, -48.1700], [-21.7950, -48.1650]],
    sprinterLeg2: [[-21.7950, -48.1650], [-21.8000, -48.1600]],
    sprinterLeg3: [[-21.8000, -48.1600], [-21.8030, -48.1700]],
};

const parked = { accelo: [-21.7990, -48.1815] };

const toOsrm = (points) => points.map(([lat, lon]) => `${lon},${lat}`).join(";");

async function route(points) {
    const res = await fetch(`${OSRM}/route/v1/driving/${toOsrm(points)}?overview=full&geometries=geojson`);
    const json = await res.json();
    if (json.code !== "Ok") throw new Error(`OSRM: ${json.code} ${json.message ?? ""}`);
    return json.routes[0].geometry.coordinates.map(([lon, lat]) => [lat, lon]);
}

async function nearest([lat, lon]) {
    const res = await fetch(`${OSRM}/nearest/v1/driving/${lon},${lat}`);
    const json = await res.json();
    const [nLon, nLat] = json.waypoints[0].location;
    return [nLat, nLon];
}

const fmt = ([lat, lon]) => `${lat.toFixed(5)} to ${lon.toFixed(5)}`;
const list = (points) => `listOf(\n${points.map((p) => `        ${fmt(p)}`).join(",\n")}\n    )`;

let body = "";
for (const [name, points] of Object.entries(routes)) {
    const path = await route(points);
    body += `\n    val ${name} = ${list(path)}\n`;
    console.log(`${name}: ${path.length} pontos`);
}
for (const [name, point] of Object.entries(parked)) {
    body += `\n    val ${name}Parked = ${fmt(await nearest(point))}\n`;
}

const kotlin = `package com.example.pocmapa.data

// Arquivo gerado por tools/gerar-rotas.mjs — não editar à mão.
// Trajetos calculados pelo OSRM sobre dados do OpenStreetMap (© colaboradores do OpenStreetMap).
object RoadRoutes {${body}}
`;

writeFileSync("app/src/main/java/com/example/pocmapa/data/RoadRoutes.kt", kotlin);
console.log("RoadRoutes.kt gerado.");
