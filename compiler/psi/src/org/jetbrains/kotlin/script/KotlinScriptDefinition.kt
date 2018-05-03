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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.scope.ScriptResolutionScope

interface KotlinScriptDefinition {
    val name: String
    // TODO: [major, platform] consider creating separate type (subtype?) for kotlin scripts
    val fileType: LanguageFileType
    val annotationsForSamWithReceivers: List<String>

    val dependencyResolver: DependenciesResolver
    val acceptedAnnotations: List<KotlinTypeWrapper>
    val additionalCompilerArguments: List<String>
    val scriptExpectedLocations: List<ScriptExpectedLocation>

    val scriptResolutionScope: ScriptResolutionScope

    val constructorParameters: List<NamedDeclarationWrapper>

    val implicitReceivers: List<KotlinTypeWrapper>
    val environmentVariables: List<NamedDeclarationWrapper>
    fun isScript(fileName: String): Boolean
    fun getScriptName(script: KtScript): Name
}

interface KotlinTypeWrapper {
    val simpleName: String
    val qualifiedName: String
    fun kotlinTypeInModule(module: ModuleDescriptor): KotlinType
    val classId: ClassId?
}

data class NamedDeclarationWrapper(
    val name: String?,
    val type: KotlinTypeWrapper
)