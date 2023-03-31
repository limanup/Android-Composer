package edu.utap.limanup.androidcomposer.ui.mymusic

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import edu.utap.limanup.androidcomposer.MainViewModel
import edu.utap.limanup.androidcomposer.R
import edu.utap.limanup.androidcomposer.databinding.FragmentLayoutBinding
import edu.utap.limanup.androidcomposer.model.MusicMeta

class MyMusicFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentLayoutBinding? = null
    private lateinit var player: MediaPlayer

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Log.d(javaClass.simpleName, "XXX MyMusicFragment onCreateView")

//        viewModel.fetchMyMusicList()
        binding.newMusicTitleET.visibility = View.GONE
        binding.newMusicButton.visibility = View.GONE

        /**
         * One time upload audiometa
         */
//        fun getJsonDataFromAsset(context: Context, fileName: String): String? {
//            val jsonString: String
//            try {
//                jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
//            } catch (ioException: IOException) {
//                ioException.printStackTrace()
//                return null
//            }
//            return jsonString
//        }
//
//        val jsonFileString = getJsonDataFromAsset(requireContext(), "DBforFirebaseFiltered.json")
//
//        val gson = Gson()
//        val listAudioMetaType = object : TypeToken<List<AudioMeta>>() {}.type
//
//        var jsonList: List<AudioMeta> = gson.fromJson(jsonFileString, listAudioMetaType)
//        Log.d(javaClass.simpleName, "XXX list size: ${jsonList.size}")
//
//        jsonList.forEach {
//            dbHelp.oneTimeUploadAudioMeta(it)
//        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(R.string.title_mymusic)

        // MediaPlayer
        player = viewModel.player
        player.setOnCompletionListener {
            viewModel.isPlaying.value = player.isPlaying
        }

        // adapter
        val rv = binding.recyclerView
        val adapter =
            MyMusicAdapter(viewModel, requireContext())
            { musicMeta: MusicMeta -> editMusicTitle(musicMeta) }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(rv.context)

        viewModel.observeMyMusicList().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        // swipeRefreshLayout
        val swipe = binding.swipeRefreshLayout
        swipe.setOnRefreshListener {
            viewModel.fetchMyMusicList()
        }
        viewModel.fetchDone.observe(viewLifecycleOwner) {
            swipe.isRefreshing = !it
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(javaClass.simpleName, "XXX MyMusicFragment onDestroyView")
    }

    private fun editMusicTitle(musicMeta: MusicMeta) {
        binding.newMusicTitleET.visibility = View.VISIBLE
        binding.newMusicTitleET.setText(musicMeta.musicTitle)
        binding.newMusicButton.visibility = View.VISIBLE
        binding.newMusicButton.setText(R.string.update_music_title)
        binding.newMusicButton.setOnClickListener {
            val newMusicTitle = binding.newMusicTitleET.text.toString()
            if (newMusicTitle.isNotEmpty()) {
                viewModel.updateMusicTitle(musicMeta, newMusicTitle)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.music_title_cannot_empty,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }


}