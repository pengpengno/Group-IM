package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import com.github.im.group.config.MediaPolicyRuntime
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.MobileCoreServices.kUTTypeData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureModePhoto
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypeCamera
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.drawInRect
import platform.UIKit.imageWithData
import platform.UIKit.scale
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.UniformTypeIdentifiers.UTTypeContent
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

@Composable
actual fun CameraPreviewView() {
}

actual fun getPlatformFilePicker(): FilePicker = IOSFilePicker()

private class IOSFilePicker : FilePicker {
    override suspend fun pickImage(): List<File> {
        val urls = IOSPickerCoordinator.pickDocument(listOf(utTypeIdentifier(UTTypeImage.identifier)))
        return urls.mapNotNull { urlToPickedFile(it) }
    }

    override suspend fun pickVideo(): List<File> {
        val urls = IOSPickerCoordinator.pickDocument(listOf(utTypeIdentifier(UTTypeMovie.identifier)))
        return urls.mapNotNull { urlToPickedFile(it) }
    }

    override suspend fun pickFile(): List<File> {
        val urls = IOSPickerCoordinator.pickDocument(listOf(utTypeIdentifier(UTTypeContent.identifier), kUTTypeData))
        return urls.mapNotNull { urlToPickedFile(it) }
    }

    override suspend fun takePhoto(): File? {
        val imageResult = IOSPickerCoordinator.takePhoto() ?: return null
        val fileName = "IMG_${NSUUID().UUIDString}.jpg"
        val imageBytes = compressImageBytesIfNeeded(
            fileName = fileName,
            mimeType = "image/jpeg",
            bytes = imageResult.toByteArray()
        )
        return File(
            name = fileName,
            path = "",
            mimeType = "image/jpeg",
            size = imageBytes.size.toLong(),
            data = FileData.Bytes(imageBytes)
        )
    }

    override suspend fun readFileBytes(file: File): ByteArray {
        val rawBytes = when (val data = file.data) {
            is FileData.Bytes -> data.data
            is FileData.Path -> {
                val url = resolveUrl(data.path) ?: error("Unsupported file path: ${data.path}")
                NSData.dataWithContentsOfURL(url)?.toByteArray()
                    ?: error("Unable to read file: ${data.path}")
            }
            FileData.None -> error("File data is empty")
        }

        return compressImageBytesIfNeeded(file.name, file.mimeType, rawBytes)
    }

    private fun urlToPickedFile(url: NSURL): File? {
        val data = NSData.dataWithContentsOfURL(url) ?: return null
        val fileName = url.lastPathComponent ?: "file_${NSUUID().UUIDString}"
        val pathExtension = url.pathExtension?.lowercase().orEmpty()
        val mimeType = guessMimeType(fileName, pathExtension)
        val bytes = compressImageBytesIfNeeded(fileName, mimeType, data.toByteArray())
        return File(
            name = fileName,
            path = url.absoluteString ?: "",
            mimeType = mimeType,
            size = bytes.size.toLong(),
            data = FileData.Bytes(bytes)
        )
    }
}

private object IOSPickerCoordinator {
    private var activeDelegate: NSObject? = null

    suspend fun pickDocument(documentTypes: List<String>): List<NSURL> = suspendCoroutine { continuation ->
        val presenter = currentViewController()
        if (presenter == null) {
            continuation.resume(emptyList())
            return@suspendCoroutine
        }

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                @Suppress("UNCHECKED_CAST")
                activeDelegate = null
                continuation.resume(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                activeDelegate = null
                continuation.resume(emptyList())
            }
        }

        val picker = UIDocumentPickerViewController(
            documentTypes = documentTypes,
            inMode = UIDocumentPickerModeImport
        )
        activeDelegate = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    suspend fun takePhoto(): NSData? = suspendCoroutine { continuation ->
        val presenter = currentViewController()
        if (presenter == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        val picker = UIImagePickerController().apply {
            sourceType = if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)) {
                UIImagePickerControllerSourceTypeCamera
            } else {
                UIImagePickerControllerSourceTypePhotoLibrary
            }
            mediaTypes = listOf(utTypeIdentifier(UTTypeImage.identifier))
            if (sourceType == UIImagePickerControllerSourceTypeCamera) {
                cameraCaptureMode = UIImagePickerControllerCameraCaptureModePhoto
            }
        }

        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage)
                    ?: (didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
                val imageData = image?.let { UIImageJPEGRepresentation(it, 0.92) }
                picker.dismissViewControllerAnimated(true) {
                    activeDelegate = null
                    continuation.resume(imageData)
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true) {
                    activeDelegate = null
                    continuation.resume(null)
                }
            }
        }

        activeDelegate = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }
}

private fun currentViewController(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    return generateSequence(root) { current ->
        current.presentedViewController
    }.lastOrNull()
}

private fun guessMimeType(fileName: String, extension: String): String? {
    val lowerName = fileName.lowercase()
    return when {
        lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || extension == "jpg" || extension == "jpeg" -> "image/jpeg"
        lowerName.endsWith(".png") || extension == "png" -> "image/png"
        lowerName.endsWith(".webp") || extension == "webp" -> "image/webp"
        lowerName.endsWith(".heic") || extension == "heic" -> "image/heic"
        lowerName.endsWith(".heif") || extension == "heif" -> "image/heif"
        lowerName.endsWith(".gif") || extension == "gif" -> "image/gif"
        lowerName.endsWith(".mov") || extension == "mov" -> "video/quicktime"
        lowerName.endsWith(".mp4") || extension == "mp4" -> "video/mp4"
        lowerName.endsWith(".m4v") || extension == "m4v" -> "video/x-m4v"
        lowerName.endsWith(".3gp") || extension == "3gp" -> "video/3gpp"
        else -> null
    }
}

private fun compressImageBytesIfNeeded(fileName: String, mimeType: String?, bytes: ByteArray): ByteArray {
    val policy = MediaPolicyRuntime.current()
    val lowerName = fileName.lowercase()
    val normalizedMime = mimeType?.lowercase().orEmpty()
    val isCompressibleImage = (
        normalizedMime.startsWith("image/") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".png") ||
            lowerName.endsWith(".webp") ||
            lowerName.endsWith(".heic") ||
            lowerName.endsWith(".heif")
        ) &&
        !lowerName.endsWith(".gif") &&
        !lowerName.endsWith(".svg")

    if (!policy.uploadCompressionEnabled || !isCompressibleImage || bytes.size <= policy.uploadCompressMinSizeKb * 1024) {
        return bytes
    }

    val sourceImage = UIImage.imageWithData(bytes.toNSData()) ?: return bytes
    val sourceWidth = sourceImage.size.width
    val sourceHeight = sourceImage.size.height
    val largestEdge = max(sourceWidth, sourceHeight)
    val maxEdge = policy.uploadMaxImageEdge.toDouble()
    val scale = if (largestEdge > maxEdge) maxEdge / largestEdge else 1.0
    val targetWidth = max(1.0, sourceWidth * scale)
    val targetHeight = max(1.0, sourceHeight * scale)

    UIGraphicsBeginImageContextWithOptions(
        size = platform.CoreGraphics.CGSizeMake(targetWidth, targetHeight),
        opaque = false,
        scale = 1.0
    )
    sourceImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val encoded = if (normalizedMime == "image/png" || lowerName.endsWith(".png")) {
        UIImagePNGRepresentation(resizedImage ?: sourceImage)
    } else {
        UIImageJPEGRepresentation(resizedImage ?: sourceImage, policy.uploadJpegQuality.toDouble() / 100.0)
    } ?: return bytes

    val compressedBytes = encoded.toByteArray()
    return if (compressedBytes.isNotEmpty() && compressedBytes.size < bytes.size) compressedBytes else bytes
}

private fun resolveUrl(path: String): NSURL? {
    return when {
        path.startsWith("file://") -> NSURL.URLWithString(path)
        path.startsWith("/") -> NSURL.fileURLWithPath(path)
        path.startsWith("http://") || path.startsWith("https://") -> NSURL.URLWithString(path)
        else -> NSURL.fileURLWithPath(path)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isEmpty()) return result
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}

private fun utTypeIdentifier(identifier: String): String = identifier
