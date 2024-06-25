package com.marcosdeuna.unilink.ui.post

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.get
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.marcosdeuna.unilink.data.model.Token
import com.marcosdeuna.unilink.databinding.FragmentListPostBinding
import com.marcosdeuna.unilink.ui.notifications.TokenViewModel
import com.marcosdeuna.unilink.ui.user.UserViewModel
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale.Category

@AndroidEntryPoint
class ListPostFragment : Fragment() {

    lateinit var binding: FragmentListPostBinding
    val authViewModel: AuthViewModel by viewModels()
    private val tokenViewModel: TokenViewModel by viewModels()
    val postViewModel: PostViewModel by viewModels()
    val userViewModel: UserViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    val adapter by lazy {
        ListPostAdapter(
            onItemClicked = { position, post ->
                // Acción al hacer clic en un elemento
                            findNavController().navigate(R.id.action_postFragment_to_detailPostFragment, Bundle().apply {
                    putParcelable("post", post)
                            })
            },
            onEditClicked = { position, post ->
                // Acción al hacer clic en editar
                findNavController().navigate(R.id.action_postFragment_to_createPostFragment, Bundle().apply {
                    putString("operation", "edit")
                    putParcelable("post", post)
                })
            },
            onDeleteClicked = { position, post ->
                // Acción al hacer clic en eliminar
                postViewModel.deletePost(post)
            },
            onSendClicked = { position, post ->
                // Acción al hacer clic en enviar
                findNavController().navigate(R.id.action_postFragment_to_messageFragment, Bundle().apply {
                    putString("receiverId", post.userId)
                    putParcelable("post", post)
                })
            },
            authViewModel = authViewModel,
            coroutineScope = coroutineScope
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListPostBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getFCMToken()

        binding.profileModal.visibility = View.GONE
        binding.recyclerViewPosts.adapter = adapter
        postViewModel.getPosts()
        postViewModel.posts.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    // Mostrar el loader
                    binding.progressBar.show()
                }
                is UIState.Success -> {
                    // Ocultar el loader
                    binding.progressBar.hide()
                    adapter.updatePosts(state.data.toMutableList())
                }
                is UIState.Error -> {
                    // Ocultar el loader
                    // Mostrar un mensaje de error
                    binding.progressBar.hide()
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }

        postViewModel.deletePost.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    // Mostrar el loader
                    binding.progressBar.show()
                }
                is UIState.Success -> {
                    // Ocultar el loader
                    binding.progressBar.hide()
                    if(binding.spinnerCategory.selectedItemPosition != 0) {
                        if (binding.searchBox.text.isNotEmpty()) {
                            adapter.filterPostByTitleAndCategory(binding.searchBox.text.toString(), binding.spinnerCategory.selectedItem.toString())
                        } else {
                            adapter.filterPostByCategory(binding.spinnerCategory.selectedItem.toString())
                        }
                    } else {
                        if (binding.searchBox.text.isNotEmpty()) {
                            adapter.filterPosyByTitle(binding.searchBox.text.toString())
                        } else {
                            postViewModel.getPosts()
                        }
                    }
                }
                is UIState.Error -> {
                    // Ocultar el loader
                    // Mostrar un mensaje de error
                    binding.progressBar.hide()
                    toast(state.exception)
                }
                UIState.Empty -> TODO()
            }
        }

        view.setOnTouchListener{_, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Ocultar el teclado cuando se toca fuera de un EditText
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
            false
        }
        setupViews()
        setUpSpinner()

        binding.eliminarCuenta.setOnClickListener {

            authViewModel.getUserSession { user ->
                for (post in adapter.getPosts()) {
                    if(post.userId == user?.id){
                        postViewModel.deletePost(post)
                    }
                }
                user?.let {
                    userViewModel.deleteUser(it)
                }
            }
            authViewModel.logout()
            authViewModel.deleteAccount()
            findNavController().navigate(R.id.action_postFragment_to_loginFragment)
        }

        binding.sorted.setOnClickListener {
            binding.sortModal.visibility = View.VISIBLE
        }

        binding.sortChronological.setOnClickListener {
            postViewModel.getPosts()
            binding.searchBox.setText("")
            binding.spinnerCategory.setSelection(0)
            binding.sortModal.visibility = View.GONE
            binding.sortChronological.isChecked = true
            binding.sortRelevance.isChecked = false
        }

        binding.sortRelevance.setOnClickListener {
            authViewModel.getUserSession { user ->
                adapter.sortedByCareer(user?.career ?: "")
            }
            binding.searchBox.setText("")
            binding.spinnerCategory.setSelection(0)
            binding.sortModal.visibility = View.GONE
            binding.sortChronological.isChecked = false
            binding.sortRelevance.isChecked = true
        }
    }

    private fun setUpSpinner() {
        val categories = arrayOf("Filtra por categoría", "Búsqueda de piso", "Asuntos académicos", "Eventos deportivos", "Eventos de ocio", "Actualidad en SdC", "Variado" )
        val Categoryadapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        Categoryadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = Categoryadapter


        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Acción al seleccionar un elemento del spinner
                if(position != 0) {
                    if (binding.searchBox.text.isNotEmpty()) {
                        adapter.filterPostByTitleAndCategory(binding.searchBox.text.toString(), categories[position])
                    } else {
                        adapter.filterPostByCategory(categories[position])
                    }
                } else {
                    postViewModel.getPosts()
                }
                binding.sortChronological.isChecked = true
                binding.sortRelevance.isChecked = false
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Acción al no seleccionar un elemento del spinner
                postViewModel.getPosts()
            }
        }

    }

    private fun setupViews() {

        binding.searchBox.addTextChangedListener(afterTextChanged = {
            if(binding.spinnerCategory.selectedItemPosition != 0) {
                adapter.filterPostByTitleAndCategory(it.toString(), binding.spinnerCategory.selectedItem.toString())
            }else{
                adapter.filterPosyByTitle(it.toString())
            }
            binding.sortChronological.isChecked = true
            binding.sortRelevance.isChecked = false
        })

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

        binding.seeProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_postFragment_to_detailUserFragment)
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
                    findNavController().navigate(R.id.action_postFragment_to_discoverPeopleFragment)
                    true
                }
                R.id.navigation_create_post -> {
                    findNavController().navigate(R.id.action_postFragment_to_createPostFragment, Bundle().apply {
                        putString("operation", "create")
                    })
                    true
                }
                R.id.navigation_discover_places -> {
                    // Acción para descubrir lugares
                    true
                }
                R.id.navigation_chats -> {
                    // Acción para chats
                    findNavController().navigate(R.id.action_postFragment_to_chatsFragment)
                    true
                }
                else -> false
            }
        }

        binding.cancelButton.setOnClickListener {
            binding.sortModal.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        binding.spinnerCategory.setSelection(0)
        binding.searchBox.setText("")
        binding.bottomNavigation.menu[0].isChecked = true
        binding.sortChronological.isChecked = true
        authViewModel.getUserSession { user ->
            user?.let {
                binding.profileName.setText(it.userName)
                val profileImageUrl = it.profilePicture
                if (profileImageUrl.isNotEmpty()) {
                    coroutineScope.launch {
                        val bitmap = downloadImage(profileImageUrl)
                        bitmap?.let {
                            binding.profilePicture.setImageBitmap(it)
                            binding.profilePictureLarge.setImageBitmap(it)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
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
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            authViewModel.getUserSession { user ->
                user?.let {
                    val token = Token(task.result.toString(),it.id )
                    tokenViewModel.saveToken(token)
                }
            }
        })
    }


}
