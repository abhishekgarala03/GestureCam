package app.abhishekgarala.gesturecam

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.abhishekgarala.gesturecam.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        binding.confidenceSlider.value = prefs.getFloat("confidence_threshold", 0.6f)
        binding.confidenceSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                Toast.makeText(this, "change to: $value", Toast.LENGTH_SHORT).show()
                prefs.edit().putFloat("confidence_threshold", value).apply()
            }
        }

        binding.autoFlashSwitch.isChecked = prefs.getBoolean("auto_flash", false)
        binding.autoFlashSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_flash", isChecked).apply()
        }
    }
}