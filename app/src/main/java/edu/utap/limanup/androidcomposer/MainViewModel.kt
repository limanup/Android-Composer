package edu.utap.limanup.androidcomposer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import edu.utap.limanup.androidcomposer.api.firebase.DatabaseHelper
import edu.utap.limanup.androidcomposer.api.firebase.StorageHelper
import edu.utap.limanup.androidcomposer.api.localDB.LocalRepo
import edu.utap.limanup.androidcomposer.auth.FirestoreAuthLiveData
import edu.utap.limanup.androidcomposer.model.AudioMeta
import edu.utap.limanup.androidcomposer.model.MusicMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class MainViewModel : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        player.reset()
        player.release()
    }

    var fetchDone = MutableLiveData<Boolean>()

    // for bottom nav bar
    var navToMyMusic = MutableLiveData(0)

    // for local environment testing
    var isLocal = MutableLiveData<Boolean>()
    private val localRepo = LocalRepo()

    // Firestore Database and Storage
    private val dbHelp = DatabaseHelper()
    private val storageHelp = StorageHelper()

    // Firestore Auth
    private var firebaseAuthLiveData = FirestoreAuthLiveData()
    fun updateUser() {
        firebaseAuthLiveData.updateUser()
        fetchAllAudioMeta()
        fetchMyMusicList()
        player.reset()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuthLiveData.getCurrentUser()
    }

    fun signoutUser() {
        firebaseAuthLiveData.signOutUser()
        player.reset()
        isPlaying.value = false
        updateNowPlaying("")
        navToMyMusic.value = 0
    }

    // util
    fun getDuration(context: Context, file: File): Float {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, file.absoluteFile.toUri())
        val durationString =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
        // returns duration in seconds
        return durationString.toFloat() / 1000
    }

    fun convertTime(millsec: Int): String {
        val minutes =
            if (millsec / 1000 / 60 > 0) String.format("%02d", millsec / 1000 / 60) else ""
        val seconds = String.format("%02d", millsec / 1000 % 60)
        return "$minutes:$seconds"
    }

    fun deleteFile(file: File) {
        file.deleteRecursively()
    }

    // MediaPlayer
    val player = MediaPlayer()
    var isPlaying = MutableLiveData(false)
    private val nowPlaying = MutableLiveData("")
    fun observeNowPlaying(): LiveData<String> {
        return nowPlaying
    }

    fun updateNowPlaying(title: String) {
        nowPlaying.value = title
    }

    fun getNextAudio(nowPlaying: String?, audioMetaList: List<AudioMeta>): AudioMeta? {
        if (nowPlaying.isNullOrEmpty()) return null
        val currentPos = audioMetaList.indexOfFirst {
            it.name === nowPlaying
        }
        if (currentPos == -1 || currentPos == audioMetaList.size - 1) {
            return null
        } else {
            return audioMetaList[currentPos + 1]
        }
    }

    fun getSubFolder(context: Context, subFolderName: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: return Environment.getDownloadCacheDirectory()
        val subFolder = File(baseDir, subFolderName)
        if (subFolder.isDirectory) return subFolder
        if (subFolder.isFile) subFolder.delete()
        if (subFolder.mkdir()) return subFolder
        return subFolder
    }

    fun getAudioFile(context: Context, audio: AudioMeta): File {
        // create the File where the audio snippets should go
        val folder = getSubFolder(context, "audios")
//        Log.d(javaClass.simpleName, "XXX subfolder: ${folder.absolutePath}")
        val file = File(folder, audio.filename)
        file.createNewFile()
        return file
    }


    // AudioMeta, composer fragment
    private var freesoundList = MutableLiveData<List<AudioMeta>>()
    val audioMetaList = MutableLiveData<List<AudioMeta>>()

    fun observeFreesoundList(): LiveData<List<AudioMeta>> {
        return freesoundList
    }

    private fun fetchAllAudioMeta() {
        if (isLocal.value == true) {
            freesoundList.value = localRepo.fetchLocalAudioMeta()
        } else {
            viewModelScope.launch(
                context = viewModelScope.coroutineContext
                        + Dispatchers.IO
            ) {
                try {
                    fetchDone.postValue(false)
                    dbHelp.fetchAudioMeta(freesoundList)
                    fetchDone.postValue(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(javaClass.simpleName, "XXX $e")
                }
            }
        }

    }

    private fun randomSelectAudios(entireList: List<AudioMeta>): List<AudioMeta> {
        val randomList = mutableListOf<AudioMeta>()
        var totalDuration = 0F
        while (totalDuration < 30F) {
            val randomAudio = entireList.filter { it.type == "wav" }.random()
            if (randomAudio !in randomList) {
                randomList.add(randomAudio)
                totalDuration += randomAudio.duration
            }
        }
        return randomList.toList()
    }

    private fun getAudioMeta(position: Int): AudioMeta? {
        return audioMetaList.value?.get(position)
    }

    fun observeAudioMetaList(): LiveData<List<AudioMeta>> {
        return audioMetaList
    }

    fun updateAudioMetaList(context: Context) {
        fetchDone.value = false
        val list = freesoundList.value?.let { randomSelectAudios(it.toList()) } ?: listOf()

        if (isLocal.value == true) {
            audioMetaList.value = list
            fetchDone.value = true
        } else {
            // only delete files and download if not in local environment
            val existFileNames = mutableListOf<String>()
            getSubFolder(context, "audios").listFiles()?.forEach { oldFile ->
                if (oldFile.name in list.map { it.filename }) {
                    existFileNames.add(oldFile.name)
                } else {
//                    deleteFile(oldFile)
//                    Log.d(javaClass.simpleName, "XXX ${oldFile.name} deleted.")
                }
            }

            val downloadedAudioList = mutableListOf<AudioMeta>()
            // only download if audio file not exist
            list.forEach { newAudio ->
                if (newAudio.filename in existFileNames) {
                    downloadedAudioList.add(newAudio)
                    audioMetaList.postValue(downloadedAudioList)
                    fetchDone.postValue(list.size == downloadedAudioList.size)
                } else {
                    storageHelp.downloadAudio(
                        newAudio, getAudioFile(context, newAudio)
                    ) {
                        downloadedAudioList.add(newAudio)
                        audioMetaList.postValue(downloadedAudioList)
                        fetchDone.postValue(list.size == downloadedAudioList.size)
//                        Log.d(
//                            javaClass.simpleName,
//                            "XXX downloadSuccess function ${audioMetaList.value?.size}"
//                        )
                    }
                }
            }
        }
    }

    fun moveAudio(from: Int, to: Int) {
        val fromAudio = getAudioMeta(from)
        val newList = audioMetaList.value?.toMutableList()
        if (fromAudio == null || newList.isNullOrEmpty()) {
            return
        }
        newList.removeAt(from)
        newList.add(to, fromAudio)
        audioMetaList.value = newList!!
    }


    // concat wav files
    private val mergeWav = MergeAudios()
    fun localMerge(context: Context, localFiles: List<Int>, outputFile: String) {
        mergeWav.localMerge(context, localFiles, outputFile)
    }

    fun mergeWavFiles(wavFiles: List<String>, outputFile: String) {
        mergeWav.mergeWavFiles(wavFiles, outputFile)
    }

    // MusicMeta, mymusic fragment
    private val myMusicList = MutableLiveData<List<MusicMeta>>()

    fun observeMyMusicList(): LiveData<List<MusicMeta>> {
        return myMusicList
    }

    fun fetchMyMusicList() {
        if (isLocal.value != true) {
            fetchDone.value = false
            viewModelScope.launch(
                context = viewModelScope.coroutineContext
                        + Dispatchers.IO
            ) {
                try {
                    getCurrentUser()?.uid?.let {
                        dbHelp.fetchMusicMeta(myMusicList, it)
                    }
                    fetchDone.postValue(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addMusicMeta(musicTitle: String, uuid: String, duration: Float) {
        val currentUser = getCurrentUser()!!
        val musicMeta = MusicMeta(
            ownerName = currentUser.displayName ?: "unfounded",
            ownerEmail = currentUser.email ?: "unfounded",
            ownerUid = currentUser.uid,
            uuid = uuid,
            duration = duration,
            musicTitle = musicTitle,
        )
        dbHelp.addMusicMeta(musicMeta, myMusicList)
    }

    fun uploadMusic(file: File, uuid: String, musicTitle: String, duration: Float) {
        storageHelp.uploadMusicFile(
            file, uuid, musicTitle, duration
        ) { addMusicMeta(musicTitle, uuid, duration) }
    }

    fun deleteMusic(music: MusicMeta) {
        dbHelp.deleteMusicMeta(music, myMusicList)
        storageHelp.deleteMusicFile(music.uuid)
    }

    fun playMusicFromUri(music: MusicMeta, context: Context) {
        storageHelp.playMusicFromUri(
            music.uuid, player, context, isPlaying
        ) { updateNowPlaying(music.musicTitle) }
    }

    fun updateMusicTitle(music: MusicMeta, newMusicTitle: String) {
        dbHelp.updateMusicTitle(music, myMusicList, newMusicTitle, nowPlaying)
    }
}