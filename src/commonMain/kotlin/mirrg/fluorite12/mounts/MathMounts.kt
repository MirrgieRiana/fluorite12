package mirrg.fluorite12.mounts

import mirrg.fluorite12.compilers.objects.FluoriteDouble
import mirrg.fluorite12.compilers.objects.FluoriteFunction
import mirrg.fluorite12.compilers.objects.FluoriteInt
import mirrg.fluorite12.compilers.objects.FluoriteNumber
import mirrg.fluorite12.compilers.objects.FluoriteObject
import mirrg.fluorite12.compilers.objects.FluoriteValue
import mirrg.fluorite12.compilers.objects.toFluoriteNumber
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

fun createMathMounts(): Map<String, FluoriteValue> {
    return mapOf(
        "MATH" to FluoriteObject(
            FluoriteObject.fluoriteClass, mutableMapOf(
                "PI" to FluoriteDouble(3.141592653589793), // TODO kotlinアップデート時に定数に置換し直す
                "E" to FluoriteDouble(2.718281828459045), // TODO kotlinアップデート時に定数に置換し直す
            )
        ),
        "FLOOR" to FluoriteFunction { arguments ->
            when (arguments.size) {
                1 -> when (val number = arguments[0]) {
                    is FluoriteDouble -> FluoriteInt(floor(number.value).toInt())
                    is FluoriteInt -> number
                    else -> usage("FLOOR(number: NUMBER): INTEGER")
                }

                else -> usage("FLOOR(number: NUMBER): INTEGER")
            }
        },
        "DIV" to FluoriteFunction { arguments ->
            if (arguments.size == 2) {
                val left = arguments[0]
                val right = arguments[1]
                when (left) {
                    is FluoriteInt -> when (right) {
                        is FluoriteInt -> FluoriteInt(left.value / right.value)
                        is FluoriteDouble -> FluoriteDouble((left.value / right.value).let { it - it.rem(1.0) })
                        else -> usage("DIV(x: NUMBER; y: NUMBER): NUMBER")
                    }

                    is FluoriteDouble -> when (right) {
                        is FluoriteInt -> FluoriteDouble((left.value / right.value).let { it - it.rem(1.0) })
                        is FluoriteDouble -> FluoriteDouble((left.value / right.value).let { it - it.rem(1.0) })
                        else -> usage("DIV(x: NUMBER; y: NUMBER): NUMBER")
                    }

                    else -> usage("DIV(x: NUMBER; y: NUMBER): NUMBER")
                }
            } else {
                usage("DIV(x: NUMBER; y: NUMBER): NUMBER")
            }
        },
        "SQRT" to FluoriteFunction { arguments ->
            when (arguments.size) {
                1 -> FluoriteDouble(sqrt((arguments[0] as FluoriteNumber).toDouble()))
                else -> usage("SQRT(number: NUMBER): NUMBER")
            }
        },
        "RAND" to FluoriteFunction { arguments ->
            when (arguments.size) {
                0 -> {
                    FluoriteDouble(Random.nextDouble())
                }

                1 -> {
                    val until = arguments[0].toFluoriteNumber().toInt()
                    FluoriteInt(Random.nextInt(until))
                }

                2 -> {
                    val from = arguments[0].toFluoriteNumber().toInt()
                    val until = arguments[1].toFluoriteNumber().toInt()
                    FluoriteInt(Random.nextInt(from, until))
                }

                else -> usage(
                    "RAND(): DOUBLE",
                    "RAND([from: NUMBER; ]until: NUMBER): INT",
                )
            }
        },
    )
}
