package com.cretix.cretixsync.ui.link

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cretix.cretixsync.NetworkManager
import com.cretix.cretixsync.R
import com.cretix.cretixsync.RegisterData
import com.cretix.cretixsync.UploadService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class LinkFragment() : Fragment() {
    private lateinit var authPrefs: SharedPreferences
    private lateinit var apiService: UploadService
    private val AUTH_PREFS_NAME = "authData"

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) =
            LinkFragment().apply {
                arguments = bundle
            }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_link, container, false)
        val bundle = requireArguments()
        val textStatusConnected = root.findViewById<TextView>(R.id.status_connected)

        authPrefs = requireContext().getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        apiService = NetworkManager.getClient(bundle.getString("BASE_URL")!!)!!.create(UploadService::class.java)

        textStatusConnected.text = getString(R.string.status_offline)
        val x= apiService.getInfo(RegisterData(authPrefs.getString("login", "")!!, authPrefs.getString("password", "")!!))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                textStatusConnected.text = "Подключен к группе \n'" + response.name + "'"
            }, {
                textStatusConnected.text = getString(R.string.status_offline)
            })
        return root
    }
}
