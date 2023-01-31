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

package com.twoeightnine.root.xvii.groups.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.base.BaseReachAdapter
import com.twoeightnine.root.xvii.extensions.getInitials
import com.twoeightnine.root.xvii.managers.Prefs
import com.twoeightnine.root.xvii.model.Group
import global.msnthrp.xvii.uikit.extensions.lowerIf
import kotlinx.android.synthetic.main.item_group.view.*

class GroupsAdapter(context: Context,
                     private val onClick: (Group) -> Unit,
                     private val onLongClick: (Group) -> Unit,
                     onLoaded: (Int) -> Unit
) : BaseReachAdapter<Group, GroupsAdapter.GroupsViewHolder>(context, onLoaded) {

    var firstItemPadding = 0

    override fun createHolder(parent: ViewGroup, viewType: Int) =
            GroupsViewHolder(inflater.inflate(R.layout.item_group, null))


    override fun bind(holder: GroupsViewHolder, item: Group) {
        holder.bind(item, items[0] == item)
    }

    override fun createStubLoadItem() = Group()

    inner class GroupsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(group: Group, isFirst: Boolean) {
            with(view) {
                val topPadding = if (isFirst) firstItemPadding else 0
                setPadding(0, topPadding, 0, 0)

                civPhoto.load(group.photo100, group.name.getInitials(), id = group.id)
                tvName.text = group.name
                tvName.lowerIf(Prefs.lowerTexts)
                tvInfo.text = group.description

                setOnClickListener {
                    items.getOrNull(adapterPosition)?.also(onClick)
                }
                setOnLongClickListener {
                    items.getOrNull(adapterPosition)?.also(onLongClick)
                    true
                }
            }
        }
    }

}