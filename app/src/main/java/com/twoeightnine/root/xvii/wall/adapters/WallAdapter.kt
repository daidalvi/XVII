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

package com.twoeightnine.root.xvii.wall.adapters

import android.content.Context
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.NonNull
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.makeramen.roundedimageview.RoundedImageView
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.chats.attachments.AttachmentsInflater
import com.twoeightnine.root.xvii.extensions.getInitials
import com.twoeightnine.root.xvii.extensions.load
import com.twoeightnine.root.xvii.managers.Prefs
import com.twoeightnine.root.xvii.model.WallPost
import com.twoeightnine.root.xvii.model.attachments.Attachment
import com.twoeightnine.root.xvii.model.attachments.Photo
import com.twoeightnine.root.xvii.model.attachments.getPhotos
import com.twoeightnine.root.xvii.utils.*
import com.twoeightnine.root.xvii.wall.fragments.WallFragment
import global.msnthrp.xvii.uikit.base.adapters.BaseAdapter
import global.msnthrp.xvii.uikit.extensions.lowerIf
import global.msnthrp.xvii.uikit.utils.DisplayUtils
import global.msnthrp.xvii.uikit.utils.SizeUtils
import kotlinx.android.synthetic.main.item_wall.view.*


class WallAdapter(
    context: Context,
    private val onClick: (WallPost) -> Unit,
    private val onLongClick: (WallPost) -> Unit,
    wallPostCallback: (Context) -> WallFragment.WallPostCallback
) : BaseAdapter<WallPost, WallAdapter.WallViewHolder>(context) {

    private val resources = context.resources

    private var parent: RecyclerView? = null

    private val defaultRadius = resources.getDimensionPixelSize(R.dimen.default_radius)

    private val defaultRadiusMin = SizeUtils.pxFromDp(context, 10)

    private val picturesSpacing = SizeUtils.pxFromDp(context, 4)

    private val wpCallback:WallFragment.WallPostCallback = wallPostCallback(context)

    private val attachmentsInflater by lazy {
        AttachmentsInflater(context, wpCallback)
    }

    override fun onViewRecycled(@NonNull holder: WallViewHolder) {
        parent?.getRecycledViewPool()?.clear()
        //parent?.removeAllViews();
        //setLayoutParamsToView(holder.itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallViewHolder {
        this.parent = parent as RecyclerView
        return WallViewHolder(inflater.inflate(R.layout.item_wall, null))
    }

    override fun onBindViewHolder(holder: WallAdapter.WallViewHolder, position: Int) {
        holder.bind(items[position])
    }

    private fun getMessageBody(context: Context, text: String, ownerId:Int): String {
        return wrapMentions(context, text, true, ownerId).toString()
    }

    inner class WallViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(post: WallPost, level:Int=0) {
            with(itemView) {
                val topPadding = 0 //if (isFirst) firstItemPadding else 0
                setPadding(0, topPadding, 0, 0)

                post.group?.let{
                    if(it.id>0) {
                        civPhoto.load(it.photo100, it.name.getInitials(), id = it.id)
                        tvName.text = it.name
                        tvName.lowerIf(Prefs.lowerTexts)
                    }
                }
                post.user?.let{
                    if(it.id>0) {
                        civPhoto.load(it.photo100, it.getTitle(), id = it.id)
                        tvName.text = it.getTitle()
                        tvName.lowerIf(Prefs.lowerTexts)
                    }
                }
                tvInfo.text = getTime(post.date, withSeconds = Prefs.showSeconds)


                post.text?.let{ messageText ->
                    if(messageText.isNotEmpty()) {

                        val preparedText = wrapMentions(context, messageText, addClickable = true, ownerId = post.ownerId)
                        tvText.text = when {
                            EmojiHelper.hasEmojis(messageText) -> EmojiHelper.getEmojied(context, messageText, preparedText)
                            else -> preparedText
                        }
                        tvText.movementMethod = LinkMovementMethod.getInstance()
                    }
                }

                if(ivPhotoAttach.childCount==0) {
                    //ivAttach::removeAllViews
                    createViewForWallPost(post)
                    post.attachments?.let{
                        val attList = ArrayList<Attachment>()
                        for(attachment in it){
                            if(attachment.type != Attachment.TYPE_PHOTO){
                                attList.add(attachment)
                            }
                        }
                        if(!attList.isEmpty()) {
                            val mPost = WallPost(attachments=attList)
                            attachmentsInflater.createViewsFor(mPost)
                                .forEach(ivAttach::addView)
                        }
                    }
                }

                if (post.copyHistory != null && post.copyHistory.size > 0) {
                    fillContent(llContainer)
                    WallViewHolder(llContainer).bind(post.copyHistory[0], level + 1)
                }

                setOnClickListener {
                    items.getOrNull(adapterPosition)?.also(onClick)
                }
                setOnLongClickListener {
                    items.getOrNull(adapterPosition)?.also(onLongClick)
                    true
                }
            }
        }


        private fun fillContent(root: ViewGroup) {
            root.addView(View.inflate(context, R.layout.item_wall, null))
        }

        private fun createViewForWallPost(wallPost: WallPost){
            val attachments = wallPost.attachments ?: return
            var k=0
            for (attachment in attachments) {
                when (attachment.type) {
                    Attachment.TYPE_PHOTO -> attachment.photo
                        ?.let { photo ->
                            createPhotoForWallPost(
                                photo,
                                attachments.getPhotos(),
                                k
                            )
                            k+=1
                        }
                    else -> null
                }
            }
        }

        private fun createPhotoForWallPost(photo: Photo, photos: List<Photo>, num:Int): View {
            val count = photos.count()
            var columns = when {
                count > 4 -> 3
                count > 1 -> 2
                else -> 1
            }
            itemView.ivPhotoAttach.setColumnCount(columns)
            //ivPhotosAttach.layoutParams = GridLayout.LayoutParams()
            val view = createPhoto(photo, columns, count, num)
            view.setOnClickListener {
                val position = photos.indexOf(photo)
                wpCallback.onPhotoClicked(position, photos)
            }
            return view
        }

        private fun createPhoto(photo: Photo, columns: Int, count:Int, num:Int): View {
            val roundedImageView = RoundedImageView(context).apply {
                updatePadding(0, 0, 0, 0)
            }
            roundedImageView.cornerRadius = defaultRadiusMin.toFloat()

            val photoSize = photo.getOptimalPhoto()
                ?: photo.getMediumPhoto()
                ?: photo.getSmallPhoto()
                ?: return roundedImageView

            val param = GridLayout.LayoutParams()


            var width = DisplayUtils.screenWidth / columns

            val scale = width * 1.0f / photoSize.width
            val hCalc = (photoSize.height * scale).toInt()

            if (columns> 1)
                width = width - picturesSpacing

            //square
            var ivHeight = width

            if(num == 0){
                when{
                    count == 1 -> {
                        ivHeight = hCalc
                        roundedImageView.cornerRadius = defaultRadius.toFloat()
                    }
                    count == 3 ->{
                        // as is in 2 cells
                        roundedImageView.cornerRadius = defaultRadiusMin.toFloat()
                        param.columnSpec = GridLayout.spec(0, 2)
                        width=(width + picturesSpacing) * 2
                        ivHeight = hCalc * 2
                    }
                    count > 4 && count % 3 == 1 ->{
                        // as is in 3 cells
                        roundedImageView.cornerRadius = defaultRadiusMin.toFloat()
                        param.columnSpec = GridLayout.spec(0, 3)
                        width=(width + picturesSpacing) * 3
                        ivHeight = hCalc * 3
                    }
                    count > 4 && count % 3 == 2 ->{
                        if(hCalc>(width + picturesSpacing)){
                            // vertical
                            param.rowSpec = GridLayout.spec(0, 2)    // First cell in first row use rowSpan 2.
                            ivHeight = (ivHeight+ picturesSpacing) * 2
                        }else{
                            // horizontal
                            param.columnSpec = GridLayout.spec(0, 2) // First cell in first column use columnSpan 2.
                            width=width * 2 + picturesSpacing
                        }
                    }

                }
            }

            param.height = ivHeight
            param.width = width
            param.rightMargin = picturesSpacing
            param.topMargin = picturesSpacing
            param.setGravity(Gravity.CENTER)


            roundedImageView.setLayoutParams(param)
            itemView.ivPhotoAttach.addView(roundedImageView)

            roundedImageView.load(photoSize.url) {
                override(width, ivHeight)
                centerCrop()
            }
            return roundedImageView
        }

    }
}