package pe.profelab.core.model

import org.junit.Test

class JsonLimitsTest {
    @Test(expected=IllegalArgumentException::class) fun nestedPayloadIsRejectedBeforeRecursiveParsing(){JsonLimits.requireBounded("[".repeat(41)+"0"+"]".repeat(41))}
    @Test fun bracketsInsideEscapedTextAreNotCountedAsContainers(){JsonLimits.requireBounded("{\"text\":\"\\\"[not a container]\\\"\"}",maxDepth=1)}
    @Test(expected=IllegalArgumentException::class) fun oversizedInputIsRejected(){JsonLimits.requireBounded("{} ".repeat(10),maxCharacters=20)}
    @Test(expected=IllegalArgumentException::class) fun unfinishedStringIsRejected(){JsonLimits.requireBounded("{\"text\":\"unfinished}")}
}
