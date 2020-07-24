package com.toggl.onboarding.welcome.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.toggl.onboarding.R
import java.lang.IllegalStateException

class WelcomeCarouselAdapter(private val context: Context) : PagerAdapter() {
    override fun getCount(): Int = 3

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context)?.inflate(when (position) {
            0 -> R.layout.fragment_welcome_first_slide
            1 -> R.layout.fragment_welcome_second_slide
            2 -> R.layout.fragment_welcome_third_slide
            else -> throw IllegalStateException()
        }, container, false) ?: throw IllegalStateException()

        container.addView(view)
        return view
    }
}