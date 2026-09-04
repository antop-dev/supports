package ai.antop.supports.service

import ai.antop.supports.common.IdGenerator
import ai.antop.supports.config.AppProperties
import ai.antop.supports.exception.InvalidUploadException
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.name

@Service
class FileStorageService(
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val root: Path = Path.of(appProperties.upload.dir).toAbsolutePath().normalize()
    private val pendingRoot: Path = root.resolve(PENDING_DIR_NAME)

    data class StoredFile(
        val originalName: String,
        val storedName: String,
        val contentType: String?,
        val size: Long,
    )

    @PostConstruct
    fun init() {
        Files.createDirectories(root)
        Files.createDirectories(pendingRoot)
        log.info("Upload directory: {}", root)
    }

    fun load(
        feedbackId: String,
        storedName: String,
    ): Path {
        val dir = feedbackDir(feedbackId)
        val target = dir.resolve(storedName).normalize()
        require(target.startsWith(dir)) { "잘못된 파일 경로입니다." }
        return target
    }

    // ---------- 청크 업로드 ----------

    /** 업로드를 시작하고 임시 id 를 발급한다. */
    fun beginUpload(declaredLength: Long): String {
        if (declaredLength <= 0) {
            throw InvalidUploadException("파일 크기를 확인할 수 없습니다.")
        }
        if (declaredLength > appProperties.upload.maxFileSizeBytes) {
            val maxMb = appProperties.upload.maxFileSizeBytes / (1024 * 1024)
            throw InvalidUploadException("파일 하나의 최대 크기(${maxMb}MB)를 초과했습니다.")
        }
        val id = IdGenerator.newId()
        val dir = pendingDir(id)
        Files.createDirectories(dir)
        Files.writeString(dir.resolve(META_FILE), declaredLength.toString())
        return id
    }

    /** 청크 하나를 지정한 오프셋에 기록한다. 재시도로 같은 오프셋이 다시 오면 덮어써서 멱등하게 처리한다. */
    fun writeChunk(
        id: String,
        offset: Long,
        chunkLength: Long,
        originalName: String?,
        input: InputStream,
    ) {
        val dir = pendingDir(id)
        if (!Files.isDirectory(dir)) {
            throw InvalidUploadException("업로드를 찾을 수 없거나 만료되었습니다.")
        }
        val declaredLength = readDeclaredLength(dir)
        if (chunkLength > appProperties.upload.chunkSizeBytes || offset < 0 || offset + chunkLength > declaredLength) {
            throw InvalidUploadException("잘못된 청크 요청입니다.")
        }

        val dataFile = dir.resolve(DATA_FILE)
        if (!Files.exists(dataFile)) {
            Files.createFile(dataFile)
        }
        RandomAccessFile(dataFile.toFile(), "rw").use { raf ->
            raf.seek(offset)
            val buffer = ByteArray(8192)
            var remaining = chunkLength
            while (remaining > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (n < 0) break
                raf.write(buffer, 0, n)
                remaining -= n
            }
        }
        if (!originalName.isNullOrBlank()) {
            Files.writeString(dir.resolve(NAME_FILE), originalName)
        }
    }

    /** 업로드를 취소하고 임시 파일을 지운다. 없는 id 를 지워도 조용히 넘어간다(이미 정리됐을 수 있음). */
    fun discardUpload(id: String) {
        val dir = pendingDirOrNull(id) ?: return
        FileSystemUtils.deleteRecursively(dir)
    }

    /**
     * 완료된 업로드를 의견의 첨부 파일로 확정한다(임시 위치 → 의견 폴더로 이동).
     * 완료되지 않았거나 없는 id 면 [InvalidUploadException].
     */
    fun claim(
        feedbackId: String,
        uploadId: String,
    ): StoredFile {
        val dir = pendingDir(uploadId)
        if (!Files.isDirectory(dir)) {
            throw InvalidUploadException("파일 업로드가 만료되었거나 잘못되었습니다. 다시 첨부해주세요.")
        }
        val declaredLength = readDeclaredLength(dir)
        val dataFile = dir.resolve(DATA_FILE)
        if (!Files.exists(dataFile) || Files.size(dataFile) != declaredLength) {
            throw InvalidUploadException("파일 업로드가 아직 완료되지 않았습니다.")
        }

        val originalName =
            dir
                .resolve(NAME_FILE)
                .takeIf { it.exists() }
                ?.let { Files.readString(it) }
                ?.takeIf { it.isNotBlank() }
                ?: "file"
        val ext = extensionOf(originalName)
        val storedName = IdGenerator.newId() + ext

        val targetDir = feedbackDir(feedbackId)
        Files.createDirectories(targetDir)
        val target = targetDir.resolve(storedName).normalize()
        require(target.startsWith(targetDir)) { "잘못된 파일 경로입니다." }

        Files.move(dataFile, target, StandardCopyOption.REPLACE_EXISTING)
        val contentType = runCatching { Files.probeContentType(target) }.getOrNull()
        val size = Files.size(target)
        FileSystemUtils.deleteRecursively(dir)

        return StoredFile(
            originalName = originalName,
            storedName = storedName,
            contentType = contentType,
            size = size,
        )
    }

    /** 폼 제출로 이어지지 않아 방치된 임시 업로드를 정기적으로 지운다. */
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 60 * 60 * 1000L)
    fun cleanupExpiredPendingUploads() {
        val ttl = Duration.ofHours(appProperties.upload.pendingTtlHours)
        val cutoff = Instant.now().minus(ttl)
        if (!Files.isDirectory(pendingRoot)) {
            return
        }
        Files.newDirectoryStream(pendingRoot).use { dirs ->
            dirs.filter(Files::isDirectory).forEach { dir ->
                val metaFile = dir.resolve(META_FILE)
                val modifiedAt =
                    runCatching { Files.getLastModifiedTime(if (metaFile.exists()) metaFile else dir).toInstant() }
                        .getOrNull()
                if (modifiedAt == null || modifiedAt.isBefore(cutoff)) {
                    runCatching { FileSystemUtils.deleteRecursively(dir) }
                        .onFailure { log.warn("Failed to clean up pending upload: {}", dir.name, it) }
                }
            }
        }
    }

    private fun readDeclaredLength(dir: Path): Long =
        runCatching { Files.readString(dir.resolve(META_FILE)).trim().toLong() }
            .getOrElse { throw InvalidUploadException("업로드를 찾을 수 없거나 만료되었습니다.") }

    private fun extensionOf(originalName: String): String =
        originalName.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }

    private fun feedbackDir(feedbackId: String): Path = root.resolve(feedbackId).normalize()

    /** 경로 조작(`..`, `/` 등)을 막기 위해 id 형식을 검증하고, pendingRoot 바로 아래 폴더만 반환한다. */
    private fun pendingDir(id: String): Path {
        if (!ID_PATTERN.matches(id)) {
            throw InvalidUploadException("잘못된 업로드 id 입니다.")
        }
        val dir = pendingRoot.resolve(id).normalize()
        if (!dir.startsWith(pendingRoot)) {
            throw InvalidUploadException("잘못된 업로드 id 입니다.")
        }
        return dir
    }

    private fun pendingDirOrNull(id: String): Path? =
        if (ID_PATTERN.matches(id)) {
            pendingDir(id).takeIf { Files.isDirectory(it) }
        } else {
            null
        }

    companion object {
        private const val PENDING_DIR_NAME = "_pending"
        private const val META_FILE = "meta"
        private const val NAME_FILE = "name"
        private const val DATA_FILE = "data"
        private val ID_PATTERN = Regex("^[0-9A-Za-z]+$")
    }
}
