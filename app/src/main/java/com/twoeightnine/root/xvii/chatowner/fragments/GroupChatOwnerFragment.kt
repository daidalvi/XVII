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

package com.twoeightnine.root.xvii.chatowner.fragments

import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.model.Group
import com.twoeightnine.root.xvii.utils.BrowsingUtils
import com.twoeightnine.root.xvii.wall.fragments.WallFragment
import global.msnthrp.xvii.uikit.extensions.setVisible
import kotlinx.android.synthetic.main.fragment_chat_owner_group.*


class GroupChatOwnerFragment : BaseChatOwnerFragment<Group>() {

    override fun getLayoutId() = R.layout.fragment_chat_owner_group

    override fun getChatOwnerClass() = Group::class.java

    var wallLoaded:Boolean = false

    var wallFragment: WallFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerWall?.let {
            val displayMetrics = DisplayMetrics()
            val act = (this@GroupChatOwnerFragment).activity
            if(act!=null) {
                act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels
                val params = it.getLayoutParams()
                params.height = height
                it.setLayoutParams(params)
                it.requestLayout()
            }
        }
    }


    override fun bindChatOwner(chatOwner: Group?) {
        val group = chatOwner ?: return

        addValue(R.drawable.ic_vk, group.screenName, ::onClick)
        addValue(R.drawable.ic_quotation, group.status)
        addValue(R.drawable.ic_sheet, group.description)

        if(group.site.isNotEmpty()){
            fabGotoSite.setVisible(true)
            fabGotoSite.setOnClickListener {
                fabGotoSite?.also {
                    BrowsingUtils.openUrlInnerBrowser(context, group.site, group.name)
                }
            }
        }
    }

    override fun onSlideDesc(offset:Float) {
        super.onSlideDesc(offset)
        showWall()
    }

    fun showWall(){
        if (wallLoaded)
            return
        wallLoaded = true
        fragmentManager?.let{
            val transaction = it.beginTransaction()
            wallFragment = WallFragment.newInstance(peerId)
            transaction.add(
                R.id.containerWall,
                wallFragment!!
            )
            transaction.commit()
        }
    }

    private fun onClick(s: String) {
        context?.let{ BrowsingUtils.openUriIntent(it, Uri.parse(URL_VK+s))}
    }
    private fun onClickSite(s: String) {
        if(s.isNotEmpty())
            BrowsingUtils.openUrlInnerBrowser(context, s)
    }

    override fun getBottomPaddableView(): View = vBottom

    companion object {

        private const val URL_VK = "https://vk.com/"

        fun newInstance(peerId: Int): GroupChatOwnerFragment {
            val fragment = GroupChatOwnerFragment()
            fragment.arguments = Bundle().apply {
                putInt(WallFragment.ARG_PEER_ID, peerId)
            }
            return fragment
        }
    }
}