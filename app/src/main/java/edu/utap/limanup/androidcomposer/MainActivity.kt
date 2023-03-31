package edu.utap.limanup.androidcomposer

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.utap.limanup.androidcomposer.auth.AuthInit
import edu.utap.limanup.androidcomposer.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var player: MediaPlayer
    val userModifyingSeekBar = AtomicBoolean(false)

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateUser()
            Log.d(
                javaClass.simpleName, "XXX sign in success ${result.resultCode}, " +
                        "user: ${viewModel.getCurrentUser()?.displayName}"
            )
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            Log.d(javaClass.simpleName, "XXX sign in failed ${result.resultCode}")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout for the layout we created
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // bottom navigation bar
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_composer, R.id.navigation_mymusic)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // check network status
        checkConnection()

        // Initialize user sign in
        AuthInit(viewModel, signInLauncher)

        // reference player
        player = viewModel.player
        val playPauseButton = binding.playPauseButton
        playPauseButton.setOnClickListener {
            if (player.duration > 0) {
                if (player.isPlaying) {
                    player.pause()
                    viewModel.isPlaying.value = false
                } else {
                    player.start()
                    viewModel.isPlaying.value = true
                }
            }
        }

        viewModel.isPlaying.observe(this) {
            if (it) {
                playPauseButton.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            } else {
                playPauseButton.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }

        viewModel.observeNowPlaying().observe(this) {
            binding.nowPlayingSongName.text = it
        }

        viewModel.navToMyMusic.observe(this) {
            if (viewModel.navToMyMusic.value!! > 0) {
                binding.navView.selectedItemId = R.id.navigation_mymusic
            }
        }

        // when user modify seekbar
        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                // set userModifyingSeekBar to true only during tracking touch
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    userModifyingSeekBar.set(true)
                }

                /**
                 * update to seekbar progress after stop tracking touch
                 * to avoid the visual jitter
                 */
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    player.seekTo(seekBar.progress)
                    userModifyingSeekBar.set(false)
                }
            }
        )

        val millsec = 50L
        launch {
            displayTime(millsec)
        }

    }


    // Inflate the top menu for user sign out
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    // Handle user sign out action
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out -> {
                viewModel.signoutUser()
                AuthInit(viewModel, signInLauncher)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass.simpleName, "XXX MainActivity onDestroy")

        cancel()

        // delete files in audio folder
        val folder = viewModel.getSubFolder(this, "audios")
        folder.listFiles()?.forEach { viewModel.deleteFile(it) }
    }

    /**
     * An independent thread to manage seekbar
     * This coroutine should not modify any data accessed
     * by the main thread (it can read property values)
     */
    private suspend fun displayTime(millsec: Long) {
        // While the coroutine is running and has not been canceled by its parent
        while (coroutineContext.isActive) {
            // only update when player is playing
            if (player.isPlaying) {
                val duration = player.duration
                val timePlayed = player.currentPosition
                val timeLeft = duration - timePlayed
                binding.timePlayed.text = viewModel.convertTime(timePlayed)
                binding.timeLeft.text = viewModel.convertTime(timeLeft)
                binding.seekBar.max = duration
                if (!userModifyingSeekBar.get()) binding.seekBar.progress = timePlayed
            }
            // reset to starting position
            if (viewModel.observeNowPlaying().value == "") {
                binding.timePlayed.text = viewModel.convertTime(0)
                binding.timeLeft.text = viewModel.convertTime(0)
                binding.seekBar.progress = 0
            }

            // a delay so that this thread does not consume too much CPU
            delay(millsec)
        }
    }

    private fun checkConnection() {
        val cm: ConnectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connection = cm.activeNetwork != null &&
                cm.getNetworkCapabilities(cm.activeNetwork) != null
        Log.d(javaClass.simpleName, "XXX check network status: $connection")
        viewModel.isLocal.value = !connection
    }

}