# PoC — Perfil administrador e atribuição de entregas

Prova de conceito **só com o que é novo** no IFRota nesta etapa. Tudo aqui foi extraído do
app principal (`app/IFRota`), com o pacote `br.edu.ifsp.ifrota.poc_admin_entregas`. O app
principal é a versão completa, integrada às rotas e ao mapa; esta PoC isola e demonstra a
feature.

## O que a PoC demonstra

1. **Papel de administrador** lido do servidor (campo `role` em `drivers/{uid}`), nunca do banco local.
2. **Área administrativa**: lista de motoristas cadastrados → detalhe do motorista → **Criar entregas**.
3. **Geração de entregas de demonstração** em Araraquara-SP a partir de um catálogo local de
   endereços reais (rua e bairro conferidos por reverse geocoding; número do imóvel fictício).
4. **Rota viária por entrega**: o trecho (da parada anterior, ou do CD, até a entrega) é calculado
   pelo OSRM na criação e gravado na própria entrega como polyline.
5. **Persistência**: Firestore (criação em transação) + Room no aparelho do motorista, com
   sincronização só das entregas dele.
6. **Visão do motorista**: ele vê apenas as próprias entregas, na ordem da fila, e conclui a da vez.
   (No app principal essa fila alimenta o mapa e a navegação; aqui ela é só listada, com a distância
   de cada trecho.)
7. **Regras do Firestore** (`firestore.rules`) que fazem a autorização de verdade.

## Onde está cada coisa

| Parte | Arquivos |
|---|---|
| Papel | `core/UserRole.kt` |
| Operações do admin | `data/admin/AdminRepository.kt` |
| Geração e planejamento | `data/delivery/` (`DemoAddressCatalog`, `DemoDeliveryGenerator`, `DeliveryPlanner`, `DeliveryBatchBuilder`) |
| Rotas | `data/routing/` (`OsrmRouteProvider`, `PolylineCodec`, `RouteMetrics`) |
| Persistência | `data/local/` (`DeliveryEntity`, `DeliveryDao`, `AppDatabase`), `data/delivery/DeliveryDocument.kt`, `data/repository/`, `data/sync/` |
| Telas | `ui/PocApp.kt` (login + papel), `ui/AdminScreens.kt`, `ui/DriverScreen.kt` |

## Como rodar

1. **Registrar o app no Firebase.** No console do projeto `ifrota-app`, adicione um app Android com o
   pacote `br.edu.ifsp.ifrota.poc_admin_entregas`, baixe o `google-services.json` e coloque em
   `app/google-services.json` (o arquivo é ignorado pelo git).
2. **Regras do Firestore.** O arquivo `firestore.rules` é o mesmo já publicado para o app principal
   (`firebase deploy --only firestore:rules`, a partir de `app/IFRota`). A PoC usa o mesmo projeto e as
   mesmas coleções (`drivers`, `deliveries`).
3. **Contas.** Use contas já criadas pelo app principal. Para tornar uma conta administradora, adicione
   no documento `drivers/{uid}` o campo `role` (string) com o valor `admin`.
4. Abra a pasta no Android Studio e execute em um emulador/aparelho com internet.

## Testes

```
./gradlew :app:testDebugUnitTest
```

Unitários: catálogo e gerador, planejamento do lote (continuar a fila, não repetir endereços
pendentes, lote repetido), `PolylineCodec`, `RouteMetrics`, `UserRole` e `AdminViewModel`
(clique duplo, nova tentativa, mensagens de erro). `DeliveryDaoTest` (androidTest) precisa de
emulador ou aparelho.

## Limitações

- Não há ordenação/otimização de pontos: a ordem é a da fila e cada trecho é traçado entre paradas
  consecutivas.
- Criar entregas exige o OSRM público online. O motorista não depende dele (o trecho fica gravado).
- A PoC não tem cadastro de contas nem mapa; ambos existem no app principal.
