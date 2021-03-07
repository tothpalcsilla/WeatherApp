package com.example.weatherapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Jelezzük ki a hőmérsékletet, szélerősséget, szélirányt és a rövid leírást.
 * Valamint az api által visszaadott hely (település, város) nevét.
 */
class FirstFragment : Fragment() {

    //lateinit var location: TextView

    lateinit var city: TextView
    lateinit var lastUpdate: TextView
    lateinit var icon: ImageView
    lateinit var temperature: TextView
    lateinit var wind: TextView
    lateinit var wind_icon: ImageView
    lateinit var wind_speed_title: TextView
    lateinit var wind_speed: TextView
    lateinit var wind_direction_title: TextView
    lateinit var wind_direction: TextView
    lateinit var short_description: TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        city = view.findViewById(R.id.city)
        lastUpdate = view.findViewById(R.id.date)
        icon = view.findViewById(R.id.icon)
        temperature = view.findViewById(R.id.temperature)
        wind = view.findViewById(R.id.wind)
        wind_icon = view.findViewById(R.id.wind_icon)
        wind_speed_title = view.findViewById(R.id.wind_speed_title)
        wind_speed = view.findViewById(R.id.wind_speed)
        wind_direction_title = view.findViewById(R.id.wind_direction_title)
        wind_direction = view.findViewById(R.id.wind_direction)
        short_description = view.findViewById(R.id.short_description)

        /*view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/
    }
}