package com.morkalork

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

class CreateStoryFileAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val fileName = virtualFile.name
        val fileExtension = virtualFile.extension ?: return

        if (fileExtension !in listOf("tsx", "jsx")) {
            Messages.showInfoMessage(project, "This action only works for .tsx or .jsx files.", "Unsupported File Type")
            return
        }

        if (fileName.contains(".stories.")) {
            Messages.showInfoMessage(project, "This file already appears to be a Story file.", "Nothing to Do")
            return
        }

        val baseName = fileName.removeSuffix(".$fileExtension")
        val storyFileName = "$baseName.stories.$fileExtension"
        val parent = virtualFile.parent ?: return

        val existingFile = parent.findChild(storyFileName)
        if (existingFile != null) {
            Messages.showInfoMessage(project, "A story file already exists:\n${existingFile.path}", "Story Exists")
            return
        }

        val componentName = baseName.replaceFirstChar { it.uppercaseChar() }
        val content = """
        import { $componentName } from "./$baseName";
        import { StoryObj } from "@storybook/react";

        export default {
          title: "components/$baseName",
          component: $componentName,
        };

        type Story = StoryObj<typeof $componentName>;

        export const Default: Story = {
          render: () => <$componentName />,
        };
    """.trimIndent()

        WriteCommandAction.runWriteCommandAction(project) {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(parent) ?: return@runWriteCommandAction
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(storyFileName, virtualFile.fileType, content)
            val createdFile = psiDirectory.add(psiFile)

            val createdVirtualFile = (createdFile as? com.intellij.psi.PsiFile)?.virtualFile
            if (createdVirtualFile != null) {
                FileEditorManager.getInstance(project).openFile(createdVirtualFile, true)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (
            virtualFile == null ||
            virtualFile.isDirectory ||
            (!virtualFile.name.endsWith(".tsx") && !virtualFile.name.endsWith(".jsx"))
        ) {
            presentation.isEnabledAndVisible = false
        } else {
            presentation.isEnabledAndVisible = true
        }
    }

}
