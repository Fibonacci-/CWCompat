package com.helwigdev.cwcompat

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import kotlinx.android.synthetic.main.activity_login.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    var prefs: SharedPreferences? = null
    val PREFS_FILENAME = "com.helwigdev.cwcompat.prefs"

    val PREF_SITE = "prefsite"
    val PREF_COMPANY_ID = "prefcompanyid"
    val PREF_USERNAME = "prefusername"
    val PREF_MEMBERHASH = "prefmemberhash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initSavedTextValues()
        company_id.requestFocus()

        // Set up the login form.
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }

    }


    private fun initSavedTextValues(){
        prefs = this.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        site.setText(prefs!!.getString(PREF_SITE, getString(R.string.url_na_site)))
        company_id.setText(prefs!!.getString(PREF_COMPANY_ID, ""))
        username.setText(prefs!!.getString(PREF_USERNAME, ""))

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        site.error = null
        company_id.error = null
        username.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val siteStr = site.text.toString()
        val companyIdStr = company_id.text.toString()
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()

        //save values to preferences to be used next time
        val editor = prefs!!.edit()
        editor.putString(PREF_SITE, siteStr)
        editor.putString(PREF_COMPANY_ID, companyIdStr)
        editor.putString(PREF_USERNAME, usernameStr)
        editor.apply()

        var cancel = false
        var focusView: View? = null

        // Check for valid site
        if (TextUtils.isEmpty(siteStr)){
            site.error = getString(R.string.error_field_required)
        }

        // Check for valid company ID
        if (TextUtils.isEmpty(companyIdStr)){
            company_id.error = getString(R.string.error_field_required)
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if(TextUtils.isEmpty(passwordStr)){
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(usernameStr)) {
            username.error = getString(R.string.error_field_required)
            focusView = username
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(siteStr, companyIdStr, usernameStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        //TODO: Replace this with your own logic
        return password.length > 1
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }



    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(private val mSite: String, private val mCompanyId: String,
                                                   private val mUsername: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        var resultStr: String? = null

        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.



            try {
                // Simulate network access.
                //Thread.sleep(2000)

                val reqUrl = "https://$mSite/v4_6_release/login/login.aspx"
                val body = listOf("username" to mUsername, "password" to mPassword, "companyname" to mCompanyId)



                val (request, response, result) = Fuel.upload(reqUrl, parameters = body)
                        .dataParts { _, _ -> listOf() }
                        .responseString()

                Log.d("URL Response",result.get())

                resultStr = result.get()


            } catch (e: InterruptedException) {
                return false
            }

            //if result is null or contains fail, return false, otherwise, return true
            if(resultStr == null){
                return false
            } else if(resultStr!!.contains("FAIL",true)){
                return false
            } else {
                val editor = prefs!!.edit()
                editor.putString(PREF_MEMBERHASH, resultStr)
                editor.apply()
                return true
            }

        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                runOnUiThread {

                    Toast.makeText(applicationContext, "Authenticated successfully:\n$resultStr",Toast.LENGTH_LONG).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to authenticate:\n$resultStr",Toast.LENGTH_LONG).show()
                }
                password.error = getString(R.string.something_wrong) + ": server returned $resultStr"
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }
}
