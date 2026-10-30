package pe.profelab.core.model

/** Teaching is a separate block, not a replacement for procedure, evidence or academic claims. */
data class TeachingBlock(val key:String,val title:String,val texts:List<String>,val multiple:Boolean=false)
data class TeachingGuide(val version:Int,val status:String,val blocks:List<TeachingBlock>)
