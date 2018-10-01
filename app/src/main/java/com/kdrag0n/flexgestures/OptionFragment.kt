package com.kdrag0n.flexgestures

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

class OptionFragment : PreferenceFragmentCompat() {
    private var handler: Callbacks? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        retainInstance = true
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.options)

        with (preferenceManager) {
            findPreference("service")?.setOnPreferenceClickListener {
                val switch = it as SwitchPreference
                val setState = handler?.onServiceChange(switch.isChecked)
                if (setState != null) {
                    switch.isChecked = setState
                }

                true
            }

            findPreference("hide_navbar")?.setOnPreferenceClickListener {
                val switch = it as SwitchPreference
                val setState = handler?.onHideNavbarChange(switch.isChecked)
                if (setState != null) {
                    switch.isChecked = setState
                }

                true
            }

            // Experimental
            findPreference("screenshot")?.setOnPreferenceClickListener {
                handler?.onScreenshot(it)
                true
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        handler = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        handler = null
    }

    internal interface Callbacks {
        fun onServiceChange(enable: Boolean): Boolean?
        fun onHideNavbarChange(hide: Boolean): Boolean?
        fun onScreenshot(preference: Preference)
    }
}