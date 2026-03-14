package com.yakup.bluescreen

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.yakup.bluescreen.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupSliders()
        setupScheduleSection()
        setupAutoStartSwitch()
        setupPowerButton()
        setupPermissionCard()

        // ── Reklam ──────────────────────────────────────────────────────────
        AdManager.loadBanner(this, binding.bannerContainer)
        AdManager.loadInterstitial(this)
    }

    override fun onResume() {
        super.onResume()
        syncUiFromPrefs()
        checkPermissionBanner()
    }

    // ─── UI Kurulum ──────────────────────────────────────────────────────────

    private fun setupPowerButton() {
        binding.btnPower.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            toggleFilter()
            animatePower()
        }
    }

    private fun setupSliders() {
        binding.sliderTemp.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            prefs.temperature = value.toInt()
            binding.tvTempValue.text = "${value.toInt()}K"
            updateColorPreview()
            if (prefs.isFilterEnabled) FilterService.update(this)
        }

        binding.sliderIntensity.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            prefs.intensity = value.toInt()
            binding.tvIntensityValue.text = "%${value.toInt()}"
            updateColorPreview()
            if (prefs.isFilterEnabled) FilterService.update(this)
        }
    }

    private fun setupScheduleSection() {
        binding.switchSchedule.setOnCheckedChangeListener { _, checked ->
            prefs.isScheduleEnabled = checked
            if (checked) {
                ScheduleManager.scheduleAlarms(this, prefs)
            } else {
                ScheduleManager.cancelAlarms(this)
            }
        }

        binding.tvStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.tvStopTime.setOnClickListener  { showTimePicker(isStart = false) }
    }

    private fun setupAutoStartSwitch() {
        binding.switchAutoStart.setOnCheckedChangeListener { _, checked ->
            prefs.autoStartOnBoot = checked
        }
    }

    private fun setupPermissionCard() {
        binding.btnGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }
    }

    // ─── Senkronizasyon ──────────────────────────────────────────────────────

    private fun syncUiFromPrefs() {
        with(binding) {
            // Sıcaklık
            sliderTemp.value       = prefs.temperature.toFloat()
            tvTempValue.text       = "${prefs.temperature}K"

            // Yoğunluk
            sliderIntensity.value  = prefs.intensity.toFloat()
            tvIntensityValue.text  = "%${prefs.intensity}"

            // Zamanlayıcı
            switchSchedule.isChecked  = prefs.isScheduleEnabled
            switchAutoStart.isChecked = prefs.autoStartOnBoot
            updateTimeLabels()

            // Güç düğmesi görünümü
            updatePowerButtonUi()

            // Renk önizleme
            updateColorPreview()
        }
    }

    private fun updatePowerButtonUi() {
        val on = prefs.isFilterEnabled
        binding.btnPower.setImageResource(
            if (on) R.drawable.ic_power_on else R.drawable.ic_power_off
        )
        binding.btnPower.alpha = if (on) 1f else 0.55f
        binding.tvPowerLabel.text = if (on)
            getString(R.string.filter_active)
        else
            getString(R.string.filter_inactive)

        binding.tvPowerLabel.setTextColor(
            if (on) getColor(R.color.amber) else getColor(R.color.text_secondary)
        )
    }

    private fun updateColorPreview() {
        val color = PreferencesManager.computeOverlayColor(
            prefs.temperature,
            prefs.intensity
        )
        // Önizleme dairesini filtre rengiyle göster; alpha çok düşükse asgari opaklık ver
        val r = (color shr 16) and 0xFF
        val g = (color shr 8)  and 0xFF
        val b =  color         and 0xFF
        val previewColor = android.graphics.Color.argb(255, r, g, b)
        binding.colorPreview.setBackgroundColor(previewColor)
    }

    private fun updateTimeLabels() {
        binding.tvStartTime.text = formatTime(prefs.scheduleStartHour, prefs.scheduleStartMinute)
        binding.tvStopTime.text  = formatTime(prefs.scheduleStopHour,  prefs.scheduleStopMinute)
    }

    private fun formatTime(h: Int, m: Int) = String.format("%02d:%02d", h, m)

    // ─── Filtre Aç/Kapat ─────────────────────────────────────────────────────

    private var toggleCount = 0

    private fun toggleFilter() {
        val newState = !prefs.isFilterEnabled
        prefs.isFilterEnabled = newState

        if (newState) FilterService.start(this)
        else          FilterService.stop(this)

        updatePowerButtonUi()

        // Her 3 toggle'da bir interstitial göster
        toggleCount++
        if (toggleCount % 3 == 0) {
            AdManager.showInterstitial(this)
        }
    }

    private fun animatePower() {
        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.btnPower.startAnimation(pulse)
    }

    // ─── Saat Seçici ─────────────────────────────────────────────────────────

    private fun showTimePicker(isStart: Boolean) {
        val hour   = if (isStart) prefs.scheduleStartHour   else prefs.scheduleStopHour
        val minute = if (isStart) prefs.scheduleStartMinute else prefs.scheduleStopMinute

        TimePickerDialog(this, { _, h, m ->
            if (isStart) {
                prefs.scheduleStartHour   = h
                prefs.scheduleStartMinute = m
            } else {
                prefs.scheduleStopHour    = h
                prefs.scheduleStopMinute  = m
            }
            updateTimeLabels()
            if (prefs.isScheduleEnabled) {
                ScheduleManager.scheduleAlarms(this, prefs)
            }
        }, hour, minute, true).show()
    }

    // ─── İzin ────────────────────────────────────────────────────────────────

    private fun checkPermissionBanner() {
        binding.cardPermission.visibility =
            if (Settings.canDrawOverlays(this)) View.GONE else View.VISIBLE
    }

    private fun requestOverlayPermission() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
}
