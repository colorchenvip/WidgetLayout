package com.rexy.ui

import android.os.Bundle
import android.view.View
import com.rexy.common.KBaseActivity
import com.rexy.widgetlayout.example.R

/**
 * Created by rexy on 17/7/28.
 */
class KActivityMain : KBaseActivity(), View.OnClickListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_entry)
        findViewById(R.id.buttonColumn)?.setOnClickListener(this)
        findViewById(R.id.buttonPageScroll)?.setOnClickListener(this)
        findViewById(R.id.buttonWrapLabel)?.setOnClickListener(this)
        findViewById(R.id.buttonNestFloat)?.setOnClickListener(this)
        findViewById(R.id.buttonRefresh)?.setOnClickListener(this)
        findViewById(R.id.buttonHierarchy)?.setOnClickListener(this)
    }

    override fun onClick(v: View) {
       when (v.id){
           R.id.buttonColumn -> KFragmentColumnLayout::class.java
           R.id.buttonPageScroll -> KFragmentPageScrollContainer::class.java
           R.id.buttonWrapLabel -> KFragmentWrapLabelLayout::class.java
           R.id.buttonNestFloat -> KFragmentNestFloatLayout::class.java
           R.id.buttonRefresh -> KFragmentRefreshLayout::class.java
           R.id.buttonHierarchy -> KFragmentHierarchyLayout::class.java
           else -> null
       }?.let { KActivityCommon.launch(this,it) }
    }
}