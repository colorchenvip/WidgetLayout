package com.rexy.model

import android.content.Context
import android.content.res.ColorStateList
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.rexy.widgetlayout.example.R
import com.rexy.widgets.view.FadeTextButton

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2017-07-28 09:24
 */
class KTestRecyclerAdapter(var mContext: Context,var mItems:List<String> ?) :RecyclerView.Adapter<KTestRecyclerAdapter.KTestRecyclerHolder>(){

    override fun getItemCount()= mItems?.size?:0

    override fun onCreateViewHolder(p: ViewGroup, type: Int): KTestRecyclerHolder {
        val textView = FadeTextButton(mContext)
        textView.textSize = 18f
        textView.isClickable = true
        textView.layoutParams = RecyclerView.LayoutParams(-1, -2)
        textView.setPadding(30, 20, 30, 20)
        textView.setTextColor(ColorStateList.valueOf(mContext.getColor(R.color.textButton)))
        textView.setBackgroundColor(mContext.getColor(R.color.itemBackground))
        return KTestRecyclerHolder(textView)

    }

    override fun onBindViewHolder(holder: KTestRecyclerHolder, position: Int) {
        with(mItems?.get(position)){
            (holder.itemView as TextView).text=this
        }
    }

    class KTestRecyclerHolder(itemView: View?) : RecyclerView.ViewHolder(itemView)
}