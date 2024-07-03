package com.marcosdeuna.unilink.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.ui.Messages.MessageViewModel
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.calendar.CalendarViewModel
import com.marcosdeuna.unilink.ui.discoverPlaces.MarkersViewModel
import com.marcosdeuna.unilink.ui.discoverPlaces.ReviewViewModel
import com.marcosdeuna.unilink.ui.notifications.TokenViewModel
import com.marcosdeuna.unilink.ui.post.PostViewModel
import com.marcosdeuna.unilink.ui.user.GroupViewModel
import com.marcosdeuna.unilink.ui.user.UserViewModel
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private var locationPermissionPref: SwitchPreferenceCompat? = null
    private var galleryPermissionPref: SwitchPreferenceCompat? = null
    private var notificationPermissionPref: SwitchPreferenceCompat? = null
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private val messageViewModel: MessageViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by viewModels()
    private val markersViewModel: MarkersViewModel by viewModels()
    private val tokenViewModel: TokenViewModel by viewModels()
    private val calendarViewModel: CalendarViewModel by viewModels()
    private var currentUser: User? = null
    private var markersToDelete: ArrayList<String> = ArrayList()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val backButtonPref: Preference? = findPreference("back_button")
        backButtonPref?.setOnPreferenceClickListener {
            findNavController().popBackStack()
            true
        }

        authViewModel.getUserSession {
            currentUser = it
        }

        // Gestión de Permisos
        locationPermissionPref = findPreference("location_permission")
        galleryPermissionPref= findPreference("gallery_permission")
        notificationPermissionPref= findPreference("notifications")

        locationPermissionPref?.setOnPreferenceClickListener {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                showRevokePermissionDialog()
            }else{
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            true
        }

        galleryPermissionPref?.setOnPreferenceClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED){
                    showRevokePermissionDialog()
                }else{
                    requestPermission(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    showRevokePermissionDialog()
                }else{
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            true
        }

        notificationPermissionPref?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    showRevokePermissionDialog()
                } else {
                    requestNotificationPermission()
                }
            }
            true
        }

        // Políticas y Términos
        val privacyPolicyPref: Preference? = findPreference("privacy_policy")
        privacyPolicyPref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/unilink-privacy-policy/9854af3b-0dc3-4113-b5be-f6ba4a371ae8/privacy"))
            startActivity(intent)
            true
        }

        val termsOfUsePref: Preference? = findPreference("terms_of_use")
        termsOfUsePref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/unilink-terms-of-use/93d1a9e2-9398-4d4c-bfa8-fedf96c21cee/terms"))
            startActivity(intent)
            true
        }

        // Gestión de Cuenta

        val updateEmailPref: Preference? = findPreference("update_email")
        updateEmailPref?.setOnPreferenceClickListener {
            showUpdateEmailDialog()
            true
        }

        val updatePasswordPref: Preference? = findPreference("update_password")
        updatePasswordPref?.setOnPreferenceClickListener {
            showUpdatePasswordDialog()
            true
        }
        val deleteAccountPref: Preference? = findPreference("delete_account")
        deleteAccountPref?.setOnPreferenceClickListener {
            showDeleteAccountConfirmationDialog()
            true
        }

        // Ayuda y Soporte
        val contactSupportPref: Preference? = findPreference("contact_support")
        contactSupportPref?.setOnPreferenceClickListener {
            showContactSupportDialog()
            true
        }

        checkPermission()
    }

    // En tu SettingsFragment
    private fun showUpdateEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_email, null)
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.current_password_edit_text)
        val newEmailEditText = dialogView.findViewById<EditText>(R.id.new_email_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actualizar Correo Electrónico")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, which ->
                val currentPassword = currentPasswordEditText.text.toString().trim()
                val newEmail = newEmailEditText.text.toString().trim()

                if (currentPassword.isNotEmpty() && newEmail.isNotEmpty()) {
                    authViewModel.updateEmail(currentPassword, newEmail) { result ->
                        when (result) {
                            is UIState.Success -> {
                                userViewModel.updateUserInfo(currentUser?.copy(email = newEmail) ?: currentUser!!)
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Éxito")
                                    .setMessage(result.data)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            is UIState.Error -> {
                                showErrorDialog(result.exception ?: "Error al actualizar el correo electrónico")
                            }
                            UIState.Loading -> {
                                // Puedes manejar el estado de carga si es necesario
                            }

                            UIState.Empty -> {}
                        }
                    }
                } else {
                    showErrorDialog("Por favor, completa todos los campos.")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showUpdatePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_password, null)
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.current_password_edit_text)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.new_password_edit_text)
        val repeatNewPasswordEditText = dialogView.findViewById<EditText>(R.id.repeat_new_password_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actualizar Contraseña")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, which ->
                val currentPassword = currentPasswordEditText.text.toString().trim()
                val newPassword = newPasswordEditText.text.toString().trim()
                val repeatNewPassword = repeatNewPasswordEditText.text.toString().trim()

                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && repeatNewPassword.isNotEmpty()) {
                    if (newPassword == repeatNewPassword) {
                        authViewModel.updatePassword(currentPassword, newPassword) { result ->
                            when (result) {
                                is UIState.Success -> {
                                    userViewModel.updateUserInfo(currentUser?.copy(password = newPassword) ?: currentUser!!)
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("Éxito")
                                        .setMessage(result.data)
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                                is UIState.Error -> {
                                    showErrorDialog(result.exception ?: "Error al actualizar la contraseña")
                                }
                                UIState.Loading -> {
                                    // Puedes manejar el estado de carga si es necesario
                                }

                                UIState.Empty -> {}
                            }
                        }
                    } else {
                        showErrorDialog("Las nuevas contraseñas no coinciden.")
                    }
                } else {
                    showErrorDialog("Por favor, completa todos los campos.")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun requestPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 0)
        } else {
            showPermissionGrantedDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                showPermissionGrantedDialog()
            }
        } else {
            // Las versiones anteriores no requieren permisos especiales para notificaciones
            showPermissionGrantedDialog()
        }
    }

    private fun showPermissionGrantedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permiso Concedido")
            .setMessage("Este permiso ya está concedido.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmación")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta?")
            .setPositiveButton("Sí") { dialog, which ->
                lifecycleScope.launch {
                    try {
                        deletePosts()
                    } catch (e: Exception) {
                        showErrorDialog("Error al eliminar las publicaciones")
                    }
                }

                deleteUser()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private suspend fun deletePosts() {
        postViewModel.getPosts()
        postViewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (posts is UIState.Success) {
                val postsToDelete = posts.data.filter { it.userId == currentUser?.id }
                val deletePostJobs = postsToDelete.map { post ->
                    lifecycleScope.launch { postViewModel.deletePost(post) }
                }
                lifecycleScope.launch {
                    deletePostJobs.forEach { it.join() }
                    deleteTokens()
                }
            } else if (posts is UIState.Error) {
                showErrorDialog("Error al obtener publicaciones")
            }
        }
    }

    private suspend fun deleteTokens() {
        tokenViewModel.getTokens()
        tokenViewModel.tokens.observe(viewLifecycleOwner) { tokens ->
            if (tokens is UIState.Success) {
                val tokensToDelete = tokens.data.filter { it.userId == currentUser?.id }
                val deleteTokenJobs = tokensToDelete.map { token ->
                    lifecycleScope.launch { tokenViewModel.deleteToken(token) }
                }
                lifecycleScope.launch {
                    deleteTokenJobs.forEach { it.join() }
                    deleteGroups()
                }
            } else if (tokens is UIState.Error){
                showErrorDialog("Error al obtener tokens")
            }
        }
    }

    private suspend fun deleteGroups() {
        groupViewModel.getGroups()
        groupViewModel.groups.observe(viewLifecycleOwner) { groups ->
            if (groups is UIState.Success) {
                val groupsToDelete = groups.data.filter { it.members.contains(currentUser?.id) }
                val deleteGroupJobs = groupsToDelete.map { group ->
                    lifecycleScope.launch { groupViewModel.deleteGroup(group) }
                }
                lifecycleScope.launch {
                    deleteGroupJobs.forEach { it.join() }
                    deleteMessages()
                }
            } else if (groups is UIState.Error) {
                showErrorDialog("Error al obtener grupos")
            }
        }
    }

    private suspend fun deleteMessages() {
        messageViewModel.getAllMessages()
        messageViewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (messages is UIState.Success) {
                val messagesToDelete = messages.data.filter { it.senderId == currentUser?.id || it.receiverId == currentUser?.id }
                val deleteMessageJobs = messagesToDelete.map { message ->
                    lifecycleScope.launch { messageViewModel.deleteMessage(message) }
                }
                lifecycleScope.launch {
                    deleteMessageJobs.forEach { it.join() }
                    deleteMarkers()
                }
            } else if (messages is UIState.Error) {
                showErrorDialog("Error al obtener mensajes")
            }
        }
    }

    private suspend fun deleteMarkers() {
        markersViewModel.getMarkers()
        markersViewModel.markers.observe(viewLifecycleOwner) { markers ->
            if (markers is UIState.Success) {
                val markersToDeleteList = markers.data.filter { it.userId == currentUser?.id }
                markersToDelete.clear()
                markersToDelete.addAll(markersToDeleteList.map { it.id })
                val deleteMarkerJobs = markersToDeleteList.map { marker ->
                    lifecycleScope.launch { markersViewModel.deleteMarker(marker) }
                }
                lifecycleScope.launch {
                    deleteMarkerJobs.forEach { it.join() }
                    deleteReviews()
                }
            } else if (markers is UIState.Error) {
                showErrorDialog("Error al obtener marcadores")
            }
        }
    }

    private suspend fun deleteReviews() {
        reviewViewModel.getReviews()
        reviewViewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            if (reviews is UIState.Success) {
                val reviewsToDelete = reviews.data.filter { it.userId == currentUser?.id || markersToDelete.contains(it.markerId) }
                val deleteReviewJobs = reviewsToDelete.map { review ->
                    lifecycleScope.launch { reviewViewModel.deleteReview(review) }
                }
                lifecycleScope.launch {
                    deleteReviewJobs.forEach { it.join() }
                    deleteEvents()
                }
            } else if (reviews is UIState.Error) {
                showErrorDialog("Error al obtener reseñas")
            }
        }
    }

    private suspend fun deleteEvents() {
        calendarViewModel.getEvents()
        calendarViewModel.events.observe(viewLifecycleOwner) { events ->
            if (events is UIState.Success) {
                val eventsToDelete = events.data.filter { it.userid == currentUser?.id }
                val deleteEventJobs = eventsToDelete.map { event ->
                    lifecycleScope.launch { calendarViewModel.deleteEvent(event) }
                }
                lifecycleScope.launch {
                    deleteEventJobs.forEach { it.join() }
                    deleteUser()
                }
            } else if (events is UIState.Error) {
                showErrorDialog("Error al obtener eventos")
            }
        }
    }

    private fun deleteUser(){
        userViewModel.getUsers()
        userViewModel.users.observe(viewLifecycleOwner) { users ->
            if (users is UIState.Success) {
                val userToDelete = users.data.find { it.id == currentUser?.id }
                userToDelete?.let {
                    lifecycleScope.launch { userViewModel.deleteUser(it) }
                }
                lifecycleScope.launch {
                    authViewModel.deleteAccount()

                }
                lifecycleScope.launch {
                    FirebaseMessaging.getInstance().deleteToken()
                        .addOnSuccessListener {
                            authViewModel.logout()
                            if (isAdded && !isDetached) {
                                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                            }
                        }
                        .addOnFailureListener {
                            showErrorDialog("Error al eliminar el token de Firebase")
                        }
                }
            } else if (users is UIState.Error) {
                showErrorDialog("Error al obtener usuarios")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK"){
                dialog, which -> dialog.dismiss()
            }
            .show()
    }

    private fun showContactSupportDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contact_support, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.email_edit_text)
        val messageEditText = dialogView.findViewById<EditText>(R.id.message_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Contactar Soporte")
            .setMessage("Por favor, describe tu consulta:")
            .setView(dialogView)
            .setPositiveButton("Enviar") { dialog, which ->
                val userEmail = emailEditText.text.toString().trim()
                val userMessage = messageEditText.text.toString().trim()

                if (userEmail.isNotEmpty() && userMessage.isNotEmpty()) {
                    sendEmail(userEmail, userMessage)
                } else {
                    // Mostrar mensaje de error si los campos están vacíos
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error")
                        .setMessage("Por favor, completa todos los campos.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendEmail(userEmail: String, userMessage: String) {
        val recipient = "unilink0002@gmail.com"
        val subject = "Soporte de la aplicación"
        val message = "Correo del usuario: $userEmail\n\nMensaje:\n$userMessage"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            // Mostrar mensaje de error si no hay aplicaciones de correo electrónico disponibles
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("No hay aplicaciones de correo electrónico instaladas.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showRevokePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Revocar Permiso")
            .setMessage("Para revocar este permiso, necesitas ir a la configuración de la aplicación.")
            .setPositiveButton("Ir a Configuración") { dialog, which ->
                // Abrir configuración de la aplicación
                openAppSettings()
            }
            .setNegativeButton("Cancelar", null)
            .show()

        checkPermission()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkPermission() {
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationPermissionPref?.isChecked = true
        }else{
            locationPermissionPref?.isChecked = false
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED){
                galleryPermissionPref?.isChecked = true
            }else{
                galleryPermissionPref?.isChecked = false
            }
        }else{
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                galleryPermissionPref?.isChecked = true
            }else{
                galleryPermissionPref?.isChecked = false

            }
        }

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU){
            notificationPermissionPref?.isVisible = false
            notificationPermissionPref?.isChecked = true
        }else{
            notificationPermissionPref?.isVisible = true
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
                notificationPermissionPref?.isChecked = true
            }else{
                notificationPermissionPref?.isChecked = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        status("online")
        checkPermission()
    }

    private fun status(status: String) {
        currentUser?.let { userViewModel.updateUserInfo(it.copy(status = status)) }
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }
}
