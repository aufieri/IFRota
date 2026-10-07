// Gera app/src/main/java/br/edu/ifsp/ifrota/data/simulation/DeliveryRoutes.kt com os trechos
// da rota de entregas do dia, seguindo as ruas de Araraquara-SP, calculados pelo roteador
// público OSRM (dados do OpenStreetMap).
//
// Uso (na pasta app/IFRota):  node tools/gerar-rotas-entregas.mjs
//
// O app não chama o OSRM em tempo de execução: os trechos ficam gravados no código e o
// simulador os percorre como uma sequência pré-determinada.

import { writeFileSync } from "node:fs";

const OSRM = "https://router.project-osrm.org";

// Pontos [lat, lon] já ajustados à rua mais próxima. O CD é o IFSP Câmpus Araraquara e as
// paradas estão em ruas reais (nome e bairro conferidos por reverse geocoding no Nominatim).
const DISTRIBUTION_CENTER = [-21.78430, -48.21055];
const STOPS = [
    [-21.77637, -48.19368], // Rua Imaculada Conceição — Quinta das Laranjeiras
    [-21.78884, -48.18405], // Avenida Prudente de Morais — Vila Santana
    [-21.79608, -48.17004], // Rua Eloy Rodrigues — Vila Melhado
    [-21.80493, -48.17971], // Rua Pedro Álvares Cabral — Parque do Carmo
];

const toOsrm = (points) => points.map(([lat, lon]) => `${lon},${lat}`).join(";");

async function route(from, to) {
    const res = await fetch(`${OSRM}/route/v1/driving/${toOsrm([from, to])}?overview=full&geometries=geojson`);
    const json = await res.json();
    if (json.code !== "Ok") throw new Error(`OSRM: ${json.code} ${json.message ?? ""}`);
    return json.routes[0].geometry.coordinates.map(([lon, lat]) => [lat, lon]);
}

const fmt = ([lat, lon]) => `${lat.toFixed(5)} to ${lon.toFixed(5)}`;

const legs = [];
let from = DISTRIBUTION_CENTER;
for (const stop of STOPS) {
    legs.push(await route(from, stop));
    from = stop;
}

const out = `package br.edu.ifsp.ifrota.data.simulation

// Arquivo gerado por tools/gerar-rotas-entregas.mjs — não editar à mão.
// Trajetos calculados pelo OSRM sobre dados do OpenStreetMap (© colaboradores do OpenStreetMap).
object DeliveryRoutes {
    /** IFSP Câmpus Araraquara. */
    val distributionCenter = ${fmt(DISTRIBUTION_CENTER)}

    /** Trecho i: da parada anterior (ou do CD) até a parada i, seguindo as ruas. */
    val legs = listOf(
${legs.map((leg) => `        listOf(\n${leg.map((p) => `            ${fmt(p)}`).join(",\n")}\n        )`).join(",\n")}
    )
}
`;

const path = "app/src/main/java/br/edu/ifsp/ifrota/data/simulation/DeliveryRoutes.kt";
writeFileSync(path, out);
console.log(`${path}: ${legs.map((l) => l.length).join(" + ")} pontos`);
