package com.marcosdeuna.unilink.ui.discoverPeople

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Context
import android.widget.ImageView
import com.marcosdeuna.unilink.data.model.Group
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.databinding.ItemUserLayoutBinding
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.DialogUserDetailsBinding
import com.marcosdeuna.unilink.util.hide

class DiscoverPeopleAdapter(val context: Context, val list: ArrayList<User>, val onSendClicked: (Int, User) -> Unit): RecyclerView.Adapter<DiscoverPeopleAdapter.DiscoverPeopleViewHolder>() {

    inner class DiscoverPeopleViewHolder (val binding: ItemUserLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverPeopleViewHolder {
        return DiscoverPeopleViewHolder(ItemUserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: DiscoverPeopleViewHolder, position: Int) {
        val currentUser = list[position]

        holder.binding.btnDelete.hide()
        holder.binding.btnEdit.hide()

        // Set user name and email
        holder.binding.userName.text =
            "${currentUser.firstName} ${currentUser.lastName}, ${currentUser.age}"
        holder.binding.userEmail.text = currentUser.email


        // Clear previous images in ViewFlipper
        holder.binding.viewFlipperImages.removeAllViews()

        // Load social pictures into ViewFlipper
        for (imageUrl in currentUser.socialPictures) {
            val imageView = createImageView(imageUrl)
            holder.binding.viewFlipperImages.addView(imageView)
        }

        holder.binding.chat.setOnClickListener {
            onSendClicked(position, currentUser)
        }

        // Start flipping images
        holder.binding.viewFlipperImages.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    holder.binding.viewFlipperImages.stopFlipping()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if (motionEvent.x < view.width / 2) {
                        holder.binding.viewFlipperImages.showNext()
                    } else {
                        holder.binding.viewFlipperImages.showPrevious()
                    }
                }
            }
            true
        }

        holder.binding.view.setOnClickListener{
            showUserDetailsDialog(currentUser)
        }
    }

    private fun createImageView(imageUrl: String): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        Glide.with(context)
            .load(imageUrl)
            .into(imageView)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
    }
    private fun showUserDetailsDialog(user: User) {
        val dialogBinding = DialogUserDetailsBinding.inflate(LayoutInflater.from(context))

        dialogBinding.userFullName.text = "${user.firstName} ${user.lastName}"
        dialogBinding.userEmailDetail.text = user.email
        dialogBinding.userAge.text = "Edad: ${user.age}"
        dialogBinding.userCareer.text = "Carrera: ${user.career}"
        dialogBinding.userGenre.text = "Género: ${user.genre}"
        dialogBinding.userDescription.text = user.description

        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}