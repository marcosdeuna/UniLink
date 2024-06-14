package com.marcosdeuna.unilink.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.databinding.FragmentForgotPasswordBinding
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.isValidEmail
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    val TAG : String = "ForgotPasswordFragment"
    lateinit var binding: FragmentForgotPasswordBinding
    val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()

        view.setOnTouchListener{_, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Ocultar el teclado cuando se toca fuera de un EditText
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
            false
        }
        binding.forgotPassBtn.setOnClickListener {
            if (validation()) {
                viewModel.forgotPassword(binding.emailEt.text.toString())
            }
        }
        binding.loginLabel.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }

    fun observer() {
        viewModel.forgotPassword.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.forgotPassBtn.setText("")
                    binding.forgotPassProgress.show()
                }
                is UIState.Success -> {
                    binding.forgotPassProgress.hide()
                    binding.forgotPassBtn.setText("Send")
                    toast(state.data)
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
                }
                is UIState.Error -> {
                    binding.forgotPassProgress.hide()
                    binding.forgotPassBtn.setText("Send")
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }
    }

    fun validation(): Boolean {
        var isValid = true
        if (binding.emailEt.text.toString().isEmpty()) {
            toast("Enter email")
            isValid = false
        } else if (!binding.emailEt.text.toString().isValidEmail()) {
            toast("Enter valid email")
            isValid = false
        }

        return isValid
    }

}