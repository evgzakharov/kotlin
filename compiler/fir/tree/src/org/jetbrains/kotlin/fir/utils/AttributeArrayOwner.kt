/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.utils

import kotlin.reflect.KClass

/**
 * [AttributeArrayOwner] based on different implementations of [ArrayMap] and switches them
 *   depending on array map fullness
 * [AttributeArrayOwner] can be used in classes with many instances,
 *   like user data for Fir elements or attributes for cone types
 */
abstract class AttributeArrayOwner<K : Any, T : Any> : AbstractArrayMapOwner<K, T>() {
    @Suppress("UNCHECKED_CAST")
    final override var arrayMap: ArrayMap<T> = EmptyArrayMap as ArrayMap<T>
        private set

    final override fun registerComponent(tClass: KClass<out K>, value: T) {
        val id = typeRegistry.getId(tClass)
        when (arrayMap.size) {
            0 -> {
                arrayMap = OneElementArrayMap(value, id)
                return
            }

            1 -> {
                arrayMap = ArrayMapImpl<T>().apply {
                    val map = arrayMap as OneElementArrayMap<T>
                    this[map.index] = map.value
                }
            }
        }

        arrayMap[id] = value
    }
}