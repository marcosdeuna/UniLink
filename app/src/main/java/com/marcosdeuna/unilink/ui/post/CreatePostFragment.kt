package com.marcosdeuna.unilink.ui.post

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
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentCreatePostBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.auth.RegisterFragment
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

@AndroidEntryPoint
class CreatePostFragment : Fragment() {

    lateinit var binding: FragmentCreatePostBinding
    val postViewModel: PostViewModel by viewModels()
    val authViewModel: AuthViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val selectedImagesUris = mutableListOf<Uri>()
    private var operation: String = ""
    private var postToEdit: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCreatePostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val categories = arrayOf("Categoría 1", "Categoría 2", "Categoría 3")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        operation = arguments?.getString("operation") ?: ""
        if (operation == "edit") {
            binding.buttonSubmit.text = "Editar"
            binding.textPageTitle.text = "Editar Post"
            postToEdit = arguments?.getParcelable("post")
            postToEdit?.let {
                binding.editTitle.setText(it.title)
                binding.editDescription.setText(it.description)
                binding.spinnerCategory.setSelection(
                    when (it.category) {
                        "Categoría 1" -> 0
                        "Categoría 2" -> 1
                        else -> 2
                    }
                )
                if(it.images.isNotEmpty()){
                    loadImages(it.images)
                    binding.imagePreviewContainer.visibility = View.VISIBLE
                    binding.buttonAddImages.text = "Cambiar imagen"
                    binding.buttonDeleteImages.visibility = View.VISIBLE
                }
            }
        }else{
            binding.buttonSubmit.text = "Publicar"
            binding.textPageTitle.text = "Crear Nuevo Post"
        }



        binding.buttonSubmit.setOnClickListener {
            if (validation()) {
                if(operation == "edit"){
                    if(selectedImagesUris.isNotEmpty()) {
                        postViewModel.onUploadImage(selectedImagesUris) { updateState ->
                            when (updateState) {
                                is UIState.Loading -> {
                                    binding.createProgress.show()
                                    binding.buttonSubmit.setText("")
                                }

                                is UIState.Success -> {
                                    val images = updateState.data
                                    val post = postToEdit?.id?.let { it1 ->
                                        postToEdit?.userId?.let { it2 ->
                                            Post(
                                                id = it1,
                                                title = binding.editTitle.text.toString(),
                                                description = binding.editDescription.text.toString(),
                                                category = binding.spinnerCategory.selectedItem.toString(),
                                                userId = it2,
                                                images = images,
                                                timestamp = Date()
                                            )
                                        }
                                    }
                                    if (post != null) {
                                        postViewModel.updatePost(post)
                                    }
                                }

                                is UIState.Error -> {
                                    binding.createProgress.hide()
                                    binding.buttonSubmit.setText("Editar")
                                    toast("Error al subir la imagen")
                                }

                                UIState.Empty -> TODO()
                            }
                        }
                    }else{
                        val post = postToEdit?.id?.let { it1 ->
                            postToEdit?.userId?.let { it2 ->
                                Post(
                                    id = it1,
                                    title = binding.editTitle.text.toString(),
                                    description = binding.editDescription.text.toString(),
                                    category = binding.spinnerCategory.selectedItem.toString(),
                                    userId = it2,
                                    images = postToEdit?.images ?: emptyList(),
                                    timestamp = Date()
                                )
                            }
                        }
                        if (post != null) {
                            postViewModel.updatePost(post)
                        }
                    }
                }else{
                    createPost()
                }
            }
        }

        binding.buttonAddImages.setOnClickListener{
            openImagePicker()
        }

        binding.buttonDeleteImages.setOnClickListener{
            selectedImagesUris.clear()
            postToEdit?.images = emptyList()
            binding.imagePreviewContainer.visibility = View.GONE
            binding.buttonAddImages.text = "Agregar imagen"
            binding.buttonDeleteImages.visibility = View.GONE
        }


        postViewModel.addPost.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.createProgress.show()
                    binding.buttonSubmit.setText("")
                }
                is UIState.Success -> {
                    binding.createProgress.hide()
                    binding.buttonSubmit.setText("Publicar")
                    toast(state.data)
                    findNavController().navigate(R.id.action_createPostFragment_to_postFragment)
                }
                is UIState.Error -> {
                    binding.createProgress.hide()
                    binding.buttonSubmit.setText("Publicar")
                    toast("Error al crear el post")
                }

                UIState.Empty -> TODO()
            }
        }

        postViewModel.updatePost.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.createProgress.show()
                    binding.buttonSubmit.setText("")
                }
                is UIState.Success -> {
                    binding.createProgress.hide()
                    binding.buttonSubmit.setText("Editar")
                    toast(state.data)
                    findNavController().navigate(R.id.action_createPostFragment_to_postFragment)
                }
                is UIState.Error -> {
                    binding.createProgress.hide()
                    binding.buttonSubmit.setText("Editar")
                    toast("Error al editar el post")
                }

                UIState.Empty -> TODO()
            }
        }
    }

    private fun createPost() {
        var user_id = ""
        authViewModel.getUserSession { user ->
            user?.let {
                user_id = it.id
            }
        }
        if(selectedImagesUris.isNotEmpty()) {
            postViewModel.onUploadImage(selectedImagesUris) { updateState ->
                when (updateState) {
                    is UIState.Loading -> {
                        binding.createProgress.show()
                        binding.buttonSubmit.setText("")
                    }
                    is UIState.Success -> {
                        val images = updateState.data
                        val post = Post(
                            title = binding.editTitle.text.toString(),
                            description = binding.editDescription.text.toString(),
                            category = binding.spinnerCategory.selectedItem.toString(),
                            userId = user_id,
                            images = images,
                            timestamp = Date()
                        )
                        postViewModel.addPost(post)
                    }
                    is UIState.Error -> {
                        binding.createProgress.hide()
                        binding.buttonSubmit.setText("Publicar")
                        toast("Error al subir la imagen")
                    }
                    UIState.Empty -> TODO()
                }
            }
        }else{
            val post = Post(
                title = binding.editTitle.text.toString(),
                description = binding.editDescription.text.toString(),
                category = binding.spinnerCategory.selectedItem.toString(),
                userId = user_id,
                images = emptyList(),
                timestamp = Date()
            )
            postViewModel.addPost(post)

        }
    }



    private fun validation(): Boolean {
        if (binding.editTitle.text.toString().isEmpty()) {
            toast("El título es requerido")
            return false
        }
        if (binding.editDescription.text.toString().isEmpty()) {
            toast("La descripción es requerida")
            return false
        }
        return true
    }

    fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, CreatePostFragment.IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        selectedImagesUris.clear()
        if (resultCode == Activity.RESULT_OK && requestCode == CreatePostFragment.IMAGE_PICK_CODE) {
            if (data?.clipData != null) {
                val clipData = data.clipData
                for (i in 0 until clipData!!.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    selectedImagesUris.add(imageUri)
                }
            } else {
                data?.data?.let {
                    selectedImagesUris.add(it)
                }
            }
            updateImagePreview()
            binding.imagePreviewContainer.visibility = View.VISIBLE
            binding.buttonAddImages.text = "Cambiar imagen"
            binding.buttonDeleteImages.visibility = View.VISIBLE
        }else{
            toast("No se seleccionó ninguna imagen")
        }

        if(selectedImagesUris.isEmpty()){
            binding.imagePreviewContainer.visibility = View.GONE
            binding.buttonAddImages.text = "Agregar imagen"
            binding.buttonDeleteImages.visibility = View.GONE
        }
    }

    // Dentro de la función updateImagePreview en tu Fragmento

    private fun updateImagePreview() {
        binding.imagePreviewContainer.removeAllViews()

        for (uri in selectedImagesUris) {
            val imageView = ImageView(binding.root.context)
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.marginEnd = 20
            layoutParams.gravity = RelativeLayout.CENTER_IN_PARENT
            layoutParams.weight = 400F
            layoutParams.height = 400
            imageView.layoutParams = layoutParams
            imageView.setImageURI(uri)
            imageView.scaleType = ImageView.ScaleType.CENTER
            binding.imagePreviewContainer.addView(imageView)

        }
    }

    private fun loadImages(imageUrls: List<String>) {
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
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = 8.dpToPx()
        imageView.layoutParams = layoutParams
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
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

    private fun Int.dpToPx(): Int = (this * binding.root.context.resources.displayMetrics.density).toInt()



    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}