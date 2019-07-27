package com.helwigdev.cwcompat

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.helwigdev.cwcompat.services.CWService
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext


/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

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

        mfa.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }

        if(prefs?.contains(PREF_MEMBERHASH) == true){
            password.isEnabled = false
            password_layout.hint = getString(R.string.loading)
            launch {
                val success = withContext(Dispatchers.IO){
                    CWService.initialize(this@LoginActivity).hasValidSession()
                }
                onSessionCheckResult(success)
            }
        }

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

        // Reset errors.
        site.error = null
        company_id.error = null
        username.error = null
        password.error = null
        mfa.error = null

        // Store values at the time of the login attempt.
        val siteStr = site.text.toString()
        val companyIdStr = company_id.text.toString()
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()
        val mfaStr = mfa.text.toString()

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

        if(mfa.visibility == View.VISIBLE && TextUtils.isEmpty(mfaStr)){
            //if it's visible, it's required
            mfa.error = getString(R.string.error_field_required)
            focusView = mfa
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

            launch {
                val result = withContext(Dispatchers.IO){
                    CWService.initialize(this@LoginActivity).login(siteStr,
                            companyIdStr, usernameStr, passwordStr, (mfa.visibility == View.VISIBLE), mfaStr)
                }
                onLoginResult(result)
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 1
    }

    private fun onLoginResult(result: JSONObject?){

        if (result != null && result.getBoolean("Success")) {
            val editor = prefs!!.edit()
            editor.putString(PREF_MEMBERHASH, result.getString("Hash"))
            editor.apply()
            launch {
                val success = withContext(Dispatchers.IO){
                    CWService.initialize(this@LoginActivity).hasValidSession()
                }
                onSessionCheckResult(success)//get member info
            }
        } else if(result != null && (result.getString("FailureType") == "multifactorauthentication")){
            if(mfa.visibility != View.VISIBLE){
                mfa.error = getString(R.string.mfa_required)
                mfa.visibility = View.VISIBLE
            } else {
                mfa.error = result.getString("FailureReason")
            }


            mfa.requestFocus()
        } else if(result != null){
            password.error = getString(R.string.auth_fail) + ": ${result.getString("FailureReason")}"
            password.requestFocus()
        } else {
            password.error = getString(R.string.no_data_from_server)
        }
        showProgress(false)
    }

    private fun onSessionCheckResult(result: JSONObject) {
        showProgress(false)

        if (result.has("id")) {//then we were successful
            runOnUiThread {
                //TODO remove this debugging bit
                Toast.makeText(applicationContext, "Existing valid auth found",Toast.LENGTH_LONG).show()
            }
            //start up the new activity
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            //kill this activity
            finish()
        } else {
            password.isEnabled = true
            password_layout.hint = getString(R.string.prompt_password)
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {

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

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}
