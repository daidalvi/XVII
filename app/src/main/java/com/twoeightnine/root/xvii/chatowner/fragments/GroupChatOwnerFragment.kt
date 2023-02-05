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

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.base.FragmentPlacementActivity.Companion.startFragment
import com.twoeightnine.root.xvii.main.MainActivity.Companion.SEARCH_OWNER_ID
import com.twoeightnine.root.xvii.main.MainActivity.Companion.SEARCH_TYPE
import com.twoeightnine.root.xvii.model.Group
import com.twoeightnine.root.xvii.search.SEARCH_TYPE.GROUP
import com.twoeightnine.root.xvii.search.SearchFragment
import com.twoeightnine.root.xvii.uikit.Munch
import com.twoeightnine.root.xvii.uikit.paint
import com.twoeightnine.root.xvii.utils.BrowsingUtils
import com.twoeightnine.root.xvii.wall.fragments.WallFragment
import global.msnthrp.xvii.uikit.extensions.applyTopInsetMargin
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

        ivSearchGroup.paint(Munch.color.color)
        ivSearchGroup.applyTopInsetMargin()
    }


    override fun bindChatOwner(chatOwner: Group?) {
        val group = chatOwner ?: return

        addValue(R.drawable.ic_vk, group.screenName, ::onClick)
        addValue(R.drawable.ic_quotation, group.status)
        addValue(R.drawable.ic_sheet, group.description)

        menuContainer.setBackgroundColor(Munch.color.color(15))
        ivPhotos.paint(Munch.color.color)
        ivBoards.paint(Munch.color.color)
        ivArticles.paint(Munch.color.color)
        ivLinks.paint(Munch.color.color)
        ivPhotos.setOnClickListener {
            context?.let{ BrowsingUtils.openUrl(it, URL_VK+"albums-"+chatOwner.id)}
        }
        ivBoards.setOnClickListener {
            context?.let{ BrowsingUtils.openUrl(it, URL_VK+"board"+chatOwner.id)}
        }
        ivArticles.setOnClickListener {
            context?.let{ BrowsingUtils.openUrl(it, URL_VK+"@"+chatOwner.screenName)}
        }
        ivLinks.setOnClickListener {
            context?.let{ BrowsingUtils.openUrl(it, URL_VK+chatOwner.screenName+"?act=links")}
        }

        if(group.site.isNotEmpty()){
            fabGotoSite.setVisible(true)
            fabGotoSite.setOnClickListener {
                fabGotoSite?.also {
                    //openUrlInnerBrowser - group.name
                    BrowsingUtils.openUrl(context, group.site, true)
                }
            }
        }

        ivSearchGroup.setOnClickListener {
            var arguments = Bundle().apply {
                putInt(SEARCH_TYPE, GROUP.ordinal)
                putInt(SEARCH_OWNER_ID, chatOwner.getPeerId())
            }
            startFragment<SearchFragment>(arguments)
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
        // openUriIntent
        context?.let{ BrowsingUtils.openUrl(it, URL_VK+s)}
    }

    override fun getBottomPaddableView(): View = vBottom

    companion object {

        private const val URL_VK = "https://m.vk.com/"

        fun newInstance(peerId: Int): GroupChatOwnerFragment {
            val fragment = GroupChatOwnerFragment()
            fragment.arguments = Bundle().apply {
                putInt(WallFragment.ARG_PEER_ID, peerId)
            }
            return fragment
        }
    }
}