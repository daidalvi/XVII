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

package com.twoeightnine.root.xvii.wall.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.base.BaseFragment
import com.twoeightnine.root.xvii.base.FragmentPlacementActivity.Companion.startFragment
import com.twoeightnine.root.xvii.chats.attachments.AttachmentsInflater
import com.twoeightnine.root.xvii.model.WallPost
import com.twoeightnine.root.xvii.model.Wrapper
import com.twoeightnine.root.xvii.model.attachments.Doc
import com.twoeightnine.root.xvii.model.attachments.Video
import com.twoeightnine.root.xvii.utils.ApiUtils
import com.twoeightnine.root.xvii.utils.PermissionHelper
import com.twoeightnine.root.xvii.utils.showError
import com.twoeightnine.root.xvii.wall.adapters.WallAdapter
import com.twoeightnine.root.xvii.wall.viewmodel.WallViewModel
import com.twoeightnine.root.xvii.wallpost.WallPostFragment
import global.msnthrp.xvii.uikit.extensions.applyBottomInsetPadding
import kotlinx.android.synthetic.main.fragment_wall.*
import javax.inject.Inject


class WallFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: WallViewModel.Factory
    private lateinit var viewModel: WallViewModel

    @Inject
    lateinit var apiUtils: ApiUtils

    private var lastAdded: Int = 0

    private val peerId by lazy {
        arguments?.getInt(ARG_PEER_ID) ?: 0
    }

    private val adapter by lazy {
        WallAdapter(requireContext(), ::onClick, ::onLongClick, ::WallPostCallback)
    }

    override fun getLayoutId() = R.layout.fragment_wall

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.appComponent?.inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory)[WallViewModel::class.java]

        //adapter.startLoading()

        //progressBar.show()
        rvWall.layoutManager = LinearLayoutManager(context)
        rvWall.adapter = adapter
        //rvWall.addOnScrollListener(AppBarLifter(xviiToolbar))

        rvWall.setItemViewCacheSize(0)
        //rvWall.setItemViewCacheSize(10000)

        /*swipeRefresh.setOnRefreshListener {
            viewModel.loadWall(peerId)
            adapter.reset()
            adapter.startLoading()
        }*/
        rvWall.addOnScrollListener(WallScrollListener())

        rvWall.applyBottomInsetPadding()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getWall().observe(viewLifecycleOwner, ::updateWall)
        rvWall.setItemViewCacheSize(WallViewModel.COUNT*10)
        viewModel.loadWall(peerId)
    }

    private fun updateWall(data: Wrapper<ArrayList<WallPost>>) {
        /*swipeRefresh.isRefreshing = false
        progressBar.hide()*/
        if (data.data != null) {
            adapter.update(data.data)
        } else {
            showError(context, data.error ?: getString(R.string.error))
        }
    }

    private fun loadMore(offset: Int) {
        //rvWall.setItemViewCacheSize(WallViewModel.COUNT+offset)
        viewModel.loadWall(peerId, offset)
    }

    private fun onClick(wall: WallPost) {
        context.startFragment<WallPostFragment>(WallPostFragment.createArgs(wall.stringId))
    }

    private fun onLongClick(wall: WallPost) {

    }


    private inner class WallScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            /*Log.d("ZEZEZE", adapter.lastVisiblePosition(rvWall.layoutManager).toString()+
                    " - "+lastAdded+ "- " +adapter.itemCount)*/
            if (rvWall!=null && lastAdded < adapter.itemCount - 1 &&
                adapter.lastVisiblePosition(rvWall.layoutManager) == adapter.itemCount - 1) {
                loadMore(adapter.itemCount)
                lastAdded = adapter.itemCount - 1
            }
        }
    }

    companion object {

        const val ARG_PEER_ID = "peerId"

        fun newInstance(peerId: Int): WallFragment {
            val fragment = WallFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_PEER_ID, peerId)
            }
            return fragment
        }
    }


    inner class WallPostCallback(context: Context)
        : AttachmentsInflater.DefaultCallback(context, PermissionHelper(this)) {

        override fun onEncryptedDocClicked(doc: Doc) {
        }

        override fun onVideoClicked(video: Video) {
            context?.also {
                apiUtils.openVideo(it, video)
            }
        }
    }
}