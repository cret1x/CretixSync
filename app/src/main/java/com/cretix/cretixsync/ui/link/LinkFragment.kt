package com.cretix.cretixsync.ui.link

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cretix.cretixsync.R
import com.cretix.cretixsync.RegisterData
import com.cretix.cretixsync.UploadService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class LinkFragment(val apiService: UploadService) : Fragment() {

    private lateinit var linkViewModel: LinkViewModel
    private val AUTH_PREFS_NAME = "authData"
    lateinit var authPrefs: SharedPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_link, container, false)
        val status_connected = root.findViewById<TextView>(R.id.status_connected)
        status_connected.text = getString(R.string.status_offline)
        authPrefs = requireContext().getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val x= apiService.getInfo(RegisterData(authPrefs.getString("login", "")!!, authPrefs.getString("password", "")!!))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                status_connected.text = "Подключен к группе \n'" + response.name + "'"
            }, {
                status_connected.text = getString(R.string.status_offline)
            })
        return root
    }
}
