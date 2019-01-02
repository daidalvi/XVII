package com.twoeightnine.root.xvii.chats.fragments

import android.view.View
import android.widget.ListView
import android.widget.RelativeLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.chats.adapters.attachments.AttachmentsAdapter
import com.twoeightnine.root.xvii.fragments.BaseFragment
import com.twoeightnine.root.xvii.model.Attachment
import com.twoeightnine.root.xvii.utils.ApiUtils
import com.twoeightnine.root.xvii.utils.AttachUtils
import javax.inject.Inject

class AttachedFragment: BaseFragment() {

    companion object {
        fun newInstance(attachUtils: AttachUtils): AttachedFragment {
            val frag = AttachedFragment()
            frag.attachUtils = attachUtils
            return frag
        }
    }

    @BindView(R.id.lvAttachments)
    lateinit var lvAttachments: ListView
    @BindView(R.id.rlForwarded)
    lateinit var rlForwarded: RelativeLayout
    @BindView(R.id.rlClose)
    lateinit var rlClose: RelativeLayout

    @Inject
    lateinit var apiUtils: ApiUtils

    lateinit var attachUtils: AttachUtils
    lateinit var adapter: AttachmentsAdapter

    private val removeList: MutableList<Attachment> = mutableListOf()
    private var viewsBind = false

    override fun bindViews(view: View) {
        super.bindViews(view)
        ButterKnife.bind(this, view)
        App.appComponent?.inject(this)
        viewsBind = true
    }

    fun initAdapter() {
        adapter = AttachmentsAdapter(
                { onRemove(it) },
                { apiUtils.showPhoto(context ?: throw Exception(), it.photoId, it.accessKey) },
                { apiUtils.openVideo(context ?: throw Exception(), it) }
        )
        adapter.add(attachUtils.attachments)
        rlClose.setOnClickListener {
            attachUtils.forwarded = ""
            rlForwarded.visibility = View.GONE
        }
        rlForwarded.visibility = if (attachUtils.forwarded.isNotEmpty()) View.VISIBLE else View.GONE
        lvAttachments.adapter = adapter
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && viewsBind) {
            initAdapter()
        } else {
            attachUtils.remove(removeList)
        }
    }

    private fun onRemove(pos: Int) {
        removeList.add(adapter.items[pos])
        adapter.remove(pos)
    }

    override fun getLayout() = R.layout.fragment_attached

}