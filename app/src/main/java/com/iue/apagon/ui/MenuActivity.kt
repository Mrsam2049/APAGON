package com.iue.apagon.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.iue.apagon.R
import com.iue.apagon.databinding.ActivityMenuBinding
import com.iue.apagon.ui.select.SelectModoFragment

/**
 * Activity de entrada. Hospeda el flujo de menú (selección de modo → municipio).
 * El juego en sí vive en GameActivity.
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.menuContainer, SelectModoFragment())
            }
        }
    }
}
