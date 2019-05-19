package com.twoeightnine.root.xvii.chats.messages.base

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.chats.messages.chat.secret.SecretChatViewModel
import com.twoeightnine.root.xvii.chats.messages.chat.usual.ChatMessagesViewModel
import com.twoeightnine.root.xvii.chats.messages.starred.StarredMessagesViewModel
import com.twoeightnine.root.xvii.model.Message
import com.twoeightnine.root.xvii.model.WrappedLiveData
import com.twoeightnine.root.xvii.model.WrappedMutableLiveData
import com.twoeightnine.root.xvii.model.Wrapper
import com.twoeightnine.root.xvii.model.attachments.Video
import com.twoeightnine.root.xvii.network.ApiService
import com.twoeightnine.root.xvii.utils.subscribeSmart
import javax.inject.Inject

abstract class BaseMessagesViewModel(protected val api: ApiService) : ViewModel() {

    protected val messagesLiveData = WrappedMutableLiveData<ArrayList<Message>>()

    fun getMessages() = messagesLiveData as WrappedLiveData<ArrayList<Message>>

    abstract fun loadMessages(offset: Int = 0)

    fun loadVideo(
            context: Context,
            video: Video,
            onLoaded: (String) -> Unit,
            onError: (String) -> Unit
    ) {
        api.getVideos(
                video.videoId,
                video.accessKey ?: "",
                1, 0
        )
                .subscribeSmart({ response ->
                    if (response.items.size > 0 && response.items[0].player != null) {
                        onLoaded(response.items[0].player ?: "")
                    } else {
                        onError(context.getString(R.string.not_playable_video))
                    }
                }, onError)
    }

    protected fun onMessagesLoaded(messages: ArrayList<Message>, offset: Int = 0) {
        val existing = if (offset == 0) {
            arrayListOf()
        } else {
            messagesLiveData.value?.data ?: arrayListOf()
        }

        messagesLiveData.value = Wrapper(existing.also { it.addAll(messages) })
    }

    protected fun onErrorOccurred(error: String) {
        messagesLiveData.value = Wrapper(error = error)
    }

    class Factory @Inject constructor(
            private val api: ApiService,
            private val context: Context
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = when (modelClass) {
            StarredMessagesViewModel::class.java -> StarredMessagesViewModel(api) as T
            ChatMessagesViewModel::class.java -> ChatMessagesViewModel(api) as T
            SecretChatViewModel::class.java -> SecretChatViewModel(api, context) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class $modelClass")
        }
    }
}