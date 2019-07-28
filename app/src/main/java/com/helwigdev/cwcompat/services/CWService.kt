package com.helwigdev.cwcompat.services

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import org.json.JSONArray
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.NoSuchElementException


object CWService {
    private var initialized = false
    lateinit var prefs: SharedPreferences

    val PREF_SITE = "prefsite"
    val PREF_COMPANY_ID = "prefcompanyid"
    val PREF_USERNAME = "prefusername"
    val PREF_MEMBERHASH = "prefmemberhash"
    val PREF_FIRSTNAME = "preffirstname"
    val PREF_LASTNAME = "preflastname"
    val PREF_FULLNAME = "preffullname"
    val PREF_DEFAULTEMAIL = "prefdefaultemail"
    val PREF_PHOTODOWNLOAD = "prefphotodownload"

    val PREFS_FILENAME = "com.helwigdev.cwcompat.prefs"

    lateinit var mSite:String
    lateinit var mUsername:String
    lateinit var mCompanyId:String
    lateinit var mMemberHash:String

    var scheduleTypes: MutableMap<Int, ScheduleType> = mutableMapOf()

    lateinit var httpClient: OkHttpClient

    fun initialize(context: Context): CWService{
        if(!initialized) {
            this.prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            mSite = prefs.getString(PREF_SITE, "na.myconnectwise.net")!!
            mUsername = prefs.getString(PREF_USERNAME, "")!!
            mCompanyId = prefs.getString(PREF_COMPANY_ID, "")!!
            mMemberHash = prefs.getString(PREF_MEMBERHASH, "")!!
            val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
            httpClient = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build()
        }

        initialized = true
        return this
    }

    //LOGIN
    suspend fun login(site: String, companyId: String,
              username: String, password: String,
              shouldMFa: Boolean, mfa: String): JSONObject{

        val reqUrl = "https://api-$site/v2019_4/login/login.aspx?response=json"

        val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username",username)
                .addFormDataPart("password",password)
                .addFormDataPart("companyname",companyId)
        if(shouldMFa){
            requestBodyBuilder.addFormDataPart("auth_pin",mfa)
        }

        val requestBody = requestBodyBuilder.build()

        val request = Request.Builder()
                .url(reqUrl)
                .post(requestBody)
                .build()

        val response = GlobalScope.async { httpClient.newCall(request).execute() }
        val result = response.await()
        val cookies = response.await().header("Set-Cookie")
        val resultStr = response.await().body()?.string() ?: return JSONObject()

        prefs.edit().putString(PREF_SITE, site).putString(PREF_USERNAME, username).apply()
        mSite = site
        mUsername = username

        Log.d("CWService",resultStr)
        return JSONObject(resultStr)

    }

    //USER METADATA
    suspend fun hasValidSession(): JSONObject{
        mSite = prefs!!.getString(PREF_SITE, "na.myconnectwise.net")!!
        mUsername = prefs!!.getString(PREF_USERNAME, "")!!

        val reqUrl = "https://api-$mSite/v4_6_release/apis/3.0/system/info/members/$mUsername"

        val request = Request.Builder()
                .url(reqUrl)
                .build()

        val response = GlobalScope.async { httpClient.newCall(request).execute() }
        val result = response.await().body()?.string() ?: return JSONObject()
        val retval = JSONObject(result)

        try{
            val editor = prefs.edit()
            editor.putString(PREF_FIRSTNAME, retval.getString("firstName"))
            editor.putString(PREF_LASTNAME, retval.getString("lastName"))
            editor.putString(PREF_FULLNAME, retval.getString("fullName"))
            editor.putString(PREF_DEFAULTEMAIL, retval.getString("defaultEmail"))
            editor.putString(PREF_PHOTODOWNLOAD, retval.getJSONObject("photo")
                    .getJSONObject("_info")
                    .getString("documentDownload_href"))
            editor.apply()
        } catch (e: Exception){
            e.printStackTrace()
        }

        Log.d("CWService",retval.toString())
        return retval

    }

    suspend fun getUserThumbnail(): Bitmap{
        if(!prefs.contains(PREF_PHOTODOWNLOAD)){throw NoSuchElementException()}
        val reqUrl = prefs.getString(PREF_PHOTODOWNLOAD,"")

        val request = Request.Builder()
                .url(reqUrl!!)
                .build()

        val response = GlobalScope.async { httpClient.newCall(request).execute() }

        if (!response.await().isSuccessful) throw IOException("Unexpected code ${response.await()}")

        val inputStream = response.await().body()?.byteStream()

        return BitmapFactory.decodeStream(inputStream)
    }


    //SCHEDULE OPERATIONS
    suspend fun getScheduleEntriesForDate(date: Date): JSONArray{
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")

        val tomorrow = format.format(date) + "T23:59:59Z"
        val today = format.format(date) + "T00:00:00Z"
        //TODO order by start date ascending(?)
        val reqUrl = "https://api-$mSite/v4_6_release/apis/3.0/schedule/entries" +
                "?conditions=member/identifier=\"" + mUsername + "\" and dateStart > [" + today +
                "] and dateStart < [" + tomorrow + "]&orderby=dateStart asc"

        val request = Request.Builder()
                .url(reqUrl)
                .build()

        val response = GlobalScope.async { httpClient.newCall(request).execute() }
        if (!response.await().isSuccessful) throw IOException("Unexpected code ${response.await()}")

        val resultStr = response.await().body()?.string() ?: return JSONArray()
        Log.d("CWService",resultStr)

        return JSONArray(resultStr)
    }

    suspend fun getScheduleEntryType(id: Int, typeHref:String):ScheduleType{
        if(scheduleTypes.containsKey(id)){return scheduleTypes[id]!!}

        val request = Request.Builder()
                .url(typeHref)
                .build()

        val response = GlobalScope.async { httpClient.newCall(request).execute() }
        if (!response.await().isSuccessful) return ScheduleType(-1,"Error","Error")

        val resultStr = response.await().body()?.string() ?:  return ScheduleType(-1,"Error","Error")

        val retObj = JSONObject(resultStr)

        return ScheduleType(retObj.getInt("id"),retObj.getString("identifier"),retObj.getString("name"))


    }


    val userFullName:String
        get() = prefs.getString(PREF_FULLNAME,"") + ""

    val userEmail:String
        get() = prefs.getString(PREF_DEFAULTEMAIL,"") + ""

    data class ScheduleType(val id: Int,val identifier:String, val name:String)

}