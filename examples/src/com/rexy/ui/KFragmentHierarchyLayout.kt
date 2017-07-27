package com.rexy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rexy.common.KBaseFragment
import com.rexy.widgetlayout.example.R

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2017-07-28 14:27
 */
class KFragmentHierarchyLayout : KBaseFragment(){
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_hierarchylayout, container, false)
    }
}