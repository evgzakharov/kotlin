/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.scope.ScriptResolutionScope
import kotlin.script.templates.standard.ScriptTemplateWithArgs

open class KotlinScriptDefinitionFromTemplate(val template: KClass<out Any>) : KotlinScriptDefinition, UserDataHolderBase() {

    override val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override val annotationsForSamWithReceivers: List<String> get() = emptyList()

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    override fun getScriptName(script: KtScript): Name =
        NameUtils.getScriptNameForFile(script.containingKtFile.name)

    override val dependencyResolver: DependenciesResolver =
        DependenciesResolver.NoDependencies

    override val acceptedAnnotations: List<KotlinTypeWrapper> get() = emptyList()

    override val additionalCompilerArguments: List<String> get() = emptyList()

    override val scriptExpectedLocations: List<ScriptExpectedLocation> =
        listOf(
            ScriptExpectedLocation.SourcesOnly,
            ScriptExpectedLocation.TestsOnly
        )

    override val scriptResolutionScope: ScriptResolutionScope =
        ScriptResolutionScope.WITH_CONTEXT

    override val constructorParameters: List<NamedDeclarationWrapper>
        get() = template.constructors.first().parameters.map {
            NamedDeclarationWrapper(
                it.name,
                KotlinReflectedType(
                    it.type
                )
            )
        }

    override val implicitReceivers: List<KotlinTypeWrapper> get() = emptyList()

    override val environmentVariables: List<NamedDeclarationWrapper> get() = emptyList()
}

class KotlinReflectedType(val type: KType) : KotlinTypeWrapper {
    override val simpleName: String
        get() = type.asKClass?.simpleName ?: throw IllegalArgumentException("Unable to extract simple name from $type")

    override val qualifiedName: String
        get() = type.asKClass?.qualifiedName ?: throw IllegalArgumentException("Unable to extract qualified name from $type")

    override fun kotlinTypeInModule(module: ModuleDescriptor): KotlinType = getKotlinTypeByKType(module, type)

    override val classId: ClassId? = type.classId
}

private val KType.asKClass: KClass<*>? get() = classifier?.let { it as? KClass<*> }

private val KClass<*>.classId: ClassId
    get() = this.java.enclosingClass?.kotlin?.classId?.createNestedClassId(Name.identifier(simpleName!!))
            ?: ClassId.topLevel(FqName(qualifiedName!!))

private val KType.classId: ClassId?
    get() = classifier?.let { it as? KClass<*> }?.classId


object StandardScriptDefinition : KotlinScriptDefinitionFromTemplate(ScriptTemplateWithArgs::class)