package edu.utap.limanup.androidcomposer.api.firebase

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.utap.limanup.androidcomposer.model.AudioMeta
import edu.utap.limanup.androidcomposer.model.MusicMeta

/**
 * Freesound API requires Auth2 authentication for download activity,
 * which is fairly complicated.
 * So as a workaround, I downloaded 180 audio snippet samples from freesound
 * and uploaded all to Firebase Storage
 * I also created the Firebase DB with all audio metadata
 * and reference to the audio file stored in Firebase Storage
 * Now the both download and upload APIs are pointing towards Firebase
 */

class DatabaseHelper {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val freesoundCollection = "freesound"
    private val myMusicCollection = "mymusic"

//    fun oneTimeUploadAudioMeta(audioMeta: AudioMeta) {
//        val audioRef = db.collection(freesoundCollection)
//        audioRef.add(audioMeta)
//            .addOnSuccessListener {
//                Log.d(
//                    javaClass.simpleName,
//                    "Audio created id: ${audioMeta.id}, title: ${audioMeta
//                        .name}, filename: ${audioMeta.filename} "
//                )
//            }
//            .addOnFailureListener {
//                Log.d(javaClass.simpleName, "Audio create FAILED ${audioMeta.id}, " +
//                        "exception: $it")
//            }
//    }

    // This is the API call to Firestore DB to retrieve entire list of AudioMeta
    fun fetchAudioMeta(freesoundList: MutableLiveData<List<AudioMeta>>) {
        db.collection(freesoundCollection)
            .get()
            .addOnSuccessListener { result ->
                Log.d(
                    javaClass.simpleName, "XXX fetch all audiometa from freesound folder. " +
                            "list size: ${result.documents.size}"
                )
                freesoundList.postValue(result.documents.mapNotNull {
                    it.toObject(AudioMeta::class.java)
                })
            }
            .addOnFailureListener {e ->
                Log.d(javaClass.simpleName, "XXX all audiometa fetch failed.", e)
            }
    }

    fun fetchMusicMeta(
        myMusicList: MutableLiveData<List<MusicMeta>>,
        ownerUid: String) {
        db.collection(myMusicCollection)
            .whereEqualTo("ownerUid", ownerUid)
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d(
                    javaClass.simpleName, "XXX fetch musicmeta. " +
                            "list size: ${result.documents.size}"
                )
                myMusicList.postValue(result.documents.mapNotNull {
                    it.toObject(MusicMeta::class.java)
                })
            }
            .addOnFailureListener {e ->
                Log.d(javaClass.simpleName, "XXX musicmeta fetch failed.", e)
            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/add-data
    fun addMusicMeta(
        music: MusicMeta,
        myMusicList: MutableLiveData<List<MusicMeta>>,
    ) {
        // can get a document id if need it.
        //music.firestoreID = db.collection(myMusicCollection).document().id
        db.collection(myMusicCollection)
            .add(music)
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "XXX create musicmeta success " +
                            "music title: ${music.musicTitle}, " +
                            "owner name: ${music.ownerName} "
                )
                fetchMusicMeta(myMusicList, music.ownerUid)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Music create FAILED: ${music.musicTitle} " +
                        "exception: $e")
            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/delete-data
    fun deleteMusicMeta(
        music: MusicMeta,
        myMusicList: MutableLiveData<List<MusicMeta>>,
    ) {
        db.collection(myMusicCollection)
            .document(music.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "XXX delete musicmeta success " +
                            "music title: ${music.musicTitle}, " +
                            "owner name: ${music.ownerName} " +
                            "firestoreID: ${music.firestoreID}"
                )
                fetchMusicMeta(myMusicList, music.ownerUid)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Music delete FAILED: ${music.musicTitle} " +
                        "exception: $e")
            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/add-data#update-data
    fun updateMusicTitle(
        music: MusicMeta,
        myMusicList: MutableLiveData<List<MusicMeta>>,
        newMusicTitle: String,
        nowPlaying: MutableLiveData<String>
    ) {
        val nowPlayingUuid = myMusicList.value?.find {
            it.musicTitle == nowPlaying.value
        }?.uuid
        db.collection(myMusicCollection)
            .document(music.firestoreID)
            .update("musicTitle", newMusicTitle)
            .addOnSuccessListener {
                fetchMusicMeta(myMusicList, music.ownerUid)
                if (music.uuid == nowPlayingUuid) {
                    nowPlaying.value = newMusicTitle
                }
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Update title FAILED: ${music.musicTitle} " +
                        "exception: $e")
            }
    }

}