package br.edu.ifsp.ifrota.data.delivery

import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity

/**
 * Decide, a partir das entregas que o motorista já tem, o que um novo lote deve criar. É a
 * parte das regras de negócio da criação que não depende de rede nem de banco.
 */
object DeliveryPlanner {

    sealed interface Plan {
        /** O lote já existe para este motorista: nada a criar. */
        data class AlreadyCreated(val count: Int) : Plan

        /** Todos os endereços de demonstração já estão pendentes para o motorista. */
        data object NoAddressesAvailable : Plan

        data class Ready(
            val stops: List<DemoStop>,
            /** Onde o primeiro trecho começa: a última parada da fila, ou o CD se a fila estiver vazia. */
            val start: Pair<Double, Double>,
            /** Posição na fila da primeira entrega do lote. */
            val firstOrder: Int
        ) : Plan
    }

    fun plan(existing: List<DeliveryEntity>, batchId: String, quantity: Int): Plan {
        val queue = existing.sortedWith(compareBy({ it.orderNumber }, { it.createdAt }))

        val alreadyInBatch = queue.count { DeliveryBatchBuilder.belongsToBatch(it.id, batchId) }
        if (alreadyInBatch > 0) return Plan.AlreadyCreated(alreadyInBatch)

        // Um endereço ainda pendente não é repetido; os já entregues podem voltar.
        val pendingAddresses = DemoDeliveryGenerator.addressIdsAt(
            queue.filter { !it.isDone }.map { it.latitude to it.longitude }
        )
        val stops = DemoDeliveryGenerator.generate(batchId, quantity, pendingAddresses)
        if (stops.isEmpty()) return Plan.NoAddressesAvailable

        return Plan.Ready(
            stops = stops,
            start = queue.lastOrNull()?.let { it.latitude to it.longitude } ?: DistributionCenter.position,
            firstOrder = (queue.maxOfOrNull { it.orderNumber } ?: 0) + 1
        )
    }
}
