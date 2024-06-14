package com.marcosdeuna.unilink.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.ui.onboarding.screens.FirstOBSFragment
import com.marcosdeuna.unilink.ui.onboarding.screens.SecondOBSFragment
import com.marcosdeuna.unilink.ui.onboarding.screens.ThirdOBSFragment
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator


class OnboardingFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)
        val fragmentList = arrayListOf<Fragment>(
            FirstOBSFragment(),
            SecondOBSFragment(),
            ThirdOBSFragment()
        )

        val adapter = viewPagerAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        val viewPager = view.findViewById<ViewPager2>(R.id.slideViewPager)

        viewPager.adapter = adapter

        val springDotsIndicator = view.findViewById<SpringDotsIndicator>(R.id.spring_dots_indicator)
        springDotsIndicator.attachTo(viewPager)

        return view
    }

}