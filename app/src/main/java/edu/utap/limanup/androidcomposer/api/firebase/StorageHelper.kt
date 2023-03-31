package edu.utap.limanup.androidcomposer.api.firebase

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import edu.utap.limanup.androidcomposer.model.AudioMeta
import java.io.File

/**
 * Freesound API requires Auth2 authentication for download activity,
 * which is fairly complicated.
 * So as a workaround, I downloaded 180 audio snippet samples from freesound
 * and uploaded all to Firebase Storage
 * I also created the Firebase DB with all audio metadata
 * and reference to the audio file stored in Firebase Storage
 * Now the both download and upload APIs are pointing towards Firebase
 */

class StorageHelper {
    private val storageRef = Firebase.storage.reference
    private val freesoundStorage = storageRef.child("freesound")
    private val mymuscStorage = storageRef.child("mymusic")

    private fun getAudioRef(audio: AudioMeta): StorageReference {
        return freesoundStorage.child(audio.filename)
    }

    // https://firebase.google.com/docs/storage/android/download-files#download_to_a_local_file
    fun downloadAudio(
        audio: AudioMeta, saveTo: File,
        downloadSuccess: () -> Unit
    ) {
        getAudioRef(audio)
            .getFile(saveTo)
            .addOnSuccessListener {
                if (it.bytesTransferred == it.totalByteCount) {
                    downloadSuccess()
                }
                Log.d(
                    javaClass.simpleName, "XXX ${it.bytesTransferred}" +
                            " = ${it.totalByteCount}" +
                            ". saved to: ${saveTo.absolutePath}"
                )
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "XXX download failed. ")
                it.printStackTrace()
            }
    }

    private fun uuid2StorageReference(uuid: String): StorageReference {
        return mymuscStorage.child(uuid)
    }

    // https://firebase.google.com/docs/storage/android/download-files#download_data_via_url
    fun playMusicFromUri(
        uuid: String,
        player: MediaPlayer,
        context: Context,
        isPlaying: MutableLiveData<Boolean>,
        updateNowPlaying: () -> Unit
    ) {
        uuid2StorageReference(uuid).downloadUrl
            .addOnSuccessListener { uri ->
                player.reset()
                player.setDataSource(context, uri)
                player.prepare()
                player.start()
                isPlaying.value = player.isPlaying
                updateNowPlaying()
            }
            .addOnFailureListener { e ->
                player.reset()
                e.printStackTrace()
            }
    }

    // https://firebase.google.com/docs/storage/android/upload-files#upload_from_a_local_file
    fun uploadMusicFile(
        localFile: File,
        uuid: String,
        musicTitle: String,
        duration: Float,
        uploadSuccess: () -> Unit
    ) {
        val file = Uri.fromFile(localFile)
        val uuidRef = uuid2StorageReference(uuid)
        val metadata = StorageMetadata.Builder()
            .setContentType("audio/wav")
            .setCustomMetadata("duration", duration.toString())
            .setCustomMetadata("musicTitle", musicTitle)
            .build()
        val uploadTask = uuidRef.putFile(file, metadata)

        uploadTask
            .addOnFailureListener { e ->
                e.printStackTrace()
                if (localFile.delete()) {
                    Log.d(javaClass.simpleName, "XXX storage upload FAILED $uuid, file deleted")
                } else {
                    Log.d(
                        javaClass.simpleName, "XXX storage upload FAILED $uuid, file delete " +
                                "FAILED"
                    )
                }
            }
            .addOnSuccessListener {
                uploadSuccess()
                if (localFile.delete()) {
                    Log.d(javaClass.simpleName, "XXX storage upload succeeded $uuid, file deleted")
                } else {
                    Log.d(
                        javaClass.simpleName, "XXX storage upload succeeded $uuid, file delete " +
                                "FAILED"
                    )
                }
            }
    }

    fun deleteMusicFile(uuid: String) {
        val uuidRef = uuid2StorageReference(uuid)
        uuidRef.delete()
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "XXX storage deleted music $uuid")
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Log.d(javaClass.simpleName, "XXX storage delete music failed $uuid")
            }
    }
}