package zip

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Zipper {
    fun zip(sourceFileOrFolder: String, outputFileName: String) {
        val fos = FileOutputStream(outputFileName)
        val zipOut = ZipOutputStream(fos)
        val fileToZip = File(sourceFileOrFolder)
        zipFile(fileToZip, fileToZip.name, zipOut)
        zipOut.close()
        fos.close()
    }

    @Throws(IOException::class)
    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) {
            return
        }
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            val children = fileToZip.listFiles()
            for (childFile in children) {
                zipFile(childFile, fileName + "/" + childFile.name, zipOut)
            }
            return
        }
        val fis = FileInputStream(fileToZip)
        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }

    fun unzip(fileZip: String, destinationDir: String) {
        try {
            val destDir = File(destinationDir)
            val unzipPath = Paths.get(destinationDir)
            if (unzipPath.toString() != "") {
                Files.createDirectory(unzipPath)
            }

            val buffer = ByteArray(1024)
            val zis = ZipInputStream(FileInputStream(fileZip))
            var zipEntry = zis.nextEntry

            while (zipEntry != null) {
                if (zipEntry.isDirectory) {
                    Files.createDirectory(unzipPath.resolve(zipEntry.name))
                    zipEntry = zis.nextEntry
                    continue
                }

                val newFile = newFile(destDir, zipEntry)
                val fos = FileOutputStream(newFile)
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
            zis.close()
        } catch (ex: Exception) {
            println(ex.message.toString())
            ex.printStackTrace()
            //println(ex.stackTrace.toString())
            throw ex
        }
    }

    @Throws(IOException::class)
    private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir.canonicalPath, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }
}
