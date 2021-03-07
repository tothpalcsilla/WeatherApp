package com.example.weatherapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

/**
 * Jelezzük ki a hőmérsékletet, szélerősséget, szélirányt és a rövid leírást.
 * Valamint az api által visszaadott hely (település, város) nevét.
 */
class FirstFragment : Fragment() {

    //lateinit var location: TextView

    lateinit var city: TextView
    lateinit var date: TextView
    lateinit var icon: ImageView
    lateinit var temperature: TextView
    lateinit var wind_speed: TextView
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
        date = view.findViewById(R.id.date)
        icon = view.findViewById(R.id.icon)
        temperature = view.findViewById(R.id.temperature)
        wind_speed = view.findViewById(R.id.wind_speed)
        wind_direction = view.findViewById(R.id.wind_direction)
        short_description = view.findViewById(R.id.short_description)

        /*view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/
    }
}