package br.edu.ifsp.ifrota.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.ui.theme.Amber100
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Amber800
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green100
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface3
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import kotlin.math.ceil

val DeliveryStatus.label: String
    get() = when (this) {
        DeliveryStatus.PENDING -> "Pendente"
        DeliveryStatus.ACTIVE -> "Em andamento"
        DeliveryStatus.DONE -> "Entregue"
    }

/** Fundo e cor do número da entrega, como nos selos de ordem do protótipo. */
private fun DeliveryStatus.orderColors(): Pair<Color, Color> = when (this) {
    DeliveryStatus.PENDING -> Amber100 to Amber500
    DeliveryStatus.ACTIVE -> Green100 to Green600
    DeliveryStatus.DONE -> Surface3 to Text3
}

/** Selo de status (ponto + rótulo). */
@Composable
fun DeliveryStatusBadge(status: DeliveryStatus, modifier: Modifier = Modifier) {
    val (background, text, dot) = when (status) {
        DeliveryStatus.PENDING -> Triple(Amber100, Amber800, Amber500)
        DeliveryStatus.ACTIVE -> Triple(Green100, Green900, Green600)
        DeliveryStatus.DONE -> Triple(Surface3, Text2, Text3)
    }
    Row(
        modifier = modifier
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(dot, CircleShape))
        Text(text = status.label, style = MaterialTheme.typography.labelSmall, color = text)
    }
}

/** Número da entrega na sequência, colorido pelo status. */
@Composable
fun DeliveryOrderBadge(
    order: Int,
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 16.dp
) {
    val (background, content) = status.orderColors()
    Box(
        modifier = modifier.size(size).background(background, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = order.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.36f).sp
            ),
            color = content
        )
    }
}

/** Etiqueta pequena com informação da entrega (tempo, distância, pacote). */
@Composable
fun MetaChip(text: String, highlighted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal
        ),
        color = if (highlighted) Green600 else Text3,
        modifier = Modifier
            .background(if (highlighted) Green100.copy(alpha = 0.5f) else Surface3, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Observação do destinatário em destaque âmbar. */
@Composable
fun DeliveryNote(note: String, modifier: Modifier = Modifier) {
    Text(
        text = note,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = Amber800,
        modifier = modifier
            .background(Amber100, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Confirmação antes de marcar a entrega como feita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeliverySheet(
    delivery: Delivery,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(BorderSubtle, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(Green600, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text("Confirmar entrega?", style = MaterialTheme.typography.headlineSmall, color = Text1)
            Text(
                text = buildAnnotatedString {
                    append("Marcar como entregue para ")
                    withStyle(SpanStyle(color = Text2, fontWeight = FontWeight.SemiBold)) { append(delivery.recipient) }
                    append("?")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Text3,
                textAlign = TextAlign.Center
            )
            PrimaryButton(text = "Confirmar entrega", onClick = onConfirm, modifier = Modifier.padding(top = 4.dp))
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        }
    }
}

fun formatDistance(meters: Double): String =
    if (meters >= 1_000) "%.1f km".format(meters / 1_000).replace('.', ',')
    else "${(meters / 10).toInt() * 10} m"

fun formatDuration(seconds: Double): String =
    "${ceil(seconds / 60).toInt().coerceAtLeast(1)} min"
