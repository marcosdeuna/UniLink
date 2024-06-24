package com.marcosdeuna.unilink.ui.post

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.databinding.FragmentDetailPostBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@AndroidEntryPoint
class DetailPostFragment : Fragment() {

    private lateinit var binding: FragmentDetailPostBinding
    private val postViewModel: PostViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDetailPostBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val post = arguments?.getParcelable<Post>("post")
        binding.textTitle.text = post?.title
        binding.textDescription.text = post?.description
        binding.textCategory.text = post?.category
        if (post != null) {
            authViewModel.getUserById(post.userId) { user ->
                binding.textUserName.setText(user?.userName?.uppercase() )
                authViewModel.getUserSession { userSession ->
                    if (userSession?.id == user?.id) {
                        binding.buttonEditPost.visibility = View.VISIBLE
                        binding.buttonDeletePost.visibility = View.VISIBLE
                        binding.buttonSendMessage.visibility = View.GONE
                    } else {
                        binding.buttonEditPost.visibility = View.GONE
                        binding.buttonDeletePost.visibility = View.GONE
                        binding.buttonSendMessage.visibility = View.VISIBLE
                    }
                }
            }
        }

        if(post?.images?.isEmpty() == true) {
            binding.imageContainer.visibility = View.GONE
        }else{
            binding.imageContainer.visibility = View.VISIBLE
            post?.images?.let { loadImages(it) }
        }


        binding.btnBack.setOnClickListener {
            //volver a la pantalla anterior de la pila
            findNavController().popBackStack()
        }

        binding.buttonEditPost.setOnClickListener {
            findNavController().navigate(R.id.action_detailPostFragment_to_createPostFragment, Bundle().apply {
                putString("operation", "edit")
                putParcelable("post", post)
            })
        }

        binding.buttonDeletePost.setOnClickListener {
            postViewModel.deletePost(post!!)
            findNavController().navigate(R.id.action_detailPostFragment_to_postFragment)
        }

        binding.imageContainer.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    binding.imageContainer.stopFlipping()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if (motionEvent.x < view.width / 2) {
                        binding.imageContainer.showNext()
                    } else {
                        binding.imageContainer.showPrevious()
                    }
                }
            }
            true
        }
    }

    private fun loadImages(imageUrls: List<String>) {
        binding.imageContainer.removeAllViews()

        for (imageUrl in imageUrls) {
            coroutineScope.launch {
                val bitmap = downloadImage(imageUrl)
                bitmap?.let {
                    val imageView = createImageView(it)
                    binding.imageContainer.addView(imageView)
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