package com.rexy.ui

import android.content.Intent
import android.os.Bundle
import com.rexy.common.KBaseActivity

/**
 * Created by rexy on 17/7/28.
 */
class KActivityLauncher : KBaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, KActivityMain::class.java))
        finish()
    }
}