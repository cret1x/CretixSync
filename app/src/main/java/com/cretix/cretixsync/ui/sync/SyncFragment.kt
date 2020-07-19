package com.cretix.cretixsync.ui.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cretix.cretixsync.AlbumItem
import com.cretix.cretixsync.AlbumsAdapter
import com.cretix.cretixsync.R


class SyncFragment(private var bundle: Bundle) : Fragment() {
    private lateinit var syncViewModel: SyncViewModel
    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "selected_albums"
    private lateinit var items: ArrayList<AlbumItem>
    private  lateinit var recycler: RecyclerView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_sync, container, false)
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        recycler = root.findViewById<RecyclerView>(R.id.album_selection)
        recycler.layoutManager = LinearLayoutManager(activity?.applicationContext)
        items = bundle.getParcelableArrayList<AlbumItem>("albums") as ArrayList<AlbumItem>
        recycler.adapter = AlbumsAdapter(requireActivity().applicationContext).apply { albumsList = items }
        return root
    }
}
