package kittoku.osc.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import kittoku.osc.R
import kittoku.osc.fragment.ServerListFragment
import kittoku.osc.fragment.SettingFragment

/**
 * New MainActivity with modern UI and server list
 */
class NewMainActivity : AppCompatActivity() {
    
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_main)
        
        // Set up bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_servers -> {
                    showFragment(ServerListFragment())
                    true
                }
                R.id.nav_settings -> {
                    showFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }
        
        // Show server list by default
        if (savedInstanceState == null) {
            showFragment(ServerListFragment())
            bottomNav.selectedItemId = R.id.nav_servers
        }
        
        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NO********IONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NO********IONS), 1)
            }
        }
    }
    
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                // Show about dialog
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
