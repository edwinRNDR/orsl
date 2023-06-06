import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shadergenerator.dsl.functions.symbol
import org.openrndr.extra.shadergenerator.dsl.shadestyle.fragmentTransform
import org.openrndr.extra.shadergenerator.phrases.dsl.functions.gradient
import org.openrndr.extra.shadergenerator.phrases.dsl.functions.symbol
import org.openrndr.extra.shadergenerator.phrases.sdf.*
import org.openrndr.math.*
import org.openrndr.math.transforms.normalMatrix

// attempt to decouple the organisation of materials from the distance functions

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {

            val ss = shadeStyle {
                fragmentTransform {
                    val p_origin by parameter<Vector3>()
                    val p_time by parameter<Double>()
                    val va_texCoord0 by parameter<Vector2>()
                    val p_viewMatrix by parameter<Matrix44>()
                    val fixedCoord = Vector2(va_texCoord0.x, 1.0 - va_texCoord0.y)

                    val rayDir by (p_viewMatrix * Vector4(fixedCoord - Vector2(0.5), -1.0, 0.0)).xyz.normalized
                    val rayOrigin by p_origin

                    var f_matQuantize by global<Boolean>()
                    var matColor by global<Vector3>()
                    var f_matColor by global<Vector3>()
                    var matQuantize by global<Boolean>()
                    var matSpecular by global<Double>()
                    var f_matSpecular by global<Double>()

                    var update by global<Boolean>()

                    update = true.symbol
                    matQuantize = true.symbol

                    // this is a Kotlin function that produces a shader function
                    // the evaluation of shade is static
                    fun scene(shade: Boolean) = function<Vector3, Double> {
                        val min by function<Double, Double, Double> { a, b ->
                            if (shade) {
                                doIf(b lt a) {
                                    f_matQuantize = matQuantize
                                    f_matSpecular = matSpecular
                                    f_matColor = matColor
                                }
                            }
                            min(a, b)
                        }

                        val radius by 1.0

                        var coord by variable<Vector3>()
                        coord = it
                        coord = Vector3(-abs(coord.x), -abs(coord.y), coord.z)

                        coord += Vector3(
                            cos(it.z + p_time),
                            cos(it.x + p_time * 0.43),
                            cos(it.y + p_time * 0.932)
                        ) * 1.0

                        if (shade) {
                            matColor = Vector3(1.0, 0.0, 0.0).symbol
                            matQuantize = true.symbol
                            matSpecular = 160.0.symbol
                        }

                        var d by variable<Double>()
                        d = min(1E6.symbol, sdSphere(coord, radius) + value13D(it * 3.0).x)

                        if (shade) {
                            matQuantize = false.symbol
                            matSpecular = 4.0.symbol
                            matColor = Vector3(0.05, 0.05, 0.05).symbol
                        }
                        d = min(d, -sdSphere(it, radius * 50.0))
                        d
                    }

                    // use that kotlin function to create two variations of the scene distance function
                    val shadeScene by scene(true)
                    val shadowScene by scene(false)

                    val marcher by march(shadeScene, tolerance = 1E-4, stepScale = 0.4)
                    val result by marcher(rayOrigin, rayDir)

                    val normal by run {
                        val sceneNormal by gradient(shadowScene, 1E-3)
                        sceneNormal(result.position).normalized
                    }

                    val c by 24
                    val qnormal by if_(f_matQuantize) { sphericalDistribution(normal, c).xyz } else_ {
                        normal
                    }

                    val light by Vector3(0.6, 0.6, 0.6).normalized

                    val hlf by (light - rayDir).normalized
                    val diffuse by saturate(qnormal.dot(hlf))

                    val specular by pow(saturate(qnormal.dot(hlf)), f_matSpecular) *
                            diffuse *
                            (0.04 + 0.96 * pow(saturate(1.0 + hlf.dot(rayDir)), 5.0.symbol));

                    val amb by run {
                        val aoCalcer by calcAO(
                            shadeScene,
                            intensity = 0.5,
                            distance = 1.0,
                            iterations = 32,
                            falloff = 0.85
                        )
                        aoCalcer(result.position, qnormal)
                    }

                    val w by Vector3.ONE * 0.25
                    val lw by Vector3(0.5, 0.5, 0.5)
                    val finalColor by f_matColor * diffuse + w * amb + lw  * specular
                    x_fill = Vector4(finalColor * 1.0, 1.0)
                }
                parameter("time", seconds)

            }
            val o = extend(Orbital())
            extend {
                ss.parameter("viewMatrix", normalMatrix(o.camera.viewMatrix().inversed))
                ss.parameter("origin", (o.camera.viewMatrix().inversed * Vector4.UNIT_W).xyz)
                ss.parameter("time", seconds)
                drawer.defaults()
                drawer.shadeStyle = ss
                drawer.rectangle(drawer.bounds)
            }
        }
    }
}