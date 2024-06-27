package com.marcosdeuna.unilink.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.marcosdeuna.unilink.data.model.Review
import com.marcosdeuna.unilink.util.UIState

class ReviewRepositoryImpl(val database: FirebaseFirestore) : ReviewRepository {
    override fun getReviews(result: (UIState<List<Review>>) -> Unit) {
        database.collection("reviews")
            .get()
            .addOnSuccessListener { documents ->
                val reviews = documents.toObjects(Review::class.java)
                result(UIState.Success(reviews))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun getReviewById(id: String, result: (UIState<Review>) -> Unit) {
        database.collection("reviews")
            .document(id)
            .get()
            .addOnSuccessListener { document ->
                val review = document.toObject(Review::class.java)
                if (review != null) {
                    result(UIState.Success(review))
                } else {
                    result(UIState.Error("Review not found"))
                }
            }

    }

    override fun addReview(review: Review, result: (UIState<String>) -> Unit) {
        val document = database.collection("reviews").document()
        review.id = document.id
        document.set(review)
            .addOnSuccessListener {
                result(UIState.Success("Review added"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun updateReview(review: Review, result: (UIState<String>) -> Unit) {
        database.collection("reviews")
            .document(review.id)
            .set(review)
            .addOnSuccessListener {
                result(UIState.Success("Review updated"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun deleteReview(id: String, result: (UIState<String>) -> Unit) {
        database.collection("reviews")
            .document(id)
            .delete()
            .addOnSuccessListener {
                result(UIState.Success("Review deleted"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun getReviewsByMarkerId(markerId: String, result: (UIState<List<Review>>) -> Unit) {
        database.collection("reviews")
            .whereEqualTo("markerId", markerId)
            .get()
            .addOnSuccessListener { documents ->
                val reviews = documents.toObjects(Review::class.java)
                result(UIState.Success(reviews))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }
}