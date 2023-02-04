/*
 * xvii - messenger for vk
 * Copyright (C) 2021  TwoEightNine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.twoeightnine.root.xvii.groups.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.BuildConfig
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.base.BaseFragment
import com.twoeightnine.root.xvii.chatowner.ChatOwnerFactory
import com.twoeightnine.root.xvii.chats.messages.chat.secret.SecretChatActivity
import com.twoeightnine.root.xvii.groups.adapters.GroupsAdapter
import com.twoeightnine.root.xvii.groups.viewmodel.GroupsViewModel
import com.twoeightnine.root.xvii.managers.Prefs
import com.twoeightnine.root.xvii.model.Group
import com.twoeightnine.root.xvii.model.Wrapper
import com.twoeightnine.root.xvii.utils.*
import com.twoeightnine.root.xvii.utils.contextpopup.ContextPopupItem
import com.twoeightnine.root.xvii.utils.contextpopup.createContextPopup
import com.twoeightnine.root.xvii.utils.notifications.NotificationUtils
import com.twoeightnine.root.xvii.views.TextInputAlertDialog
import global.msnthrp.xvii.data.dialogs.Dialog
import global.msnthrp.xvii.uikit.extensions.applyBottomInsetPadding
import global.msnthrp.xvii.uikit.extensions.hide
import global.msnthrp.xvii.uikit.extensions.show
import kotlinx.android.synthetic.main.fragment_groups.*
import kotlinx.android.synthetic.main.fragment_wall.*
import javax.inject.Inject

class GroupsFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: GroupsViewModel.Factory
    private lateinit var viewModel: GroupsViewModel

    private val adapter by lazy {
        GroupsAdapter(requireContext(), ::onClick, ::onLongClick,  ::loadMore)
    }

    override fun getLayoutId() = R.layout.fragment_groups

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.appComponent?.inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[GroupsViewModel::class.java]
        adapter.startLoading()

        progressBar.show()
        rvGroups.layoutManager = LinearLayoutManager(context)
        rvGroups.adapter = adapter
        rvGroups.setItemViewCacheSize(0)
        rvGroups.addOnScrollListener(AppBarLifter(xviiToolbar))

        swipeRefresh.setOnRefreshListener {
            viewModel.loadGroups()
            adapter.reset()
            adapter.startLoading()
        }
        rvGroups.applyBottomInsetPadding()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getGroups().observe(viewLifecycleOwner, ::updateGroups)
        viewModel.loadGroups()
    }

    private fun updateGroups(data: Wrapper<ArrayList<Group>>) {
        swipeRefresh.isRefreshing = false
        progressBar.hide()
        if (data.data != null) {
            adapter.update(data.data)
        } else {
            showError(context, data.error ?: getString(R.string.error))
        }
    }

    private fun loadMore(offset: Int) {
        viewModel.loadGroups(offset)
    }

    private fun onClick(group: Group) {
        ChatOwnerFactory.launch(context, group.getPeerId())
    }

    private fun onLongClick(group: Group) {

        val isStarred = viewModel.starredIds.contains(group.id.toString())

        val items = arrayListOf(

            ContextPopupItem(
                if (isStarred) R.drawable.ic_star_crossed else R.drawable.ic_star,
                if (isStarred) R.string.remove_shortcut_from_chat_list
                else R.string.add_shortcut_to_chat_list) { viewModel.toggleStarred(group.id.toString(), isStarred) }
        )

        createContextPopup(context ?: return, items).show()
    }

    companion object {
        fun newInstance() = GroupsFragment()
    }
}