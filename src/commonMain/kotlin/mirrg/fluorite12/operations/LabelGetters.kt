package mirrg.fluorite12.operations

import mirrg.fluorite12.Environment
import mirrg.fluorite12.compilers.objects.FluoriteNull
import mirrg.fluorite12.compilers.objects.FluoriteValue

class LabelContext(val label: String) {
    val throwable = LabelBreak(this)
    var value: FluoriteValue? = null
}

class LabelBreak(val context: LabelContext) : Throwable()

/**
 * ラベル宣言とスコープ制御: `formula !: label`
 * - 実行前に env.labelTable[label] に LabelContext を push
 * - 内部で label ! value がスローした LabelBreak をキャッチして value を返す
 * - 最後に pop してクリーンアップ
 */
class LabelScopeGetter(private val label: String, private val content: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val stack = env.labelTable.getOrPut(label) { mutableListOf() }
        val ctx = LabelContext(label)
        stack.add(ctx)
        try {
            return content.evaluate(env)
        } catch (e: LabelBreak) {
            if (e.context === ctx) {
                return ctx.value ?: FluoriteNull
            } else {
                throw e
            }
        } finally {
            // recycle: 値をクリアし、スコープを外す
            ctx.value = null
            if (stack.isNotEmpty() && stack.last() === ctx) stack.removeAt(stack.size - 1) else stack.remove(ctx)
            if (stack.isEmpty()) env.labelTable.remove(label)
        }
    }

    override val code get() = "LabelScopeGetter[${label};${content.code}]"
}

/**
 * ラベルブレーク: `label ! value`
 * - env.labelTable[label] のトップを取得
 * - 値をセットし、再利用可能な Throwable を throw
 */
class LabelBreakGetter(private val label: String, private val valueGetter: Getter) : Getter {
    override suspend fun evaluate(env: Environment): FluoriteValue {
        val stack = env.labelTable[label] ?: throw IllegalStateException("Label not found: $label")
        val ctx = (stack.lastOrNull() as? LabelContext) ?: throw IllegalStateException("Label stack empty: $label")
        ctx.value = valueGetter.evaluate(env)
        throw ctx.throwable
    }

    override val code get() = "LabelBreakGetter[${label};${valueGetter.code}]"
}
