package com.marcosdeuna.unilink.ui.user

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.ChatItemLeftBinding
import com.marcosdeuna.unilink.databinding.ChatItemRightBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(
    private val context: Context,
    private val list: List<Message>,
    private val image:String,
    private val currentUser: User,
    private val MSG_TYPE_LEFT: Int = 0,
    private val MSG_TYPE_RIGHT: Int = 1
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MessageViewHolderLeft(val binding: ChatItemLeftBinding) : RecyclerView.ViewHolder(binding.root)
    inner class MessageViewHolderRight(val binding: ChatItemRightBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == MSG_TYPE_LEFT) {
            val binding = ChatItemLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MessageViewHolderLeft(binding)
        } else {
            val binding = ChatItemRightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MessageViewHolderRight(binding)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = list[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(currentMessage.timestamp)

        when (holder.itemViewType) {
            MSG_TYPE_LEFT -> {
                val viewHolderLeft = holder as MessageViewHolderLeft
                viewHolderLeft.binding.showMessage.text = currentMessage.message
                viewHolderLeft.binding.timeLabel.text = formattedDate
                Glide.with(context)
                    .load(image)
                    .into(viewHolderLeft.binding.profilePicture)
                // Handle other left-side view bindings as needed
            }
            MSG_TYPE_RIGHT -> {
                val viewHolderRight = holder as MessageViewHolderRight
                viewHolderRight.binding.showMessage.text = currentMessage.message
                viewHolderRight.binding.timeLabel.text = formattedDate
                if(position == list.size - 1){
                    if(currentMessage.isseen){
                        viewHolderRight.binding.seenIndicator.visibility = View.VISIBLE
                        viewHolderRight.binding.sendIndicator.visibility = View.GONE
                    }else{
                        viewHolderRight.binding.seenIndicator.visibility = View.GONE
                        viewHolderRight.binding.sendIndicator.visibility = View.VISIBLE

                    }
                }else{
                    viewHolderRight.binding.sendIndicator.visibility = View.GONE
                    viewHolderRight.binding.seenIndicator.visibility = View.GONE

                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        if(list[position].senderId.equals(currentUser.id)){
            return MSG_TYPE_RIGHT
        }else{
            return MSG_TYPE_LEFT
        }
    }
}
