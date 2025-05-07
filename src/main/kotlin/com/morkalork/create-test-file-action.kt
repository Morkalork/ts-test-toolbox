package com.morkalork

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.nio.charset.StandardCharsets

class CreateTestFileAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val fileName = virtualFile.name
        val fileExtension = virtualFile.extension ?: return
        if (fileExtension !in listOf("ts", "js")) return

        val baseName = fileName.removeSuffix(".$fileExtension")
        val testFileName = "$baseName.test.$fileExtension"
        val parent = virtualFile.parent ?: return

        val psiManager = PsiManager.getInstance(project)
        val psiDirectory = psiManager.findDirectory(parent) ?: return

        val fileText = String(virtualFile.contentsToByteArray(), StandardCharsets.UTF_8)
        val exportRegex = Regex("""export\s+(?:const|function)\s+(\w+)""")
        val exports = exportRegex.findAll(fileText).map { it.groupValues[1] }.toList()

        if (exports.isEmpty()) {
            Messages.showInfoMessage(project, "No named exports found in ${virtualFile.name}", "No Exports Found")
            return
        }

        val framework = detectTestFramework(parent)
        val testUtilImport = when (framework) {
            "vitest" -> "import { describe, it, expect } from 'vitest'"
            else -> "import { describe, it, expect } from 'jest'" // default to jest
        }

        val importLine = "import { ${exports.joinToString(", ")} } from './$baseName'"

        val testBlocks = exports.joinToString("\n\n") { name ->
            listOf(
                "describe('$name', () => {",
                "  it('should work', () => {",
                "    const result = $name()",
                "    // expect(result).to...",
                "  })",
                "})"
            ).joinToString("\n")
        }

        val testFileContent = listOf(testUtilImport, importLine, "", testBlocks).joinToString("\n")

        WriteCommandAction.runWriteCommandAction(project) {
            val factory = PsiFileFactory.getInstance(project)
            val newFile = factory.createFileFromText(testFileName, virtualFile.fileType, testFileContent)
            val createdFile = psiDirectory.add(newFile)

            val createdVirtualFile = (createdFile as? com.intellij.psi.PsiFile)?.virtualFile
            if (createdVirtualFile != null) {
                FileEditorManager.getInstance(project).openFile(createdVirtualFile, true)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
    }

    private fun detectTestFramework(startDir: VirtualFile): String {
        var dir: VirtualFile? = startDir
        while (dir != null) {
            val pkgJson = dir.findChild("package.json")
            if (pkgJson != null && !pkgJson.isDirectory) {
                val text = String(pkgJson.contentsToByteArray(), StandardCharsets.UTF_8)
                return when {
                    "vitest" in text -> "vitest"
                    "jest" in text -> "jest"
                    else -> "jest"
                }
            }
            dir = dir.parent
        }
        return "jest"
    }
}
