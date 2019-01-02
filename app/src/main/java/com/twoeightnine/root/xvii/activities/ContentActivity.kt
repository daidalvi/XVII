package com.twoeightnine.root.xvii.activities

import android.os.Bundle
import android.support.v4.app.Fragment
import com.twoeightnine.root.xvii.R

abstract class ContentActivity : BaseActivity() {

    abstract fun getLayoutId(): Int

    abstract fun getFragment(args: Bundle?): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        loadFragment(getFragment(intent.extras))
    }

    protected fun loadFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.flContainer, fragment)
                .commitAllowingStateLoss()
    }
}