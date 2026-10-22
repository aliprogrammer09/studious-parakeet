package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import com.example.data.VaultMappers.toDomain
import com.example.data.VaultMappers.toEntity
import com.example.model.AppearanceConfig
import com.example.model.DirectoryConfig
import com.example.model.MediaItem
import com.example.model.MediaPageItem
import com.example.model.MediaType
import com.example.utils.ImageHeaderParser
import com.example.utils.MediaLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class VaultRepository(private val context: Context) {

    private val db: VaultDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            VaultDatabase::class.java,
            "domina_vault.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    private val dao: VaultDao get() = db.vaultDao()

    val vaultRootDir: File
        get() = File(context.filesDir, "vault").apply { mkdirs() }

    fun defaultDirectories(): DirectoryConfig {
        val root = vaultRootDir
        return DirectoryConfig(
            imagesPath = File(root, "images").apply { mkdirs() }.absolutePath,
            videosPath = File(root, "videos").apply { mkdirs() }.absolutePath,
            comicsPath = File(root, "comics").apply { mkdirs() }.absolutePath,
            imageSetsPath = File(root, "imagesets").apply { mkdirs() }.absolutePath,
            lastIndexedAt = nowIsoString(),
            autoScanOnStartup = true
        )
    }

    fun observeMediaItems(): Flow<List<MediaItem>> =
        dao.observeAllMediaItems().map { list -> list.map { it.toDomain() } }

    fun observeConfig(): Flow<Pair<DirectoryConfig, AppearanceConfig>> =
        dao.observeVaultConfig().map { entity ->
            if (entity == null) {
                Pair(defaultDirectories(), AppearanceConfig())
            } else {
                Pair(
                    VaultMappers.directoryFromJson(entity.directoriesJson),
                    VaultMappers.appearanceFromJson(entity.appearanceJson)
                )
            }
        }

    suspend fun initializeVaultIfNeeded(): Pair<DirectoryConfig, AppearanceConfig> =
        withContext(Dispatchers.IO) {
            val existingConfig = dao.getVaultConfigOnce()
            val dirs: DirectoryConfig
            val app: AppearanceConfig
            if (existingConfig == null) {
                dirs = defaultDirectories()
                app = AppearanceConfig()
                dao.saveVaultConfig(
                    VaultConfigEntity(
                        directoriesJson = VaultMappers.directoryToJson(dirs),
                        appearanceJson = VaultMappers.appearanceToJson(app),
                        presetsJson = "[]"
                    )
                )
            } else {
                dirs = VaultMappers.directoryFromJson(existingConfig.directoriesJson)
                app = VaultMappers.appearanceFromJson(existingConfig.appearanceJson)
            }

            val existingItems = dao.getAllMediaItemsOnce()
            if (existingItems.isEmpty()) {
                val seeded = VaultAssetGenerator.ensureDefaultVaultMediaOnDisk(context)
                dao.insertAllMediaItems(seeded.map { it.toEntity() })
            }
            Pair(dirs, app)
        }

    suspend fun saveDirectories(directories: DirectoryConfig) = withContext(Dispatchers.IO) {
        val current = dao.getVaultConfigOnce()
        val appJson = current?.appearanceJson ?: VaultMappers.appearanceToJson(AppearanceConfig())
        val presetsJson = current?.presetsJson ?: "[]"
        dao.saveVaultConfig(
            VaultConfigEntity(
                directoriesJson = VaultMappers.directoryToJson(directories),
                appearanceJson = appJson,
                presetsJson = presetsJson
            )
        )
    }

    suspend fun saveAppearance(appearance: AppearanceConfig) = withContext(Dispatchers.IO) {
        val current = dao.getVaultConfigOnce()
        val dirsJson = current?.directoriesJson ?: VaultMappers.directoryToJson(defaultDirectories())
        val presetsJson = current?.presetsJson ?: "[]"
        dao.saveVaultConfig(
            VaultConfigEntity(
                directoriesJson = dirsJson,
                appearanceJson = VaultMappers.appearanceToJson(appearance),
                presetsJson = presetsJson
            )
        )
    }

    suspend fun updateMediaItem(updated: MediaItem) = withContext(Dispatchers.IO) {
        dao.insertMediaItem(updated.toEntity())
    }

    suspend fun deleteMediaItem(id: String, deleteFromDisk: Boolean = true) =
        withContext(Dispatchers.IO) {
            val all = dao.getAllMediaItemsOnce().map { it.toDomain() }
            val target = all.find { it.id == id }
            if (target != null && deleteFromDisk && target.localPath.startsWith(vaultRootDir.absolutePath)) {
                try {
                    File(target.localPath).deleteRecursively()
                } catch (_: Exception) {
                }
            }
            dao.deleteMediaItemById(id)
        }

    suspend fun clearAllVaultItems() = withContext(Dispatchers.IO) {
        dao.clearAllMediaItems()
    }

    /**
     * Scans all 4 configured directories on disk, preserving existing ratings, tags, bookmarks, and notes.
     */
    suspend fun scanFilesystemLibrary(customDirs: DirectoryConfig? = null): Int =
        withContext(Dispatchers.IO) {
            VaultAssetGenerator.ensureDefaultVaultMediaOnDisk(context)
            val currentConfig = dao.getVaultConfigOnce()
            val activeDirs = customDirs ?: if (currentConfig != null) {
                VaultMappers.directoryFromJson(currentConfig.directoriesJson)
            } else {
                defaultDirectories()
            }

            val existingItems = dao.getAllMediaItemsOnce().map { it.toDomain() }
            val existingByPath = existingItems.associateBy { it.localPath }
            val nextItems = mutableListOf<MediaItem>()

            // 1. Scan Single Images (recursive)
            val imageFiles = walkFilesRecursive(File(activeDirs.imagesPath))
            for (file in imageFiles) {
                val ext = "." + file.extension.lowercase(Locale.ROOT)
                if (!IMAGE_EXTENSIONS.contains(ext)) continue
                val existing = existingByPath[file.absolutePath]
                val dims = readImageDimensions(file)
                val cleanTitle = file.nameWithoutExtension.replace(Regex("[-_]+"), " ").trim()
                val defaultTags = sanitizeTagsFromPath(File(activeDirs.imagesPath), file, "image")

                nextItems.add(
                    MediaItem(
                        id = existing?.id ?: stableIdFromPath("image", file.absolutePath),
                        type = MediaType.IMAGE,
                        title = existing?.title?.ifBlank { cleanTitle } ?: cleanTitle.ifBlank { file.name },
                        filename = file.name,
                        localPath = file.absolutePath,
                        folderPath = file.parentFile?.absolutePath ?: activeDirs.imagesPath,
                        thumbnailUrl = file.absolutePath,
                        mediaUrl = file.absolutePath,
                        rating = existing?.rating ?: 0,
                        isFavorite = existing?.isFavorite ?: false,
                        notes = existing?.notes ?: "",
                        tags = if (existing != null && existing.tags.isNotEmpty()) existing.tags else defaultTags,
                        dateAdded = existing?.dateAdded ?: isoFromMillis(file.lastModified()),
                        sizeBytes = file.length(),
                        format = file.extension.uppercase(Locale.ROOT).ifBlank { "IMG" },
                        width = dims.width,
                        height = dims.height,
                        artist = existing?.artist ?: ""
                    )
                )
            }

            // 2. Scan Single Videos (recursive)
            val videoFiles = walkFilesRecursive(File(activeDirs.videosPath))
            for (file in videoFiles) {
                val ext = "." + file.extension.lowercase(Locale.ROOT)
                if (!VIDEO_EXTENSIONS.contains(ext)) continue
                val existing = existingByPath[file.absolutePath]
                val cleanTitle = file.nameWithoutExtension.replace(Regex("[-_]+"), " ").trim()
                val defaultTags = sanitizeTagsFromPath(File(activeDirs.videosPath), file, "video")

                nextItems.add(
                    MediaItem(
                        id = existing?.id ?: stableIdFromPath("video", file.absolutePath),
                        type = MediaType.VIDEO,
                        title = existing?.title?.ifBlank { cleanTitle } ?: cleanTitle.ifBlank { file.name },
                        filename = file.name,
                        localPath = file.absolutePath,
                        folderPath = file.parentFile?.absolutePath ?: activeDirs.videosPath,
                        thumbnailUrl = existing?.thumbnailUrl ?: "",
                        mediaUrl = file.absolutePath,
                        rating = existing?.rating ?: 0,
                        isFavorite = existing?.isFavorite ?: false,
                        notes = existing?.notes ?: "",
                        tags = if (existing != null && existing.tags.isNotEmpty()) existing.tags else defaultTags,
                        dateAdded = existing?.dateAdded ?: isoFromMillis(file.lastModified()),
                        sizeBytes = file.length(),
                        format = file.extension.uppercase(Locale.ROOT).ifBlank { "VIDEO" },
                        width = existing?.width ?: 1920,
                        height = existing?.height ?: 1080,
                        durationSeconds = existing?.durationSeconds ?: 42,
                        fps = existing?.fps ?: 60,
                        codec = existing?.codec ?: file.extension.uppercase(Locale.ROOT),
                        artist = existing?.artist ?: ""
                    )
                )
            }

            // 3. Scan Comics (subfolder per comic)
            nextItems.addAll(
                scanCollectionDirectory(
                    File(activeDirs.comicsPath),
                    MediaType.COMIC,
                    existingByPath
                )
            )

            // 4. Scan Image Sets (subfolder per image set)
            nextItems.addAll(
                scanCollectionDirectory(
                    File(activeDirs.imageSetsPath),
                    MediaType.IMAGESET,
                    existingByPath
                )
            )

            dao.clearAllMediaItems()
            dao.insertAllMediaItems(nextItems.map { it.toEntity() })

            val updatedDirs = activeDirs.copy(lastIndexedAt = nowIsoString())
            saveDirectories(updatedDirs)
            nextItems.size
        }

    /**
     * Imports user-selected Uri files (from Android Photo Picker or Document Picker) into the vault directory and scans.
     */
    suspend fun importUrisToVault(
        uris: List<Uri>,
        targetType: MediaType,
        collectionName: String? = null
    ): Int = withContext(Dispatchers.IO) {
        val currentConfig = dao.getVaultConfigOnce()
        val dirs = if (currentConfig != null) {
            VaultMappers.directoryFromJson(currentConfig.directoriesJson)
        } else {
            defaultDirectories()
        }

        val baseDir = when (targetType) {
            MediaType.VIDEO -> File(dirs.videosPath)
            MediaType.COMIC -> File(dirs.comicsPath)
            MediaType.IMAGESET -> File(dirs.imageSetsPath)
            MediaType.IMAGE -> File(dirs.imagesPath)
        }.apply { mkdirs() }

        val targetSubfolder = if (targetType == MediaType.COMIC || targetType == MediaType.IMAGESET) {
            val safeName = (collectionName ?: "Imported_${targetType.key}_${System.currentTimeMillis() % 10000}")
                .replace(Regex("[^a-zA-Z0-9-_ ]"), "_")
            File(baseDir, safeName).apply { mkdirs() }
        } else {
            baseDir
        }

        var importedCount = 0
        uris.forEachIndexed { idx, uri ->
            try {
                val displayName = resolveUriDisplayName(uri)
                    ?: "imported_${idx + 1}.${if (targetType == MediaType.VIDEO) "mp4" else "png"}"
                val destFile = File(targetSubfolder, displayName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                importedCount++
            } catch (_: Exception) {
            }
        }

        scanFilesystemLibrary(dirs)
        importedCount
    }

    suspend fun exportDatabaseJson(): String = withContext(Dispatchers.IO) {
        val configEntity = dao.getVaultConfigOnce()
        val dirs = if (configEntity != null) {
            VaultMappers.directoryFromJson(configEntity.directoriesJson)
        } else {
            defaultDirectories()
        }
        val app = if (configEntity != null) {
            VaultMappers.appearanceFromJson(configEntity.appearanceJson)
        } else {
            AppearanceConfig()
        }
        val items = dao.getAllMediaItemsOnce().map { it.toDomain() }

        val root = JSONObject()
        root.put("version", "2.0.0")
        root.put("exportedAt", nowIsoString())
        root.put("directories", JSONObject(VaultMappers.directoryToJson(dirs)))
        root.put("appearance", JSONObject(VaultMappers.appearanceToJson(app)))

        val itemsArr = JSONArray()
        items.forEach { item ->
            val entity = item.toEntity()
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type.key)
            obj.put("title", item.title)
            obj.put("filename", item.filename)
            obj.put("localPath", item.localPath)
            obj.put("folderPath", item.folderPath)
            obj.put("thumbnailUrl", item.thumbnailUrl)
            obj.put("mediaUrl", item.mediaUrl)
            obj.put("rating", item.rating)
            obj.put("isFavorite", item.isFavorite)
            obj.put("notes", item.notes)
            obj.put("tags", JSONArray(entity.tagsJson))
            obj.put("dateAdded", item.dateAdded)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("format", item.format)
            obj.put("width", item.width)
            obj.put("height", item.height)
            obj.put("durationSeconds", item.durationSeconds)
            obj.put("pageCount", item.pageCount)
            obj.put("lastReadPage", item.lastReadPage)
            obj.put("readPages", JSONArray(entity.readPagesJson))
            obj.put("pages", JSONArray(entity.pagesJson))
            itemsArr.put(obj)
        }
        root.put("mediaItems", itemsArr)
        root.toString(2)
    }

    suspend fun importDatabaseJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val mediaArr = root.optJSONArray("mediaItems") ?: return@withContext false

            val entities = mutableListOf<MediaItemEntity>()
            for (i in 0 until mediaArr.length()) {
                val obj = mediaArr.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val pagesArr = obj.optJSONArray("pages") ?: JSONArray()
                val readArr = obj.optJSONArray("readPages") ?: JSONArray()

                entities.add(
                    MediaItemEntity(
                        id = obj.optString("id", "item-$i"),
                        type = obj.optString("type", "image"),
                        title = obj.optString("title", "Untitled"),
                        filename = obj.optString("filename", "file"),
                        localPath = obj.optString("localPath", ""),
                        folderPath = obj.optString("folderPath", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", ""),
                        mediaUrl = obj.optString("mediaUrl", ""),
                        rating = obj.optInt("rating", 0),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        notes = obj.optString("notes", ""),
                        tagsJson = tagsArr.toString(),
                        dateAdded = obj.optString("dateAdded", nowIsoString()),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        format = obj.optString("format", "PNG"),
                        width = obj.optInt("width", 1920),
                        height = obj.optInt("height", 1080),
                        durationSeconds = obj.optInt("durationSeconds", 0),
                        fps = obj.optInt("fps", 60),
                        codec = obj.optString("codec", "MP4"),
                        pagesJson = pagesArr.toString(),
                        pageCount = obj.optInt("pageCount", maxOf(1, pagesArr.length())),
                        lastReadPage = obj.optInt("lastReadPage", 1),
                        readPagesJson = readArr.toString(),
                        lastReadAt = obj.optString("lastReadAt", ""),
                        artist = obj.optString("artist", ""),
                        series = obj.optString("series", "")
                    )
                )
            }

            dao.clearAllMediaItems()
            dao.insertAllMediaItems(entities)

            val dirsObj = root.optJSONObject("directories")
            if (dirsObj != null) {
                saveDirectories(VaultMappers.directoryFromJson(dirsObj.toString()))
            }
            val appObj = root.optJSONObject("appearance")
            if (appObj != null) {
                saveAppearance(VaultMappers.appearanceFromJson(appObj.toString()))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun scanCollectionDirectory(
        rootDir: File,
        collectionType: MediaType,
        existingByPath: Map<String, MediaItem>
    ): List<MediaItem> {
        val results = mutableListOf<MediaItem>()
        if (!rootDir.exists() || !rootDir.isDirectory) return results

        val folderToImages = linkedMapOf<File, MutableList<File>>()
        val allFiles = walkFilesRecursive(rootDir)
        for (file in allFiles) {
            val ext = "." + file.extension.lowercase(Locale.ROOT)
            if (!IMAGE_EXTENSIONS.contains(ext)) continue
            val parent = file.parentFile ?: continue
            folderToImages.getOrPut(parent) { mutableListOf() }.add(file)
        }

        for ((folder, imageFiles) in folderToImages) {
            val sortedFiles = imageFiles.sortedWith { a, b ->
                MediaLogic.naturalCompare(a.name, b.name)
            }
            if (sortedFiles.isEmpty()) continue

            val folderName = folder.name
            val itemId = stableIdFromPath(collectionType.key, folder.absolutePath)
            val existing = existingByPath[folder.absolutePath]

            val pages = mutableListOf<MediaPageItem>()
            var totalBytes = 0L
            var latestMtime = 0L

            sortedFiles.forEachIndexed { idx, imgFile ->
                val sz = imgFile.length()
                totalBytes += sz
                if (imgFile.lastModified() > latestMtime) {
                    latestMtime = imgFile.lastModified()
                }
                val dims = readImageDimensions(imgFile)
                val ext = imgFile.extension.uppercase(Locale.ROOT).ifBlank { "IMG" }
                val existingPage = existing?.pages?.find {
                    it.filename == imgFile.name || it.pageNumber == idx + 1
                }

                pages.add(
                    MediaPageItem(
                        id = "$itemId-page-${idx + 1}",
                        pageNumber = idx + 1,
                        title = "Page ${String.format(Locale.US, "%02d", idx + 1)}",
                        filename = imgFile.name,
                        localPath = imgFile.absolutePath,
                        url = imgFile.absolutePath,
                        width = dims.width,
                        height = dims.height,
                        sizeBytes = sz,
                        format = ext,
                        rating = existingPage?.rating ?: 0,
                        isFavorite = existingPage?.isFavorite ?: false,
                        tags = existingPage?.tags ?: emptyList(),
                        notes = existingPage?.notes ?: ""
                    )
                )
            }

            val firstPage = pages.first()
            val defaultTags = sanitizeTagsFromPath(rootDir, sortedFiles.first(), collectionType.key)

            results.add(
                MediaItem(
                    id = existing?.id ?: itemId,
                    type = collectionType,
                    title = existing?.title ?: folderName.replace(Regex("[-_]+"), " "),
                    filename = folderName,
                    localPath = folder.absolutePath,
                    folderPath = folder.parentFile?.absolutePath ?: rootDir.absolutePath,
                    thumbnailUrl = firstPage.url,
                    mediaUrl = firstPage.url,
                    rating = existing?.rating ?: 0,
                    isFavorite = existing?.isFavorite ?: false,
                    notes = existing?.notes ?: "",
                    tags = if (existing != null && existing.tags.isNotEmpty()) existing.tags else defaultTags,
                    dateAdded = existing?.dateAdded ?: isoFromMillis(latestMtime),
                    sizeBytes = totalBytes,
                    format = "FOLDER",
                    width = firstPage.width,
                    height = firstPage.height,
                    pages = pages,
                    pageCount = pages.size,
                    lastReadPage = if (collectionType == MediaType.COMIC) {
                        (existing?.lastReadPage ?: 1).coerceIn(1, pages.size)
                    } else 1,
                    readPages = if (collectionType == MediaType.COMIC) {
                        existing?.readPages?.filter { it in 1..pages.size } ?: emptyList()
                    } else emptyList(),
                    lastReadAt = existing?.lastReadAt ?: "",
                    artist = existing?.artist ?: "",
                    series = existing?.series ?: ""
                )
            )
        }

        return results
    }

    private fun walkFilesRecursive(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val result = mutableListOf<File>()
        dir.listFiles()?.forEach { child ->
            if (child.name.startsWith(".")) return@forEach
            if (child.isDirectory) {
                result.addAll(walkFilesRecursive(child))
            } else if (child.isFile) {
                result.add(child)
            }
        }
        return result
    }

    private fun readImageDimensions(file: File): ImageHeaderParser.ImageDimensions {
        try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(65536)
                val read = fis.read(buffer)
                if (read > 0) {
                    val dims = ImageHeaderParser.parseImageDimensionsFromBuffer(
                        buffer.copyOf(read)
                    )
                    if (dims != null) return dims
                }
            }
        } catch (_: Exception) {
        }
        return ImageHeaderParser.ImageDimensions(1200, 1600)
    }

    private fun sanitizeTagsFromPath(rootDir: File, file: File, defaultTag: String): List<String> {
        val tags = linkedSetOf(defaultTag)
        try {
            val rel = file.parentFile?.relativeToOrSelf(rootDir)?.path ?: ""
            if (rel.isNotEmpty() && rel != "." && !rel.startsWith("..")) {
                rel.split(File.separatorChar).forEach { seg ->
                    val cleaned = seg.trim().lowercase(Locale.ROOT)
                        .replace(Regex("[^a-z0-9-_]+"), "-")
                        .trim('-')
                    if (cleaned.isNotEmpty()) tags.add(cleaned)
                }
            }
        } catch (_: Exception) {
        }
        return tags.toList()
    }

    private fun resolveUriDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(idx)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stableIdFromPath(type: String, filePath: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest("$type:$filePath".toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(14)
        return "$type-$hex"
    }

    private fun nowIsoString(): String = isoFromMillis(System.currentTimeMillis())

    private fun isoFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(if (millis > 0L) millis else System.currentTimeMillis()))
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf(
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".avif", ".bmp", ".svg"
        )
        val VIDEO_EXTENSIONS = setOf(
            ".mp4", ".webm", ".mkv", ".mov", ".avi", ".m4v", ".ogv"
        )
    }
}
