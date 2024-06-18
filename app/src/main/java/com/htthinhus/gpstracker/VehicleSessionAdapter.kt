package com.htthinhus.gpstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class VehicleSessionAdapter(val vehicleSessionList: ArrayList<VehicleSession>): RecyclerView.Adapter<VehicleSessionAdapter.MyViewHolder>() {
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvTimestampStart: TextView = itemView.findViewById(R.id.tvTimestampStart)
        val tvTimestampEnd: TextView = itemView.findViewById(R.id.tvTimestampEnd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_vehicle_session, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return vehicleSessionList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
        val startTime = dateFormat.format(Date(vehicleSessionList[position].startTime!!.seconds*1000))
        val endTime = dateFormat.format(Date(vehicleSessionList[position].endTime!!.seconds*1000))

        holder.tvTimestampStart.text = startTime.toString()
        holder.tvTimestampEnd.text = endTime.toString()
    }
}