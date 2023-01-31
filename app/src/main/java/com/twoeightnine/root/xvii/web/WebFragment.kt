/*
 * xvii - messenger for vk
 * Copyright (C) 2021  TwoEightNine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.twoeightnine.root.xvii.web

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.view.View
import android.webkit.*
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.base.BaseFragment
import com.twoeightnine.root.xvii.base.FragmentPlacementActivity
import com.twoeightnine.root.xvii.lg.L
import com.twoeightnine.root.xvii.utils.BrowsingUtils
import global.msnthrp.xvii.uikit.extensions.applyBottomInsetPadding
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_web.*
import kotlinx.android.synthetic.main.fragment_web.webView


private const val MOBILE_USERAGENT = "Mozilla/5.0 (Linux; Android 12; Tab 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

class WebFragment : BaseFragment() {

    private val url by lazy { arguments?.getString(ARG_URL) }
    private val title by lazy { arguments?.getString(ARG_TITLE) }

    override fun getLayoutId() = R.layout.fragment_web

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {

        // WebView.setWebContentsDebuggingEnabled(true)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.flush()
        // if(!cookieManager.acceptCookie()){}
        //CookieSyncManager.createInstance(this@WebFragment.activity).sync()

        webView?.apply {

            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setAppCacheEnabled(false)
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE)
            setWebChromeClient(WebChromeClient())
            settings.userAgentString = MOBILE_USERAGENT
            settings.setDatabaseEnabled(true)
            settings.setAllowUniversalAccessFromFileURLs(true)


            webViewClient = object : WebViewClient() {
                @TargetApi(Build.VERSION_CODES.N)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()?: ""
                    if(url.startsWith("http://") || url.startsWith("https://")) {
                        view.loadUrl(url)
                    }else{
                        //Intent intent = Intent(Intent.ACTION_VIEW, request.url)
                        //view.context.startActivity(intent)
                        BrowsingUtils.openUriIntent(view.context, request.url)
                    }

                    return true
                }
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    // do your handling codes here, which url is the requested url
                    // probably you need to open that url rather than redirect:

                    view.loadUrl(url)
                    return true // then it is not handled by default action
                }
                override fun onReceivedError(
                    view: WebView?, errorCode: Int,
                    description: String?, failingUrl: String?
                ) {
                }
            }

        }
        url?.also(webView::loadUrl)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        xviiToolbar.title = title ?: url ?: getString(R.string.app_name)
        webView.applyBottomInsetPadding()
    }

    companion object {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"

        fun newInstance(url: String, title: String = ""): WebFragment {
            val frag = WebFragment()
            frag.arguments = createArgs(url, title)
            return frag
        }

        fun createArgs(url: String, title: String = "") = Bundle().apply {
            putString(ARG_URL, url)
            putString(ARG_TITLE, if (title.isNotEmpty()) title else url)
        }
    }

}