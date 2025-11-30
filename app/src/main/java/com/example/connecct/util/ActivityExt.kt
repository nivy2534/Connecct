package com.example.connecct.util

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun getActivity(): Activity? {
    var activity = LocalContext.current
    while (activity is ContextWrapper) {
        if (activity is Activity) return activity
        activity = activity.baseContext
    }
    return null
}
