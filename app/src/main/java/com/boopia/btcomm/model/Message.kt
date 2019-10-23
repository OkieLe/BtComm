package com.boopia.btcomm.model

enum class MessageType {
    TEXT,
    EMOJI;

    companion object {
        fun typeOf(ordinal: Int?): MessageType {
            return values()
                .firstOrNull { it.ordinal == ordinal }
                ?: TEXT
        }
    }
}

data class Message(
    val type: MessageType = MessageType.TEXT,
    val content: String = "",
    val date: Long = System.currentTimeMillis() / 1000
)