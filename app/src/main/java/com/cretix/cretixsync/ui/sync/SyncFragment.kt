package com.cretix.cretixsync.ui.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cretix.cretixsync.*
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class SyncFragment() : Fragment() {
    private lateinit var albumListPrefs: SharedPreferences
    private lateinit var authPrefs: SharedPreferences
    private lateinit var apiService: UploadService
    private lateinit var items: ArrayList<AlbumItem>
    private lateinit var recycler: RecyclerView
    private val PREFS_NAME = "selected_albums"
    private val AUTH_PREFS_NAME = "authData"
    private val PULSE_DELAY = 30000L
    private var uploadUrl = ""

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) =
            SyncFragment().apply {
                arguments = bundle
            }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val bundle = requireArguments()
        val root = inflater.inflate(R.layout.fragment_sync, container, false)
        val apiService: UploadService = NetworkManager.getClient(bundle.getString("BASE_URL")!!)!!.create(UploadService::class.java)
        val syncBtn: Button = root.findViewById(R.id.btn_start_sync)
        val pulseHandler = Handler()

        albumListPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authPrefs = requireActivity().getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        items = bundle.getParcelableArrayList<AlbumItem>("albums") as ArrayList<AlbumItem>


        recycler = root.findViewById(R.id.album_selection)
        recycler.layoutManager = LinearLayoutManager(activity?.applicationContext)
        recycler.adapter = AlbumsAdapter(requireActivity().applicationContext).apply { albumsList = items }

        syncBtn.isEnabled = false

        pulseHandler.post(object : Runnable {
            override fun run() {
                val x = apiService.pulse(RegisterData(authPrefs.getString("login", "")!!, authPrefs.getString("password", "")!!))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                        syncBtn.isEnabled = true
                    }, {
                        syncBtn.isEnabled = false
                    })
                pulseHandler.postDelayed(this, PULSE_DELAY)
            }
        })
        syncBtn.setOnClickListener {
            syncBtn.setText(R.string.cancel)
            startSync(syncBtn)
        }

        return root
    }


    private fun startSync(btn: Button) {
        val sel = mutableListOf<String>()
        val albs = mutableListOf<AlbumItem>()
        for (i in 0 until items.size) {
            if (albumListPrefs.getBoolean(i.toString(), false)) {
                sel.add(items[i].name)
                albs.add(items[i])
            }
        }
        val x = apiService.startSync(SyncInfo(sel,authPrefs.getString("login", "")!!, authPrefs.getString("password", "")!!))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                    result ->
                run {
                    uploadUrl = result.url
                    transferFiles(albs,btn)
                }
            }, {
                    error -> Toast.makeText(context, "Ошибка при подключении к серверу", Toast.LENGTH_SHORT).show()
                    btn.setText(R.string.btn_start_sync)
            })

    }

    private fun transferFiles(albums: List<AlbumItem>, btn: Button) {
        Observable.fromIterable(albums)
            .concatMapEager { t -> getModifiedObservable(t) }
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<AlbumItem> {
                override fun onComplete() {
                    activity!!.runOnUiThread {
                        Toast.makeText(requireContext(),"Загрузка завершена", Toast.LENGTH_SHORT).show()
                    }

                    btn.setText(R.string.btn_start_sync)
                }

                override fun onSubscribe(d: Disposable) {
                    println("startUpload")
                }

                override fun onNext(t: AlbumItem) {
                    val up = UploadBgService(t, requireContext(), uploadUrl, apiService)
                    up.startUpload()
                }

                override fun onError(e: Throwable) {
                }

            })
    }

    private fun getModifiedObservable(album: AlbumItem): Observable<AlbumItem> {
        return Observable.create(ObservableOnSubscribe<AlbumItem> { emitter ->
            emitter.onNext(album)
            emitter.onComplete()
        })
            .subscribeOn(Schedulers.io())
    }


}
