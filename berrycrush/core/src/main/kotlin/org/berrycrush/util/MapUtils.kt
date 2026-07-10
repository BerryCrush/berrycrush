package org.berrycrush.util

internal fun <K, R> Map<K, R?>.toNonNullMap() = mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

internal inline fun <K, V> Map<K, V?>.forEachNonNull(action: (k: K, v: V) -> Unit) {
    this.forEach { (k, v) -> v?.let { action(k, it) } }
}
