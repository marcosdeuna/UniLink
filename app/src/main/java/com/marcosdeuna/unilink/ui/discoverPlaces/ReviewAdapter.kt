package com.marcosdeuna.unilink.ui.discoverPlaces

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marcosdeuna.unilink.data.model.Review
import com.marcosdeuna.unilink.databinding.ItemReviewBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import java.util.Date
import java.util.concurrent.TimeUnit

class ReviewAdapter (val context: Context, val list: ArrayList<Review>, val authViewModel: AuthViewModel, private val onDeleteReviewClicked: (Review) -> Unit,
                     private val onEditReviewClicked: (Review) -> Unit): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    inner class ReviewViewHolder(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        return ReviewViewHolder(ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getReviews(): ArrayList<Review> {
        return list
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val currentReview = list[position]

        authViewModel.getUserById(currentReview.userId) { user ->
            user?.let {
                holder.binding.userFullName.text = user.userName
                Glide.with(context)
                    .load(it.profilePicture)
                    .into(holder.binding.profilePicture)
                authViewModel.getUserSession {
                    if (it?.id == user.id) {
                        holder.binding.deleteReviewButton.visibility = View.VISIBLE
                        holder.binding.editReviewButton.visibility = View.VISIBLE
                    }else{
                        holder.binding.deleteReviewButton.visibility = View.GONE
                        holder.binding.editReviewButton.visibility = View.GONE
                    }

                }
            }
        }
        holder.binding.reviewDescription.text = currentReview.comment
        holder.binding.reviewRating.rating = currentReview.valoration.toFloat()
        holder.binding.reviewTimestamp.text = dateFormatted(currentReview.timestamp!!)

        holder.binding.editReviewButton.setOnClickListener {
            onEditReviewClicked(currentReview)
        }

        holder.binding.deleteReviewButton.setOnClickListener {
            onDeleteReviewClicked(currentReview)
        }
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
}