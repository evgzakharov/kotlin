/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.*
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType
import kotlin.script.experimental.api.ScriptCompileConfigurationProperties
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.api.ScriptDefinitionProperties
import kotlin.script.experimental.api.ScriptingEnvironmentProperties
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.scope.ScriptResolutionScope

abstract class KotlinScriptDefinitionAdapterFromNewAPIBase : KotlinScriptDefinition {

    protected abstract val scriptDefinition: ScriptDefinition

    protected abstract val scriptFileExtensionWithDot: String

    open val baseClass: KClass<*>
        get() = scriptDefinition.compilationConfigurator.defaultConfiguration[ScriptingEnvironmentProperties.baseClass]

    override val name: String
        get() = scriptDefinition.properties.getOrNull(ScriptDefinitionProperties.name) ?: "Kotlin Script"

    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(scriptFileExtensionWithDot)

    override fun getScriptName(script: KtScript): Name {
        val fileBasedName = NameUtils.getScriptNameForFile(script.containingKtFile.name)
        return Name.identifier(fileBasedName.identifier.removeSuffix(scriptFileExtensionWithDot))
    }

    override val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    override val dependencyResolver: DependenciesResolver by lazy {
        BridgeDependenciesResolver(scriptDefinition.compilationConfigurator)
    }

    override val acceptedAnnotations: List<KotlinTypeWrapper> by lazy {
        scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationProperties.refineConfigurationOnAnnotations)?.map {
            KotlinReflectedType(it.starProjectedType)
        } ?: emptyList()
    }

    override val implicitReceivers: List<KotlinTypeWrapper> by lazy {
        scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationProperties.scriptImplicitReceivers)?.map {
            KotlinReflectedType(it)
        } ?: emptyList()
    }

    override val environmentVariables: List<NamedDeclarationWrapper> by lazy {
        scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationProperties.contextVariables)?.map { (name, type) ->
            NamedDeclarationWrapper(name, KotlinReflectedType(type))
        } ?: emptyList()
    }

    override val additionalCompilerArguments: List<String>
        get() = scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationProperties.compilerOptions)
                ?: emptyList()

    override val scriptExpectedLocations: List<ScriptExpectedLocation> =
        listOf(
            ScriptExpectedLocation.SourcesOnly,
            ScriptExpectedLocation.TestsOnly
        )

    override val scriptResolutionScope: ScriptResolutionScope =
        ScriptResolutionScope.WITH_CONTEXT

    override val constructorParameters: List<NamedDeclarationWrapper>
        get() = baseClass.constructors.first().parameters.map {
            NamedDeclarationWrapper(
                it.name,
                KotlinReflectedType(
                    it.type
                )
            )
        }
}


class KotlinScriptDefinitionAdapterFromNewAPI(
    override val scriptDefinition: ScriptDefinition
) : KotlinScriptDefinitionAdapterFromNewAPIBase() {

    override val name: String get() = scriptDefinition.properties.getOrNull(ScriptDefinitionProperties.name) ?: super.name

    override val scriptFileExtensionWithDot =
        "." + (scriptDefinition.properties.getOrNull(ScriptDefinitionProperties.fileExtension) ?: "kts")
}
