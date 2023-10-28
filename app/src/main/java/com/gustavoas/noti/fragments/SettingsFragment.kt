package com.gustavoas.noti.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import com.gustavoas.noti.AccessibilityDialogPrefCompat
import com.gustavoas.noti.AccessibilityPermissionDialog
import com.gustavoas.noti.R
import com.gustavoas.noti.Utils.hasAccessibilityPermission
import com.gustavoas.noti.Utils.hasNotificationListenerPermission
import com.gustavoas.noti.Utils.hasSystemAlertWindowPermission
import com.gustavoas.noti.Utils.showColorDialog
import com.kizitonwose.colorpreferencecompat.ColorPreferenceCompat
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.color.SimpleColorDialog

class SettingsFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener, SimpleDialog.OnDialogResultListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "progressBarStyle" || key == "onlyInPortrait") {
            updateProgressBarStyleVisibility()
        } else if (key == "progressBarColor") {
            updateColorPreferenceSummary()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        updateSetupVisibility()

        updateProgressBarStyleVisibility()

        updateShowInLockscreenVisibility()

        findPreference<Preference>("showForMedia")?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)

        findPreference<Preference>("progressBarColor")?.setOnPreferenceClickListener {
            val color = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
                    "progressBarColor", ContextCompat.getColor(requireContext(), R.color.purple_500)
                )
            showColorDialog(this, color, "colorPicker")
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is AccessibilityPermissionDialog) {
            val dialogFragment = AccessibilityDialogPrefCompat.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, null)
        } else super.onDisplayPreferenceDialog(preference)
    }

    override fun onStart() {
        super.onStart()

        updateSetupVisibility()

        updateColorPreferenceSummary()

        updateShowInLockscreenVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == BUTTON_POSITIVE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean(
                        "usingMaterialYouColor",
                        extras.getInt(SimpleColorDialog.COLOR) == ContextCompat.getColor(
                            requireContext(), R.color.system_accent_color
                        ) && extras.getInt(SimpleColorDialog.SELECTED_SINGLE_POSITION) != 19
                    ).apply()
            }

            findPreference<ColorPreferenceCompat>("progressBarColor")?.value = extras.getInt(
                SimpleColorDialog.COLOR,
                ContextCompat.getColor(requireContext(), R.color.purple_500)
            )
        }
        return true
    }

    private fun updateColorPreferenceSummary() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val color = sharedPreferences.getInt(
            "progressBarColor", ContextCompat.getColor(requireContext(), R.color.purple_500)
        )
        val colorPosition = resources.getIntArray(R.array.colorsArrayValues).indexOf(color)
        var colorName = resources.getStringArray(R.array.colorsArray).getOrNull(colorPosition)
        val useMaterialYou = sharedPreferences.getBoolean("usingMaterialYouColor", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && colorName == null && useMaterialYou) {
            sharedPreferences.edit().putInt(
                    "progressBarColor", ContextCompat.getColor(
                        requireContext(), R.color.system_accent_color
                    )
                ).apply()

            findPreference<ColorPreferenceCompat>("progressBarColor")?.value =
                ContextCompat.getColor(requireContext(), R.color.system_accent_color)
            colorName = resources.getString(R.string.colorMaterialYou)
        }

        findPreference<Preference>("progressBarColor")?.summary =
            colorName ?: "#${Integer.toHexString(color).drop(2).uppercase()}"
    }

    private fun updateProgressBarStyleVisibility() {
        val progressBarStyle = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("progressBarStyle", "linear")
        val useOnlyInPortrait = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("onlyInPortrait", false)

        findPreference<Preference>("CircularBarFragment")?.isVisible =
            progressBarStyle == "circular"
        findPreference<Preference>("LinearBarFragment")?.isVisible =
            (progressBarStyle == "linear" || useOnlyInPortrait)
    }

    private fun updateShowInLockscreenVisibility() {
        findPreference<Preference>("showInLockScreen")?.isVisible =
            (hasAccessibilityPermission(requireContext()) || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
    }

    private fun updateSetupVisibility() {
        val hasAccessibilityPermission = hasAccessibilityPermission(requireContext())
        val hasNotificationListenerPermission = hasNotificationListenerPermission(requireContext())
        val hasSystemAlertWindowPermission = hasSystemAlertWindowPermission(requireContext())

        // xiaomi, samsung, vivo, etc are killing the accessibility service in the background
        val brand = Build.BRAND.lowercase()
        findPreference<Preference>("accessibilityPermission")?.isVisible =
            !(hasAccessibilityPermission || brand != "google")
        findPreference<Preference>("notificationPermission")?.isVisible =
            !hasNotificationListenerPermission
        findPreference<Preference>("systemAlertWindowPermission")?.isVisible =
            !hasSystemAlertWindowPermission
        findPreference<Preference>("batteryOptimizationsInfoCard")?.isVisible = brand != "google"
        findPreference<PreferenceCategory>("setup")?.isVisible =
            !(hasNotificationListenerPermission && hasSystemAlertWindowPermission && hasAccessibilityPermission && brand == "google")
    }
}