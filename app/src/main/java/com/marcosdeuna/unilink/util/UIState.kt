package com.marcosdeuna.unilink.util

sealed class UIState<out T> {
    data class Success<out T>(val data: T) : UIState<T>()
    data class Error(val exception: String) : UIState<Nothing>()
    object Loading : UIState<Nothing>()
    object Empty : UIState<Nothing>()
}