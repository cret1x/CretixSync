package com.cretix.cretixsync

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_album.view.*

class AlbumsAdapter(ctx: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val PREFS_NAME = "selected_albums"
    private var prefs: SharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val contentResolver = ctx.contentResolver

    var albumsList: List<AlbumItem> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AlbumViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false))
    }

    override fun getItemCount(): Int = albumsList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AlbumViewHolder).onBind(albumsList[position].name, albumsList[position].selected, albumsList[position].thumbnail, position)
    }

    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).maximumColorCount(4).generate()

    inner class AlbumViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val albumName = root.name
        private val card = root.cardBg
        private val albumCover = root.cover
        private val checkBox = root.selected

        fun onBind(name: String, box_checked: Boolean, thumbnail: Uri, pos: Int) {
            albumName.text = name
            checkBox.isChecked = box_checked
            if (thumbnail != Uri.EMPTY) {
                val bitmap = contentResolver.loadThumbnail(thumbnail, Size(64,64), null)
                albumCover.setImageBitmap(bitmap)
                /*
                val palette = createPaletteSync(bitmap)
                val gradient = GradientDrawable()
                gradient.colors = palette.swatches.map { s -> s.rgb }.toIntArray()
                gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
                gradient.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                gradient.cornerRadius  = 0f
                card.background = gradient
                Log.d("COLORS", gradient.colors.toString())*/
            }


            checkBox.setOnClickListener {
                albumsList[pos].selected = checkBox.isChecked
                prefs.edit().putBoolean((pos).toString(), checkBox.isChecked).apply()
            }
        }
    }
}