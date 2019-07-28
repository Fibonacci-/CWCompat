package com.helwigdev.cwcompat

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.helwigdev.cwcompat.services.CWService
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CoroutineScope, ScheduleFragment.OnListFragmentInteractionListener {



    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "TODO create ticket dialog", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        navView.menu.getItem(0).isChecked = true
        onNavigationItemSelected(navView.menu.getItem(0))

        launch {
            val pic = withContext(Dispatchers.IO){
                CWService.initialize(this@MainActivity).getUserThumbnail()
            }
            setUserInfo(pic)
        }
    }

    private fun setUserInfo(bitmap: Bitmap){
        Log.d("MainActivity","Got profile bitmap")
        iv_profile.setImageBitmap(bitmap)
        tv_fullname.text = CWService.initialize(this).userFullName
        tv_email.text = CWService.initialize(this).userEmail
    }

    private fun onSchedulesLoaded(array:JSONArray){
        Log.d("MainActivity", "Got array: $array")
        if(array.length() == 0){
            val o = JSONObject()
            o.put("name","No schedule entries")
            o.put("id", -1)
            array.put(o)
        }
        replaceFragmenty(fragment = ScheduleFragment.newInstance(array),
                        allowStateLoss = true,
                        containerViewId = cl_content.id)
        pb_fragmentLoading.visibility = View.GONE

    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        pb_fragmentLoading.visibility = View.VISIBLE
        when (item.itemId) {
            R.id.nav_calendar -> {
                title = getString(R.string.schedule)
                // Handle the calendar action
                Toast.makeText(this,"Selected schedule",Toast.LENGTH_LONG).show()
                launch {
                    val schedules = withContext(Dispatchers.IO){
                        CWService.initialize(this@MainActivity).getScheduleEntriesForDate(Date())
                    }
                    onSchedulesLoaded(schedules)
                }
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(item: JSONObject) {
        Toast.makeText(this,"Clicked item " + item.getInt("id"),Toast.LENGTH_LONG).show()//To change body of created functions use File | Settings | File Templates.
    }

}
