package pe.profelab.core.model

/** Pre-parser resource guard. Actual JSON syntax and field validation remain the caller's job. */
object JsonLimits {
    fun requireBounded(text:String,maxCharacters:Int=2_000_000,maxDepth:Int=40){
        require(text.length<=maxCharacters){"JSON demasiado grande"}
        var depth=0;var quoted=false;var escaped=false
        for(c in text){
            if(quoted){if(escaped)escaped=false else if(c=='\\')escaped=true else if(c=='"')quoted=false}
            else when(c){'"'->quoted=true;'[','{'->{depth++;require(depth<=maxDepth){"JSON demasiado profundo"}};']','}'->{depth--;require(depth>=0)}}
        }
        require(!quoted && depth==0){"JSON incompleto"}
    }
}
