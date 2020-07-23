package com.cretix.cretixsync.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cretix.cretixsync.MainActivity
import com.cretix.cretixsync.R

class SettingsFragment : Fragment() {
    private lateinit var albumsDatePrefs: SharedPreferences
    private lateinit var nightModePrefs: SharedPreferences
    private val NIGHT_MODE_PREFS_NAME = "nightMode"
    private val PREFS_NAME = "albumsDates"

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) =
            SettingsFragment().apply {
                arguments = bundle
            }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        val resetSyncDate: Button = root.findViewById(R.id.resetSyncDate)
        val logOutButton: Button = root.findViewById(R.id.logOutButton)
        val nightSwitch: Switch = root.findViewById(R.id.night_switch)

        albumsDatePrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        nightModePrefs = requireActivity().getSharedPreferences(NIGHT_MODE_PREFS_NAME, Context.MODE_PRIVATE)

        nightSwitch.isChecked = nightModePrefs.getBoolean("night", false)

        resetSyncDate.setOnClickListener {
            albumsDatePrefs.edit().clear().apply()
        }
        logOutButton.setOnClickListener {
            Toast.makeText(requireContext(), "тык", Toast.LENGTH_SHORT).show()
        }
        nightSwitch.setOnCheckedChangeListener { _, isChecked ->
            MainActivity.setNightMode(isChecked)
            nightModePrefs.edit().putBoolean("night", isChecked).apply()
        }
        return root
    }

}
