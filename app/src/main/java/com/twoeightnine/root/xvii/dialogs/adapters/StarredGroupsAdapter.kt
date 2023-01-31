package com.twoeightnine.root.xvii.dialogs.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.extensions.load
import com.twoeightnine.root.xvii.model.Group
import global.msnthrp.xvii.uikit.base.adapters.BaseAdapter
import global.msnthrp.xvii.uikit.extensions.setVisible
import kotlinx.android.synthetic.main.star_dialog.view.*


class StarredGroupsAdapter(
    context: Context,
    private val onClick: (Group) -> Unit,
    private val onLongClick: (Group) -> Unit
) : BaseAdapter<Group, StarredGroupsAdapter.StarredViewHolder>(context) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StarredViewHolder(inflater.inflate(
            R.layout.star_dialog, null))

    override fun onBindViewHolder(holder: StarredViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class StarredViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(group: Group) {
            with(itemView) {
                civPhoto.load(group.photo50)

                ivOnlineDot.setVisible(false)

                rlItemContainer.setOnClickListener {
                    items.getOrNull(adapterPosition)
                        ?.also(onClick)
                }
                rlItemContainer.setOnLongClickListener {
                    items.getOrNull(adapterPosition)
                        ?.also(onLongClick)
                    true
                }
            }
        }
    }

}