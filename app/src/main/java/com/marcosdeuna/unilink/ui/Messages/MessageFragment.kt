package com.marcosdeuna.unilink.ui.Messages

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentMessageBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.user.MessageAdapter
import com.marcosdeuna.unilink.ui.user.UserViewModel
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.toast
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date


@AndroidEntryPoint
class MessageFragment : Fragment() {

    private lateinit var binding: FragmentMessageBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val messageViewModel: MessageViewModel by viewModels()
    private val receiverId: String by lazy {
        arguments?.getString("receiverId") ?: ""
    }

    private val fromPost: Post by lazy {
        arguments?.getParcelable("post") ?: Post()
    }
    private var receiverUser = User()
    private var currentUser = User()
    private var keyboardsize = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        authViewModel.getUserSession { user ->
            if (user != null) {
                currentUser = user
            }
        }
        authViewModel.getUserById(receiverId){
            if (it != null) {
                receiverUser = it
            }
            binding.username.text = receiverUser.firstName + " " + receiverUser.lastName
            Glide.with(requireContext()).load(receiverUser.profilePicture).into(binding.profilePicture)
            messageViewModel.getMessages(currentUser.id, receiverUser.id)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.message.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    updateBottomMargin(keyboardsize+60)
                    binding.aux.visibility = View.VISIBLE
                }, 400)
            }
        }

        binding.aux.setOnClickListener{
            updateBottomMargin(0)
            //ocultar teclado
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.message.windowToken, 0)
            //Eedit text pierde el foco
            binding.message.clearFocus()
            binding.aux.visibility = View.GONE
        }

        messageViewModel.messages.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UIState.Loading -> {
                    // Show loading
                }
                is UIState.Success -> {
                    val messages = result.data
                    val adapter = MessageAdapter(requireContext(), messages, receiverUser.profilePicture, currentUser)
                    binding.recyclerViewPosts.adapter = adapter
                    binding.recyclerViewPosts.scrollToPosition(messages.size - 1)
                }
                is UIState.Error -> {
                    toast(result.exception)
                }

                UIState.Empty -> TODO()
                is UIState.Error -> TODO()
                UIState.Loading -> TODO()
                is UIState.Success -> TODO()
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardsize = keypadHeight
        }

        seenMessage()



    }

    private fun updateBottomMargin(newMargin: Int) {
        val layoutParams = binding.bottom.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.setMargins(0, 0, 0, newMargin)
        binding.bottom.layoutParams = layoutParams
    }

    private fun seenMessage() {
        messageViewModel.getMessages("", "")
        messageViewModel.messages.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UIState.Loading -> {
                    // Show loading
                }
                is UIState.Success -> {
                    for(message in result.data){
                        if(message.receiverId.equals(currentUser.id) && message.senderId.equals(receiverId)){
                            messageViewModel.updateMessage(message.copy(isseen = true))
                        }
                    }
                }
                is UIState.Error -> {
                    toast(result.exception)
                }

                UIState.Empty -> TODO()
                is UIState.Error -> TODO()
                UIState.Loading -> TODO()
                is UIState.Success -> TODO()
            }
        }


    }
    
    private fun sendMessage() {
        val message = binding.message.text.toString()
        if (message.isEmpty()) {
            toast("Mensaje vacío")
            return
        }
        val senderId = currentUser.id
        val mensaje= Message(
            senderId = senderId,
            receiverId = receiverId,
            message = message,
            timestamp = Date(),
            isseen = false
        )
        messageViewModel.sendMessage(mensaje)
        binding.message.text.clear()
    }
    private fun status(status: String) {
        currentUser?.let { userViewModel.updateUserInfo(it.copy(status = status)) }
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

