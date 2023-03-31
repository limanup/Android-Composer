package edu.utap.limanup.androidcomposer.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class MusicMeta(
    var ownerName: String = "",
    var ownerEmail: String = "",
    var ownerUid: String = "",
    var uuid: String = "",
    var duration: Float = 0F,
    var musicTitle: String = "",
    @ServerTimestamp val timeStamp: Timestamp? = null,
    @DocumentId var firestoreID: String = ""
    )


