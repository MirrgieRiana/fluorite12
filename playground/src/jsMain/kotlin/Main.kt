@JsModule("codemirror")
external object `_codemirror`

@JsModule("@codemirror/state")
external object `_@codemirror/state`

@JsModule("@codemirror/view")
external object `_@codemirror/view`

@JsModule("@codemirror/commands")
external object `_@codemirror/commands`

@JsModule("@codemirror/lang-javascript")
external object `_@codemirror/lang-javascript`

@JsModule("pako")
external object `_pako`

@OptIn(ExperimentalJsExport::class)
@JsExport
object Utilities {
    fun `codemirror`() = `_codemirror`
    fun `@codemirror/state`() = `_@codemirror/state`
    fun `@codemirror/view`() = `_@codemirror/view`
    fun `@codemirror/commands`() = `_@codemirror/commands`
    fun `@codemirror/lang-javascript`() = `_@codemirror/lang-javascript`
    fun `pako`() = `_pako`
}
