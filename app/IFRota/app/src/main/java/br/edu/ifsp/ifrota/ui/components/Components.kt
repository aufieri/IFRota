package br.edu.ifsp.ifrota.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text3

/** Iniciais exibidas no avatar do entregador. */
fun DriverEntity?.initials(fallbackEmail: String = ""): String {
    val source = this?.name?.trim().orEmpty().ifBlank { fallbackEmail.substringBefore('@') }
    val parts = source.split(" ", ".", "_").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

/**
 * Cabeçalho verde das telas internas, encostado na status bar como no protótipo.
 * O conteúdo abaixo dele costuma subir 12dp para sobrepor a primeira carta.
 */
@Composable
fun GreenHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Green600)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        content = content
    )
}

/** Rótulo de seção em caixa alta, cinza-esverdeado. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Text3
        )
        trailing?.invoke()
    }
}

/** Carta branca de cantos 24dp com borda sutil — a base de quase toda a interface. */
@Composable
fun IFRotaCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderSubtle,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(24.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(24.dp)),
        content = content
    )
}

/** Avatar quadrado com gradiente verde e as iniciais do entregador. */
@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(listOf(Green900, Green600)),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}

/** Bloco de estatística usado nas linhas de resumo. */
@Composable
fun RowScope.StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Surface1,
    valueColor: Color = Green600,
    labelColor: Color = Text3,
    bordered: Boolean = true
) {
    Column(
        modifier = modifier
            .weight(1f)
            .background(background, RoundedCornerShape(16.dp))
            .then(
                if (bordered) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            textAlign = TextAlign.Center
        )
    }
}

/** Estado vazio: ícone grande, título e explicação, opcionalmente com uma ação. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.displaySmall)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Text1,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Text3,
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}
