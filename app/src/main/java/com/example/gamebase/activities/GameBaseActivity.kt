package com.example.gamebase.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamebase.R
import com.example.gamebase.model.GameData
import com.example.gamebase.repository.GameRepository
import com.example.gamebase.viewmodel.GameGridAdapter
import com.example.gamebase.viewmodel.MainViewModel
import com.example.gamebase.viewmodel.MainViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
import com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList

class GameBaseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mainViewModel: MainViewModel
    private lateinit var gameList: ArrayList<GameData>
    private lateinit var newGame: MutableLiveData<GameData>

    private lateinit var  topAppBar: MaterialToolbar
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var addGameFAB: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_base)
        gameList = ArrayList()

        auth = FirebaseAuth.getInstance()

        val uid :String = auth.currentUser?.uid.toString()
        mDatabase = FirebaseDatabase.getInstance()
        mDbRef = mDatabase.getReference("GameBase")

        mDbRef.addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GameBaseActivity, "Database error!", Toast.LENGTH_LONG).show()
            }

            override fun onDataChange (snapshot: DataSnapshot) {
                gameList = ArrayList()
                for (row in snapshot.children) {
                    if (row.key == uid) {
                        for (innerRow in row.children) {
                            val newRow = innerRow.getValue(GameData::class.java)
                            gameList.add(newRow!!)
                        }
                    }
                }
                setAdapter(gameList)
            }
        })

        val repository = GameRepository()
        val viewModelFactory = MainViewModelFactory(repository)
        mainViewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
        val myAdapter = GameGridAdapter(newGameList(), mDbRef)

        topAppBar = findViewById(R.id.topAppBar)
        bottomAppBar = findViewById(R.id.bottomAppBar)
        addGameFAB = findViewById(R.id.addGameFAB)
        recyclerView = findViewById(R.id.recyclerViewGameList)
        recyclerView.apply {
            this.layoutManager = GridLayoutManager(this@GameBaseActivity, 2, LinearLayoutManager.VERTICAL, false)
            this.adapter = myAdapter
        }

        iconCheckboxControl() // Function for Icon Tint / Checkbox State Control due to dark mode related restart of this activity

        //bottomAppBar.setNavigationOnClickListener {}

        val searchItem = topAppBar.menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        val queryTextListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNotEmpty()) {
                    //myAdapter.filter.filter(newText)
                    val filtered = gameList.filter{s -> s.title.toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))} as ArrayList<GameData>
                    val newAdapter = GameGridAdapter(filtered, mDbRef)
                    recyclerView.adapter = newAdapter
                }
                return true }
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) { }
                return true
            }
        }
        searchView.setOnQueryTextListener(queryTextListener)

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.dark_mode -> {

                    if (this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    true
                }
                R.id.fingerprint -> {
                    menuItem.isChecked = !menuItem.isChecked
                    mainViewModel.isFinger = menuItem.isChecked
                    true
                }
                R.id.logout -> {
                    auth.signOut()
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    intentLogin.putExtra("isFinger", mainViewModel.isFinger)
                    startActivity(intentLogin)
                    finish()
                    true
                }
                else -> false
            }
        }

        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.favorite -> {
                    menuItem.isChecked = !menuItem.isChecked
                    menuItemIconTintControl(menuItem)
                    mainViewModel.isFavorite = menuItem.isChecked
                    val newAdapter = GameGridAdapter(newGameList(), mDbRef)
                    recyclerView.adapter = newAdapter
                    true
                }
                R.id.played -> {
                    menuItem.isChecked = !menuItem.isChecked
                    menuItemIconTintControl(menuItem)
                    mainViewModel.isPlayed = menuItem.isChecked
                    val newAdapter = GameGridAdapter(newGameList(), mDbRef)
                    recyclerView.adapter = newAdapter
                    true
                }
                else -> false
            }
        }

        addGameFAB.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Add New Game")

            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
            val titleInput = textInputLayout("Title", this)
            //val noteInput = textInputLayout("Note", this)

            linearLayout.addView(titleInput)
            //linearLayout.addView(noteInput)
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.gravity = Gravity.CENTER
            builder.setView(linearLayout)

            builder.setPositiveButton("Add") { _, _ ->
                val key = Date().time.toString()
                val title = titleInput.editText?.text.toString().toLowerCase(Locale.ROOT)
                val note  = "note" //noteInput.editText?.text.toString()

                if (!isAdded(title)) {

                    newGame = MutableLiveData(GameData(key = key, title = title, note = note))

                    mainViewModel.getGame(title.replace(' ', '-'))
                    mainViewModel.gameResponse.observe(this, Observer{ response->
                        if(response.isSuccessful){
                            //Log.d("Response", response.body()!!.description)
                            newGame.value = GameData(
                                key,
                                title,
                                genre = response.body()?.genres?.get(0)?.name,
                                releaseYear = response.body()?.released,
                                //studio = response.body()?.publishers?.get(0)?.name,
                                description = response.body()?.description,
                                note = note,
                                imageUrl = response.body()?.background_image,
                                favorite = false,
                                played = false
                            )
                        } else{
                            Log.e("Adding response failed", response.errorBody().toString())
                            newGame.value = GameData(key = key, title = title, note = note)
                        }
                    })
                    newGame.observe(this, Observer { game ->
                        mDbRef.child(uid).child(key).setValue(game)
                    })
                    Toast.makeText(applicationContext, "Added ${title.capitalize(Locale.ROOT)}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Game Already Added", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }
    }

    private fun setAdapter(gameList: ArrayList<GameData>){
        recyclerView.adapter = GameGridAdapter(gameList, mDbRef)
    }

    private fun textInputLayout(hint: String, context: Context): TextInputLayout {

        val layoutParams = LinearLayout.LayoutParams(800, 160 )
        layoutParams.setMargins(16, 24, 16, 16)

        val textInputLayout = TextInputLayout(context, null, R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox)
        textInputLayout.hint = hint
        textInputLayout.endIconMode = END_ICON_CLEAR_TEXT
        textInputLayout.boxBackgroundMode = BOX_BACKGROUND_OUTLINE
        textInputLayout.setBoxCornerRadii(5F, 5F, 5F, 5F)

        val textInputEditText = TextInputEditText(textInputLayout.context)
        textInputEditText.layoutParams = layoutParams
        textInputLayout.addView(textInputEditText, layoutParams)

        return textInputLayout
    }

    private fun iconCheckboxControl() {
        val b = (this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        //bottomAppBar.menu.getItem(0).isChecked = mainViewModel.isFavorite
        //bottomAppBar.menu.getItem(1).isChecked = mainViewModel.isPlayed
        topAppBar.menu.getItem(1).isChecked = b
        topAppBar.menu.getItem(2).isChecked = mainViewModel.isFinger
        menuItemIconTintControl(bottomAppBar.menu.getItem(0))
        menuItemIconTintControl(bottomAppBar.menu.getItem(1))
        if (b) {
            bottomAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_34))
            topAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_34))
        }
    }

    private fun newGameList(): ArrayList<GameData> {
        val f = gameList.filter{s -> s.favorite}
        val p = gameList.filter{s -> s.played}
        val fp = gameList.filter{s -> s.favorite || s.played}
        return if (mainViewModel.isFavorite && mainViewModel.isPlayed) fp as ArrayList<GameData>
        else if (mainViewModel.isFavorite) f as ArrayList<GameData>
        else if (mainViewModel.isPlayed) p as ArrayList<GameData>
        else gameList
    }

    private fun menuItemIconTintControl(menuItem: MenuItem)
    {
        if (menuItem.isChecked)
            menuItem.icon.alpha = 255
        else
            menuItem.icon.alpha = 127
    }

    private fun isAdded(title: String): Boolean {
        var r = false
        for (game in gameList) {
            if (game.title.equals(title, ignoreCase = true)) r = true
        }
        return r
    }

}