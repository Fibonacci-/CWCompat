package com.helwigdev.cwcompat

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import org.json.JSONArray
import org.json.JSONObject

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ScheduleFragment.OnListFragmentInteractionListener] interface.
 */
class ScheduleFragment : Fragment() {

    private lateinit var schedules: JSONArray

    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            schedules = JSONArray(it.getString(KEY_SCHEDULE))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)

                adapter = MyScheduleRecyclerViewAdapter(schedules, listener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: JSONObject)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val KEY_SCHEDULE = "schedulekey"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(schedules: JSONArray) =
                ScheduleFragment().apply {
                    arguments = Bundle().apply {
                        putString(KEY_SCHEDULE, schedules.toString())
                    }
                }
    }
}
