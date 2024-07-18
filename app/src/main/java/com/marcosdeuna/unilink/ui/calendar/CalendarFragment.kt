package com.marcosdeuna.unilink.ui.calendar

import EventDecorator
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.Events
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentCalendarBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.notifications.TokenViewModel
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.toast
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private val calendarViewModel: CalendarViewModel by viewModels()
    private lateinit var eventsAdapter: EventsAdapter
    private var currentEvent: Events? = null
    private val authViewModel: AuthViewModel by viewModels()
    private val tokenViewModel: TokenViewModel by viewModels()
    private var currentUser: User? = null
    private var selectedDate: Date? = null
    private val currentDecorators = mutableListOf<DayViewDecorator>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()

        calendarViewModel.getEvents()

        authViewModel.getUserSession { user ->
            currentUser = user
        }

        eventsAdapter = EventsAdapter(
            onEditAction = { event -> showEditEventDialog(event) },
            onDeleteAction = { event -> showDeleteConfirmationDialog(event) }
        )
        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.eventsRecyclerView.adapter = eventsAdapter

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addEventButton.setOnClickListener {
            showEditEventDialog(null)
        }

        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            selectedDate = date.date
            calendarViewModel.getEvents()
        }

        calendarViewModel.events.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    // Show loading
                }
                is UIState.Success -> {
                        val filteredUser = state.data.filter { event ->
                            event.userid == currentUser?.id
                        }
                        if(selectedDate != null){
                            val filteredList = filteredUser.filter { event ->
                                selectedDate?.let { selectedDate ->
                                    val eventCalendar = Calendar.getInstance().apply {
                                        time = event.time!!
                                    }
                                    eventCalendar.get(Calendar.YEAR) == selectedDate.year + 1900 &&
                                            eventCalendar.get(Calendar.MONTH) == selectedDate.month &&
                                            eventCalendar.get(Calendar.DAY_OF_MONTH) == selectedDate.date
                                } ?: true
                            }
                            eventsAdapter.submitList(filteredList)
                        }
                    updateCalendarEvents(filteredUser)

                }
                is UIState.Error -> {
                    toast(state.exception)
                }
                UIState.Empty -> {
                    // Handle empty state
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Notif chanel"
        val descriptionText = "Notif chanel description"
        val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
        val channel = android.app.NotificationChannel(channelID, name, importance)
        channel.description = descriptionText

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

    }

    private fun updateCalendarEvents(events: List<Events>) {

        for (decorator in currentDecorators) {
            binding.calendarView.removeDecorator(decorator)
        }
        currentDecorators.clear()

        val eventDates = events.map { event ->
            val calendar = Calendar.getInstance().apply {
                time = event.time!!
            }
            CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
        val eventDecorator = EventDecorator(eventDates)
        currentDecorators.add(eventDecorator)
        binding.calendarView.addDecorator(eventDecorator)
    }


    private fun showEditEventDialog(event: Events?) {
        currentEvent = event ?: Events()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_event, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(if (event == null) "Añadir Evento" else "Editar Evento")
            .setPositiveButton("Guardar") { _, _ ->
                val title = dialogView.findViewById<EditText>(R.id.eventTitle).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.eventDescription).text.toString()
                val location = dialogView.findViewById<EditText>(R.id.eventLocation).text.toString()
                val timestamp = currentEvent?.time ?: Date()
                var firstReminder: String? = null
                if(!dialogView.findViewById<Button>(R.id.selectRemindersButton).text.toString().equals("Añadir Recordatorios")){
                    firstReminder = dialogView.findViewById<Button>(R.id.selectRemindersButton).text.toString()
                }
                var secondReminder: String? = null
                if(!dialogView.findViewById<Button>(R.id.secondReminderButton).text.toString().equals("Añadir Segundo Recordatorio")){
                    secondReminder = dialogView.findViewById<Button>(R.id.secondReminderButton).text.toString()
                }

                if(title.isEmpty() || description.isEmpty() || location.isEmpty() || currentEvent!!.time == null){
                    toast("Por favor, rellena todos los campos")
                    return@setPositiveButton
                }

                val newEvent = currentUser?.let {
                    Events(
                        id = currentEvent?.id ?: "",
                        userid = it.id,
                        title = title,
                        description = description,
                        location = location,
                        first_reminder = firstReminder,
                        second_reminder = secondReminder,
                        time = timestamp
                    )
                }

                if (event == null) {
                    if (newEvent != null) {
                        calendarViewModel.addEvent(newEvent)
                    }
                } else {
                    if (newEvent != null) {
                        calendarViewModel.updateEvent(newEvent)
                        cancelNotification(event)
                    }
                }

                scheduleNotifications(newEvent)

                calendarViewModel.getEvents()
                dialogView.findViewById<Button>(R.id.selectDateButton).text = "Seleccionar Fecha"
                dialogView.findViewById<Button>(R.id.selectRemindersButton).text = "Añadir Recordatorios"
                dialogView.findViewById<Button>(R.id.secondReminderButton).text = "Añadir Segundo Recordatorio"
                dialogView.findViewById<Button>(R.id.secondReminderButton).visibility = View.GONE
            }
            .setNegativeButton("Cancelar", null)
            .create()

        event?.let {
            dialogView.findViewById<EditText>(R.id.eventTitle).setText(it.title)
            dialogView.findViewById<EditText>(R.id.eventDescription).setText(it.description)
            dialogView.findViewById<EditText>(R.id.eventLocation).setText(it.location)
            val formatDate = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm a", it.time)
            dialogView.findViewById<Button>(R.id.selectDateButton).text = formatDate

            // Mostrar los recordatorios actuales en el diálogo de edición
            if(it.first_reminder?.isNotBlank() == true){
                dialogView.findViewById<Button>(R.id.selectRemindersButton).text = it.first_reminder
            }
            if (it.second_reminder?.isNotBlank() == true) {
                dialogView.findViewById<Button>(R.id.secondReminderButton).text = it.second_reminder
                dialogView.findViewById<Button>(R.id.secondReminderButton).visibility = View.VISIBLE
            }
        }

        dialogView.findViewById<Button>(R.id.selectDateButton).setOnClickListener {
            showDateTimePicker { date ->
                currentEvent?.time = date
                val formatDate = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm a", date)
                dialogView.findViewById<Button>(R.id.selectDateButton).text = formatDate
            }
        }

        dialogView.findViewById<Button>(R.id.selectRemindersButton).setOnClickListener {
            showRemindersDialog {
                dialogView.findViewById<Button>(R.id.selectRemindersButton).text = it
                dialogView.findViewById<Button>(R.id.secondReminderButton).visibility = View.VISIBLE
            }
        }

        dialogView.findViewById<Button>(R.id.secondReminderButton).setOnClickListener {
            showRemindersDialog {
                dialogView.findViewById<Button>(R.id.secondReminderButton).text = it
            }
        }

        builder.show()
    }


    private fun scheduleNotifications(event: Events?) {
        event?.let {
            scheduleNotification(
                event.title,
                event.description,
                event.time,
                "ES HORA DE TU EVENTO!!!",
                event.id.hashCode()
            )

            val firstReminder = parseReminder(event.first_reminder)
            if (firstReminder != null) {
                val reminderTime = event.time?.time?.minus(firstReminder)
                if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                    scheduleNotification(
                        event.title,
                        event.description,
                        Date(reminderTime),
                        "RECORDATORIO: quedan ${event.first_reminder} antes del evento",
                        event.id.hashCode() + 1
                    )
                }
            }

            val secondReminder = parseReminder(event.second_reminder)
            if (secondReminder != null) {
                val reminderTime = event.time?.time?.minus(secondReminder)
                if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                    scheduleNotification(
                        event.title,
                        event.description,
                        Date(reminderTime),
                        "RECORDATORIO: quedan ${event.second_reminder} antes del evento",
                        event.id.hashCode() + 2
                    )
                }
            }
        }
    }


    private fun parseReminder(reminder: String?): Long? {
        if (reminder.isNullOrEmpty()) return null

        val parts = reminder.split(" ")
        val amount = parts[0].toIntOrNull() ?: return null
        val unit = parts[1]

        return when (unit) {
            "minutos" -> amount * 60 * 1000L
            "horas" -> amount * 60 * 60 * 1000L
            "días" -> amount * 24 * 60 * 60 * 1000L
            "semanas" -> amount * 7 * 24 * 60 * 60 * 1000L
            else -> null
        }
    }

    private fun cancelNotification(event: Events?) {
        event?.let {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            val mainIntent = Intent(requireContext(), NotificationReceiver::class.java)
            val mainPendingIntent = PendingIntent.getBroadcast(requireContext(), event.id.hashCode(), mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(mainPendingIntent)

            val firstReminderIntent = Intent(requireContext(), NotificationReceiver::class.java)
            val firstReminderPendingIntent = PendingIntent.getBroadcast(requireContext(), event.id.hashCode() + 1, firstReminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(firstReminderPendingIntent)

            val secondReminderIntent = Intent(requireContext(), NotificationReceiver::class.java)
            val secondReminderPendingIntent = PendingIntent.getBroadcast(requireContext(), event.id.hashCode() + 2, secondReminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(secondReminderPendingIntent)
        }
    }


    private fun scheduleNotification(
        title: String?,
        description: String?,
        time: Date?,
        message: String,
        requestCode: Int
    ) {
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time?.time ?: 0, pendingIntent)

    }


    private fun showDeleteConfirmationDialog(event: Events) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este evento?")
            .setPositiveButton("Eliminar") { _, _ ->
                cancelNotification(event)
                calendarViewModel.deleteEvent(event)
                calendarViewModel.getEvents()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDateTimePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                onDateSelected(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showRemindersDialog(reminder: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_reminder, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Selecciona un recordatorio")
            .setPositiveButton("Aceptar") { _, _ ->
                val numberEditText = dialogView.findViewById<EditText>(R.id.numberEditText)
                val timeMeasureSpinner = dialogView.findViewById<Spinner>(R.id.timeMeasureSpinner)

                val number = numberEditText.text.toString().toIntOrNull()
                val timeMeasure = timeMeasureSpinner.selectedItemPosition

                if (number != null && number > 0) {
                    val reminderText = when (timeMeasure) {
                        0 -> "$number minutos"
                        1 -> "$number horas"
                        2 -> "$number días"
                        3 -> "$number semanas"
                        else -> ""
                    }

                    // Actualizar el texto del botón y el recordatorio en el evento actual
                    val reminder = "$number;$timeMeasure"
                    reminder(reminderText)
                } else {
                    toast("Por favor, introduce un número válido")
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        builder.show()
    }
}