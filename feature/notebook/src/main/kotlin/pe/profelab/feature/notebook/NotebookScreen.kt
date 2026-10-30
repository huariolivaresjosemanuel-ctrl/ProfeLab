package pe.profelab.feature.notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import pe.profelab.core.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookScreen(initialSection: String="Apuntes") {
    var section by rememberSaveable(initialSection) { mutableStateOf(initialSection) }
    Page {
        PageTitle("Cuaderno","Notas, prácticas y reflexiones")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Apuntes","Prácticas","Ideas").forEach { label -> FilterChip(selected=section==label,onClick={section=label},label={Text(label)}) }
        }
        InfoCard("$section · sin registros","El cuaderno y el portafolio estarán disponibles en una próxima versión. La cámara, OCR y grabación de voz todavía no están habilitados.")
    }
}
