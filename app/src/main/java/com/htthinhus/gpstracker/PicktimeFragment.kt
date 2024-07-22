package com.htthinhus.gpstracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.htthinhus.gpstracker.databinding.FragmentPicktimeBinding
import com.htthinhus.gpstracker.fragments.VehicleSessionsFragmentDirections
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

class PicktimeFragment : Fragment() {

    private lateinit var binding: FragmentPicktimeBinding
    private var startTimestamp: Long = 0L
    private var endTimestamp: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPicktimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cvStartTime.setOnClickListener {
            startTimePicker()
        }
        binding.cvEndTime.setOnClickListener {
            endTimePicker()
        }
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnOK.setOnClickListener {
            if (startTimestamp != 0L && endTimestamp != 0L) {
                val direction =
                    PicktimeFragmentDirections
                        .actionPicktimeFragmentToVehicleSessionDetailFragment(
                            startTimestamp,
                            endTimestamp
                        )
                findNavController().navigate(direction)
            } else {
                Toast.makeText(context,"Pick time first", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startTimePicker() {

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val selectedDateTime = LocalDateTime.of(
                    selectedYear, selectedMonth + 1, selectedDay,
                    selectedHour, selectedMinute, 0
                )
                val timestamp = selectedDateTime.toEpochSecond(ZoneOffset.ofHours(7))
                startTimestamp = timestamp
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val formattedDateTime = selectedDateTime.format(formatter)
                binding.tvStartTimestamp.text = formattedDateTime

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, year, month, day).show()
    }

    private fun endTimePicker() {

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val selectedDateTime = LocalDateTime.of(
                    selectedYear, selectedMonth + 1, selectedDay,
                    selectedHour, selectedMinute, 0
                )

                val timestamp = selectedDateTime.toEpochSecond(ZoneOffset.ofHours(7))
                endTimestamp = timestamp
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val formattedDateTime = selectedDateTime.format(formatter)
                binding.tvEndTimestamp.text = formattedDateTime

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, year, month, day).show()
    }

}