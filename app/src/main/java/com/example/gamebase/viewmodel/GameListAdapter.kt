package com.example.gamebase.viewmodel

import android.app.AlertDialog
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.gamebase.R
import com.example.gamebase.model.GameData
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

class GameListAdapter(
        var games: ArrayList<GameData>,
        var databaseReference: DatabaseReference,
        var isListModeOn: Boolean
    ): RecyclerView.Adapter<GameListAdapter.GameHolder>(), Filterable {
    private lateinit var view: View

    inner class GameHolder(view: View): RecyclerView.ViewHolder(view) {

        var card: MaterialCardView = itemView.findViewById(R.id.game_card)
        var boxArt: ImageView = itemView.findViewById(R.id.game_box_art)
        var title: TextView = itemView.findViewById(R.id.game_title)
        var producer: TextView = itemView.findViewById(R.id.game_producer)
        var favorite: Chip = itemView.findViewById(R.id.add_to_favorite)
        var played: Chip = itemView.findViewById(R.id.add_to_played)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameHolder {
        view = LayoutInflater.from(parent.context).inflate(
            R.layout.fragment_item_card_view, parent,false )
        return GameHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: GameHolder, position: Int) {
        val game = games[position]

        holder.title.text = game.title.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT)
        holder.producer.text = game.studio
        holder.played.isChecked = game.played
        holder.favorite.isChecked = game.favorite
        if(games[position].imageUrl != "") {
            Picasso.with(holder.itemView.context).load(games[position].imageUrl).into(holder.boxArt)
        }

        holder.played.setOnCheckedChangeListener { _, isChecked ->
            databaseReference.child(FirebaseAuth.getInstance().currentUser!!.uid).child(game.key).child("played").setValue(isChecked)
        }
        holder.favorite.setOnCheckedChangeListener { _, isChecked ->
            databaseReference.child(FirebaseAuth.getInstance().currentUser!!.uid).child(game.key).child("favorite").setValue(isChecked)
        }
        holder.card.setOnLongClickListener {

            val builder = AlertDialog.Builder(view.context)
            builder.setTitle("Delete Game?")
            builder.setMessage("Do you really want to delete this game?")

            builder.setPositiveButton("Yes") { _, _ ->
                databaseReference.child(FirebaseAuth.getInstance().currentUser!!.uid).child(games[position].key).setValue(null)
            }
            builder.setNegativeButton("No") { _, _ -> }
            builder.show()

            true
        }
    }

    override fun getItemCount(): Int {
       return games.size
    }

    override fun getFilter(): Filter {
        return filter
    }

    private val filter: Filter = object : Filter() {
        var filteredList: MutableList<GameData> = arrayListOf()
        override fun performFiltering(constraint: CharSequence): FilterResults {

            if (constraint.isEmpty()) {
                filteredList.addAll(games)
            } else {
                val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim { it <= ' ' }
                for (game in games) {
                    if (game.title.toLowerCase(Locale.ROOT).contains(filterPattern)) {
                        filteredList.add(game)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
            filteredList = filterResults.values as MutableList<GameData>
            notifyDataSetChanged()
        }
    }



}


