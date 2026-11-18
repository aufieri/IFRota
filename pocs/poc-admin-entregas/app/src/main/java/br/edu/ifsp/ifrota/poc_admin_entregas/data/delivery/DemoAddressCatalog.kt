package br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery

/**
 * Endereço de demonstração em Araraquara-SP. A rua e o bairro são reais e a coordenada
 * está sobre a própria rua: cada ponto foi encaixado na via pelo OSRM e conferido por
 * reverse geocoding no Nominatim (OpenStreetMap), que devolveu esta mesma rua e este bairro.
 * O número do imóvel NÃO é verificado — é sorteado na geração (ver [DemoDeliveryGenerator]).
 */
data class DemoAddress(
    val id: String,
    val street: String,
    val neighborhood: String,
    /** Região da cidade, usada para espalhar as entregas por áreas diferentes. */
    val zone: String,
    val latitude: Double,
    val longitude: Double
)

object DemoAddressCatalog {
    const val CITY = "Araraquara - SP"

    val all: List<DemoAddress> = listOf(
        DemoAddress("imaculada-conceicao", "Rua Imaculada Conceição", "Quinta das Laranjeiras", "Noroeste", -21.77637, -48.19368),
        DemoAddress("prudente-de-morais", "Avenida Prudente de Morais", "Vila Santana", "Centro", -21.78884, -48.18405),
        DemoAddress("eloy-rodrigues", "Rua Eloy Rodrigues", "Vila Melhado", "Leste", -21.79608, -48.17004),
        DemoAddress("pedro-alvares-cabral", "Rua Pedro Álvares Cabral", "Parque do Carmo", "Sul", -21.80493, -48.17971),
        DemoAddress("bento-de-abreu", "Avenida Bento de Abreu", "Vila Ferroviária", "Norte", -21.77284, -48.17289),
        DemoAddress("voluntarios-da-patria", "Rua Voluntários da Pátria", "Vila Melhado", "Sul", -21.80505, -48.17421),
        DemoAddress("avenida-espanha", "Avenida Espanha", "Vila Ferroviária", "Centro", -21.79124, -48.17318),
        DemoAddress("avenida-brasil", "Avenida Brasil", "Jardim Salto Grande II", "Sudoeste", -21.79793, -48.18831),
        DemoAddress("padre-duarte", "Rua Padre Duarte", "Jardim Dom Pedro I", "Norte", -21.77351, -48.18347),
        DemoAddress("bandeirantes", "Avenida Bandeirantes", "Vila Santana", "Oeste", -21.79000, -48.19332),
        DemoAddress("djalma-dutra", "Avenida Djalma Dutra", "Jardim Salto Grande II", "Sudoeste", -21.80164, -48.18599),
        DemoAddress("sao-bento", "Rua São Bento", "Vila Ferroviária", "Centro", -21.78852, -48.17660),
        DemoAddress("antonio-prado", "Rua Antônio Prado", "Vila Melhado", "Leste", -21.79770, -48.17110),
        DemoAddress("nove-de-julho", "Rua Nove de Julho", "Jardim Dom Pedro I", "Norte", -21.77203, -48.18017),
        DemoAddress("carlos-gomes", "Rua Carlos Gomes", "Vila Melhado", "Centro", -21.79396, -48.17898),
        DemoAddress("romulo-lupo", "Avenida Rômulo Lupo", "Jardim Universal", "Oeste", -21.79315, -48.20660),
        DemoAddress("carlos-batista-magalhaes", "Avenida Carlos Batista Magalhães", "Jardim Brasil", "Leste", -21.78783, -48.16297),
        DemoAddress("leopoldo-silva", "Avenida Leopoldo Silva", "Jardim Paulistano", "Nordeste", -21.77529, -48.15987),
        DemoAddress("jose-marques-pinheiro-filho", "Rua José Marques Pinheiro Filho", "Vila Harmonia", "Norte", -21.76699, -48.17401),
        DemoAddress("edmundo-vicentini", "Avenida Edmundo Vicentini", "Jardim Maria Luiza I", "Norte", -21.74112, -48.19369),
        DemoAddress("albert-einstein", "Avenida Doutor Albert Einstein", "Parque São Paulo", "Leste", -21.78108, -48.13064)
    )
}
