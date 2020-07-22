package com.cretix.cretixsync

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cretix.cretixsync.ui.link.LinkFragment
import com.cretix.cretixsync.ui.settings.SettingsFragment
import com.cretix.cretixsync.ui.sync.SyncFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "albums"
    private lateinit var albumsList: Bundle
    private lateinit var nManager: UploadService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkSelfPermission())
            requestPermission()

        albumsList = loadAllImages()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val base_url = intent.getStringExtra("BASE_URL")!!

        nManager = NetworkManager.getClient(base_url)!!.create(UploadService::class.java)
        val pulseTimer =  Timer("pulse")
        loadFragment(SyncFragment(albumsList, nManager))
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)



    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {

            R.id.navigation_sync -> {
                var fragment = SyncFragment(albumsList, nManager)
                loadFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_link -> {
                var fragment = LinkFragment(nManager)
                loadFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                var fragment = SettingsFragment()
                loadFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun loadFragment(fragment: Fragment){
        var transaction = supportFragmentManager.beginTransaction()
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
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val names = mutableListOf<String>()
        val sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC"
        val cur = this.contentResolver.query(
            images,
            projection,
            null,
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
            val bucketColumn = cur.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bIdColumn = cur.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val idColumn = cur.getColumnIndex(MediaStore.Images.Media._ID)
            do {
                bucket = cur.getString(bucketColumn)
                if (!names.contains(bucket)) {
                    names.add(bucket)
                    bId = cur.getLong(bIdColumn)
                    imageId = cur.getLong(idColumn)
                    path = Uri.withAppendedPath(images, "" + imageId)
                    Log.d("PATH", path.toString())
                    try {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, path))
                    } catch (e: Exception){
                        path = Uri.EMPTY
                    }

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
}
