package pe.profelab.core.model.education

import java.net.URLEncoder

/** Offline, deterministic authorization. No delivery/receipt claims and no networking. */
object EducationFamilyPolicy {
    fun authorizationBlock(authorized:Boolean,verified:Boolean,scope:String,validFrom:Long,validUntil:Long,revokedAt:Long?,archived:Boolean,now:Long):String? = when {
        archived -> "El contacto está archivado."
        revokedAt != null -> "La autorización fue revocada."
        !authorized || !verified -> "Falta verificar la autorización del contacto."
        scope != "LEARNING_REPORT" -> "La autorización no incluye informes de aprendizaje."
        validUntil < validFrom || now < validFrom || now > validUntil -> "La autorización no está vigente."
        else -> null
    }
    fun validPhone(value:String):Boolean = value.isBlank() || (value.length<=30 && value.matches(Regex("\\+?[0-9][0-9 ()-]{5,28}")) && value.filter(Char::isDigit).length in 6..15)
    fun validEmail(value:String):Boolean = value.isBlank() || (value.length<=254 && value.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,}")))
    fun safeBody(value:String):Boolean = value.isNotBlank() && value.length<=10000 && value.none {it.code<32 && it!='\n' && it!='\t'}
    private fun encode(value:String)=URLEncoder.encode(value,"UTF-8").replace("+","%20")
    fun externalUri(channel:String,phone:String,email:String,body:String):String {
        require(safeBody(body)){"Revisa el texto del informe."}
        require(validPhone(phone) && validEmail(email)){"Revisa el teléfono o correo individual."}
        val digits=phone.filter(Char::isDigit)
        return when(channel){
            "WHATSAPP" -> {require(digits.isNotBlank()){ "Falta un teléfono internacional para WhatsApp." };"https://wa.me/$digits?text=${encode(body)}"}
            "SMS" -> {require(digits.isNotBlank()){ "Falta un teléfono para SMS." };"smsto:${if(phone.startsWith("+"))"+"else ""}$digits?body=${encode(body)}"}
            "EMAIL" -> {require(email.isNotBlank()){ "Falta un correo individual." };"mailto:${encode(email)}?subject=${encode("Informe de aprendizaje")}&body=${encode(body)}"}
            else -> error("Canal no compatible.")
        }
    }
}
