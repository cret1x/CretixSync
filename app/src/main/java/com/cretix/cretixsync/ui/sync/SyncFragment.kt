package com.cretix.cretixsync.ui.sync

import android.content.Context
import android.content.SharedPreferences
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cretix.cretixsync.*
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.DisposableSubscriber
import okhttp3.MediaType
import okhttp3.RequestBody
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.io.File
import java.util.concurrent.TimeUnit


class SyncFragment(private var bundle: Bundle, private var apiService: UploadService) : Fragment() {
    private lateinit var prefs: SharedPreferences
    private lateinit var authPrefs: SharedPreferences
    private val PREFS_NAME = "selected_albums"
    private val AUTH_PREFS_NAME = "authData"
    private lateinit var items: ArrayList<AlbumItem>
    private lateinit var recycler: RecyclerView
    private var uploadUrl = ""

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_sync, container, false)
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authPrefs = requireActivity().getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        recycler = root.findViewById<RecyclerView>(R.id.album_selection)
        recycler.layoutManager = LinearLayoutManager(activity?.applicationContext)
        items = bundle.getParcelableArrayList<AlbumItem>("albums") as ArrayList<AlbumItem>
        recycler.adapter = AlbumsAdapter(requireActivity().applicationContext).apply { albumsList = items }


        val syncBtn = root.findViewById<Button>(R.id.btn_start_sync)
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
            if (prefs.getBoolean(i.toString(), false)) {
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
                    println("onSubscribe")
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
