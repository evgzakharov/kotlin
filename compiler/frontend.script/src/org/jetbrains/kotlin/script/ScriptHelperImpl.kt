/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.checkers.JvmSimpleNameBacktickChecker
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor

class ScriptHelperImpl : ScriptHelper {
    override fun getScriptParameters(kotlinScriptDefinition: KotlinScriptDefinition, scriptDefinition: ScriptDescriptor) =
        kotlinScriptDefinition.getScriptParameters(scriptDefinition.module)

    override fun getScriptSuperType(scriptDescriptor: ScriptDescriptor, scriptDefinition: KotlinScriptDefinition) =
        when (scriptDefinition) {
            is KotlinScriptDefinitionFromTemplate -> getKotlinTypeByKClass(scriptDescriptor.module, scriptDefinition.template)
            else -> scriptDescriptor.builtIns.anyType
        }

    override fun getCheckedEnvironmentProperties(
        scriptDescriptor: ScriptDescriptor,
        scriptDefinition: KotlinScriptDefinition
    ): List<NamedDeclarationWrapper> {
        val script = scriptDescriptor as LazyScriptDescriptor
        val duplicates = run {
            val namesMet = hashSetOf<String?>()
            val duplicates = hashSetOf<String?>()
            for ((name, _) in scriptDefinition.environmentVariables) {
                when {
                    name.isNullOrBlank() -> {}
                    namesMet.contains(name) -> duplicates.add(name)
                    else -> namesMet.add(name)
                }
            }
            duplicates
        }
        return scriptDefinition.environmentVariables.filter { (name, _) ->
            when {
                name.isNullOrBlank() -> {
                    script.resolveSession.trace.report(
                        Errors.INVALID_SCRIPT_ENVIRONMENT_PROPERTY_NAME.on(script.scriptInfo.script, name ?: "<null>")
                    )
                    false
                }
                duplicates.contains(name) -> {
                    script.resolveSession.trace.report(
                        Errors.NON_UNIQUE_SCRIPT_ENVIRONMENT_PROPERTY_NAME.on(script.scriptInfo.script, name)
                    )
                    false
                }
                else -> {
                    JvmSimpleNameBacktickChecker.checkStringIdentifier(name, { script.scriptInfo.script }, script.resolveSession.trace)
                }
            }
        }
    }
}