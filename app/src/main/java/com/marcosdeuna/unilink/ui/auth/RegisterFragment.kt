package com.marcosdeuna.unilink.ui.auth
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentRegisterBinding
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.isValidEmail
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    val TAG: String = "RegisterFragment"
    lateinit var binding: FragmentRegisterBinding
    val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(layoutInflater)
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

        binding.buttonRegister.setOnClickListener {
            if (validation()) {
                val imageUri = binding.profileImage.tag as? Uri
                if (imageUri != null) {
                    // Subir la imagen de perfil al Storage y luego registrar al usuario
                    viewModel.onUploadProfilePicture(imageUri) { uploadState ->
                        when (uploadState) {
                            is UIState.Success -> {
                                // Obtener la URL de la imagen de perfil subida
                                val profilePictureUrl = uploadState.data

                                // Registrar al usuario con la URL de la imagen de perfil
                                val user = getUserObject().copy(profilePicture = profilePictureUrl)
                                viewModel.register(
                                    binding.editTextEmail.text.toString(),
                                    binding.editTextPassword.text.toString(),
                                    user
                                )
                            }
                            is UIState.Error -> {
                                toast("Error uploading profile picture: ${uploadState.exception}")
                            }
                            UIState.Loading -> {
                                // Puedes mostrar un indicador de carga si lo deseas
                            }

                            UIState.Empty -> TODO()
                        }
                    }
                } else {
                    // Si no se seleccionó una imagen de perfil
                    toast("Please select a profile picture")
                }
            }
        }

        binding.loginLaber.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.editProfileImage.setOnClickListener {
            openImagePicker()
        }
    }

    fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            // Handle image picked from gallery
            val imageUri = data?.data
            binding.profileImage.setImageURI(imageUri) // Update profile image preview
            binding.profileImage.tag = imageUri
        }
    }

    fun observer() {
        viewModel.register.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.buttonRegister.text = ""
                    binding.progressBar.show()
                }
                is UIState.Success -> {
                    binding.progressBar.hide()
                    binding.buttonRegister.text = "Register"
                    toast(state.data)
                    findNavController().navigate(R.id.action_registerFragment_to_postFragment)
                }
                is UIState.Error -> {
                    binding.progressBar.hide()
                    binding.buttonRegister.text = "Register"
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }
    }

    fun getUserObject(): User {
        return User(
            id = "",
            firstName = binding.editTextName.text.toString(),
            lastName = binding.editTextLastName.text.toString(),
            userName = binding.editTextUsername.text.toString(),
            email = binding.editTextEmail.text.toString(),
            password = binding.editTextPassword.text.toString(),
            profilePicture = binding.profileImage.toString()
        )
    }

    fun validation(): Boolean {
        var isValid = true

        if (binding.editTextName.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter first name")
        }

        if (binding.editTextLastName.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter last name")
        }

        if (binding.editTextUsername.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter username")
        }

        if (binding.editTextEmail.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter email")
        } else {
            if (!binding.editTextEmail.text.toString().isValidEmail()) {
                isValid = false
                toast("Invalid email")
            }
        }
        if (binding.editTextPassword.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter password")
        } else {
            if (binding.editTextPassword.text.toString().length < 6) {
                isValid = false
                toast("Password must be at least 6 characters")
            }
        }
        if (binding.editTextConfirmPassword.text.isNullOrEmpty()) {
            isValid = false
            toast("Enter confirm password")
        } else {
            if (binding.editTextPassword.text.toString() != binding.editTextConfirmPassword.text.toString()) {
                isValid = false
                toast("Password does not match")
            }
        }

        return isValid
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
