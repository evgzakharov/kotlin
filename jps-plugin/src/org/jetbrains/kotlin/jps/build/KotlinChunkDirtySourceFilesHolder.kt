/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.FileProcessor
import org.jetbrains.jps.builders.impl.DirtyFilesHolderBase
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import java.io.File

/**
 * This is same as [org.jetbrains.jps.incremental.fs.BuildFSState.processFilesToRecompile], but with build scope ignored for kotlin common modules. This is required for multiplatform projects building support in case of first build called from platform module. In this case build scope contains only platform files without common module source root which isn't what we need.
 *
 * Also this implementation improves performance a bit by traversing files only on given build target by providing [processDirtyFiles] with additional [ModuleBuildTarget] parameter.
 */
class KotlinChunkDirtySourceFilesHolder(
    val chunk: ModuleChunk,
    val context: CompileContext
) : DirtyFilesHolderBase<JavaSourceRootDescriptor, ModuleBuildTarget>(context) {
    private val fsState get() = context.projectDescriptor.fsState
    private val scope get() = context.scope

    override fun processDirtyFiles(processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>) {
        chunk.targets.forEach { target ->
            if (!processDirtyFiles(target, processor)) {
                return
            }
        }
    }

    private fun processDirtyFiles(
        target: ModuleBuildTarget,
        processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>
    ): Boolean {
        val delta = fsState.getEffectiveFilesDelta(context, target)
        delta.lockData()
        try {
            for (entry in delta.sourcesToRecompile.entries) {
                val sourceRoot = entry.key
                if (sourceRoot is JavaSourceRootDescriptor) {
                    val isCommonRoot = sourceRoot is KotlinCommonModuleSourceRoot
                    entry.value.forEach { file ->
                        if (sourceRoot.target == target) {
                            if (isCommonRoot || scope.isAffected(target, file)) {
                                if (file.isKotlinSourceFile) {
                                    if (!processor.apply(target, file, sourceRoot)) {
                                        return false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            delta.unlockData()
        }

        return true
    }

    private fun getRemovedFilesList(removed: MutableSet<File>, target: ModuleBuildTarget) {
        super.getRemovedFiles(target).mapNotNullTo(removed) {
            val file = File(it)
            if (file.isKotlinSourceFile) file else null
        }
    }

    private val byTarget: Map<ModuleBuildTarget, TargetFiles> by lazy {
        val result = mutableMapOf<ModuleBuildTarget, TargetFiles>()
        for (target in chunk.targets) {
            val dirty: MutableSet<File> = mutableSetOf()
            val removed: MutableSet<File> = mutableSetOf()

            processDirtyFiles(target, FileProcessor { _, file: File, _ ->
                dirty.add(file)
                true
            })

            getRemovedFilesList(removed, target)

            if (dirty.isNotEmpty() || removed.isNotEmpty()) {
                result[target] = TargetFiles(dirty, removed)
            }
        }
        result
    }

    private class TargetFiles(
        val dirty: MutableSet<File>,
        val removed: MutableSet<File>
    )

    fun getDirtyFiles(target: ModuleBuildTarget) = byTarget[target]?.dirty ?: setOf<File>()

    fun getRemovedFilesSet(target: ModuleBuildTarget) = byTarget[target]?.removed ?: setOf<File>()

    val dirtyFiles: Set<File>
        get() = byTarget.flatMapTo(mutableSetOf()) { it.value.dirty }

    val dirtyOrRemovedFilesSet: Set<File>
        get() {
            val result = mutableSetOf<File>()
            byTarget.forEach {
                result.addAll(it.value.dirty)
                result.addAll(it.value.removed)
            }
            return result
        }

    val hasDirtyOrRemovedFiles: Boolean
        get() = byTarget.isNotEmpty()
}

val File.isKotlinSourceFile: Boolean
    get() = FileUtilRt.extensionEquals(name, "kt")
