package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import pe.profelab.core.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TallerScreen() {
    var section by rememberSaveable { mutableStateOf("Materiales") }
    Page {
        PageTitle("Mi taller","Crea materiales didácticos, paso a paso")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Materiales","Experimentos","Mis ideas").forEach { label -> FilterChip(selected=section==label,onClick={section=label},label={Text(label)}) }
        }
        InfoCard(section,when(section) {
            "Materiales" -> "El catálogo, las guías y el inventario estarán disponibles en una próxima versión. Aún no hay materiales guardados."
            "Experimentos" -> "Las propuestas necesitan revisión pedagógica y de seguridad antes de poder usarse. Todavía no hay experimentos disponibles."
            else -> "El editor de tus creaciones estará disponible en una próxima versión. Tus futuras ideas se guardarán aparte de las plantillas."
        })
    }
}
