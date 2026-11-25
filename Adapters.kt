package com.meuiptv.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/** Faz o item "crescer" um pouco quando recebe foco do controle remoto. */
private fun View.focusZoom(scale: Float) {
    onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        val s = if (hasFocus) scale else 1f
        v.animate().scaleX(s).scaleY(s).setDuration(120).start()
    }
}

class TextAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Boolean = { false },
) : RecyclerView.Adapter<TextAdapter.VH>() {

    class VH(val text: TextView) : RecyclerView.ViewHolder(text)

    var items: List<String> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }

    var selected: Int = -1
        set(value) {
            val old = field
            field = value
            if (old in items.indices) notifyItemChanged(old)
            if (value in items.indices) notifyItemChanged(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_text, parent, false) as TextView
        tv.focusZoom(1.04f)
        return VH(tv)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.text.text = items[position]
        holder.text.isActivated = position == selected
        holder.text.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onClick(p)
        }
        holder.text.setOnLongClickListener {
            val p = holder.bindingAdapterPosition
            p != RecyclerView.NO_POSITION && onLongClick(p)
        }
    }
}

class ChannelAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit,
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.logo)
        val name: TextView = view.findViewById(R.id.name)
        val star: TextView = view.findViewById(R.id.star)
    }

    var items: List<Channel> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }

    var favoriteUrls: Set<String> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        v.focusZoom(1.08f)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = items[position]
        holder.name.text = ch.name
        holder.star.visibility = if (ch.url in favoriteUrls) View.VISIBLE else View.GONE
        if (ch.logo != null) {
            holder.logo.load(ch.logo) {
                crossfade(true)
                placeholder(R.drawable.ic_tv)
                error(R.drawable.ic_tv)
            }
        } else {
            holder.logo.setImageResource(R.drawable.ic_tv)
        }
        holder.itemView.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onClick(p)
        }
        holder.itemView.setOnLongClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onLongClick(p)
            true
        }
    }
}
