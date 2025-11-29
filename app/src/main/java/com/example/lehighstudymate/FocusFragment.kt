package com.example.lehighstudymate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lehighstudymate.databinding.FragmentFocusBinding

// Fragment responsible for the Focus Mode screen, primarily providing an entry point to SuperFocusActivity.
class FocusFragment : Fragment() {
    // Backing property for View Binding, allowing nullable initialization.
    private var _binding: FragmentFocusBinding? = null
    // Non-nullable accessor property for the binding object.
    private val binding get() = _binding!!

    // Called to create the view hierarchy associated with the fragment.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentFocusBinding.inflate(inflater, container, false)

        // Set up the click listener for the 'Start Focus' button.
        // The original logic is kept: only one entry point is maintained: starting Super Focus Mode.
        binding.btnStartFocus.setOnClickListener {
            // Create an Intent to launch the SuperFocusActivity
            startActivity(Intent(context, SuperFocusActivity::class.java))
        }

        return binding.root // Return the root view
    }

    // Called when the view previously created by onCreateView has been detached from the fragment.
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding object to avoid memory leaks
        _binding = null
    }
}