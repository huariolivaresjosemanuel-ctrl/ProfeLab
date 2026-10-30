package pe.profelab.core.content

/** Categories describe content that actually exists. They do not activate empty modules. */
object WorkshopCategories {
    val labels=linkedMapOf("didactic" to "Material didáctico","experiment" to "Experimento","creative" to "Creación libre",
        "dance" to "Danza","music" to "Música","heritage" to "Patrimonio","theater" to "Teatro","psychopedagogy" to "Fundamentos pedagógicos")
    val supported get()=labels.keys
}
