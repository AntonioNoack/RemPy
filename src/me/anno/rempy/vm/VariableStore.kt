package me.anno.rempy.vm

class VariableStore {
    internal val vars = HashMap<String, Any>()

    fun get(name: String): Any = vars[name] ?: ""
    fun set(name: String, value: Any) { vars[name] = value }

    fun snapshot(): Map<String, Any> = HashMap(vars)
    fun restore(data: Map<String, Any>) {
        check(data !== vars)
        vars.clear()
        vars.putAll(data)
    }
}