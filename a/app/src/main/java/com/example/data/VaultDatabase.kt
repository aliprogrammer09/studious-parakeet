package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.model.AppearanceConfig
import com.example.model.DirectoryConfig
import com.example.model.MediaItem
import com.example.model.MediaPageItem
import com.example.model.MediaType
import com.example.model.ReaderDirection
import com.example.model.ReaderFitMode
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val filename: String,
    val localPath: String,
    val folderPath: String,
    val thumbnailUrl: String,
    val mediaUrl: String,
    val rating: Int,
    val isFavorite: Boolean,
    val notes: String,
    val tagsJson: String,
    val dateAdded: String,
    val sizeBytes: Long,
    val format: String,
    val width: Int,
    val height: Int,
    val durationSeconds: Int,
    val fps: Int,
    val codec: String,
    val pagesJson: String,
    val pageCount: Int,
    val lastReadPage: Int,
    val readPagesJson: String,
    val lastReadAt: String,
    val artist: String,
    val series: String
)

@Entity(tableName = "vault_config")
data class VaultConfigEntity(
    @PrimaryKey val configKey: String = "singleton",
    val directoriesJson: String,
    val appearanceJson: String,
    val presetsJson: String
)

@Dao
interface VaultDao {
    @Query("SELECT * FROM media_items")
    fun observeAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItemsOnce(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMediaItems(items: List<MediaItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: String)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMediaItems()

    @Query("SELECT * FROM vault_config WHERE configKey = 'singleton' LIMIT 1")
    fun observeVaultConfig(): Flow<VaultConfigEntity?>

    @Query("SELECT * FROM vault_config WHERE configKey = 'singleton' LIMIT 1")
    suspend fun getVaultConfigOnce(): VaultConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVaultConfig(config: VaultConfigEntity)
}

@Database(
    entities = [MediaItemEntity::class, VaultConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
}

object VaultMappers {

    fun MediaItemEntity.toDomain(): MediaItem {
        val tagsList = mutableListOf<String>()
        try {
            val arr = JSONArray(tagsJson)
            for (i in 0 until arr.length()) {
                tagsList.add(arr.getString(i))
            }
        } catch (_: Exception) {
        }

        val readPagesList = mutableListOf<Int>()
        try {
            val arr = JSONArray(readPagesJson)
            for (i in 0 until arr.length()) {
                readPagesList.add(arr.getInt(i))
            }
        } catch (_: Exception) {
        }

        val pagesList = mutableListOf<MediaPageItem>()
        try {
            val arr = JSONArray(pagesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pageTags = mutableListOf<String>()
                val ptArr = obj.optJSONArray("tags")
                if (ptArr != null) {
                    for (j in 0 until ptArr.length()) {
                        pageTags.add(ptArr.getString(j))
                    }
                }
                pagesList.add(
                    MediaPageItem(
                        id = obj.optString("id"),
                        pageNumber = obj.optInt("pageNumber", i + 1),
                        title = obj.optString("title", "Page ${i + 1}"),
                        filename = obj.optString("filename"),
                        localPath = obj.optString("localPath"),
                        url = obj.optString("url"),
                        width = obj.optInt("width", 1600),
                        height = obj.optInt("height", 2400),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        format = obj.optString("format", "PNG"),
                        rating = obj.optInt("rating", 0),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        tags = pageTags,
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {
        }

        return MediaItem(
            id = id,
            type = MediaType.fromKey(type),
            title = title,
            filename = filename,
            localPath = localPath,
            folderPath = folderPath,
            thumbnailUrl = thumbnailUrl,
            mediaUrl = mediaUrl,
            rating = rating,
            isFavorite = isFavorite,
            notes = notes,
            tags = tagsList,
            dateAdded = dateAdded,
            sizeBytes = sizeBytes,
            format = format,
            width = width,
            height = height,
            durationSeconds = durationSeconds,
            fps = fps,
            codec = codec,
            pages = pagesList,
            pageCount = pageCount,
            lastReadPage = lastReadPage,
            readPages = readPagesList,
            lastReadAt = lastReadAt,
            artist = artist,
            series = series
        )
    }

    fun MediaItem.toEntity(): MediaItemEntity {
        val tagsArr = JSONArray()
        tags.forEach { tagsArr.put(it) }

        val readArr = JSONArray()
        readPages.forEach { readArr.put(it) }

        val pagesArr = JSONArray()
        pages.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("pageNumber", p.pageNumber)
            pObj.put("title", p.title)
            pObj.put("filename", p.filename)
            pObj.put("localPath", p.localPath)
            pObj.put("url", p.url)
            pObj.put("width", p.width)
            pObj.put("height", p.height)
            pObj.put("sizeBytes", p.sizeBytes)
            pObj.put("format", p.format)
            pObj.put("rating", p.rating)
            pObj.put("isFavorite", p.isFavorite)
            val ptArr = JSONArray()
            p.tags.forEach { ptArr.put(it) }
            pObj.put("tags", ptArr)
            pObj.put("notes", p.notes)
            pagesArr.put(pObj)
        }

        return MediaItemEntity(
            id = id,
            type = type.key,
            title = title,
            filename = filename,
            localPath = localPath,
            folderPath = folderPath,
            thumbnailUrl = thumbnailUrl,
            mediaUrl = mediaUrl,
            rating = rating,
            isFavorite = isFavorite,
            notes = notes,
            tagsJson = tagsArr.toString(),
            dateAdded = dateAdded,
            sizeBytes = sizeBytes,
            format = format,
            width = width,
            height = height,
            durationSeconds = durationSeconds,
            fps = fps,
            codec = codec,
            pagesJson = pagesArr.toString(),
            pageCount = pageCount,
            lastReadPage = lastReadPage,
            readPagesJson = readArr.toString(),
            lastReadAt = lastReadAt,
            artist = artist,
            series = series
        )
    }

    fun directoryToJson(dir: DirectoryConfig): String {
        val obj = JSONObject()
        obj.put("imagesPath", dir.imagesPath)
        obj.put("videosPath", dir.videosPath)
        obj.put("comicsPath", dir.comicsPath)
        obj.put("imageSetsPath", dir.imageSetsPath)
        obj.put("lastIndexedAt", dir.lastIndexedAt)
        obj.put("autoScanOnStartup", dir.autoScanOnStartup)
        return obj.toString()
    }

    fun directoryFromJson(json: String): DirectoryConfig {
        return try {
            val obj = JSONObject(json)
            DirectoryConfig(
                imagesPath = obj.optString("imagesPath", "/storage/emulated/0/Domina/vault/images"),
                videosPath = obj.optString("videosPath", "/storage/emulated/0/Domina/vault/videos"),
                comicsPath = obj.optString("comicsPath", "/storage/emulated/0/Domina/vault/comics"),
                imageSetsPath = obj.optString("imageSetsPath", "/storage/emulated/0/Domina/vault/imagesets"),
                lastIndexedAt = obj.optString("lastIndexedAt", "2026-10-22T09:39:19.023Z"),
                autoScanOnStartup = obj.optBoolean("autoScanOnStartup", true)
            )
        } catch (_: Exception) {
            DirectoryConfig()
        }
    }

    fun appearanceToJson(app: AppearanceConfig): String {
        val obj = JSONObject()
        obj.put("accentColor", app.accentColor)
        obj.put("customAccentHex", app.customAccentHex)
        obj.put("accentIntensity", app.accentIntensity)
        obj.put("bgDarkness", app.bgDarkness)
        obj.put("borderStyle", app.borderStyle)
        obj.put("gridDensity", app.gridDensity)
        obj.put("gridColumns", app.gridColumns)
        obj.put("cardGap", app.cardGap)
        obj.put("cardRadius", app.cardRadius)
        obj.put("thumbnailAspect", app.thumbnailAspect)
        obj.put("thumbnailFit", app.thumbnailFit)
        obj.put("showCardSize", app.showCardSize)
        obj.put("showCardTags", app.showCardTags)
        obj.put("showCardRating", app.showCardRating)
        obj.put("showCardDimensions", app.showCardDimensions)
        obj.put("hoverZoom", app.hoverZoom)
        obj.put("defaultReaderFit", app.defaultReaderFit.key)
        obj.put("defaultReaderDirection", app.defaultReaderDirection.key)
        obj.put("defaultVideoLoop", app.defaultVideoLoop)
        obj.put("defaultAutoPlayNext", app.defaultAutoPlayNext)
        obj.put("slideshowIntervalSeconds", app.slideshowIntervalSeconds)
        obj.put("slideshowLoop", app.slideshowLoop)
        obj.put("panicModeBlur", app.panicModeBlur)
        return obj.toString()
    }

    fun appearanceFromJson(json: String): AppearanceConfig {
        return try {
            val obj = JSONObject(json)
            val fitStr = obj.optString("defaultReaderFit", "height")
            val dirStr = obj.optString("defaultReaderDirection", "ltr")
            AppearanceConfig(
                accentColor = obj.optString("accentColor", "amethyst"),
                customAccentHex = obj.optString("customAccentHex", "#9D4EDD"),
                accentIntensity = obj.optInt("accentIntensity", 90),
                bgDarkness = obj.optString("bgDarkness", "obsidian"),
                borderStyle = obj.optString("borderStyle", "glow"),
                gridDensity = obj.optString("gridDensity", "standard"),
                gridColumns = obj.optInt("gridColumns", 0),
                cardGap = obj.optString("cardGap", "standard"),
                cardRadius = obj.optString("cardRadius", "rounded"),
                thumbnailAspect = obj.optString("thumbnailAspect", "portrait"),
                thumbnailFit = obj.optString("thumbnailFit", "cover"),
                showCardSize = obj.optBoolean("showCardSize", true),
                showCardTags = obj.optBoolean("showCardTags", true),
                showCardRating = obj.optBoolean("showCardRating", true),
                showCardDimensions = obj.optBoolean("showCardDimensions", true),
                hoverZoom = obj.optBoolean("hoverZoom", true),
                defaultReaderFit = ReaderFitMode.entries.find { it.key == fitStr } ?: ReaderFitMode.HEIGHT,
                defaultReaderDirection = ReaderDirection.entries.find { it.key == dirStr } ?: ReaderDirection.LTR,
                defaultVideoLoop = obj.optBoolean("defaultVideoLoop", false),
                defaultAutoPlayNext = obj.optBoolean("defaultAutoPlayNext", true),
                slideshowIntervalSeconds = obj.optInt("slideshowIntervalSeconds", 3),
                slideshowLoop = obj.optBoolean("slideshowLoop", true),
                panicModeBlur = obj.optBoolean("panicModeBlur", false)
            )
        } catch (_: Exception) {
            AppearanceConfig()
        }
    }
}
