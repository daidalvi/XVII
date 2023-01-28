package com.twoeightnine.root.xvii.chats.messages.chat.base

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.extensions.load
import com.twoeightnine.root.xvii.uikit.Munch
import com.twoeightnine.root.xvii.uikit.paint
import global.msnthrp.xvii.data.dialogs.Dialog
import global.msnthrp.xvii.uikit.base.adapters.BaseAdapter
import global.msnthrp.xvii.uikit.extensions.setVisible
import kotlinx.android.synthetic.main.item_user.view.*
import kotlinx.android.synthetic.main.star_dialog.view.*
import kotlinx.android.synthetic.main.star_dialog.view.civPhoto
import kotlinx.android.synthetic.main.star_dialog.view.ivOnlineDot
import kotlinx.android.synthetic.main.star_dialog.view.rlItemContainer


class StarredAdapter(
    context: Context,
    private val onClick: (Dialog) -> Unit,
    private val onLongClick: (Dialog) -> Unit
) : BaseAdapter<Dialog, StarredAdapter.StarredViewHolder>(context) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StarredViewHolder(inflater.inflate(
        R.layout.star_dialog, null))

    override fun onBindViewHolder(holder: StarredViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class StarredViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(dialog: Dialog) {
            with(itemView) {
                civPhoto.load(dialog.photo)

                ivOnlineDot.setVisible(dialog.isOnline)
                ivOnlineDot.paint(Munch.color.color)

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