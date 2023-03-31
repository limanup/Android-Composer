package edu.utap.limanup.androidcomposer.ui.composer

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.limanup.androidcomposer.MainViewModel
import edu.utap.limanup.androidcomposer.R
import edu.utap.limanup.androidcomposer.api.localDB.LocalRepo
import edu.utap.limanup.androidcomposer.model.AudioMeta
import edu.utap.limanup.androidcomposer.databinding.FragmentLayoutBinding
import java.io.File
import java.util.UUID


class ComposerFragment :
    Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentLayoutBinding? = null
    private val localRepo = LocalRepo()
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
        Log.d(javaClass.simpleName, "XXX ComposerFragment onCreateView")

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Log.d(javaClass.simpleName, "XXX ComposerFragment onViewCreated")
        // set title
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(R.string.title_composer)

        // MediaPlayer
        player = viewModel.player

        // adapter
        val rv = binding.recyclerView
        val adapter = ComposerAdapter(
            viewModel,
            { audio: AudioMeta -> switchAudio(audio) },
            itemTouchHelper
        )
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(itemDecor)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(rv.context)
        itemTouchHelper.attachToRecyclerView(rv)

        binding.newMusicButton.setOnClickListener {
            createMusic(binding.newMusicTitleET.text.toString())
            binding.newMusicTitleET.text.clear()
        }

        viewModel.observeFreesoundList().observe(viewLifecycleOwner) {
            Log.d(javaClass.simpleName, "XXX observe freesoundList triggered")
            if (viewModel.audioMetaList.value.isNullOrEmpty()) {
                viewModel.updateAudioMetaList(requireContext())
            }
        }

        viewModel.observeAudioMetaList().observe(viewLifecycleOwner) { list ->
            Log.d(
                javaClass.simpleName, "XXX AudioMetaList observe len: " +
                        "${list.size}, ${list.map { it.name }}"
            )
            adapter.submitList(list)

            player.setOnCompletionListener {
                val nextAudio = viewModel.getNextAudio(viewModel.observeNowPlaying().value, list)
                if (nextAudio == null) {
                    viewModel.isPlaying.value = player.isPlaying
                } else {
                    switchAudio(nextAudio)
                }
            }
        }

        // swipeRefreshLayout
        val swipe = binding.swipeRefreshLayout
        swipe.setOnRefreshListener {
            viewModel.updateAudioMetaList(requireContext())
        }
        viewModel.fetchDone.observe(viewLifecycleOwner) {
            swipe.isRefreshing = !it
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        player.setOnCompletionListener {
            viewModel.isPlaying.value = player.isPlaying
        }
        Log.d(javaClass.simpleName, "XXX ComposerFragment onDestroyView")
    }


    // https://yfujiki.medium.com/drag-and-reorder-recyclerview-items-in-a-user-friendly-manner-1282335141e9
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
//                val adapter = recyclerView.adapter as ComposerAdapter
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition
                    viewModel.moveAudio(from, to)
//                adapter.notifyItemMoved(from, to)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                // Highlight the row while being selected.
                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                }
            }

        ItemTouchHelper(simpleItemTouchCallback)
    }


    private fun switchAudio(audio: AudioMeta) {
        player.reset()
        if (viewModel.isLocal.value == false) {
            player.setDataSource(viewModel.getAudioFile(requireContext(), audio).absolutePath)
        } else {
            val localAudioFiles = localRepo.fetchLocalAudioFiles()
            val audioRawId = localAudioFiles[audio.filename]
            audioRawId?.let {
                val afd: AssetFileDescriptor = resources.openRawResourceFd(audioRawId)
                player.setDataSource(afd)
            }
        }
        try {
            player.prepare()
            player.start()
            viewModel.isPlaying.value = player.isPlaying
            viewModel.updateNowPlaying(audio.name)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createMusic(musicTitle: String) {
        if (musicTitle.isNotEmpty()) {
            val folder = viewModel.getSubFolder(requireContext(), "mymusics")
            val uuid = UUID.randomUUID().toString()
            val file = File(folder, uuid)
            file.createNewFile()
            val myMusicPath = file.absolutePath
            Log.d(javaClass.simpleName, "XXX mymusicpath $myMusicPath")
            if (viewModel.isLocal.value == true) {
                val localAudioFiles = viewModel.audioMetaList.value!!.map { audio ->
                    localRepo.fetchLocalAudioFiles()[audio.filename]!!
                }
                viewModel.localMerge(requireContext(), localAudioFiles, myMusicPath)
            } else {
                viewModel.mergeWavFiles(getInputAudioFilePaths(), myMusicPath)
            }
            viewModel.uploadMusic(
                file, uuid, musicTitle,
                viewModel.getDuration(requireContext(), file)
            )
            viewModel.navToMyMusic.value = viewModel.navToMyMusic.value?.plus(1)
        } else {
            Toast.makeText(
                requireContext(),
                R.string.music_title_cannot_empty,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getInputAudioFilePaths(): List<String> {
        val returnList = mutableListOf<String>()
        for (element in viewModel.audioMetaList.value!!) {
            val file = viewModel.getAudioFile(requireContext(), element)
            returnList.add(file.absolutePath)
        }
        Log.d(javaClass.simpleName, "XXX returnList: $returnList")
        return returnList
    }


}
