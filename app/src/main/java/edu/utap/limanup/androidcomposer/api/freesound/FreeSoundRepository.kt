package edu.utap.limanup.androidcomposer.api.freesound

import edu.utap.limanup.androidcomposer.model.AudioMeta

class FreeSoundRepository(private val freeSoundApi: FreeSoundApi) {
    suspend fun getAllAudioSnippets(api_key: String) : List<AudioMeta> {
        return freeSoundApi.getAudioSnippets(api_key).results
    }
}