package com.twoeightnine.root.xvii.search

enum class SEARCH_TYPE {
    CHAT,
    FRIENDS,
    GROUP
}

data class SearchDialog(

    var peerId: Int,

    var messageId: Int,

    var title: String,

    var text: String = "",

    var photo: String?,

    var isOnline: Boolean = false,

    var isOut: Boolean = true,

    var type: SEARCH_TYPE = SEARCH_TYPE.CHAT

)