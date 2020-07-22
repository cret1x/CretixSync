package com.cretix.cretixsync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val ipX = ipInput
        val loginX = loginInput
        val passwordX = passwordInput
        val btnLogin = loginButton

        btnLogin.text = getString(R.string.hingReg)
        val prefs = getSharedPreferences("authData", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isAuth", false)) {
            btnLogin.text = getString(R.string.hintLogIn)
            ipX.setText(prefs.getString("ip", ""))
            loginX.setText(prefs.getString("login", ""))
            passwordX.setText(prefs.getString("password", ""))
        }

        btnLogin.setOnClickListener {
            val ip = ipX.text.toString()
            val login = loginX.text.toString()
            val password = passwordX.text.toString()

            val API: UploadService = NetworkManager.getClient(ip)!!.create(UploadService::class.java)

            if (prefs.getBoolean("isAuth", false)) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("BASE_URL", prefs.getString("ip", ""))
                }
                startActivity(intent)
            } else {
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
                        }, { error -> Toast.makeText(applicationContext, "Ошибка при регистраци", Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }
}
