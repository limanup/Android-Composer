package edu.utap.limanup.androidcomposer.ui.mymusic

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.utap.limanup.androidcomposer.MainViewModel
import edu.utap.limanup.androidcomposer.databinding.ComposedMusicRowBinding
import edu.utap.limanup.androidcomposer.model.MusicMeta

class MyMusicAdapter(
    private val viewModel: MainViewModel,
    private val context: Context,
    private val editMusicTitle: (musicMeta: MusicMeta) -> Unit
) : ListAdapter<MusicMeta, MyMusicAdapter.VH>(Diff()) {
    class Diff : DiffUtil.ItemCallback<MusicMeta>() {
        // Item identity
        override fun areItemsTheSame(oldItem: MusicMeta, newItem: MusicMeta): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        // Item contents are the same, but the object might have changed
        override fun areContentsTheSame(oldItem: MusicMeta, newItem: MusicMeta): Boolean {
            return oldItem.uuid == newItem.uuid
                    && oldItem.firestoreID == newItem.firestoreID
                    && oldItem.ownerUid == newItem.ownerUid
                    && oldItem.ownerName == newItem.ownerName
                    && oldItem.ownerEmail == newItem.ownerEmail
                    && oldItem.musicTitle == newItem.musicTitle
                    && oldItem.duration == newItem.duration
                    && oldItem.timeStamp == newItem.timeStamp
        }
    }

    inner class VH(val rowBinding: ComposedMusicRowBinding) :
        RecyclerView.ViewHolder(rowBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val rowBinding = ComposedMusicRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return VH(rowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val musicMeta = currentList[position]
        holder.rowBinding.musicTitle.text = musicMeta.musicTitle
        holder.rowBinding.musicDuration.text =
            viewModel.convertTime(musicMeta.duration.toInt() * 1000)
        holder.rowBinding.root.setOnClickListener {
            viewModel.playMusicFromUri(musicMeta, context)
        }
        holder.rowBinding.editMusicTitleButton.setOnClickListener {
            editMusicTitle(musicMeta)
            Log.d(javaClass.simpleName, "XXX edit music title button")
        }
        holder.rowBinding.deleteMusicButton.setOnClickListener {
            viewModel.deleteMusic(musicMeta)
        }
    }
}