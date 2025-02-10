package org.carstenf.wordfinder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View


fun slideUpAndHide(toHide: View, toReveal: View) {
    val animator = ObjectAnimator.ofFloat(toHide, "translationY", 0f, -toHide.height.toFloat())
    animator.duration = 300 // Animation duration in milliseconds
    animator.start()

    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            toHide.visibility = View.GONE // Hide the view after animation
            toReveal.visibility= View.VISIBLE
            toHide.translationY = 0f
        }
    })
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true // Can't really check so assume yes
    }
    val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
