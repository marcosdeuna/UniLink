package com.marcosdeuna.unilink.ui.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentEditUserBinding
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.isValidEmail
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@AndroidEntryPoint
class EditUserFragment : Fragment() {

    private lateinit var binding: FragmentEditUserBinding
    private var newProfilePicture: Uri? = null
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEditUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.getUserSession { user ->
            binding.editTextUsername.setText(user?.userName)
            binding.editTextEmail.setText(user?.email)
            binding.editTextName.setText(user?.firstName)
            binding.editTextLastName.setText(user?.lastName)
            loadProfilePicture(user?.profilePicture)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.editProfileImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnSaveChanges.setOnClickListener {
            if(validation()){
                val firstName = binding.editTextName.text.toString()
                val lastName = binding.editTextLastName.text.toString()
                val email = binding.editTextEmail.text.toString()
                val userName = binding.editTextUsername.text.toString()

                // Obtener el usuario actual y actualizar los datos
                authViewModel.getUserSession { user ->
                    user?.let { currentUser ->
                        val updatedUser = currentUser.copy(
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            userName = userName
                        )

                        newProfilePicture?.let {uri ->
                            authViewModel.onUploadProfilePicture(uri) {uploadState ->
                                when (uploadState) {
                                    is UIState.Success -> {
                                        // Obtener la URL de la imagen de perfil subida
                                        val profilePictureUrl = uploadState.data
                                        val finalUser = updatedUser.copy(profilePicture = profilePictureUrl)

                                        // Actualizar el usuario en la base de datos
                                        userViewModel.updateUserInfo(finalUser)
                                    }
                                    is UIState.Error -> {
                                        toast("Error uploading profile picture: ${uploadState.exception}")
                                    }
                                    UIState.Loading -> { /* Show loading if needed */ }
                                    UIState.Empty -> { /* Handle empty state if needed */ }
                                }
                            }
                        } ?: run {
                            // Si no se seleccionó una nueva imagen de perfil, actualizar solo los datos
                            userViewModel.updateUserInfo(updatedUser)
                        }
                    }
                }
            }
        }

        userViewModel.updateUserInfo.observe(viewLifecycleOwner) { state ->
            when(state){
                is UIState.Success -> {
                    toast("User updated successfully")
                    findNavController().popBackStack()
                }
                is UIState.Error -> {
                    toast("Error updating user: ${state.exception}")
                }
                UIState.Loading -> { /* Show loading if needed */ }
                UIState.Empty -> { /* Handle empty state if needed */ }
            }
        }
    }

    private fun loadProfilePicture(imageUrl: String?){
        imageUrl?.let { url ->
            if (url.isNotEmpty()) {
                coroutineScope.launch {
                    val bitmap = downloadImage(url)
                    bitmap?.let {
                        binding.profileImage.setImageBitmap(it)
                    }
                }
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageUrl = URL(url)
            val connection: HttpURLConnection = imageUrl.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            return@withContext BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
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

        return isValid
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
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
            newProfilePicture = imageUri
        }
    }
}