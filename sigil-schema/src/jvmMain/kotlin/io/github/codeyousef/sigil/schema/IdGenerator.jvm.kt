package io.github.codeyousef.sigil.schema

import java.util.UUID

/**
 * JVM-specific ID generation using java.util.UUID
 */
actual fun generateNodeId(): String = UUID.randomUUID().toString()
