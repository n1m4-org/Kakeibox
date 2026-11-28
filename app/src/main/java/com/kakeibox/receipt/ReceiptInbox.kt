package com.kakeibox.receipt

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 他のアプリから共有されたレシート画像を、読み取り画面に渡すための受け口 */
object ReceiptInbox {
    private val _pending = MutableStateFlow<List<Uri>>(emptyList())
    val pending: StateFlow<List<Uri>> = _pending.asStateFlow()

    fun offer(uris: List<Uri>) { if (uris.isNotEmpty()) _pending.value = uris }

    /** 受け取った画像を取り出す（取り出したら空にする） */
    fun take(): List<Uri> = _pending.value.also { _pending.value = emptyList() }
}
