package com.marcosdeuna.unilink.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.hide(){
    this.visibility = View.INVISIBLE
}

fun View.show(){
    this.visibility = View.VISIBLE
}

fun Fragment.toast(message: String?){
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches() && this.isNotEmpty()
}