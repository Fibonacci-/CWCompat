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
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import okhttp3.OkHttpClient
import java.io.IOException


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

        if(prefs?.contains(PREF_MEMBERHASH) == true){
            password.isEnabled = false
            password_layout.hint = getString(R.string.loading)
            val validateLogin = CheckMemberHash(prefs!!.getString(PREF_MEMBERHASH,"")!!)
            validateLogin.execute(null as Void?)
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

        @SuppressLint("ApplySharedPref")
        override fun doInBackground(vararg params: Void): Boolean? {

            try {
                val reqUrl = "https://$mSite/v4_6_release/login/login.aspx"

                val client = OkHttpClient()

                var requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("username",mUsername)
                        .addFormDataPart("password",mPassword)
                        .addFormDataPart("companyname",mCompanyId)
                        .build()

                var request = Request.Builder()
                        .url(reqUrl)
                        .post(requestBody)
                        .build()

                val response = client.newCall(request).execute()
                resultStr = response.body()?.string()

                Log.d("URL Response",resultStr)


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
                editor.commit()
                return true
            }

        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                runOnUiThread {
                    //TODO remove this debugging bit
                    Toast.makeText(applicationContext, "Authenticated successfully:\n$resultStr",Toast.LENGTH_LONG).show()
                }
                //start up the new activity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                //kill this activity
                finish()
            } else {
                password.error = getString(R.string.auth_fail) + ": server returned $resultStr"
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    inner class CheckMemberHash internal constructor(private val mMemberHash: String) : AsyncTask<Void, Void, Boolean>() {

        var resultStr: String? = null

        @SuppressLint("ApplySharedPref")
        override fun doInBackground(vararg params: Void): Boolean? {
            try {
                val mSite = prefs?.getString(PREF_SITE, "na.myconnectwise.net")
                val mUsername = prefs?.getString(PREF_USERNAME,"")
                val mCompanyId = prefs?.getString(PREF_COMPANY_ID, "")

                //TODO use an endoint with a smaller body
                val reqUrl = "https://$mSite/v4_6_release/apis/3.0/service/tickets"


               /* val (request, response, result) = reqUrl.httpGet()
                        .header(Pair("Cookie", "companyname=$mCompanyId,memberhash=$mMemberHash,MemberID=$mUsername"))
                        .responseString()


                Log.d("URL Response",result.get())*/


                val client = OkHttpClient()

                val request = Request.Builder()
                        .url(reqUrl)
                        .addHeader("Cookie", "companyname=$mCompanyId")
                        .addHeader("Cookie","memberHash=$mMemberHash")
                        .addHeader("Cookie","MemberID=$mUsername")
                        .build()

                val response = client.newCall(request).execute()

                val responseStr = response.body()?.string()
                Log.d("AuthCheckCode",response.code().toString())
                Log.d("AuthCheckBody",responseStr)

                return response.isSuccessful



            } catch (e: InterruptedException) {
                return false
            }

        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
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

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }
}
