package com.helwigdev.cwcompat

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.widget.ExpandableListView





class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mDrawerLayout: DrawerLayout? = null
    var mMenuAdapter: ExpandableListAdapter? = null
    var expandableList: ExpandableListView? = null
    var listDataHeader: List<ExpandedMenuModel>? = null
    var listDataChild: HashMap<ExpandedMenuModel, List<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        mDrawerLayout = drawer_layout
        expandableList = navigationmenu
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
/*
        if (navigationView != null) {
            setupDrawerContent(navigationView)
        }*/

        prepareListData()
        mMenuAdapter = ExpandableListAdapter(this, listDataHeader, listDataChild, expandableList)

        // setting list adapter
        expandableList?.setAdapter(mMenuAdapter)

        expandableList?.setOnChildClickListener(object : ExpandableListView.OnChildClickListener {
            override fun onChildClick(expandableListView: ExpandableListView, view: View, i: Int, i1: Int, l: Long): Boolean {
                //Log.d("DEBUG", "submenu item clicked");
                return false
            }
        })
        expandableList?.setOnGroupClickListener(object : ExpandableListView.OnGroupClickListener {
            override fun onGroupClick(expandableListView: ExpandableListView, view: View, i: Int, l: Long): Boolean {
                //Log.d("DEBUG", "heading clicked");
                return false
            }
        })
    }

    private fun prepareListData() {
        var tListDataHeader: List<ExpandedMenuModel> = ArrayList()
        var tListDataChild: HashMap<ExpandedMenuModel, List<String>> = HashMap()

        val item1 = ExpandedMenuModel()
        item1.setIconName("heading1")
        item1.setIconImg(android.R.drawable.ic_delete)
        // Adding data header
        tListDataHeader += item1


        val item2 = ExpandedMenuModel()
        item2.setIconName("heading2")
        item2.setIconImg(android.R.drawable.ic_delete)
        tListDataHeader += item2

        val item3 = ExpandedMenuModel()
        item3.setIconName("heading3")
        item3.setIconImg(android.R.drawable.ic_delete)
        tListDataHeader+= item3

        // Adding child data
        val heading1 = ArrayList<String>()
        heading1.add("Submenu of item 1")

        val heading2 = ArrayList<String>()
        heading2.add("Submenu of item 2")
        heading2.add("Submenu of item 2")
        heading2.add("Submenu of item 2")

        tListDataChild[tListDataHeader[0]] = heading1// Header, Child data
        tListDataChild[tListDataHeader[1]] = heading2

        listDataHeader = tListDataHeader
        listDataChild = tListDataChild

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
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
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
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

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
