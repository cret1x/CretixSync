package com.cretix.cretixsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.zip.Adler32

class UploadBgService(val albumItem: AlbumItem, val context: Context, val BASE_URL: String, val API: UploadService){
    private val CHANNEL_ID = "Upload Service"
    lateinit var notificationBuilder: NotificationCompat.Builder
    lateinit var manager: NotificationManager
    lateinit var prefs: SharedPreferences
    var PROGRESS_CURRENT: Int = 0
    var PROGRESS_MAX: Int = 0

    fun startUpload(){
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC"
        val selection = MediaStore.Images.Media.BUCKET_ID + "=" + albumItem.id
        val cur = context.contentResolver.query(
            images,
            projection,
            selection,
            null,
            sortOrder
        )

        Log.i("ListingImages",albumItem.name + " query count=" + cur!!.count);
        PROGRESS_MAX = cur.count
        PROGRESS_CURRENT = 1
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
        createNotificationChannel()
        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("Загрузка альбома " + albumItem.name)
            setContentText("Загрузка")
            setSmallIcon(R.drawable.baseline_publish_24)
        }
        prefs = context.getSharedPreferences("authData", Context.MODE_PRIVATE)
        val login = prefs.getString("login", "user1")!!

        if (cur.moveToFirst()) {
            var path: Uri
            var imageId: Long
            val idColumn = cur.getColumnIndex(MediaStore.Images.Media._ID)
            do {
                imageId = cur.getLong(idColumn)
                path = Uri.withAppendedPath(images, "" + imageId)
                try {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, path))
                } catch (e: Exception){
                    continue
                }
                val file: File = File(getRealPathFromURI(context, path))
                val requestFile = RequestBody.create(MediaType.parse(context.contentResolver.getType(path)), file)
                val body = MultipartBody.Part.createFormData("picture", file.name, requestFile)
                val adler = Adler32()
                adler.update(albumItem.name.toByteArray())
                val URL = BASE_URL + "/" + login + "/" + adler.value + "/" + file.name
                val x = API.upload(URL, body)
                val resp = x.execute()
                notificationBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false).setContentText(
                    "Загружено $PROGRESS_CURRENT/$PROGRESS_MAX"
                )
                PROGRESS_CURRENT++
                manager.notify(1, notificationBuilder.build())
            } while (cur.moveToNext())
        }
        cur.close()

        notificationBuilder.setProgress(0, 0, false).setContentText("Загрузка завершена")
        manager.notify(1, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(serviceChannel)
        }
    }


}

fun getRealPathFromURI(context: Context, uri: Uri): String? {
    when {
        // DocumentProvider
        DocumentsContract.isDocumentUri(context, uri) -> {
            when {
                // ExternalStorageProvider
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    // This is for checking Main Memory
                    return if ("primary".equals(type, ignoreCase = true)) {
                        if (split.size > 1) {
                            Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        } else {
                            Environment.getExternalStorageDirectory().toString() + "/"
                        }
                        // This is for checking SD Card
                    } else {
                        "storage" + "/" + docId.replace(":", "/")
                    }
                }
                isDownloadsDocument(uri) -> {
                    val fileName = getFilePath(context, uri)
                    if (fileName != null) {
                        return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                    }
                    var id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) {
                        id = id.replaceFirst("raw:".toRegex(), "")
                        val file = File(id)
                        if (file.exists()) return id
                    }
                    val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                    return getDataColumn(context, contentUri, null, null)
                }
                isMediaDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        "video" -> {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        "audio" -> {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        }
        "content".equals(uri.scheme, ignoreCase = true) -> {
            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        }
        "file".equals(uri.scheme, ignoreCase = true) -> {
            return uri.path
        }
    }
    return null
}

fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                  selectionArgs: Array<String>?): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )
    try {
        if (uri == null) return null
        cursor = context.contentResolver.query(uri, projection, selection, selectionArgs,
            null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}


fun getFilePath(context: Context, uri: Uri?): String? {
    var cursor: Cursor? = null
    val projection = arrayOf(
        MediaStore.MediaColumns.DISPLAY_NAME
    )
    try {
        if (uri == null) return null
        cursor = context.contentResolver.query(uri, projection, null, null,
            null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Photos.
 */
fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}