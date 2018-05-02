/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.scope.ScriptResolutionScope
import kotlin.script.templates.standard.ScriptTemplateWithArgs

interface KotlinScriptDefinition {
    val name: String
    // TODO: [major, platform] consider creating separate type (subtype?) for kotlin scripts
    val fileType: LanguageFileType
    val annotationsForSamWithReceivers: List<String>

    val dependencyResolver: DependenciesResolver
    val acceptedAnnotations: List<KClass<out Annotation>>
    val additionalCompilerArguments: List<String>
    val scriptExpectedLocations: List<ScriptExpectedLocation>

    val scriptResolutionScope: ScriptResolutionScope

    val constructorParameters: List<KParameter>

    val implicitReceivers: List<KType>
    val environmentVariables: List<Pair<String, KType>>
    fun isScript(fileName: String): Boolean
    fun getScriptName(script: KtScript): Name
}

open class KotlinScriptDefinitionFromTemplate(val template: KClass<out Any>) : KotlinScriptDefinition, UserDataHolderBase() {

    override val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override val annotationsForSamWithReceivers: List<String> get() = emptyList()

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    override fun getScriptName(script: KtScript): Name =
        NameUtils.getScriptNameForFile(script.containingKtFile.name)

    override val dependencyResolver: DependenciesResolver = DependenciesResolver.NoDependencies

    override val acceptedAnnotations: List<KClass<out Annotation>> get() = emptyList()

    override val additionalCompilerArguments: List<String> get() = emptyList()

    override val scriptExpectedLocations: List<ScriptExpectedLocation> =
        listOf(ScriptExpectedLocation.SourcesOnly, ScriptExpectedLocation.TestsOnly)

    override val scriptResolutionScope: ScriptResolutionScope = ScriptResolutionScope.WITH_CONTEXT

    override val constructorParameters: List<KParameter>
        get() = template.constructors.first().parameters

    override val implicitReceivers: List<KType> get() = emptyList()

    override val environmentVariables: List<Pair<String, KType>> get() = emptyList()
}

object StandardScriptDefinition : KotlinScriptDefinitionFromTemplate(ScriptTemplateWithArgs::class)

