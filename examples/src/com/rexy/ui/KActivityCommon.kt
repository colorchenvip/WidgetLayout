package com.rexy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import com.rexy.common.KBaseActivity

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2017-07-27 17:35
 */
class KActivityCommon : KBaseActivity() {
    companion object {

        val KEY_FRAGMENT_NAME: String = "KEY_FRAGMENT_NAME"

        fun launch(context: Context, fragment: Class<out Fragment>) {
            val t = Intent(context, KActivityCommon::class.java)
            t.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            t.putExtra(KEY_FRAGMENT_NAME, fragment.name)
            context.startActivity(t)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(KEY_FRAGMENT_NAME)?.let {
            with(supportFragmentManager, { beginTransaction() })
                    .apply {
                        add(android.R.id.content, Fragment.instantiate(this@KActivityCommon, it, Bundle()), "root")
                        commitAllowingStateLoss()
                    }

        }
    }
}