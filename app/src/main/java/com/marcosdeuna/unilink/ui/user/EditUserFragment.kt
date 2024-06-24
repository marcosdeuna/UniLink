package com.marcosdeuna.unilink.ui.user

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
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.databinding.FragmentEditUserBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.post.CreatePostFragment
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
    private val selectedImagesUris = mutableListOf<Uri>()

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
            binding.editTextCareer.setText(user?.career)
            binding.editTextDescription.setText(user?.description)
            if(user?.age != 0) {
                binding.editTextAge.setText(user?.age.toString())
            }else{
                binding.editTextAge.setText("")
            }
            if(user?.genre.equals("Mujer")) {
                binding.radioButtonFemale.isChecked = true
            }else{
                binding.radioButtonMale.isChecked = true
            }
            loadProfilePicture(user?.profilePicture)
            if(user?.socialPictures?.isNotEmpty() == true){
                loadSocialPictures(user.socialPictures)
                binding.imagePreviewContainer.visibility = View.VISIBLE
                binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.ic_edit))
                binding.buttonDeleteImages.visibility = View.VISIBLE
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_editUserFragment_to_detailUserFragment)
        }

        binding.editProfileImage.setOnClickListener {
            openImagePicker(0)
        }

        binding.buttonAddImages.setOnClickListener {
            openImagePicker(1)
        }

        binding.buttonDeleteImages.setOnClickListener {
            selectedImagesUris.clear()
            binding.imagePreviewContainer.removeAllViews()
            binding.imagePreviewContainer.visibility = View.GONE
            binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.addpicture))
            binding.buttonDeleteImages.visibility = View.GONE
        }

        binding.btnSaveChanges.setOnClickListener {
            if(validation()){
                val firstName = binding.editTextName.text.toString()
                val lastName = binding.editTextLastName.text.toString()
                val email = binding.editTextEmail.text.toString()
                val userName = binding.editTextUsername.text.toString()
                val career = binding.editTextCareer.text.toString()
                val description = binding.editTextDescription.text.toString()
                val age = binding.editTextAge.text.toString().toInt()
                val genre = if(binding.radioButtonFemale.isChecked) "Mujer" else "Hombre"


                // Obtener el usuario actual y actualizar los datos
                authViewModel.getUserSession { user ->
                    user?.let { currentUser ->
                        val updatedUser = currentUser.copy(
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            userName = userName,
                            career = career,
                            description = description,
                            age = age,
                            genre = genre
                        )

                        newProfilePicture?.let {uri ->
                            authViewModel.onUploadProfilePicture(uri) {uploadState ->
                                when (uploadState) {
                                    is UIState.Success -> {
                                        // Obtener la URL de la imagen de perfil subida
                                        val profilePictureUrl = uploadState.data
                                        val finalUser = updatedUser.copy(profilePicture = profilePictureUrl)

                                        // Subir las imágenes sociales si se seleccionaron
                                        if (selectedImagesUris.isNotEmpty()) {
                                            userViewModel.onUploadSocialImage(selectedImagesUris) { uploadState ->
                                                when (uploadState) {
                                                    is UIState.Success -> {
                                                        val socialPicturesUrls = uploadState.data
                                                        val finalUserWithSocialPictures = finalUser.copy(socialPictures = socialPicturesUrls)
                                                        userViewModel.updateUserInfo(finalUserWithSocialPictures)
                                                    }
                                                    is UIState.Error -> {
                                                        toast("Error subiendo las imágenes sociales: ${uploadState.exception}")
                                                    }
                                                    UIState.Loading -> { /* Show loading if needed */ }
                                                    UIState.Empty -> { /* Handle empty state if needed */ }
                                                }
                                            }
                                        } else {
                                            userViewModel.updateUserInfo(finalUser)
                                        }
                                    }
                                    is UIState.Error -> {
                                        toast("Error subiendo la imagen: ${uploadState.exception}")
                                    }
                                    UIState.Loading -> { /* Show loading if needed */ }
                                    UIState.Empty -> { /* Handle empty state if needed */ }
                                }
                            }
                        } ?: run {
                            // Si no se seleccionó una nueva imagen de perfil, actualizar solo los datos
                            if (selectedImagesUris.isNotEmpty()) {
                                userViewModel.onUploadSocialImage(selectedImagesUris) { uploadState ->
                                    when (uploadState) {
                                        is UIState.Success -> {
                                            val socialPicturesUrls = uploadState.data
                                            val finalUserWithSocialPictures = updatedUser.copy(socialPictures = socialPicturesUrls)
                                            userViewModel.updateUserInfo(finalUserWithSocialPictures)
                                        }
                                        is UIState.Error -> {
                                            toast("Error subiendo las imágenes sociales: ${uploadState.exception}")
                                        }
                                        UIState.Loading -> { /* Show loading if needed */ }
                                        UIState.Empty -> { /* Handle empty state if needed */ }
                                    }
                                }
                            } else {
                                userViewModel.updateUserInfo(updatedUser)
                            }
                        }
                    }
                }
            }
        }

        userViewModel.updateUserInfo.observe(viewLifecycleOwner) { state ->
            when(state){
                is UIState.Success -> {
                    toast("Usuario actualizado correctamente")
                    findNavController().popBackStack()
                }
                is UIState.Error -> {
                    toast("Error actualizando el usuario: ${state.exception}")
                }
                UIState.Loading -> { /* Show loading if needed */ }
                UIState.Empty -> { /* Handle empty state if needed */ }
            }
        }
    }

    private fun loadSocialPictures(imageUrls: List<String>) {
        binding.imagePreviewContainer.removeAllViews()

        for (imageUrl in imageUrls) {
            coroutineScope.launch {
                val bitmap = downloadImage(imageUrl)
                bitmap?.let {
                    val imageView = createImageView(it)
                    binding.imagePreviewContainer.addView(imageView)
                }
            }
        }
    }

    private fun createImageView(bitmap: Bitmap): ImageView {
        val imageView = ImageView(binding.root.context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        imageView.layoutParams = layoutParams
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
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
        if(binding.editTextAge.text.toString().toInt() < 18){
            isValid = false
            toast("Debes ser mayor de edad")
        }
        if (binding.editTextName.text.isNullOrEmpty()) {
            isValid = false
            toast("Inserte nombre")
        }

        if (binding.editTextLastName.text.isNullOrEmpty()) {
            isValid = false
            toast("Inserte apellidos")
        }

        if (binding.editTextUsername.text.isNullOrEmpty()) {
            isValid = false
            toast("Inserte nombre de usuario")
        }else{
            userViewModel.existeUserName(binding.editTextUsername.text.toString(), binding.editTextEmail.text.toString()){state ->
                if(state is UIState.Success){
                    if(state.data){
                        isValid = false
                        toast("El nombre de usuario ya existe")
                    }
                }
            }
        }

        if (binding.editTextEmail.text.isNullOrEmpty()) {
            isValid = false
            toast("Inserte correo")
        } else {
            if (!binding.editTextEmail.text.toString().isValidEmail()) {
                isValid = false
                toast("Correo inválido")
            }
        }

        return isValid
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val IMAGE_PICK_CODE1 = 1001
    }

    fun openImagePicker(mode: Int) {
        if(mode == 0){
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }else{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, IMAGE_PICK_CODE1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            // Handle image picked from gallery
            val imageUri = data?.data
            binding.profileImage.setImageURI(imageUri) // Update profile image preview
            newProfilePicture = imageUri
        }

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE1) {
            selectedImagesUris.clear()
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    selectedImagesUris.add(imageUri)
                }
                binding.imagePreviewContainer.removeAllViews()
                for (uri in selectedImagesUris) {
                    val imageView = ImageView(requireContext())
                    imageView.setImageURI(uri)
                    imageView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.imagePreviewContainer.addView(imageView)
                }
                binding.imagePreviewContainer.visibility = View.VISIBLE
                binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.ic_edit))
                binding.buttonDeleteImages.visibility = View.VISIBLE
            }
        }
    }

    private fun status(status: String) {
        authViewModel.getUserSession { user ->
            user?.let { userViewModel.updateUserInfo(it.copy(status = status)) }
        }
    }

    override fun onResume() {
        super.onResume()
        status("online")
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }
}