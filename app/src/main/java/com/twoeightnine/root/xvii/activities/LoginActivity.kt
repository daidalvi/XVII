package com.twoeightnine.root.xvii.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.lg.Lg
import com.twoeightnine.root.xvii.managers.Session
import com.twoeightnine.root.xvii.model.Account
import com.twoeightnine.root.xvii.model.LongPollServer
import com.twoeightnine.root.xvii.utils.*
import io.realm.Realm
import java.util.regex.Pattern
import javax.inject.Inject

class LoginActivity : BaseActivity() {

    private lateinit var web: WebView
    private lateinit var rlLoader: RelativeLayout

    @Inject
    lateinit var apiUtils: ApiUtils

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        startPrimeGenerator(this)

        web = findViewById(R.id.webView)
        rlLoader = findViewById(R.id.rlLoader)
        App.appComponent?.inject(this)

        checkToken()
    }

    private fun checkToken() {
        val token = Session.token
        val uid = Session.uid
        if (!TextUtils.isEmpty(token)) {
            Lg.i("TOKEN = ${token.substring(0, 2)}...${token.substring(token.length - 2, token.length)}")
        } else {
            Lg.i("NO TOKEN")
        }
        if (token.isEmpty()) {
            toLogIn()
        } else {
            apiUtils.checkAccount(token, uid, { toPin() }, { toLogIn() }, { toPin() })
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun toLogIn() {
        web.visibility = View.GONE
        web.settings?.javaScriptEnabled = true
        CookieSyncManager.createInstance(web.context).sync()
        val man = CookieManager.getInstance()
        man.removeAllCookie()
        web.settings?.javaScriptCanOpenWindowsAutomatically = true
        web.webViewClient = ParsingWebClient()

        web.loadUrl(LOGIN_URL)
        if (!isOnline()) {
            showCommon(this, R.string.no_internet)
            finish()
            return
        }
        web.visibility = View.VISIBLE
    }

    private fun toPin() {
        updateAccount()
        toDialogs()
    }

    private fun toDialogs() {
        startActivity(Intent(this, RootActivity::class.java))
        startNotificationService(this)
        this.finish()
    }

    fun doneWithThis(url: String) {
        val token = extract(url, "access_token=(.*?)&")
        val uid: Int
        try {
            uid = extract(url, "user_id=(\\d*)").toInt()
        } catch (e: Exception) {
            onFailed(getString(R.string.invalid_user_id))
            return
        }
        Lg.i("LOGIN: token obtained ...${token.substring(token.length - 6)}")

        rlLoader.visibility = View.VISIBLE
        web.visibility = View.GONE

        Session.token = token
        Session.uid = uid

        apiUtils.checkAccount(
                Session.token,
                Session.uid,
                ::onChecked,
                ::onFailed,
                ::onLater
        )
    }

    private fun onLater(error: String) {
        goNext()
    }

    private fun onFailed(error: String) {
        showError(this, error)
        Session.token = ""
        restartApp(getString(R.string.auth_error))
    }

    private fun onChecked() {
        updateAccount()
        goNext()
    }

    private fun updateAccount() {
        val realm = Realm.getDefaultInstance()
        val account = Account()
        account.token = Session.token
        account.uid = Session.uid
        account.name = Session.fullName
        account.photo = Session.photo
        realm.beginTransaction()
        realm.copyToRealmOrUpdate(account)
        realm.commitTransaction()
    }

    private fun extract(from: String, regex: String): String {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(from)
        if (!matcher.find()) {
            return ""
        }
        return matcher.toMatchResult().group(1)
    }

    private fun goNext() {
        val intent = Intent()
        intent.setClass(applicationContext, RootActivity::class.java)
        Session.longPoll = LongPollServer("", "", 0)
        startActivity(intent)
        finish()
    }

    companion object {

        private val LOGIN_URL = "https://oauth.vk.com/authorize?" +
                "client_id=${App.APP_ID}&" +
                "scope=${App.SCOPE_ALL}&" +
                "redirect_uri=${App.REDIRECT_URL}&" +
                "display=touch&" +
                "v=${App.VERSION}&" +
                "response_type=token"

    }

    /**
     * handles redirect and calls token parsing
     */
    private inner class ParsingWebClient : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            rlLoader.visibility = View.GONE
            if (url.startsWith(App.REDIRECT_URL)) {
                doneWithThis(url)
            }
        }
    }
}