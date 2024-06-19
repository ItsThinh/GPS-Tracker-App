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

    var onItemClick: ((VehicleSession) -> Unit)? = null
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvTimestampStart: TextView = itemView.findViewById(R.id.tvTimestampStart)
        val tvTimestampEnd: TextView = itemView.findViewById(R.id.tvTimestampEnd)
        val tvDrivingTime: TextView = itemView.findViewById(R.id.tvDrivingTime)
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

        val drivingTimeSeconds =
            vehicleSessionList[position].endTime!!.seconds - vehicleSessionList[position].startTime!!.seconds

        holder.tvTimestampStart.text = startTime.toString()
        holder.tvTimestampEnd.text = endTime.toString()
        holder.tvDrivingTime.text =  formatSecondsToHHMMSS(drivingTimeSeconds)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(vehicleSessionList[position])
        }
    }

    private fun formatSecondsToHHMMSS(seconds: Long): String {
        val hour = seconds / 3600
        val minute = (seconds % 3600) / 60
        val second = seconds % 60
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }


}