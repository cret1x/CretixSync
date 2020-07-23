package com.cretix.cretixsync

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cretix.cretixsync.ui.link.LinkFragment
import com.cretix.cretixsync.ui.settings.SettingsFragment
import com.cretix.cretixsync.ui.sync.SyncFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var albumListPrefs: SharedPreferences
    private lateinit var nightModePrefs: SharedPreferences
    private lateinit var albumsList: Bundle
    private lateinit var nManager: UploadService
    private lateinit var baseUrl: String
    private val ALBUM_LIST_PREFS_NAME = "albums"
    private val NIGHT_MODE_PREFS_NAME = "nightMode"
    private var fragId = 0

    companion object {
        fun setNightMode(night: Boolean) {
            if (night) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkSelfPermission())
            requestPermission()

        albumsList = loadAllImages()

        fragId = savedInstanceState?.getInt("currentFragment") ?: R.id.navigation_sync
        albumListPrefs = getSharedPreferences(ALBUM_LIST_PREFS_NAME, Context.MODE_PRIVATE)
        baseUrl = intent.getStringExtra("BASE_URL")!!
        albumsList.putString("BASE_URL", baseUrl)
        nManager = NetworkManager.getClient(baseUrl)!!.create(UploadService::class.java)


        navItemSelected(fragId)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    private fun navItemSelected(itemId: Int) : Boolean {
        fragId = itemId
        when (itemId) {
            R.id.navigation_sync -> {
                val fragment = SyncFragment.newInstance(albumsList)
                loadFragment(fragment)
                return true
            }
            R.id.navigation_link -> {
                val b = Bundle()
                b.putString("BASE_URL", baseUrl)
                val fragment = LinkFragment.newInstance(b)
                loadFragment(fragment)
                return true
            }
            R.id.navigation_settings -> {
                val fragment = SettingsFragment.newInstance(Bundle())
                loadFragment(fragment)
                return true
            }
        }
        return false
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        return@OnNavigationItemSelectedListener navItemSelected(item.itemId)
    }

    private fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 6036)
    }

    private fun checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAlbumNames() : ArrayList<AlbumItem> {
        val prefs = getSharedPreferences("selected_albums", Context.MODE_PRIVATE)
        val albums = mutableListOf<AlbumItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DATE_TAKEN
        )
        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val media = MediaStore.Files.getContentUri("external")
        val names = mutableListOf<String>()
        val sortOrder =MediaStore.Files.FileColumns.DATE_TAKEN + " DESC"
        val cur = contentResolver.query(
            media,
            projection,
            selection,
            null,
            sortOrder
        )
        Log.i("ListingImages"," query count=" + cur!!.count);
        var pos: Int = 0
        if (cur.moveToFirst()) {
            var bucket: String
            var bId: Long
            var path: Uri
            var imageId: Long
            val bucketColumn = cur.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val bIdColumn = cur.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
            val idColumn = cur.getColumnIndex(MediaStore.Files.FileColumns._ID)
            do {
                bucket = cur.getString(bucketColumn)
                if (!names.contains(bucket)) {
                    names.add(bucket)
                    bId = cur.getLong(bIdColumn)
                    imageId = cur.getLong(idColumn)
                    path = Uri.withAppendedPath(media, "" + imageId)
                    Log.d(bucket, path.toString())
                    albums.add(AlbumItem(bId, bucket, prefs.getBoolean(pos.toString(), false), path))
                    pos++
                }
            } while (cur.moveToNext())
        }
        cur.close()
        return albums as ArrayList<AlbumItem>
    }

    private fun loadAllImages() : Bundle {
        val imagesList = getAlbumNames()
        val bundle = Bundle()
        bundle.putParcelableArrayList("albums", imagesList)
        return bundle
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            6036 -> {
                if (grantResults.isNotEmpty()) {
                    var permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        loadAllImages()
                    } else {
                        Toast.makeText(this, "Permission Denied! Cannot load images.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentFragment", fragId)
    }
}
