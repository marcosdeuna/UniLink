package com.marcosdeuna.unilink.ui.post

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.databinding.FragmentPostBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostFragment : Fragment() {

    lateinit var binding: FragmentPostBinding
    val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        view.setOnTouchListener{_, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Ocultar el teclado cuando se toca fuera de un EditText
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
            false
        }
    }

    private fun setupViews() {
        // Configurar la acción de la foto de perfil
        binding.profilePicture.setOnClickListener {
            binding.profileModal.visibility = View.VISIBLE
        }

        binding.cerrarModal.setOnClickListener {
            binding.profileModal.visibility = View.GONE
        }

        // Configurar la acción del botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_postFragment_to_loginFragment)
        }

        // Configurar la barra de navegación
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Acción para Home
                    true
                }
                R.id.navigation_discover_people -> {
                    // Acción para descubrir personas
                    true
                }
                R.id.navigation_create_post -> {
                    // Acción para crear post
                    true
                }
                R.id.navigation_discover_places -> {
                    // Acción para descubrir lugares
                    true
                }
                R.id.navigation_chats -> {
                    // Acción para chats
                    true
                }
                else -> false
            }
        }
    }
}
