package com.cretix.cretixsync

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



        val prefs = getSharedPreferences("authData", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isAuth", false)) {
            val API: UploadService = NetworkManager.getClient(prefs.getString("ip", "")!!)!!.create(UploadService::class.java)
            val x = API.loginDevice(RegisterData(prefs.getString("login", "")!!, prefs.getString("password", "")!!))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                        result ->
                    run {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("BASE_URL", prefs.getString("ip", ""))
                        }

                        startActivity(intent)
                    }
                }, {
                        error -> Toast.makeText(applicationContext, "Ошибка при подключении к серверу", Toast.LENGTH_SHORT).show()
                })

        }

        val ipX = ipInput
        val loginX = loginInput
        val passwordX = passwordInput
        val btnLogin = loginButton

        btnLogin.setOnClickListener {
            val ip = ipX.text.toString()
            val login = loginX.text.toString()
            val password = passwordX.text.toString()
            val API: UploadService = NetworkManager.getClient(ip)!!.create(UploadService::class.java)
            val x = API.registerDevice(RegisterData(login, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                        result ->
                    run {
                        prefs.edit().apply {
                            putBoolean("isAuth", true)
                            putString("ip", ip)
                            putString("login", login)
                            putString("password", password)
                        }.apply()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("BASE_URL", ip)
                        }
                        startActivity(intent)
                    }
                }, {
                        error -> Toast.makeText(applicationContext, "Неправильные данные", Toast.LENGTH_SHORT).show()
                })
        }

    }
}
