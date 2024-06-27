package com.marcosdeuna.unilink.ui.discoverPlaces

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marcosdeuna.unilink.data.model.Review
import com.marcosdeuna.unilink.data.repository.ReviewRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor( val repository: ReviewRepository): ViewModel() {
    private val _createReview = MutableLiveData<UIState<String>>()
    val createReview: MutableLiveData<UIState<String>>
        get() = _createReview

    fun createReview(review: Review){
        _createReview.postValue(UIState.Loading)
        repository.addReview(review){
            _createReview.postValue(it)
        }
    }

    private val _reviews = MutableLiveData<UIState<List<Review>>>()
    val reviews: MutableLiveData<UIState<List<Review>>>
        get() = _reviews

    fun getReviews() {
        _reviews.value = UIState.Loading
        repository.getReviews { result ->
            _reviews.value = result
        }
    }

    private val _deleteReview = MutableLiveData<UIState<String>>()

    val deleteReview: MutableLiveData<UIState<String>>
        get() = _deleteReview

    fun deleteReview(review: Review){
        _deleteReview.postValue(UIState.Loading)
        repository.deleteReview(review.id){
            _deleteReview.postValue(it)
        }
    }

    private val _updateReview = MutableLiveData<UIState<String>>()

    val updateReview: MutableLiveData<UIState<String>>
        get() = _updateReview

    fun updateReview(review: Review){
        _updateReview.postValue(UIState.Loading)
        repository.updateReview(review){
            _updateReview.postValue(it)
        }
    }


}