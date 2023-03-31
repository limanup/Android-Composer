package edu.utap.limanup.androidcomposer.ui.composer

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.utap.limanup.androidcomposer.MainViewModel
import edu.utap.limanup.androidcomposer.databinding.AudioSnippetRowBinding
import edu.utap.limanup.androidcomposer.model.AudioMeta

class ComposerAdapter(
    private val viewModel: MainViewModel,
    private val clickToListen: (audio: AudioMeta) -> Unit,
    private val itemTouchHelper: ItemTouchHelper
) :
    ListAdapter<AudioMeta, ComposerAdapter.VH>(Diff()) {
    class Diff : DiffUtil.ItemCallback<AudioMeta>() {
        // Item identity
        override fun areItemsTheSame(oldItem: AudioMeta, newItem: AudioMeta): Boolean {
            return oldItem.id == newItem.id
        }

        // Item contents are the same, but the object might have changed
        override fun areContentsTheSame(oldItem: AudioMeta, newItem: AudioMeta): Boolean {
            return oldItem.id == newItem.id
                    && oldItem.duration == newItem.duration
                    && oldItem.type == newItem.type
                    && oldItem.name == newItem.name
        }
    }

    inner class VH(private val rowBinding: AudioSnippetRowBinding) :
        RecyclerView.ViewHolder(rowBinding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(holder: VH, position: Int) {
//            val audioMeta = viewModel.getAudioMeta(position)
            val audioMeta = currentList[position]
            audioMeta?.let { audio ->
                holder.rowBinding.root.setOnClickListener {
                    Log.d(javaClass.simpleName, "XXX clicked on ${audio.name} ${audio.filename}")
                    clickToListen(audio)
                }
                holder.rowBinding.audioSnippetTitle.text = audio.name
                holder.rowBinding.audioSnippetDuration.text =
                    viewModel.convertTime((audio.duration.toInt() * 1000))
                holder.rowBinding.audioReorderButton.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        Log.d(javaClass.simpleName, "XXX reorder button on touch")
                        itemTouchHelper.startDrag(holder)
                    }
                    return@setOnTouchListener true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val rowBinding = AudioSnippetRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return VH(rowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, position)
    }


}