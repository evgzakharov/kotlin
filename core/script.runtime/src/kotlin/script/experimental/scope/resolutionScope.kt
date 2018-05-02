/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.scope

enum class ScriptResolutionScope {
    WITH_CONTEXT, // script is part of the context (e.g. project or environment)
    ISOLATED // script should be resolved isolated from the context
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScriptResolutionContext(val value: ScriptResolutionScope)

