package org.openrndr.orsl.shadergenerator.dsl.shadestyle

import org.openrndr.draw.ShadeStyle
import org.openrndr.orsl.shadergenerator.dsl.activeGenerator
import org.openrndr.orsl.shadergenerator.phrases.PhraseResolver
import org.openrndr.orsl.shadergenerator.phrases.dsl.ArrayPhrases
import org.openrndr.orsl.shadergenerator.phrases.dsl.ArrayPhrasesIndex
import org.openrndr.orsl.shadergenerator.phrases.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun preprocessor(): PhraseResolver {
    val resolver = PhraseResolver()
    resolver.indices.add(ArrayPhrasesIndex(ArrayPhrases()))
    resolver.indices.add(FastMathPhrasesIndex(FastMathPhrases()))
    resolver.indices.add(ConstPhrasesIndex(ConstPhrases()))
    resolver.indices.add(TransformPhrasesIndex(TransformPhrases()))
    return resolver
}

@OptIn(ExperimentalContracts::class)
fun ShadeStyle.fragmentTransform(f: FragmentTransformBuilder.() -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }

    val builder = FragmentTransformBuilder()
    builder.push()
    require(activeGenerator() == builder)
    builder.f()
    require(activeGenerator() == builder)
    val prep = preprocessor()
    fragmentPreamble = prep.preprocessShader(builder.preamble)
    fragmentTransform = prep.preprocessShader(builder.code)
    builder.pop()
}

@OptIn(ExperimentalContracts::class)
fun ShadeStyle.vertexTransform(f: VertexTransformBuilder.() -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    val builder = VertexTransformBuilder()
    builder.push()
    builder.f()
    val prep = preprocessor()
    vertexPreamble = prep.preprocessShader(builder.preamble)
    vertexTransform = prep.preprocessShader(builder.code)
    builder.pop()
}