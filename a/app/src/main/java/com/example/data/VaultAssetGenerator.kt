package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.model.MediaItem
import com.example.model.MediaPageItem
import com.example.model.MediaType
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object VaultAssetGenerator {

    fun ensureDefaultVaultMediaOnDisk(context: Context): List<MediaItem> {
        val vaultRoot = File(context.filesDir, "vault")
        val imagesDir = File(vaultRoot, "images").apply { mkdirs() }
        val videosDir = File(vaultRoot, "videos").apply { mkdirs() }
        val comicsDir = File(vaultRoot, "comics").apply { mkdirs() }
        val imageSetsDir = File(vaultRoot, "imagesets").apply { mkdirs() }

        // 1. Generate Standalone Images matching domina-db.json + rich Gothic art plates
        val img1File = File(imagesDir, "amethyst_portrait_1.png")
        if (!img1File.exists()) {
            renderGothicArtwork(
                targetFile = img1File,
                width = 1200,
                height = 1600,
                topColor = 0xFF2E1065.toInt(),
                midColor = 0xFF4C1D95.toInt(),
                bottomColor = 0xFF090414.toInt(),
                accentColor = 0xFFC084FC.toInt(),
                secondaryColor = 0xFFE879F9.toInt(),
                title = "AMETHYST SANCTUM I",
                subtitle = "PORTRAIT PLATE • 1200×1600",
                motif = 0
            )
        }

        val img2File = File(imagesDir, "crimson_portrait_2.png")
        if (!img2File.exists()) {
            renderGothicArtwork(
                targetFile = img2File,
                width = 1200,
                height = 1600,
                topColor = 0xFF4C0519.toInt(),
                midColor = 0xFF581C87.toInt(),
                bottomColor = 0xFF090410.toInt(),
                accentColor = 0xFFE11D48.toInt(),
                secondaryColor = 0xFFA855F7.toInt(),
                title = "CRIMSON CORSET II",
                subtitle = "VELVET ARCHIVE • 1200×1600",
                motif = 1
            )
        }

        val img3File = File(imagesDir, "obsidian_cathedral_3.png")
        if (!img3File.exists()) {
            renderGothicArtwork(
                targetFile = img3File,
                width = 1920,
                height = 1080,
                topColor = 0xFF1E1B4B.toInt(),
                midColor = 0xFF3B0764.toInt(),
                bottomColor = 0xFF06030C.toInt(),
                accentColor = 0xFF818CF8.toInt(),
                secondaryColor = 0xFFC084FC.toInt(),
                title = "OBSIDIAN CATHEDRAL",
                subtitle = "ULTRAWIDE STUDY • 1920×1080",
                motif = 2
            )
        }

        // 2. Generate Comic Volume "Nocturne Issue 01" with 3 pages
        val comicFolder = File(comicsDir, "Nocturne Issue 01").apply { mkdirs() }
        val comicPageSpecs = listOf(
            Triple("001_cover.png", "NOCTURNE ISSUE 01 • COVER", 0xFF9333EA.toInt()),
            Triple("002_scene.png", "NOCTURNE • PAGE 02 • THE CITADEL", 0xFFC084FC.toInt()),
            Triple("003_finale.png", "NOCTURNE • PAGE 03 • ECLIPSE FINALE", 0xFFE879F9.toInt())
        )
        val comicPages = comicPageSpecs.mapIndexed { idx, (fname, label, color) ->
            val pFile = File(comicFolder, fname)
            if (!pFile.exists()) {
                renderGothicArtwork(
                    targetFile = pFile,
                    width = 1200,
                    height = 1600,
                    topColor = 0xFF270E47.toInt(),
                    midColor = 0xFF150828.toInt(),
                    bottomColor = 0xFF06030C.toInt(),
                    accentColor = color,
                    secondaryColor = 0xFFA855F7.toInt(),
                    title = label,
                    subtitle = "NOCTURNE VOL. 1 • PAGE 0${idx + 1} OF 03",
                    motif = idx
                )
            }
            MediaPageItem(
                id = "comic-d3350996dc1a34-page-${idx + 1}",
                pageNumber = idx + 1,
                title = "Page 0${idx + 1}",
                filename = fname,
                localPath = pFile.absolutePath,
                url = pFile.absolutePath,
                width = 1200,
                height = 1600,
                sizeBytes = pFile.length().coerceAtLeast(64500L),
                format = "PNG",
                rating = if (idx == 0) 4 else if (idx == 1) 5 else 0,
                isFavorite = idx == 1,
                tags = emptyList(),
                notes = ""
            )
        }

        // 3. Generate Image Set "Obsidian Studio Set" with 3 plates
        val setFolder = File(imageSetsDir, "Obsidian Studio Set").apply { mkdirs() }
        val setPageSpecs = listOf(
            Triple("plate_01.png", "OBSIDIAN STUDIO • PLATE I", 0xFFA855F7.toInt()),
            Triple("plate_02.png", "OBSIDIAN STUDIO • PLATE II", 0xFFD946EF.toInt()),
            Triple("plate_03.png", "OBSIDIAN STUDIO • PLATE III", 0xFF8B5CF6.toInt())
        )
        val setPages = setPageSpecs.mapIndexed { idx, (fname, label, color) ->
            val pFile = File(setFolder, fname)
            if (!pFile.exists()) {
                renderGothicArtwork(
                    targetFile = pFile,
                    width = 1200,
                    height = 1600,
                    topColor = 0xFF1F0C3D.toInt(),
                    midColor = 0xFF120824.toInt(),
                    bottomColor = 0xFF07040E.toInt(),
                    accentColor = color,
                    secondaryColor = 0xFFC084FC.toInt(),
                    title = label,
                    subtitle = "STUDIO COLLECTION • PLATE 0${idx + 1}",
                    motif = (idx + 1) % 3
                )
            }
            MediaPageItem(
                id = "imageset-6a1c604a4ad693-page-${idx + 1}",
                pageNumber = idx + 1,
                title = "Page 0${idx + 1}",
                filename = fname,
                localPath = pFile.absolutePath,
                url = pFile.absolutePath,
                width = 1200,
                height = 1600,
                sizeBytes = pFile.length().coerceAtLeast(58400L),
                format = "PNG",
                rating = if (idx == 0) 5 else 0,
                isFavorite = idx == 0,
                tags = emptyList(),
                notes = ""
            )
        }

        // 4. Generate Video Poster & Playable MP4 file if supported
        val videoPosterFile = File(videosDir, "velvet_nocturne_poster.png")
        if (!videoPosterFile.exists()) {
            renderGothicArtwork(
                targetFile = videoPosterFile,
                width = 1280,
                height = 720,
                topColor = 0xFF3B0764.toInt(),
                midColor = 0xFF1E1B4B.toInt(),
                bottomColor = 0xFF07040E.toInt(),
                accentColor = 0xFFC084FC.toInt(),
                secondaryColor = 0xFFE11D48.toInt(),
                title = "VELVET NOCTURNE REEL",
                subtitle = "CINEMA STUDIO STREAM • 1080P 60FPS",
                motif = 2
            )
        }
        val videoMp4File = File(videosDir, "velvet_nocturne_stream.mp4")
        if (!videoMp4File.exists()) {
            generateShortMp4Safely(videoMp4File)
        }

        val videoPoster2File = File(videosDir, "amethyst_eclipse_poster.png")
        if (!videoPoster2File.exists()) {
            renderGothicArtwork(
                targetFile = videoPoster2File,
                width = 1280,
                height = 720,
                topColor = 0xFF4C0519.toInt(),
                midColor = 0xFF2E1065.toInt(),
                bottomColor = 0xFF06030C.toInt(),
                accentColor = 0xFFE879F9.toInt(),
                secondaryColor = 0xFF8B5CF6.toInt(),
                title = "AMETHYST ECLIPSE STUDIO",
                subtitle = "AMBIENT VISUALIZER • 1080P 60FPS",
                motif = 1
            )
        }
        val video2Mp4File = File(videosDir, "amethyst_eclipse_stream.mp4")
        if (!video2Mp4File.exists()) {
            if (videoMp4File.exists()) {
                videoMp4File.copyTo(video2Mp4File, overwrite = true)
            }
        }

        return listOf(
            MediaItem(
                id = "image-e6fbb5abb3363e",
                type = MediaType.IMAGE,
                title = "amethyst portrait 1",
                filename = img1File.name,
                localPath = img1File.absolutePath,
                folderPath = imagesDir.absolutePath,
                thumbnailUrl = img1File.absolutePath,
                mediaUrl = img1File.absolutePath,
                rating = 5,
                isFavorite = true,
                notes = "High-contrast royal amethyst study with velvet ambient lighting.",
                tags = listOf("image", "velvet", "amethyst", "gothic", "portrait"),
                dateAdded = "2026-10-22T02:38:16.216Z",
                sizeBytes = img1File.length().coerceAtLeast(148200L),
                format = "PNG",
                width = 1200,
                height = 1600,
                artist = "Domina Studio"
            ),
            MediaItem(
                id = "image-228126dcfc92b9",
                type = MediaType.IMAGE,
                title = "crimson portrait 2",
                filename = img2File.name,
                localPath = img2File.absolutePath,
                folderPath = imagesDir.absolutePath,
                thumbnailUrl = img2File.absolutePath,
                mediaUrl = img2File.absolutePath,
                rating = 4,
                isFavorite = false,
                notes = "Crimson and deep obsidian lighting composition.",
                tags = listOf("image", "corset", "crimson", "noir", "portrait"),
                dateAdded = "2026-10-22T02:39:16.216Z",
                sizeBytes = img2File.length().coerceAtLeast(152400L),
                format = "PNG",
                width = 1200,
                height = 1600,
                artist = "Domina Studio"
            ),
            MediaItem(
                id = "image-98ab12cd34ef56",
                type = MediaType.IMAGE,
                title = "obsidian cathedral 3",
                filename = img3File.name,
                localPath = img3File.absolutePath,
                folderPath = imagesDir.absolutePath,
                thumbnailUrl = img3File.absolutePath,
                mediaUrl = img3File.absolutePath,
                rating = 5,
                isFavorite = true,
                notes = "Architectural gothic nave in deep midnight indigo.",
                tags = listOf("image", "obsidian", "architecture", "gothic", "violet"),
                dateAdded = "2026-10-22T03:15:00.000Z",
                sizeBytes = img3File.length().coerceAtLeast(184320L),
                format = "PNG",
                width = 1920,
                height = 1080,
                artist = "Domina Studio"
            ),
            MediaItem(
                id = "video-7f8e9d1a2b3c4d",
                type = MediaType.VIDEO,
                title = "Velvet Nocturne Reel",
                filename = videoMp4File.name,
                localPath = videoMp4File.absolutePath,
                folderPath = videosDir.absolutePath,
                thumbnailUrl = videoPosterFile.absolutePath,
                mediaUrl = videoMp4File.absolutePath,
                rating = 5,
                isFavorite = true,
                notes = "60fps atmospheric gothic cinema sequence with A-B loop points.",
                tags = listOf("video", "velvet", "nocturne", "cinema", "violet"),
                dateAdded = "2026-10-22T04:00:00.000Z",
                sizeBytes = videoMp4File.length().coerceAtLeast(2458000L),
                format = "MP4",
                width = 1920,
                height = 1080,
                durationSeconds = 42,
                fps = 60,
                codec = "MP4",
                artist = "Domina Cinema"
            ),
            MediaItem(
                id = "video-3a4b5c6d7e8f9a",
                type = MediaType.VIDEO,
                title = "Amethyst Eclipse Studio",
                filename = video2Mp4File.name,
                localPath = video2Mp4File.absolutePath,
                folderPath = videosDir.absolutePath,
                thumbnailUrl = videoPoster2File.absolutePath,
                mediaUrl = video2Mp4File.absolutePath,
                rating = 4,
                isFavorite = false,
                notes = "Secondary studio reel for testing queue auto-play next.",
                tags = listOf("video", "amethyst", "eclipse", "noir"),
                dateAdded = "2026-10-22T04:12:00.000Z",
                sizeBytes = video2Mp4File.length().coerceAtLeast(1980000L),
                format = "MP4",
                width = 1920,
                height = 1080,
                durationSeconds = 95,
                fps = 60,
                codec = "MP4",
                artist = "Domina Cinema"
            ),
            MediaItem(
                id = "comic-d3350996dc1a34",
                type = MediaType.COMIC,
                title = "Nocturne Issue 01",
                filename = "Nocturne Issue 01",
                localPath = comicFolder.absolutePath,
                folderPath = comicsDir.absolutePath,
                thumbnailUrl = comicPages.first().url,
                mediaUrl = comicPages.first().url,
                rating = 5,
                isFavorite = true,
                notes = "Complete 3-page gothic illustrated comic volume with smart bookmark tracking.",
                tags = listOf("comic", "nocturne-issue-01", "gothic", "violet"),
                dateAdded = "2026-10-22T02:38:16.216Z",
                sizeBytes = comicPages.sumOf { it.sizeBytes },
                format = "FOLDER",
                width = 1200,
                height = 1600,
                pages = comicPages,
                pageCount = comicPages.size,
                lastReadPage = 2,
                readPages = listOf(1, 2),
                lastReadAt = "2026-10-22T09:41:22.563Z",
                artist = "Domina Press",
                series = "Nocturne"
            ),
            MediaItem(
                id = "imageset-6a1c604a4ad693",
                type = MediaType.IMAGESET,
                title = "Obsidian Studio Set",
                filename = "Obsidian Studio Set",
                localPath = setFolder.absolutePath,
                folderPath = imageSetsDir.absolutePath,
                thumbnailUrl = setPages.first().url,
                mediaUrl = setPages.first().url,
                rating = 4,
                isFavorite = false,
                notes = "Curated high-resolution darkroom plate collection.",
                tags = listOf("imageset", "obsidian-studio-set", "obsidian", "velvet"),
                dateAdded = "2026-10-22T02:38:16.217Z",
                sizeBytes = setPages.sumOf { it.sizeBytes },
                format = "FOLDER",
                width = 1200,
                height = 1600,
                pages = setPages,
                pageCount = setPages.size
            )
        )
    }

    fun renderGothicArtwork(
        targetFile: File,
        width: Int,
        height: Int,
        topColor: Int,
        midColor: Int,
        bottomColor: Int,
        accentColor: Int,
        secondaryColor: Int,
        title: String,
        subtitle: String,
        motif: Int
    ) {
        try {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Background linear gradient
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(topColor, midColor, bottomColor),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Ambient radial glow
            val cx = width * 0.5f
            val cy = height * 0.42f
            val radius = minOf(width, height) * 0.44f
            paint.shader = RadialGradient(
                cx, cy, radius * 1.3f,
                intArrayOf(
                    (accentColor and 0x00FFFFFF) or 0x66000000,
                    (secondaryColor and 0x00FFFFFF) or 0x22000000,
                    0x00000000
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 1.3f, paint)
            paint.shader = null

            // Outer ornate frame border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = (accentColor and 0x00FFFFFF) or 0x88000000.toInt()
            val inset = 42f
            canvas.drawRoundRect(
                RectF(inset, inset, width - inset, height - inset),
                24f, 24f, paint
            )
            paint.strokeWidth = 1.5f
            paint.color = (secondaryColor and 0x00FFFFFF) or 0x55000000.toInt()
            canvas.drawRoundRect(
                RectF(inset + 14f, inset + 14f, width - inset - 14f, height - inset - 14f),
                18f, 18f, paint
            )

            // Central geometric Gothic rose / diamond motif
            when (motif) {
                0 -> {
                    val path = Path().apply {
                        moveTo(cx, cy - radius * 0.68f)
                        lineTo(cx + radius * 0.52f, cy)
                        lineTo(cx, cy + radius * 0.68f)
                        lineTo(cx - radius * 0.52f, cy)
                        close()
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = (accentColor and 0x00FFFFFF) or 0x44000000
                    canvas.drawPath(path, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f
                    paint.color = accentColor
                    canvas.drawPath(path, paint)

                    paint.strokeWidth = 2.5f
                    paint.color = secondaryColor
                    canvas.drawCircle(cx, cy, radius * 0.42f, paint)
                    canvas.drawCircle(cx, cy, radius * 0.18f, paint)
                }
                1 -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f
                    paint.color = accentColor
                    canvas.drawCircle(cx, cy, radius * 0.55f, paint)
                    for (i in 0 until 8) {
                        val angle = Math.toRadians((i * 45).toDouble())
                        val x2 = (cx + Math.cos(angle) * radius * 0.55f).toFloat()
                        val y2 = (cy + Math.sin(angle) * radius * 0.55f).toFloat()
                        canvas.drawLine(cx, cy, x2, y2, paint)
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = (secondaryColor and 0x00FFFFFF) or 0x66000000
                    canvas.drawCircle(cx, cy, radius * 0.24f, paint)
                }
                else -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4.5f
                    paint.color = accentColor
                    val archRect = RectF(
                        cx - radius * 0.55f,
                        cy - radius * 0.65f,
                        cx + radius * 0.55f,
                        cy + radius * 0.65f
                    )
                    canvas.drawRoundRect(archRect, radius * 0.55f, radius * 0.55f, paint)
                    paint.color = secondaryColor
                    canvas.drawLine(cx, cy - radius * 0.65f, cx, cy + radius * 0.65f, paint)
                    canvas.drawLine(cx - radius * 0.55f, cy, cx + radius * 0.55f, cy, paint)
                }
            }

            // Title & Typography at bottom
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.color = 0xFFFAF5FF.toInt()
            paint.textSize = (width * 0.036f).coerceIn(28f, 48f)
            paint.isFakeBoldText = true
            canvas.drawText(title, cx, height - inset - 110f, paint)

            paint.isFakeBoldText = false
            paint.color = 0xFFD8B4FE.toInt()
            paint.textSize = (width * 0.022f).coerceIn(18f, 28f)
            canvas.drawText(subtitle, cx, height - inset - 64f, paint)

            FileOutputStream(targetFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            bmp.recycle()
        } catch (_: Exception) {
        }
    }

    private fun generateShortMp4Safely(outputFile: File) {
        try {
            val width = 640
            val height = 360
            val bitRate = 1_200_000
            val frameRate = 15
            val totalFrames = 45 // 3 seconds

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false

            val frameSize = width * height * 3 / 2
            val yuvData = ByteArray(frameSize)

            for (frameIdx in 0 until totalFrames) {
                val inputBufferIndex = encoder.dequeueInputBuffer(10_000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer: ByteBuffer? = encoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    // Fill animated purple/amethyst YUV pattern
                    val yBase = (28 + (frameIdx * 2) % 45).toByte()
                    for (i in 0 until width * height) {
                        yuvData[i] = yBase
                    }
                    for (i in width * height until frameSize step 2) {
                        yuvData[i] = 175.toByte()
                        if (i + 1 < frameSize) {
                            yuvData[i + 1] = 165.toByte()
                        }
                    }
                    inputBuffer?.put(yuvData)
                    val ptsUsec = frameIdx * 1_000_000L / frameRate
                    val flags = if (frameIdx == totalFrames - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    encoder.queueInputBuffer(inputBufferIndex, 0, frameSize, ptsUsec, flags)
                }

                var draining = true
                while (draining) {
                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outputIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                draining = false
                            }
                        }
                        else -> draining = false
                    }
                }
            }

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            encoder.stop()
            encoder.release()
        } catch (_: Throwable) {
            // If hardware/software AVC encoder is unavailable in headless JVM tests, ignore gracefully
        }
    }
}
