package com.marcosdeuna.unilink.ui.user

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.databinding.FragmentDetailUserBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.post.ListPostAdapter
import com.marcosdeuna.unilink.ui.post.PostViewModel
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.show
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@AndroidEntryPoint
class DetailUserFragment : Fragment() {

    private lateinit var binding: FragmentDetailUserBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val adapter by lazy {
        ListPostAdapter(
            onItemClicked = { position, post ->
                // Acción al hacer clic en un elemento
                findNavController().navigate(R.id.action_detailUserFragment_to_detailPostFragment, Bundle().apply {
                    putParcelable("post", post)
                })
            },
            onEditClicked = { position, post ->
                // Acción al hacer clic en editar
                findNavController().navigate(R.id.action_detailUserFragment_to_createPostFragment, Bundle().apply {
                    putString("operation", "edit")
                    putParcelable("post", post)
                })
            },
            onDeleteClicked = { position, post ->
                // Acción al hacer clic en eliminar
                postViewModel.deletePost(post)
            },
            authViewModel = authViewModel,
            coroutineScope = coroutineScope
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDetailUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener(){
            findNavController().navigate(R.id.action_detailUserFragment_to_postFragment)
        }

        binding.btnEdit.setOnClickListener(){
            findNavController().navigate(R.id.action_detailUserFragment_to_editUserFragment)
        }

        binding.recyclerView.adapter = adapter
        postViewModel.getPosts()
        postViewModel.posts.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.progressBar.show()
                }
                is UIState.Success -> {
                    binding.progressBar.hide()
                    adapter.updatePosts(state.data.toMutableList())
                    authViewModel.getUserSession { user ->
                        adapter.filterPostByUser(user?.id ?: "")
                    }
                    if (adapter.itemCount == 0) {
                        binding.textNoPost.visibility = View.VISIBLE
                    } else {
                        binding.textNoPost.visibility = View.GONE
                    }
                }
                is UIState.Error -> {
                    binding.progressBar.hide()
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }

        postViewModel.deletePost.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.progressBar.show()
                }
                is UIState.Success -> {
                    binding.progressBar.hide()
                    postViewModel.getPosts()
                }
                is UIState.Error -> {
                    binding.progressBar.hide()
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }


    }

    override fun onStart() {
        super.onStart()
        authViewModel.getUserSession { user ->
            binding.textUserName.text = user?.userName?.uppercase()
            binding.textEmail.text = user?.email
            binding.textName.text = user?.firstName
            binding.textLastName.text = user?.lastName
            binding.textCareer.text = user?.career
            val profileImageUrl = user?.profilePicture
            if (profileImageUrl != null) {
                if (profileImageUrl.isNotEmpty()) {
                    coroutineScope.launch {
                        val bitmap = downloadImage(profileImageUrl)
                        bitmap?.let {
                            binding.imgUser.setImageBitmap(it)
                        }
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
}