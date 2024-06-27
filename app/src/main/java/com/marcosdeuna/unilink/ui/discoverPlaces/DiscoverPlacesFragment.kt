package com.marcosdeuna.unilink.ui.discoverPlaces

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.preference.PreferenceManager
import android.provider.MediaStore
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentDiscoverPlacesBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.post.ListPostAdapter
import com.marcosdeuna.unilink.ui.post.PostViewModel
import com.marcosdeuna.unilink.ui.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.marcosdeuna.unilink.data.model.Markers
import com.marcosdeuna.unilink.data.model.Review
import com.marcosdeuna.unilink.ui.user.UserListAdapter
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.toast
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class DiscoverPlacesFragment : Fragment(), MapEventsReceiver {

    private lateinit var binding: FragmentDiscoverPlacesBinding
    private val selectedImagesUris = mutableListOf<Uri>()
    private lateinit var map: MapView
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val markersViewModel: MarkersViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by viewModels()
    val postViewModel: PostViewModel by viewModels()
    private var currentUser: User? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var alertDialog: AlertDialog
    private lateinit var imagePreviewContainer: LinearLayout
    private lateinit var addButton: ImageView
    private lateinit var deleteButton: ImageView
    private var reviewAdapter: ReviewAdapter? = null
    val adapter by lazy {
        ListPostAdapter(
            onItemClicked = { position, post ->
                // Acción al hacer clic en un elemento
                findNavController().navigate(
                    R.id.action_postFragment_to_detailPostFragment,
                    Bundle().apply {
                        putParcelable("post", post)
                    })
            },
            onEditClicked = { position, post ->
                // Acción al hacer clic en editar
                findNavController().navigate(
                    R.id.action_postFragment_to_createPostFragment,
                    Bundle().apply {
                        putString("operation", "edit")
                        putParcelable("post", post)
                    })
            },
            onDeleteClicked = { position, post ->
                // Acción al hacer clic en eliminar
                postViewModel.deletePost(post)
            },
            onSendClicked = { position, post ->
                // Acción al hacer clic en enviar
                findNavController().navigate(
                    R.id.action_postFragment_to_messageFragment,
                    Bundle().apply {
                        putString("receiverId", post.userId)
                        putParcelable("post", post)
                    })
            },
            authViewModel = authViewModel,
            coroutineScope = coroutineScope
        )
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiscoverPlacesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        map = view.findViewById(R.id.map)
        map.tileProvider.clearTileCache()
        Configuration.getInstance().setCacheMapTileCount(12.toShort())
        Configuration.getInstance().setCacheMapTileOvershoot(12.toShort())

        // Create a custom tile source
        map.setTileSource(object : OnlineTileSourceBase(
            "", 1, 20, 512, ".png",
            arrayOf("https://a.tile.openstreetmap.org/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                        MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
            }
        })

        map.setMultiTouchControls(true)
        val mapController: IMapController = map.controller
        getLastKnownLocationAndCenterMap(mapController)
        map.invalidate()
        setup()

        val mapEventsOverlay = MapEventsOverlay(this)
        map.overlays.add(mapEventsOverlay)

        observeMarkers()

        binding.searchBox.addTextChangedListener(afterTextChanged = {
            val search = it.toString()
            if(search.isNotEmpty()) {
                filterMarkers(search)
            } else {
                getLastKnownLocationAndCenterMap(mapController)
                observeMarkers()
            }
        })
    }

    private fun filterMarkers(search: String) {
        markersViewModel.markers.value?.let { state ->
            if (state is UIState.Success) {
                val filteredMarkers = state.data.filter { marker ->
                    marker.title.contains(search, ignoreCase = true)
                }

                // Actualizar el mapa con los marcadores filtrados
                updateMapWithFilteredMarkers(filteredMarkers)
            }
        }
    }

    private fun updateMapWithFilteredMarkers(filteredMarkers: List<Markers>) {
        // Limpiar marcadores actuales en el mapa
        map.overlays.clear()

        // Añadir los marcadores filtrados al mapa
        filteredMarkers.forEach { marker ->
            val geoPoint = GeoPoint(marker.latitude, marker.longitude)
            createMarker(geoPoint, marker.title, marker)
        }

        // Centrar el mapa en el primer marcador filtrado si existe
        if (filteredMarkers.isNotEmpty()) {
            val firstMarker = filteredMarkers.first()
            val geoPoint = GeoPoint(firstMarker.latitude, firstMarker.longitude)
            centerMap(geoPoint)
        }

    }

    private fun centerMap(geoPoint: GeoPoint) {
        val mapController = map.controller
        mapController.setCenter(geoPoint)
        mapController.setZoom(19.0) // Zoom level según tus preferencias
    }

    private fun observeMarkers() {
        markersViewModel.markers.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Success -> {

                    state.data.forEach { marker ->
                        val geoPoint = GeoPoint(marker.latitude, marker.longitude)
                        createMarker(geoPoint, marker.title, marker)
                    }
                }

                is UIState.Error -> {
                    toast("Error obteniendo marcadores: ${state.exception}")
                }

                else -> Unit
            }
        }
        markersViewModel.getMarkers()
    }


    private fun showAddMarkerDialog(
        latitude: Double?,
        longitude: Double?,
        markerData: Markers? = null
    ) {
        val dialogView = layoutInflater.inflate(R.layout.add_marker_modal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        alertDialog = dialogBuilder.show()

        val titleEditText = dialogView.findViewById<EditText>(R.id.title_edittext)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.description_edittext)
        val latEditText = dialogView.findViewById<EditText>(R.id.latitude_edittext)
        val lonEditText = dialogView.findViewById<EditText>(R.id.longitude_edittext)
        imagePreviewContainer = dialogView.findViewById<LinearLayout>(R.id.image_preview_container)
        val valoracion = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        deleteButton = dialogView.findViewById<ImageView>(R.id.button_delete_images)
        deleteButton.setOnClickListener {
            selectedImagesUris.clear()
            imagePreviewContainer.visibility = View.GONE
            addButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.addpicture
                )
            )
            deleteButton.visibility = View.GONE
        }

        addButton = dialogView.findViewById<ImageView>(R.id.button_add_images)
        addButton.setOnClickListener {
            openImagePicker()
        }

        markerData?.let {
            titleEditText.setText(it.title)
            descriptionEditText.setText(it.description)
            latEditText.setText(it.latitude.toString())
            lonEditText.setText(it.longitude.toString())
            valoracion.rating = it.valoration.toFloat()
            if (it.images.isNotEmpty()) {
                loadImages(it.images)
                imagePreviewContainer.visibility = View.VISIBLE
                addButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_edit
                    )
                )
                deleteButton.visibility = View.VISIBLE
            }

        }

        latitude?.let { latEditText.setText(it.toString()) }
        longitude?.let { lonEditText.setText(it.toString()) }

        dialogView.findViewById<View>(R.id.closeButton).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.save_button).setOnClickListener {
            val title = titleEditText.text.toString()
            val lat = latEditText.text.toString().toDoubleOrNull()
            val lon = lonEditText.text.toString().toDoubleOrNull()

            if (lat != null && lon != null) {
                if (titleEditText.text.isNotEmpty() && descriptionEditText.text.isNotEmpty()) {
                    if (selectedImagesUris.isNotEmpty()) {
                        markersViewModel.onUploadImage(selectedImagesUris) { uploadState ->
                            when (uploadState) {
                                is UIState.Loading -> {
                                    toast("Uploading image...")
                                }

                                is UIState.Success -> {
                                    // Update existing marker if markerData is not null
                                    if (markerData != null) {
                                        markerData.title = titleEditText.text.toString()
                                        markerData.description = descriptionEditText.text.toString()
                                        markerData.latitude = latEditText.text.toString().toDouble()
                                        markerData.longitude =
                                            lonEditText.text.toString().toDouble()
                                        markerData.images = uploadState.data
                                        markerData.valoration = valoracion.rating.toDouble()
                                        markersViewModel.updateMarker(markerData)
                                    } else {
                                        // Create new marker if markerData is null (fallback, though it's not expected here)
                                        val marker = Markers(
                                            title = title,
                                            description = descriptionEditText.text.toString(),
                                            latitude = lat,
                                            longitude = lon,
                                            images = uploadState.data,
                                            userId = currentUser?.id ?: "",
                                            valoration = valoracion.rating.toDouble()
                                        )
                                        markersViewModel.createMarker(marker)
                                        val geoPoint = GeoPoint(lat, lon)
                                        createMarker(geoPoint, title, marker)

                                    }
                                }

                                is UIState.Error -> {
                                    addButton.setImageDrawable(
                                        ContextCompat.getDrawable(
                                            requireContext(),
                                            R.drawable.ic_edit
                                        )
                                    )
                                    toast("Error uploading image.")
                                }

                                is UIState.Empty -> {
                                    // Handle empty state if needed
                                }
                            }
                        }
                    } else {
                        // Update existing marker if markerData is not null
                        if (markerData != null) {
                            markerData.title = title
                            markerData.description = descriptionEditText.text.toString()
                            markerData.latitude = lat
                            markerData.longitude = lon
                            markerData.valoration = valoracion.rating.toDouble()
                            markersViewModel.updateMarker(markerData)
                        } else {
                            // Create new marker if markerData is null (fallback, though it's not expected here)
                            val marker = Markers(
                                title = title,
                                description = descriptionEditText.text.toString(),
                                latitude = lat,
                                longitude = lon,
                                userId = currentUser?.id ?: "",
                                valoration = valoracion.rating.toDouble()
                            )
                            markersViewModel.createMarker(marker)
                            val geoPoint = GeoPoint(lat, lon)
                            createMarker(geoPoint, title, marker)
                        }
                    }
                    alertDialog.dismiss()
                } else {
                    toast("Please enter title and description.")
                }
            } else {
                toast("Please enter valid latitude and longitude.")
            }
        }

    }

    private fun removeMarkerFromMap(markerData: Markers) {
        for (overlay in map.overlays) {
            if (overlay is Marker && overlay.title == markerData.title) {
                map.overlays.remove(overlay)
                map.invalidate()
                break
            }
        }
    }

    private fun updateImagePreview(container: LinearLayout) {
        container.removeAllViews()

        for (uri in selectedImagesUris) {
            val imageView = ImageView(container.context)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginEnd = 20
            layoutParams.width = 400
            layoutParams.height = 400
            imageView.layoutParams = layoutParams
            imageView.setImageURI(uri)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            container.addView(imageView)
        }

        container.visibility = if (selectedImagesUris.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddReviewDialog(markerId: String, reviewToEdit: Review? = null) {
        val dialogView = layoutInflater.inflate(R.layout.add_review_modal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val alertDialog = dialogBuilder.show()
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val commentEditText = dialogView.findViewById<EditText>(R.id.review_description_edittext)
        val saveButton = dialogView.findViewById<Button>(R.id.save_review_button)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)

        // Initialize fields if editing existing review
        if (reviewToEdit != null) {
            ratingBar.rating = reviewToEdit.valoration.toFloat()
            commentEditText.setText(reviewToEdit.comment)
        }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        saveButton.setOnClickListener {
            val rating = ratingBar.rating.toDouble()
            val comment = commentEditText.text.toString().trim()

            if (comment.isNotEmpty()) {
                if (reviewToEdit != null) {
                    // Editing existing review
                    val updatedReview = reviewToEdit.copy(comment = comment, valoration = rating)
                    reviewViewModel.updateReview(updatedReview)
                    alertDialog.dismiss()
                } else {
                    // Creating new review
                    val newReview = Review(
                        markerId = markerId,
                        userId = currentUser?.id ?: "",
                        comment = comment,
                        valoration = rating,
                        timestamp = Date()
                    )
                    reviewViewModel.createReview(newReview)
                    alertDialog.dismiss()
                }
            } else {
                toast("Please enter a comment.")
            }
        }

        // Observer for creating reviews
        reviewViewModel.createReview.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    toast("Submitting review...")
                }

                is UIState.Success -> {
                    toast("Review submitted successfully.")
                    reviewViewModel.getReviews()
                }

                is UIState.Error -> {
                    toast("Error submitting review: ${state.exception}")
                }

                is UIState.Empty -> {}
            }
        }

        // Observer for updating reviews
        reviewViewModel.updateReview.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    toast("Updating review...")
                }

                is UIState.Success -> {
                    toast("Review updated successfully.")
                    reviewViewModel.getReviews()
                }

                is UIState.Error -> {
                    toast("Error updating review: ${state.exception}")
                }

                is UIState.Empty -> {}
            }
        }
    }


    private fun getLastKnownLocationAndCenterMap(mapController: IMapController) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLocation = GeoPoint(it.latitude, it.longitude)
                    mapController.setZoom(18.0)
                    mapController.animateTo(userLocation)
                    createMarker(userLocation, "Mi ubicación")
                }
            }
        }
    }

    private fun createMarker(geoPoint: GeoPoint, title: String, markerData: Markers? = null) {
        if (!this::map.isInitialized) {
            return
        }

        val bitmap = if (title.equals("Mi ubicación")) {
            BitmapFactory.decodeResource(resources, R.drawable.ubicacion)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.marcador)
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

        val drawable = BitmapDrawable(resources, scaledBitmap)

        val myMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            icon = drawable
            setPanToView(true)
            setOnMarkerClickListener { marker, mapView ->
                if (markerData != null) {
                    showMarkerDetailDialog(markerData)
                }
                true
            }
        }
        map.overlays.add(myMarker)
        map.invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showMarkerDetailDialog(markerData: Markers) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.detail_marker_modal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()



        authViewModel.getUserById(markerData.userId) { user ->
            authViewModel.getUserSession {
                if (user != null) {
                    if (user.id == it?.id) {
                        dialogView.findViewById<TextView>(R.id.userFullName).text = " Mi"
                        dialogView.findViewById<ImageView>(R.id.add_review).visibility = View.GONE

                        dialogView.findViewById<ImageView>(R.id.editButton).visibility =
                            View.VISIBLE
                        dialogView.findViewById<ImageView>(R.id.deleteButton).visibility =
                            View.VISIBLE

                        dialogView.findViewById<ImageView>(R.id.editButton).setOnClickListener {
                            showAddMarkerDialog(
                                markerData.latitude,
                                markerData.longitude,
                                markerData
                            )
                            alertDialog.dismiss()
                        }

                        // Delete button click listener
                        dialogView.findViewById<ImageView>(R.id.deleteButton).setOnClickListener {
                            reviewAdapter?.getReviews()?.forEach { review ->
                                if (review.markerId == markerData.id) {
                                    reviewViewModel.deleteReview(review)
                                }
                            }
                            removeMarkerFromMap(markerData)
                            markersViewModel.deleteMarker(markerData)
                            toast("Marker deleted successfully.")
                            alertDialog.dismiss()
                        }

                    } else {
                        dialogView.findViewById<TextView>(R.id.userFullName).text = user.userName
                        val addReview = dialogView.findViewById<ImageView>(R.id.add_review)
                        addReview.visibility = View.VISIBLE
                        addReview.setOnClickListener {
                            showAddReviewDialog(markerData.id)
                        }

                        dialogView.findViewById<ImageView>(R.id.editButton).visibility = View.GONE
                        dialogView.findViewById<ImageView>(R.id.deleteButton).visibility = View.GONE
                    }
                }
            }
        }
        dialogView.findViewById<TextView>(R.id.markerTitle).text = markerData.title
        dialogView.findViewById<TextView>(R.id.markerDescription).text = markerData.description

        reviewViewModel.getReviews()
        reviewViewModel.reviews.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Success -> {
                    if (state.data.isNotEmpty()) {
                        val reviews = state.data.filter { it.markerId == markerData.id }
                        if (reviews.isNotEmpty()){
                            dialogView.findViewById<TextView>(R.id.no_reviews).visibility = View.GONE
                        }else{
                            dialogView.findViewById<TextView>(R.id.no_reviews).visibility = View.VISIBLE
                        }
                        val recyclerView =
                            dialogView.findViewById<RecyclerView>(R.id.recycler_view_reviews)
                        recyclerView.visibility = View.VISIBLE
                        reviewAdapter = ReviewAdapter(requireContext(),
                            reviews as ArrayList<Review>,
                            authViewModel,
                            onEditReviewClicked = { review ->
                                showAddReviewDialog(markerData.id, review)
                            },
                            onDeleteReviewClicked = { review ->
                                reviewViewModel.deleteReview(review)
                            }
                        )

                        recyclerView.adapter = reviewAdapter

                        val valorations = reviews.map { it.valoration } + markerData.valoration
                        dialogView.findViewById<RatingBar>(R.id.ratingBar).rating =
                            valorations.average().toFloat()

                    } else {
                        dialogView.findViewById<RecyclerView>(R.id.recycler_view_reviews).visibility =
                            View.GONE
                        dialogView.findViewById<RatingBar>(R.id.ratingBar).rating =
                            markerData.valoration.toFloat()


                    }
                }

                is UIState.Error -> {
                    toast("Error obteniendo reviews: ${state.exception}")
                }

                else -> Unit
            }
        }

        reviewViewModel.deleteReview.observe(viewLifecycleOwner) {
            when (it) {
                is UIState.Loading -> {
                    toast("Deleting review...")
                }

                is UIState.Success -> {
                    toast("Review deleted successfully.")
                    reviewViewModel.getReviews()
                }

                is UIState.Error -> {
                    toast("Error deleting review: ${it.exception}")
                }

                is UIState.Empty -> {}
            }
        }


        val imageFlipper = dialogView.findViewById<ViewFlipper>(R.id.imageFlipper)
        imageFlipper.removeAllViews()
        markerData.images.forEach { imageUrl ->
            val imageView = createImageView(imageUrl)
            imageFlipper.addView(imageView)
        }

        imageFlipper.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    imageFlipper.stopFlipping()
                }

                android.view.MotionEvent.ACTION_UP -> {
                    if (motionEvent.x < view.width / 2) {
                        imageFlipper.showNext()
                    } else {
                        imageFlipper.showPrevious()
                    }
                }
            }
            true
        }

        imageFlipper.visibility = if (markerData.images.isEmpty()) View.GONE else View.VISIBLE
        dialogView.findViewById<TextView>(R.id.imagenes_label).visibility =
            if (markerData.images.isEmpty()) View.GONE else View.VISIBLE

        dialogView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            alertDialog.dismiss()
        }
    }


    private fun createImageView(imageUri: String): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        context?.let {
            Glide.with(it)
                .load(imageUri)
                .into(imageView)
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
    }


    fun setup() {
        authViewModel.getUserSession {
            currentUser = it
        }

        binding.profileModal.visibility = View.GONE
        binding.eliminarCuenta.setOnClickListener {

            authViewModel.getUserSession { user ->
                for (post in adapter.getPosts()) {
                    if (post.userId == user?.id) {
                        postViewModel.deletePost(post)
                    }
                }
                user?.let {
                    userViewModel.deleteUser(it)
                }
            }
            authViewModel.logout()
            authViewModel.deleteAccount()
            findNavController().navigate(R.id.action_discoverPlacesFragment_to_loginFragment)
        }


        binding.profilePicture.setOnClickListener {
            binding.profileModal.visibility = View.VISIBLE
        }

        binding.cerrarModal.setOnClickListener {
            binding.profileModal.visibility = View.GONE
        }

        // Configurar la acción del botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        authViewModel.logout()
                        findNavController().navigate(R.id.action_discoverPlacesFragment_to_loginFragment)
                    }
                }

        }

        binding.seeProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_discoverPlacesFragment_to_detailUserFragment)
        }

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Acción para Home
                    findNavController().navigate(R.id.action_discoverPlacesFragment_to_postFragment)
                    true
                }

                R.id.navigation_discover_people -> {
                    // Acción para descubrir personas
                    findNavController().navigate(R.id.action_discoverPlacesFragment_to_discoverPeopleFragment)
                    true
                }

                R.id.navigation_create_post -> {
                    findNavController().navigate(
                        R.id.action_discoverPlacesFragment_to_createPostFragment,
                        Bundle().apply {
                            putString("operation", "create")
                        })
                    true
                }

                R.id.navigation_discover_places -> {
                    // Acción para descubrir lugares
                    true
                }

                R.id.navigation_chats -> {
                    // Acción para chats
                    findNavController().navigate(R.id.action_discoverPlacesFragment_to_chatsFragment)
                    true
                }

                else -> false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.bottomNavigation.selectedItemId = R.id.navigation_discover_places
        binding.searchBox.setText("")
        authViewModel.getUserSession { user ->
            user?.let {
                binding.profileName.setText(it.userName)
                val profileImageUrl = it.profilePicture
                if (profileImageUrl.isNotEmpty()) {
                    coroutineScope.launch {
                        val bitmap = downloadImage(profileImageUrl)
                        bitmap?.let {
                            binding.profilePicture.setImageBitmap(it)
                            binding.profilePictureLarge.setImageBitmap(it)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private suspend fun downloadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageUrl = URL(url)
            val connection: HttpURLConnection = imageUrl.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            return@withContext BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }


    private fun status(status: String) {
        currentUser?.let { userViewModel.updateUserInfo(it.copy(status = status)) }
    }

    override fun onResume() {
        super.onResume()
        status("online")
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PERMISSION_REQUEST_CODE = 2
        private const val IMAGE_PICK_CODE = 1000
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        p?.let {
            showAddMarkerDialog(it.latitude, it.longitude)
        }
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")
    }

    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                pickImageFromGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                pickImageFromGallery()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery()
            } else {
                toast("Permission denied to access your gallery.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            selectedImagesUris.clear()
            if (data?.clipData != null) {
                val clipData = data.clipData
                for (i in 0 until clipData!!.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    selectedImagesUris.add(imageUri)
                }
            } else {
                data?.data?.let {
                    selectedImagesUris.add(it)
                }
            }
            updateImagePreview(imagePreviewContainer)
            imagePreviewContainer.visibility = View.VISIBLE
            addButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_edit
                )
            )
            deleteButton.visibility = View.VISIBLE
        } else {
            toast("No se seleccionó ninguna imagen")
        }

        if (selectedImagesUris.isEmpty()) {
            imagePreviewContainer.visibility = View.GONE
            addButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.addpicture
                )
            )
            deleteButton.visibility = View.GONE
        }
    }


    private fun loadImages(imageUrls: List<String>) {
        imagePreviewContainer.removeAllViews()

        for (imageUrl in imageUrls) {
            coroutineScope.launch {
                val bitmap = downloadImage(imageUrl)
                bitmap?.let {
                    createImageView2(it)
                }
            }
        }
    }

    private fun createImageView2(bitmap: Bitmap) {
        val imageView = ImageView(imagePreviewContainer.context)
        imageView.setImageBitmap(bitmap)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = 20
        layoutParams.width = 400
        layoutParams.height = 400
        imageView.layoutParams = layoutParams
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imagePreviewContainer.addView(imageView)
    }

}



