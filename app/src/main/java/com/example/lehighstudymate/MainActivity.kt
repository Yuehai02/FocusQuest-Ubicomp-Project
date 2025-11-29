package com.example.lehighstudymate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.lehighstudymate.databinding.ActivityMainBinding

// The main activity of the application, responsible for hosting the bottom navigation bar
// and managing the display of different fragments (Home, Focus, Profile).
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the default fragment (HomeFragment) only when the activity is first created
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Set up the listener for item selections in the Bottom Navigation View
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment()) // Navigate to the Home screen
                    true
                }
                R.id.navigation_focus -> {
                    loadFragment(FocusFragment()) // Navigate to the Focus mode screen
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment()) // Navigate to the User Profile screen
                    true
                }
                else -> false // Item ID not recognized
            }
        }
    }

    /**
     * Replaces the current fragment displayed in the container with a new one.
     * @param fragment The new Fragment to load.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            // Replace the fragment in the designated container (nav_host_fragment)
            .replace(R.id.nav_host_fragment, fragment)
            .commit() // Execute the transaction
    }
}