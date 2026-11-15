package pe.profelab.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** App owns the private-file boundary; features request an in-app review by opaque ID. */
val LocalReviewPrivateEvidence=staticCompositionLocalOf<((String)->Unit)?> { null }
