package pe.profelab.feature.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import pe.profelab.core.ui.*

@Composable
fun AboutScreen(back: ()->Unit) {
    Page {
        PageTitle("Acerca de ProfeLab",back=back)
        InfoCard("ProfeLab · 0.1.0","Organización local para estudiantes y docentes de Educación Primaria. Esta primera versión incluye Inicio, tareas, perfil y consulta de referencias.")
        InfoCard("Independiente del MINEDU","Los títulos y enlaces de Biblioteca proceden del catálogo entregado para este proyecto. No hay PDFs oficiales incluidos. Los materiales y trabajos del usuario no tienen aprobación oficial.")
        InfoCard("Créditos de software","AndroidX, Compose, Room y WorkManager: Apache 2.0. Kotlin y kotlinx.coroutines: Apache 2.0. Iconos Material: Apache 2.0. Ilustraciones de libro y planta reconstruidas en código para ProfeLab. Avisos completos en la entrega de fuentes.")
        Text("No se incluyen código, firmas ni recursos de ScanPro o DocReader. La selección de texto PDF, las anotaciones y el OCR están pendientes de implementación y pruebas.")
    }
}
