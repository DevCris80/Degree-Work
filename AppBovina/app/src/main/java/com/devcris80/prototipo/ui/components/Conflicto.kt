package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** La marca "en conflicto" de una fila con Siblings sin resolver (RN-17). */
@Composable
fun MarcaEnConflicto(modifier: Modifier = Modifier) {
    PillBadge(
        texto = "En conflicto",
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

/** Aviso que se toca para abrir la pantalla de resolver el Conflicto. */
@Composable
fun BannerConflicto(
    texto: String,
    onResolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BovinaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onResolver,
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Resolver ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BannerConflictoPreview() {
    Prototipo1Theme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MarcaEnConflicto()
            BannerConflicto(texto = "Dos personas editaron este animal a la vez.", onResolver = {})
        }
    }
}
