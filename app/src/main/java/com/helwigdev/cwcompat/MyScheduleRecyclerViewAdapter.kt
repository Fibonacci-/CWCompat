package com.helwigdev.cwcompat

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


import com.helwigdev.cwcompat.ScheduleFragment.OnListFragmentInteractionListener

import kotlinx.android.synthetic.main.fragment_schedule.view.*
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text

/**
 * [RecyclerView.Adapter] that can display a [JSONObject] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MyScheduleRecyclerViewAdapter(
        private val mValues: JSONArray,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyScheduleRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as JSONObject
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item:JSONObject = mValues.getJSONObject(position)
        holder.mIdView.text = item.getString("id")
        holder.mContentView.text = item.getString("name")
        if(item.has("where")){
            holder.mLocation.text = item.getJSONObject("where").getString("name")
            holder.mLocation.visibility = View.VISIBLE
        } else {
            holder.mLocation.visibility = View.GONE
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.length()

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_number
        val mContentView: TextView = mView.content
        val mLocation:TextView = mView.tv_location

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
