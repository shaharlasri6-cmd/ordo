package com.shahar.quickcontacts

import android.app.Activity
import android.os.Bundle

@Deprecated("Widget calls now use CallOverlayService")
class CallTransitionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
