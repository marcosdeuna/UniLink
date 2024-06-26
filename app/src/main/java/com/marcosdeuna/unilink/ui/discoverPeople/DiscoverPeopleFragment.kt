package com.marcosdeuna.unilink.ui.discoverPeople

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.marcosdeuna.unilink.R
import com.marcosdeuna.unilink.data.model.Group
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.databinding.FragmentDiscoverPeopleBinding
import com.marcosdeuna.unilink.ui.auth.AuthViewModel
import com.marcosdeuna.unilink.ui.auth.RegisterFragment
import com.marcosdeuna.unilink.ui.post.ListPostAdapter
import com.marcosdeuna.unilink.ui.post.PostViewModel
import com.marcosdeuna.unilink.ui.user.GroupViewModel
import com.marcosdeuna.unilink.ui.user.UserListAdapter
import com.marcosdeuna.unilink.ui.user.UserViewModel
import com.marcosdeuna.unilink.util.UIState
import com.marcosdeuna.unilink.util.toast
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale


@AndroidEntryPoint
class DiscoverPeopleFragment : Fragment() {

    private lateinit var binding: FragmentDiscoverPeopleBinding
    private val userViewModel: UserViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private lateinit var manager: CardStackLayoutManager
    private lateinit var manager2: CardStackLayoutManager
    private lateinit var manager3: CardStackLayoutManager
    private var list: ArrayList<User> = arrayListOf()
    private var selectedUsers: ArrayList<String> = arrayListOf()
    private var originalList: ArrayList<User> = arrayListOf()
    private var groupList: ArrayList<Group> = arrayListOf()
    private var adminGroupList: ArrayList<Group> = arrayListOf()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val selectedImagesUris = mutableListOf<Uri>()
    private var imagesloaded = false
    private var flag = false
    val adapter by lazy {
        ListPostAdapter(
            onItemClicked = { position, post ->
                // Acción al hacer clic en un elemento
                findNavController().navigate(R.id.action_postFragment_to_detailPostFragment, Bundle().apply {
                    putParcelable("post", post)
                })
            },
            onEditClicked = { position, post ->
                // Acción al hacer clic en editar
                findNavController().navigate(R.id.action_postFragment_to_createPostFragment, Bundle().apply {
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
                findNavController().navigate(R.id.action_postFragment_to_messageFragment, Bundle().apply {
                    putString("receiverId", post.userId)
                    putParcelable("post", post)
                })
            },
            authViewModel = authViewModel,
            coroutineScope = coroutineScope
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiscoverPeopleBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.profileModal.visibility = View.GONE
        binding.refresh.setOnClickListener {
            userViewModel.getUsers()
            if(adminGroupList.isNotEmpty()){
                binding.myGroups.visibility = View.VISIBLE
            }else{
                binding.myGroups.visibility = View.GONE
            }
            binding.cardStackViewGroup.visibility = View.GONE
            binding.cardStackViewAdminGroup.visibility = View.GONE
            binding.cardStackView.visibility = View.VISIBLE
            binding.refresh.visibility = View.GONE
            binding.noUsers.visibility = View.GONE
            binding.searchBox.setText("")
            binding.ageFrom.setText("")
            binding.ageTo.setText("")
            binding.careerFilter.setText("")
            binding.keywordFilter.setText("")
            binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
            binding.cardStackViewGroup.visibility = View.GONE
            binding.cardStackView.visibility = View.VISIBLE
            binding.cardStackViewAdminGroup.visibility = View.GONE
            binding.cardStackViewAdminGroup.scrollToPosition(0)
            binding.cardStackView.scrollToPosition(0)
            binding.cardStackViewGroup.scrollToPosition(0)
            userViewModel.getUsers()
            groupViewModel.getGroups()
            flag = false
        }
        authViewModel.getUserSession { user ->
            if (user != null) {
                if(user.description.equals("") || user.age == 0 || user.socialPictures.isEmpty() || user.genre.equals("")){
                    findNavController().navigate(R.id.action_discoverPeopleFragment_to_editUserFragment)
                    toast("Por favor completa tu perfil con tus datos")
                }
            }
        }
        userViewModel.getUsers()
        userViewModel.users.observe(viewLifecycleOwner) { state ->
            when(state){
                is UIState.Loading -> {
                }
                is UIState.Success -> {
                    list = arrayListOf()

                    for (user in state.data){
                        authViewModel.getUserSession { currentUser ->
                            if (currentUser?.id != user.id) {
                                list.add(user)
                            }
                        }
                    }
                    list.shuffle()
                    originalList = list
                    manager = CardStackLayoutManager(requireContext(), object : CardStackListener{
                        override fun onCardDragging(direction: Direction?, ratio: Float) {
                        }

                        override fun onCardSwiped(direction: Direction?) {
                            if (manager.topPosition == list.size){
                                toast("No hay más usuarios")

                                binding.noUsers.visibility = View.VISIBLE
                                binding.refresh.visibility = View.VISIBLE
                            }
                        }

                        override fun onCardRewound() {
                        }

                        override fun onCardCanceled() {
                        }

                        override fun onCardAppeared(view: View?, position: Int) {
                        }

                        override fun onCardDisappeared(view: View?, position: Int) {
                        }

                    })

                    manager.setVisibleCount(3)
                    manager.setTranslationInterval(0.6f)
                    manager.setScaleInterval(0.8f)
                    manager.setMaxDegree(20.0f)
                    manager.setDirections(Direction.HORIZONTAL)
                    binding.cardStackView.layoutManager = manager
                    binding.cardStackView.itemAnimator = DefaultItemAnimator()
                    binding.cardStackView.adapter = DiscoverPeopleAdapter(requireContext(), list, onSendClicked = { position, user ->
                        findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                            putString("receiverId", user.id)
                        })
                    })
                    if(list.isEmpty()){
                        binding.noUsers.visibility = View.VISIBLE
                        binding.refresh.visibility = View.VISIBLE
                    }else{
                        binding.noUsers.visibility = View.GONE
                        binding.refresh.visibility = View.GONE
                    }
                    binding.recyclerViewUsers.adapter = UserListAdapter(
                        requireContext(),
                        list,
                        mapOf(),
                        selectedUsers,
                        true,
                        onItemClicked = { _, _ ->
                        },
                        authViewModel
                    )
                }
                is UIState.Error -> {
                    toast(state.exception)
                }

                UIState.Empty -> TODO()
            }
        }
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Acción para Home
                    findNavController().navigate(R.id.action_discoverPeopleFragment_to_postFragment)
                    true
                }
                R.id.navigation_discover_people -> {
                    // Acción para descubrir personas
                    true
                }
                R.id.navigation_create_post -> {
                    findNavController().navigate(R.id.action_discoverPeopleFragment_to_createPostFragment, Bundle().apply {
                        putString("operation", "create")
                    })
                    true
                }
                R.id.navigation_discover_places -> {
                    // Acción para descubrir lugares
                    if(hasLocationPermission()){
                        findNavController().navigate(R.id.action_discoverPeopleFragment_to_discoverPlacesFragment)
                    }else{
                        requestLocationPermission()
                        toast("Para acceder necesitas permitir el acceso a tu ubicación.")
                    }
                    false
                }
                R.id.navigation_chats -> {
                    // Acción para chats
                    findNavController().navigate(R.id.action_discoverPeopleFragment_to_chatsFragment)
                    true
                }
                else -> false
            }
        }

        binding.profilePicture.setOnClickListener {
            binding.profileModal.visibility = View.VISIBLE
        }

        binding.cerrarModal.setOnClickListener {
            binding.profileModal.visibility = View.GONE
        }

        // Configurar la acción del botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_discoverPeopleFragment_to_loginFragment)
        }

        binding.seeProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_discoverPeopleFragment_to_detailUserFragment)
        }

        binding.eliminarCuenta.setOnClickListener {

            authViewModel.getUserSession { user ->
                for (post in adapter.getPosts()) {
                    if(post.userId == user?.id){
                        postViewModel.deletePost(post)
                    }
                }
                user?.let {
                    userViewModel.deleteUser(it)
                }
            }
            authViewModel.logout()
            authViewModel.deleteAccount()
            findNavController().navigate(R.id.action_discoverPeopleFragment_to_loginFragment)
        }

        binding.searchBox.addTextChangedListener(afterTextChanged = {
            binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
            flag = false
            binding.cardStackView.scrollToPosition(0)
            binding.cardStackView.visibility = View.VISIBLE
            binding.cardStackViewGroup.visibility = View.GONE
            binding.cardStackViewAdminGroup.visibility = View.GONE
            binding.cardStackViewGroup.scrollToPosition(0)
            binding.cardStackViewAdminGroup.scrollToPosition(0)
            val search = it.toString()
            if (search.isNotEmpty()) {
                val filteredList = originalList.filter {
                    val completename = "${it.firstName} ${it.lastName}"
                    completename.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT), ignoreCase = true)
                }
                binding.cardStackView.adapter = DiscoverPeopleAdapter(requireContext(),
                    filteredList as ArrayList<User>,
                    onSendClicked = { position, user ->
                        findNavController().navigate(
                            R.id.action_discoverPeopleFragment_to_messageFragment,
                            Bundle().apply {
                                putString("receiverId", user.id)
                            }
                        )
                    }
                )
                if(filteredList.isEmpty()){
                    binding.refresh.visibility = View.VISIBLE
                    binding.noUsers.visibility = View.VISIBLE
                }else{
                    binding.refresh.visibility = View.GONE
                    binding.noUsers.visibility = View.GONE
                    list = filteredList as ArrayList<User>
                }
            } else {
                binding.cardStackView.adapter = DiscoverPeopleAdapter(requireContext(), list, onSendClicked = { position, user ->
                    findNavController().navigate(
                        R.id.action_discoverPeopleFragment_to_messageFragment,
                        Bundle().apply {
                            putString("receiverId", user.id)
                        }
                    )
                })
            }
        })

        binding.searchBoxGroup.addTextChangedListener(afterTextChanged = {
            val search = it.toString()
            if (search.isNotEmpty()) {
                val filteredList = originalList.filter {
                    val completename = "${it.firstName} ${it.lastName}"
                    completename.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT), ignoreCase = true)
                }
                binding.recyclerViewUsers.adapter = UserListAdapter(
                    requireContext(),
                    filteredList as ArrayList<User>,
                    mapOf(),
                    selectedUsers,
                    true,
                    onItemClicked = { _, _ ->
                    },
                    authViewModel
                )
                if(filteredList.isNotEmpty()){
                    list = filteredList as ArrayList<User>
                }
            } else {
                binding.recyclerViewUsers.adapter = UserListAdapter(
                    requireContext(),
                    originalList,
                    mapOf(),
                    selectedUsers,
                    true,
                    onItemClicked = { _, _ ->
                    },
                    authViewModel
                )
            }
        })

        binding.btnFilterPeople.setOnClickListener{
            binding.filterModal.visibility = View.VISIBLE
        }

        binding.closeFiltersButton.setOnClickListener{
            binding.filterModal.visibility = View.GONE
        }

        binding.other.setOnClickListener{
            if(binding.other.isChecked){
                binding.ageFrom.isEnabled = false
                binding.ageTo.isEnabled = false
                binding.careerFilter.isEnabled = false
                binding.keywordFilter.isEnabled = false
            }
        }

        binding.male.setOnClickListener{
            if(binding.male.isChecked){
                binding.ageFrom.isEnabled = true
                binding.ageTo.isEnabled = true
                binding.careerFilter.isEnabled = true
                binding.keywordFilter.isEnabled = true
            }
        }

        binding.female.setOnClickListener{
            if(binding.female.isChecked){
                binding.ageFrom.isEnabled = true
                binding.ageTo.isEnabled = true
                binding.careerFilter.isEnabled = true
                binding.keywordFilter.isEnabled = true
            }
        }


        binding.applyFiltersButton.setOnClickListener{
            binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
            flag = false
            binding.noUsers.visibility = View.GONE
            binding.refresh.visibility = View.GONE
            binding.cardStackView.scrollToPosition(0)
            binding.cardStackViewGroup.scrollToPosition(0)
            binding.cardStackViewAdminGroup.scrollToPosition(0)
            binding.cardStackViewAdminGroup.visibility = View.GONE
            binding.cardStackViewGroup.visibility = View.GONE
            binding.cardStackView.visibility = View.VISIBLE
            var ageTo = ""
            var ageFrom = ""
            if(binding.ageTo.isEnabled){
                 ageTo = binding.ageTo.text.toString()
            }

            if(binding.ageFrom.isEnabled){
                ageFrom = binding.ageFrom.text.toString()
            }

            var genre =""
            if(binding.male.isChecked){
                genre = "Hombre"
            }else{
                if(binding.female.isChecked){
                    genre = "Mujer"
                }else{
                    if(binding.other.isChecked){
                        binding.cardStackViewGroup.visibility = View.VISIBLE
                        binding.cardStackView.visibility = View.GONE
                        binding.cardStackViewAdminGroup.visibility = View.GONE

                    }
                }
            }
            var career = ""
            if(binding.careerFilter.isEnabled){
                career = binding.careerFilter.text.toString()
            }

            var keywords = listOf<String>()

            if(binding.keywordFilter.isEnabled){
                keywords = binding.keywordFilter.text.toString().split(" ")
            }

            val filteredList = originalList.filter { user ->
                val matchesAge = (ageFrom.isEmpty() || user.age >= ageFrom.toString().toInt()) && (ageTo.isEmpty() || user.age <= ageTo.toString().toInt())
                val matchesGenre = genre.isEmpty() || user.genre.equals(genre, ignoreCase = true)
                val matchesCareer = career.isEmpty() || user.career.contains(career, ignoreCase = true)
                val matchesKeywords = keywords.isEmpty() || keywords.any { keyword ->
                    user.description.contains(keyword, ignoreCase = true)
                }

                matchesAge && matchesGenre && matchesCareer && matchesKeywords
            }
            binding.cardStackView.adapter = DiscoverPeopleAdapter(requireContext(), filteredList as ArrayList<User>, onSendClicked = { position, user ->
                findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                    putString("receiverId", user.id)
                })
            })
            if(filteredList.isEmpty()){
                binding.refresh.visibility = View.VISIBLE
                binding.noUsers.visibility = View.VISIBLE
            }else{
                list = filteredList as ArrayList<User>
            }
            binding.filterModal.visibility = View.GONE
        }

        binding.myGroups.setOnClickListener {
            if(!flag){
                binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_close, null))
                binding.cardStackViewGroup.visibility = View.GONE
                binding.cardStackView.visibility = View.GONE
                binding.cardStackViewAdminGroup.visibility = View.VISIBLE
                binding.cardStackViewAdminGroup.scrollToPosition(0)
                binding.cardStackView.scrollToPosition(0)
                binding.cardStackViewGroup.scrollToPosition(0)
                flag = true
            }else{
                binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
                binding.cardStackViewGroup.visibility = View.GONE
                binding.cardStackView.visibility = View.VISIBLE
                binding.cardStackViewAdminGroup.visibility = View.GONE
                binding.cardStackViewAdminGroup.scrollToPosition(0)
                binding.cardStackView.scrollToPosition(0)
                binding.cardStackViewGroup.scrollToPosition(0)
                userViewModel.getUsers()
                groupViewModel.getGroups()
                flag = false
            }


        }

        binding.btnCreateGroup.setOnClickListener {
            binding.groupModal.visibility = View.VISIBLE
            binding.createGroupButton.visibility = View.VISIBLE
            binding.updateGroupButton.visibility = View.GONE
        }

        binding.closeGroupButton.setOnClickListener {
            binding.groupModal.visibility = View.GONE
            binding.createGroupButton.visibility = View.VISIBLE
            binding.updateGroupButton.visibility = View.GONE
        }

        binding.buttonAddImages.setOnClickListener{
            openImagePicker()
        }

        binding.buttonDeleteImages.setOnClickListener{
            selectedImagesUris.clear()
            binding.imagePreviewContainer.visibility = View.GONE
            binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.addpicture, null))
            binding.buttonDeleteImages.visibility = View.GONE
        }

        binding.createGroupButton.setOnClickListener {
            if (validation()) {
                val groupName = binding.groupName.text.toString()
                val groupDescription = binding.groupDescription.text.toString()
                var groupUsers = selectedUsers
                var userid = ""
                authViewModel.getUserSession { user ->
                    userid = user?.id.toString()
                    groupUsers.add(userid)
                }
                if (selectedImagesUris.isNotEmpty()) {
                    groupViewModel.onUploadImage(selectedImagesUris) { result ->
                        when (result) {
                            is UIState.Success -> {
                                val group = Group(
                                    name = groupName,
                                    description = groupDescription,
                                    members = groupUsers,
                                    admin = userid,
                                    images = result.data
                                )
                                groupViewModel.createGroup(group)
                            }

                            is UIState.Error -> {
                                toast(result.exception)
                            }

                            UIState.Empty -> {
                            }
                            UIState.Loading -> {

                            }
                        }
                    }
                }
            }

            groupViewModel.createGroup.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UIState.Loading -> {
                    }

                    is UIState.Success -> {
                        binding.myGroups.visibility = View.VISIBLE
                        binding.groupModal.visibility = View.GONE
                        binding.groupName.setText("")
                        binding.groupDescription.setText("")
                        binding.recyclerViewUsers.adapter = UserListAdapter(
                            requireContext(),
                            list,
                            mapOf(),
                            selectedUsers,
                            true,
                            onItemClicked = { _, _ ->
                            },
                            authViewModel
                        )
                        selectedImagesUris.clear()
                        binding.imagePreviewContainer.visibility = View.GONE
                        binding.buttonAddImages.setImageDrawable(
                            resources.getDrawable(
                                R.drawable.addpicture,
                                null
                            )
                        )
                        binding.buttonDeleteImages.visibility = View.GONE
                        toast("Grupo creado exitosamente")
                        binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
                        binding.cardStackViewGroup.visibility = View.GONE
                        binding.cardStackView.visibility = View.VISIBLE
                        binding.cardStackViewAdminGroup.visibility = View.GONE
                        binding.cardStackViewAdminGroup.scrollToPosition(0)
                        binding.cardStackView.scrollToPosition(0)
                        binding.cardStackViewGroup.scrollToPosition(0)
                        userViewModel.getUsers()
                        groupViewModel.getGroups()
                        flag = false
                    }

                    is UIState.Error -> {
                        toast(state.exception)
                    }

                    UIState.Empty -> {

                    }
                }
            }
        }

        groupViewModel.getGroups()
        groupViewModel.groups.observe(viewLifecycleOwner) { state ->
            when(state){
                is UIState.Loading -> {
                }
                is UIState.Success -> {
                    groupList = arrayListOf()
                    adminGroupList = arrayListOf()
                    authViewModel.getUserSession { user ->
                        for (group in state.data){
                            if(!group.members.contains(user?.id.toString())){
                                groupList.add(group)
                            }
                            if(group.admin == user?.id){
                                adminGroupList.add(group)
                            }
                        }
                        if(adminGroupList.isNotEmpty()){
                            binding.myGroups.visibility = View.VISIBLE
                        }else{
                            binding.myGroups.visibility = View.GONE
                        }
                    }
                    groupList.shuffle()
                    manager2 = CardStackLayoutManager(requireContext(), object : CardStackListener{
                        override fun onCardDragging(direction: Direction?, ratio: Float) {
                        }

                        override fun onCardSwiped(direction: Direction?) {
                            if (manager2.topPosition == groupList.size){
                                toast("No hay más grupos")
                                binding.noUsers.visibility = View.VISIBLE
                                binding.refresh.visibility = View.VISIBLE
                            }
                        }

                        override fun onCardRewound() {
                        }

                        override fun onCardCanceled() {
                        }

                        override fun onCardAppeared(view: View?, position: Int) {
                        }

                        override fun onCardDisappeared(view: View?, position: Int) {
                        }

                    })

                    manager2.setVisibleCount(3)
                    manager2.setTranslationInterval(0.6f)
                    manager2.setScaleInterval(0.8f)
                    manager2.setMaxDegree(20.0f)
                    manager2.setDirections(Direction.HORIZONTAL)
                    binding.cardStackViewGroup.layoutManager = manager2
                    binding.cardStackViewGroup.itemAnimator = DefaultItemAnimator()
                    binding.cardStackViewGroup.adapter = DiscoverGroupAdapter(requireContext(), groupList, authViewModel, onSendClicked = { position, user ->
                        findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                            putString("receiverId", user.id)
                        })
                    },
                        onItemClicked = { position, user ->
                            findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                                putString("receiverId", user.id)
                            })},
                        onEditClicked = {_, _ ->},
                        onDeleteClicked = {_, _ ->}
                        )

                    if (groupList.isEmpty() && binding.other.isChecked){
                        binding.noUsers.visibility = View.VISIBLE
                        binding.refresh.visibility = View.VISIBLE
                    }else{
                        binding.noUsers.visibility = View.GONE
                        binding.refresh.visibility = View.GONE
                    }

                    adminGroupList.shuffle()
                    manager3 = CardStackLayoutManager(requireContext(), object : CardStackListener{
                        override fun onCardDragging(direction: Direction?, ratio: Float) {
                        }

                        override fun onCardSwiped(direction: Direction?) {
                            if (manager3.topPosition == adminGroupList.size){
                                toast("No hay más grupos")
                                binding.noUsers.visibility = View.VISIBLE
                                binding.refresh.visibility = View.VISIBLE
                            }
                        }

                        override fun onCardRewound() {
                        }

                        override fun onCardCanceled() {
                        }

                        override fun onCardAppeared(view: View?, position: Int) {
                        }

                        override fun onCardDisappeared(view: View?, position: Int) {
                        }

                    })

                    manager3.setVisibleCount(3)
                    manager3.setTranslationInterval(0.6f)
                    manager3.setScaleInterval(0.8f)
                    manager3.setMaxDegree(20.0f)
                    manager3.setDirections(Direction.HORIZONTAL)
                    binding.cardStackViewAdminGroup.layoutManager = manager3
                    binding.cardStackViewAdminGroup.itemAnimator = DefaultItemAnimator()
                    binding.cardStackViewAdminGroup.adapter = DiscoverGroupAdapter(requireContext(), adminGroupList, authViewModel, onSendClicked = { position, user ->
                        findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                            putString("receiverId", user.id)
                        })
                    },
                        onItemClicked = { position, user ->
                            findNavController().navigate(R.id.action_discoverPeopleFragment_to_messageFragment, Bundle().apply {
                                putString("receiverId", user.id)
                            })},
                        onEditClicked = { position, group ->
                            binding.groupModal.visibility = View.VISIBLE
                            binding.createGroupButton.visibility = View.GONE
                            binding.updateGroupButton.visibility = View.VISIBLE

                            binding.groupName.setText(group.name)
                            binding.groupDescription.setText(group.description)
                            selectedUsers = group.members as ArrayList<String>
                            binding.recyclerViewUsers.adapter = UserListAdapter(
                                requireContext(),
                                list,
                                mapOf(),
                                selectedUsers,
                                true,
                                onItemClicked = { _, _ ->
                                },
                                authViewModel
                            )

                            if(group.images.isNotEmpty()){
                                loadImages(group.images)
                                imagesloaded = true
                                binding.imagePreviewContainer.visibility = View.VISIBLE
                                binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.ic_edit, null))
                                binding.buttonDeleteImages.visibility = View.VISIBLE
                            }

                            binding.updateGroupButton.setOnClickListener {
                                if (validation()) {
                                    val groupName = binding.groupName.text.toString()
                                    val groupDescription = binding.groupDescription.text.toString()
                                    var groupUsers = selectedUsers
                                    if (selectedImagesUris.isNotEmpty()) {
                                        groupViewModel.onUploadImage(selectedImagesUris) { result ->
                                            when (result) {
                                                is UIState.Success -> {
                                                    groupViewModel.updateGroup(group.copy(
                                                        name = groupName,
                                                        description = groupDescription,
                                                        members = groupUsers,
                                                        images = result.data
                                                    ))
                                                }

                                                is UIState.Error -> {
                                                    toast(result.exception)
                                                }

                                                UIState.Empty -> {
                                                }
                                                UIState.Loading -> {

                                                }
                                            }
                                        }
                                    }else{
                                        groupViewModel.updateGroup(group.copy(
                                            name = groupName,
                                            description = groupDescription,
                                            members = groupUsers
                                        ))
                                    }
                                }

                                groupViewModel.updateGroup.observe(viewLifecycleOwner) { state ->
                                    when (state) {
                                        is UIState.Loading -> {
                                        }

                                        is UIState.Success -> {
                                            imagesloaded = false
                                            binding.myGroups.visibility = View.VISIBLE
                                            binding.groupModal.visibility = View.GONE
                                            binding.groupName.setText("")
                                            binding.groupDescription.setText("")
                                            binding.recyclerViewUsers.adapter = UserListAdapter(
                                                requireContext(),
                                                list,
                                                mapOf(),
                                                selectedUsers,
                                                true,
                                                onItemClicked = { _, _ ->
                                                },
                                                authViewModel
                                            )
                                            selectedImagesUris.clear()
                                            binding.imagePreviewContainer.visibility = View.GONE
                                            binding.buttonAddImages.setImageDrawable(
                                                resources.getDrawable(
                                                    R.drawable.addpicture,
                                                    null
                                                )
                                            )
                                            binding.buttonDeleteImages.visibility = View.GONE
                                            binding.updateGroupButton.visibility = View.GONE
                                            binding.createGroupButton.visibility = View.VISIBLE
                                            toast("Grupo actualizado exitosamente")
                                            binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
                                            binding.cardStackViewGroup.visibility = View.GONE
                                            binding.cardStackView.visibility = View.VISIBLE
                                            binding.cardStackViewAdminGroup.visibility = View.GONE
                                            binding.cardStackViewAdminGroup.scrollToPosition(0)
                                            binding.cardStackView.scrollToPosition(0)
                                            binding.cardStackViewGroup.scrollToPosition(0)
                                            userViewModel.getUsers()
                                            groupViewModel.getGroups()
                                            flag = false
                                        }

                                        is UIState.Error -> {
                                            toast(state.exception)
                                        }

                                        UIState.Empty -> {

                                        }
                                    }
                                }
                            }




                        },
                        onDeleteClicked = { position, group ->
                            groupViewModel.deleteGroup(group)

                            groupViewModel.deleteGroup.observe(viewLifecycleOwner) { state ->
                                when (state) {
                                    is UIState.Loading -> {
                                    }

                                    is UIState.Success -> {
                                        toast("Grupo eliminado exitosamente")
                                        binding.myGroups.setImageDrawable(resources.getDrawable(R.drawable.ic_my_groups, null))
                                        binding.cardStackViewGroup.visibility = View.GONE
                                        binding.cardStackView.visibility = View.VISIBLE
                                        binding.cardStackViewAdminGroup.visibility = View.GONE
                                        binding.cardStackViewAdminGroup.scrollToPosition(0)
                                        binding.cardStackView.scrollToPosition(0)
                                        binding.cardStackViewGroup.scrollToPosition(0)
                                        userViewModel.getUsers()
                                        groupViewModel.getGroups()
                                        flag = false
                                    }

                                    is UIState.Error -> {
                                        toast(state.exception)
                                    }

                                    UIState.Empty -> {

                                    }
                                }
                            }
                        }
                    )

                    if (adminGroupList.isEmpty() && binding.other.isChecked){
                        binding.noUsers.visibility = View.VISIBLE
                        binding.refresh.visibility = View.VISIBLE
                    }else{
                        binding.noUsers.visibility = View.GONE
                        binding.refresh.visibility = View.GONE
                    }

                }
                is UIState.Error -> {
                    toast(state.exception)
                }

                UIState.Empty -> {}
            }
        }


    }

    override fun onStart() {
        super.onStart()
        binding.cardStackView.visibility = View.VISIBLE
        binding.cardStackViewGroup.visibility = View.GONE
        binding.cardStackViewAdminGroup.visibility = View.GONE
        binding.ageFrom.isEnabled = true
        binding.ageTo.isEnabled = true
        binding.careerFilter.isEnabled = true
        binding.keywordFilter.isEnabled = true
        binding.bottomNavigation.menu[1].isChecked = true
        binding.searchBox.setText("")
        binding.ageFrom.setText("")
        binding.ageTo.setText("")
        binding.careerFilter.setText("")
        binding.keywordFilter.setText("")
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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }



    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission has already been granted
                pickImageFromGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission has already been granted
                pickImageFromGallery()
            }
        }
    }

    fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, open the gallery
                pickImageFromGallery()
            } else {
                // Permission was denied, show a toast or handle accordingly
                toast("Permission denied to access your gallery.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        selectedImagesUris.clear()
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
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
            updateImagePreview()
            binding.imagePreviewContainer.visibility = View.VISIBLE
            binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.ic_edit, null))
            binding.buttonDeleteImages.visibility = View.VISIBLE
        }else{
            toast("No se seleccionó ninguna imagen")
        }

        if(selectedImagesUris.isEmpty()){
            binding.imagePreviewContainer.visibility = View.GONE
            binding.buttonAddImages.setImageDrawable(resources.getDrawable(R.drawable.addpicture, null))
            binding.buttonDeleteImages.visibility = View.GONE
        }
    }

    // Dentro de la función updateImagePreview en tu Fragmento

    private fun updateImagePreview() {
        binding.imagePreviewContainer.removeAllViews()

        for (uri in selectedImagesUris) {
            val imageView = ImageView(binding.root.context)
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.marginEnd = 20
            layoutParams.gravity = RelativeLayout.CENTER_IN_PARENT
            layoutParams.weight = 400F
            layoutParams.height = 400
            imageView.layoutParams = layoutParams
            imageView.setImageURI(uri)
            imageView.scaleType = ImageView.ScaleType.CENTER
            binding.imagePreviewContainer.addView(imageView)

        }
    }

    private fun loadImages(imageUrls: List<String>) {
        binding.imagePreviewContainer.removeAllViews()

        for (imageUrl in imageUrls) {
            coroutineScope.launch {
                val bitmap = downloadImage(imageUrl)
                bitmap?.let {
                    val imageView = createImageView(it)
                    binding.imagePreviewContainer.addView(imageView)
                }
            }
        }
    }

    private fun createImageView(bitmap: Bitmap): ImageView {
        val imageView = ImageView(binding.root.context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = 8.dpToPx()
        imageView.layoutParams = layoutParams
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
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

    private fun Int.dpToPx(): Int = (this * binding.root.context.resources.displayMetrics.density).toInt()



    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1005

    }

    private fun validation(): Boolean {
        if (binding.groupName.text.toString().isEmpty()) {
            toast("Por favor ingrese un nombre para el grupo")
            return false
        }

        if(binding.groupDescription.text.toString().isEmpty()){
            toast("Por favor ingrese una descripción para el grupo")
            return false
        }

        if(selectedUsers.isEmpty()){
            toast("Por favor seleccione al menos un usuario")
            return false
        }

        if(selectedImagesUris.isEmpty() && !imagesloaded){
            toast("Por favor seleccione al menos una imagen")
            return false
        }

        return true
    }

    private fun status(status: String) {
        authViewModel.getUserSession { user ->
            user?.let {
                userViewModel.updateUserInfo(it.copy(status = status))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        status("online")
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }


}