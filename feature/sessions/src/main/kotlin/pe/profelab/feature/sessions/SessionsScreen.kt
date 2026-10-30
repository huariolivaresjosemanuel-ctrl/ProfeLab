package pe.profelab.feature.sessions

import androidx.compose.runtime.Composable
import pe.profelab.core.ui.*

@Composable
fun SessionsScreen() {
    Page {
        PageTitle("Sesiones","Planifica tu clase")
        InfoCard("Aún no hay sesiones","El editor de sesiones de 1.º a 6.º de Primaria estará disponible en una próxima versión. Incluirá propósito, evidencia, criterios y secuencia de clase.")
    }
}
