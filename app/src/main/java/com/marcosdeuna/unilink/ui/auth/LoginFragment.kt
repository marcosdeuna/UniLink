package com.marcosdeuna.unilink.ui.auth

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.databinding.FragmentLoginBinding
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.isValidEmail
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    val TAG: String = "LoginFragment"
    lateinit var binding: FragmentLoginBinding
    val viewModel: AuthViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater)
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

        binding.loginBtn.setOnClickListener {
            if (validation()) {
                viewModel.login(
                    binding.emailEt.text.toString(),
                    binding.passEt.text.toString()
                )
            }
        }
        binding.registerLabel.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.forgotPassLabel.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    fun observer() {
        viewModel.login.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.loginBtn.setText("")
                    binding.loginProgress.show()
                }
                is UIState.Success -> {
                    binding.loginProgress.hide()
                    binding.loginBtn.setText("Iniciar sesión")
                    toast(state.data)
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && user.isEmailVerified) {
                        findNavController().navigate(R.id.action_loginFragment_to_postFragment)
                    } else {
                        showEmailVerificationDialog(user)
                    }
                }
                is UIState.Error -> {
                    binding.loginProgress.hide()
                    binding.loginBtn.setText("Iniciar sesión")
                    toast(state.exception)
                }

                UIState.Empty -> {}
            }
        }
    }

    private fun showEmailVerificationDialog(user: FirebaseUser?) {
        user ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email_verification, null)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = "Por favor, verifica tu correo electrónico. Hemos enviado un enlace de verificación a ${user.email}."

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.buttonResend).setOnClickListener {
            user.sendEmailVerification().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    toast("Correo de verificación reenviado")
                } else {
                    toast("Error al enviar el correo de verificación")
                }
            }
        }

        dialogView.findViewById<Button>(R.id.buttonVerify).setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful && user.isEmailVerified) {
                    toast("Correo verificado exitosamente")
                    alertDialog.dismiss()
                    findNavController().navigate(R.id.action_loginFragment_to_postFragment)

                } else {
                    toast("El correo aún no ha sido verificado")
                    showEmailVerificationDialog(user)
                }
            }
        }
        alertDialog.show()
    }


    fun validation(): Boolean {
        var isValid = true

        if (binding.emailEt.text.toString().isEmpty()) {
            isValid = false
            toast("Email is required")
        }else{
            if(!binding.emailEt.text.toString().isValidEmail()){
                isValid = false
                toast("Invalid email")
            }
        }

        if (binding.passEt.text.toString().isEmpty()) {
            isValid = false
            toast("Password is required")
        }else{
            if(binding.passEt.text.toString().length < 6){
                isValid = false
                toast("Password must be at least 6 characters")
            }
        }

        return isValid

    }

    override fun onStart() {
        super.onStart()
        viewModel.getUserSession {
            if (it != null) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null && user.isEmailVerified) {
                    findNavController().navigate(R.id.action_loginFragment_to_postFragment)
                } else {
                    showEmailVerificationDialog(user)
                }
            }
        }
    }

}