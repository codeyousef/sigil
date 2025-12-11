package codes.yousef.sigil.schema

/**
 * JS-specific ID generation using crypto.randomUUID or fallback
 */
actual fun generateNodeId(): String = js("crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substr(2, 9)") as String
