package com.pankajgaur.batch_svg_converter

import com.android.ide.common.vectordrawable.Svg2Vector
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * An IntelliJ IDEA plugin action that enables batch conversion of SVG files into Android VectorDrawable (XML) format.
 *
 * This action performs the following steps:
 * 1. Validates the selection context to ensure a target directory is identified.
 * 2. Opens a file chooser to allow the user to select one or more source SVG files.
 * 3. Prompts the user to provide a destination filename for each selected asset.
 * 4. Utilizes the Android [Svg2Vector] library to parse the SVG content and generate VectorDrawable XML.
 * 5. Writes the resulting XML files to the project's file system within a write action.
 * 6. Displays a notification summary indicating the number of successful and failed conversions.
 *
 */
class SvgToVectorConverter : AnAction() {

    override fun update(e: AnActionEvent) {
        // Always show the action as long as a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }

    /**
     * Handles the action when triggered by the user.
     *
     * This method performs the following steps:
     * 1. Determines the target directory based on the user's selection in the IDE.
     * 2. Displays a file chooser to select one or more SVG files from the local file system.
     * 3. Prompts the user to provide a destination filename for each selected SVG.
     * 4. Converts the SVGs to Android VectorDrawable XML format.
     * 5. Saves the resulting XML files into the target directory within a write action.
     * 6. Displays a notification summary indicating the number of successful and failed conversions.
     *
     * @param e The action event containing the context of where the action was invoked.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        var targetDirectory = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // If a file is selected, use its parent folder as the destination
        if (targetDirectory != null && !targetDirectory.isDirectory) {
            targetDirectory = targetDirectory.parent
        }

        if (targetDirectory == null || !targetDirectory.isDirectory) {
            showNotification(project, "Please right-click on a directory (e.g., res/drawable) to import SVGs.", NotificationType.WARNING)
            return
        }

        // Open FileChooser to select external SVGs
        val descriptor = FileChooserDescriptor(true, false, false, false, false, true).apply {
            title = "Select SVG Files to Convert"
            description = "Choose one or more SVG files to convert to VectorDrawable"
            withFileFilter { it.extension?.equals("svg", ignoreCase = true) == true }
        }

        val selectedFiles = FileChooser.chooseFiles(descriptor, project, null)
        
        if (selectedFiles.isEmpty()) {
            return
        }

        // Prompt user to rename each file
        val fileToNameMap = mutableMapOf<VirtualFile, String>()
        for (svgFile in selectedFiles) {
            val defaultName = svgFile.nameWithoutExtension.lowercase()
            val newName = com.intellij.openapi.ui.Messages.showInputDialog(
                project,
                "Enter name for ${svgFile.name}:",
                "Rename Vector Asset",
                null,
                defaultName,
                null
            )
            
            // If user clicks Cancel or enters empty string, skip this file
            if (!newName.isNullOrBlank()) {
                val finalName = if (newName.endsWith(".xml", ignoreCase = true)) newName else "$newName.xml"
                fileToNameMap[svgFile] = finalName
            }
        }

        if (fileToNameMap.isEmpty()) {
            return
        }

        ApplicationManager.getApplication().runWriteAction {
            var successCount = 0
            var errorCount = 0

            for ((svgFile, newFileName) in fileToNameMap) {
                try {
                    val xmlContent = convertSvgToXmlContent(svgFile)
                    if (xmlContent != null) {
                        val existingFile = targetDirectory.findChild(newFileName)
                        val outputFile = existingFile ?: targetDirectory.createChildData(this, newFileName)
                        
                        VfsUtil.saveText(outputFile, xmlContent)
                        successCount++
                    } else {
                        errorCount++
                    }
                } catch (ex: Exception) {
                    errorCount++
                }
            }

            val message = if (errorCount == 0) {
                "Successfully imported $successCount SVG(s) to VectorDrawable."
            } else {
                "Imported $successCount SVG(s). Failed to convert $errorCount SVG(s)."
            }
            val type = if (errorCount == 0) NotificationType.INFORMATION else NotificationType.WARNING
            showNotification(project, message, type)
        }
    }

    /**
     * Converts an SVG file to Android VectorDrawable XML content.
     *
     * This method creates a temporary file on the local file system to facilitate the conversion
     * using the [Svg2Vector] utility, as it requires a [java.nio.file.Path].
     *
     * @param svgFile The [VirtualFile] representing the source SVG.
     * @return A string containing the converted XML content, or `null` if the conversion fails
     *         or produces empty output.
     */
    private fun convertSvgToXmlContent(svgFile: VirtualFile): String? {
        val tempFile = java.nio.file.Files.createTempFile("tempSvg", ".svg")
        return try {
            // Write the virtual file content to a real temp file because Svg2Vector needs a Path
            java.nio.file.Files.write(tempFile, svgFile.contentsToByteArray())

            val outputStream = ByteArrayOutputStream()
            val errorLog = Svg2Vector.parseSvgToXml(tempFile, outputStream)
            
            // Depending on sdk-common version, parseSvgToXml might log some errors but still succeed.
            // We just check if it generated valid xml.
            val xmlContent = outputStream.toString(Charsets.UTF_8.name())
            if (xmlContent.isBlank()) null else xmlContent
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(tempFile)
            } catch (ignored: IOException) {}
        }
    }

    private fun showNotification(project: com.intellij.openapi.project.Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SVG to VectorDrawable")
            .createNotification("SVG Converter", content, type)
            .notify(project)
    }
}
