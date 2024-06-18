package com.marcosdeuna.unilink.ui.post

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.recyclerview.widget.RecyclerView
import com.marcosdeuna.unilink.data.model.Post
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.marcosdeuna.unilink.databinding.ItemNoteLayoutBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.util.hide
import com.marcosdeuna.unilink.util.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.TimeUnit

class ListPostAdapter (
    val onItemClicked: (Int, Post) -> Unit,
    val onEditClicked: (Int, Post) -> Unit,
    val onDeleteClicked: (Int, Post) -> Unit,
    val authViewModel: AuthViewModel,
    val coroutineScope: CoroutineScope
): RecyclerView.Adapter<ListPostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Post>()
    private var originalList = mutableListOf<Post>()
    private var imagenCache = mutableMapOf<String, Bitmap>()

    fun setPosts(posts: List<Post>) {
        this.posts.clear()
        this.posts.addAll(posts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PostViewHolder {
        val view = ItemNoteLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(posts: MutableList<Post>) {
        this.posts.clear()
        this.posts.addAll(posts)
        this.originalList = posts
        notifyDataSetChanged()
    }

    fun filterPosyByTitle(title: String) {
        if (title.isEmpty()){
            posts.clear()
            posts.addAll(originalList)
            notifyDataSetChanged()
        } else {
            posts.clear()
            posts.addAll(originalList.filter { it.title.contains(title, ignoreCase = true) })
            notifyDataSetChanged()
        }
    }

    fun filterPostByUser(user: String) {
        if (user.isEmpty()) {
            posts.clear()
            posts.addAll(originalList)
            notifyDataSetChanged()
        } else {
            posts.clear()
            posts.addAll(originalList.filter { it.userId.contains(user, ignoreCase = true) })
            notifyDataSetChanged()
        }
    }

    fun filterPostByCategory(category: String) {
        if (category.isEmpty()) {
            posts.clear()
            posts.addAll(originalList)
            notifyDataSetChanged()
        } else {
            posts.clear()
            posts.addAll(originalList.filter { it.category.contains(category, ignoreCase = true) })
            notifyDataSetChanged()
        }
    }

    fun deletePost(post: Post) {
        val index = posts.indexOfFirst { it.id == post.id }
        if (index != -1) {
            posts.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class PostViewHolder(val binding: ItemNoteLayoutBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Post) {
            binding.postTitle.setText(item.title)
            binding.postCategory.setText(item.category)
            if (binding.postDescription.lineCount > 2) {
                binding.postDescription.maxLines = 2
                binding.postDescription.ellipsize = android.text.TextUtils.TruncateAt.END
            }
            binding.postDescription.setText(item.description)
            authViewModel.getUserById(item.userId) { user ->
                binding.postUser.setText(user?.userName)
                authViewModel.getUserSession { currentUser ->
                    if (currentUser?.id == user?.id) {
                        binding.editPost.show()
                        binding.deletePost.show()
                        binding.sendMessage.visibility = android.view.View.GONE
                    } else {
                        binding.editPost.hide()
                        binding.deletePost.hide()
                        binding.sendMessage.show()
                    }
                }
            }
            binding.postTimestamp.setText(item.timestamp?.let { dateFormatted(it) })
            if (item.images.isEmpty()) {
                binding.postImagesContainer.visibility = android.view.View.GONE
            } else {
                binding.postImagesContainer.visibility = android.view.View.VISIBLE
                loadImages(item.images)
            }
            binding.root.setOnClickListener {
                onItemClicked(adapterPosition, item)
            }
            binding.editPost.setOnClickListener {
                onEditClicked(adapterPosition, item)
            }
            binding.deletePost.setOnClickListener {
                onDeleteClicked(adapterPosition, item)
            }
        }

        private fun loadImages(imageUrls: List<String>) {

            binding.postImagesContainer.removeAllViews()

            for (imageUrl in imageUrls) {
                coroutineScope.launch {
                    val bitmap = imagenCache[imageUrl] ?: downloadImage(imageUrl)
                    bitmap?.let {
                        imagenCache[imageUrl] = it
                        val imageView = createImageView(it)
                        withContext(Dispatchers.Main) {
                            binding.postImagesContainer.addView(imageView)
                        }
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
            imageView.scaleType = ImageView.ScaleType.CENTER
            return imageView
        }

        fun dateFormatted(date: Date): String {
            val now = Date()
            val diffInMillis = now.time - date.time

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val weeks = days / 7
            val months = days / 30
            val years = days / 365

            return when {
                years > 0 -> if (years == 1L) "hace $years año" else "hace $years años"
                months > 0 -> if (months == 1L) "hace $months mes" else "hace $months meses"
                weeks > 0 -> if (weeks == 1L) "hace $weeks semana" else "hace $weeks semanas"
                days > 0 -> if (days == 1L) "hace $days día" else "hace $days días"
                hours > 0 -> if (hours == 1L) "hace $hours hora" else "hace $hours horas"
                minutes > 0 -> if (minutes == 1L) "hace $minutes minuto" else "hace $minutes minutos"
                else -> "hace un momento"
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

        private fun Int.dpToPx(): Int = (this * binding.root.context.resources.displayMetrics.density).toInt()
    }
}