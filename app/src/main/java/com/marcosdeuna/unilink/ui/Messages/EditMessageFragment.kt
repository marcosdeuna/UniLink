package com.marcosdeuna.unilink.ui.Messages

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.databinding.FragmentEditMessageBinding

class EditMessageFragment(private val message: Message,
                          private val onMessageUpdated: (Message) -> Unit
) : DialogFragment() {

    private lateinit var binding: FragmentEditMessageBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentEditMessageBinding.inflate(LayoutInflater.from(context))

        binding.editMessage.setText(message.message)

        binding.btnSave.setOnClickListener {
            val updatedText = binding.editMessage.text.toString()
            if (updatedText.isNotEmpty()) {
                onMessageUpdated(message.copy(message = updatedText))
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return Dialog(requireContext()).apply {
            setContentView(binding.root)
        }
    }
}