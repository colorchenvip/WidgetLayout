package com.rexy.ui

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rexy.common.KBaseFragment
import com.rexy.model.KDecorationOffsetLinear
import com.rexy.model.KTestRecyclerAdapter
import com.rexy.widgetlayout.example.R

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2017-07-28 13:32
 */
class KFragmentNestFloatLayout : KBaseFragment() {

    internal val mListView by lazy { view!!.findViewById(R.id.listView) as RecyclerView }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_nestfloatlayout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView(mListView, 50)
    }

    private fun initRecyclerView(recyclerView: RecyclerView, initCount: Int) {
        recyclerView.adapter = KTestRecyclerAdapter(activity, createData("item", initCount))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.addItemDecoration(KDecorationOffsetLinear(false, 20))
    }

    private fun createData(prefix: String, count: Int): List<String> {
        val list = MutableList(count + 1){
            prefix + " " + (it + 1)
        }
        return list
    }
}