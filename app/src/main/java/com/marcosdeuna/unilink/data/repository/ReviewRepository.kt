package com.marcosdeuna.unilink.data.repository

import com.marcosdeuna.unilink.data.model.Review
import com.marcosdeuna.unilink.util.UIState

interface ReviewRepository {

    fun getReviews(result: (UIState<List<Review>>) -> Unit)

    fun getReviewById(id: String, result: (UIState<Review>) -> Unit)

    fun addReview(review: Review, result: (UIState<String>) -> Unit)

    fun updateReview(review: Review, result: (UIState<String>) -> Unit)

    fun deleteReview(id: String, result: (UIState<String>) -> Unit)

    fun getReviewsByMarkerId(markerId: String, result: (UIState<List<Review>>) -> Unit)
}