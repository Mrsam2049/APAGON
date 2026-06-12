package com.iue.apagon.ui.logros

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.iue.apagon.R
import com.iue.apagon.databinding.ActivityLogrosBinding

/** Contenedor de [LogrosFragment]. */
class LogrosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLogrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.logrosContainer, LogrosFragment())
            }
        }
    }
}
