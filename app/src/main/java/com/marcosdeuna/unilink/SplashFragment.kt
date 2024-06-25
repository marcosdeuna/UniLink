package com.marcosdeuna.unilink

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_splash, container, false)

        authViewModel.getUserSession {
                if(activity?.intent?.extras != null && it != null){
                    val userid = activity?.intent?.extras?.getString("userId")
                    if (userid != null) {
                        authViewModel.getUserById(userid){
                            if(it != null){
                                findNavController().navigate(R.id.action_splashFragment_to_messageFragment, Bundle().apply {
                                    putString("receiverId", it.id)
                                })
                            }
                        }
                    }

                }else{
                    Handler(Looper.getMainLooper()).postDelayed({

                        if (onBoardingFinished()) {
                            findNavController().navigate(R.id.navigate_splashFragment_to_loginFragment)
                        }else{
                            findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
                        }
                    }, 1000)
                }
        }

        return view
    }

    private fun onBoardingFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("Finished", false)
    }

}