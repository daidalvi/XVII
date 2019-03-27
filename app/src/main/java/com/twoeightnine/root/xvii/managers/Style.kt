package com.twoeightnine.root.xvii.managers

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.DialogTitle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.utils.pxFromDp

object Style {
    private var STROKE = 3

    const val PHOTO_STUB_URL = "https://dummyimage.com/200x200/%s/%s.png"

    const val MAIN_TAG = "main"
    const val LIGHT_TAG = "light"
    const val DARK_TAG = "dark"
    const val API_20_TAG = "api20"

    private var isDay: Boolean = false

    private var defaultColor: Int = 0
    private var darkColor: Int = 0
    private var mainColor: Int = 0
    private var lightColor: Int = 0
    private var extraLightColor: Int = 0

    fun init(context: Context) {
        STROKE = pxFromDp(context, 2)
        mainColor = Prefs.color
        val other = getFromMain(mainColor)
        darkColor = other[0]
        lightColor = other[2]
        extraLightColor = other[3]
        defaultColor = ContextCompat.getColor(context, R.color.avatar)
        isDay = !Prefs.isNight
    }

    fun getFromMain(mainColor: Int = this.mainColor): IntArray { //[dark, main, light, extraLight]

        val dark = -120
        val light = 75 // #b0b0b0
        val extraLight = 25 // #e0e0e0


        val result = IntArray(4)

        result[0] = getOtherColor(mainColor, dark)
        result[1] = mainColor
        result[2] = getOtherColor(mainColor, light)
        result[3] = getOtherColor(mainColor, extraLight)

        return result
    }

    fun getPhotoStub(): String {
        val color = if (Prefs.isNight) mainColor else defaultColor
        val colorHex = String.format("%X", color).substring(2)
        return String.format(PHOTO_STUB_URL, colorHex, colorHex)
    }

    private fun getOtherColor(color: Int, coeff: Int): Int {
        val red = color and 0x00ffffff shr 16
        val green = color and 0x0000ffff shr 8
        val blue = color and 0x000000ff
        return Color.argb(255, shiftColor(red, coeff), shiftColor(green, coeff), shiftColor(blue, coeff))
    }

    private fun shiftColor(color: Int, shift: Int) =
            if (shift > 0) {
                255 - shift + color * shift / 255
            } else {
                color * -shift / 255
            }


    fun setStatusBar(activity: Activity, color: Int = mainColor) {
        if (ignore()) return

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color
    }

    private fun l(s: String) {
        Log.i("styler", s)
    }

    fun forToolbar(t: Toolbar) {
        if (ignore()) return
        t.setBackgroundColor(mainColor)
    }

    fun forEditText(et: EditText, tag: String) {
        //goddamn pre-lollipop
//        if (API_20_TAG == tag && !is21Sdk()) {
//            et.setTextColor(0xffffffff.toInt())
//        }
    }

    private fun forEditText(et: EditText) {
        val tag = et.tag
        if (tag == null || tag !is String) return
        forEditText(et, tag)
    }

    private fun getColorByTag(tag: String): Int {
        when (tag) {
            LIGHT_TAG -> return lightColor
            DARK_TAG -> return darkColor
        }
        return mainColor
    }

    fun forMessage(vg: ViewGroup, level: Int) {
        if (ignore()) return
        if (level % 2 == 0) {
            (vg.background as GradientDrawable)
                    .setColor(lightColor)
        } else {
            (vg.background as GradientDrawable)
                    .setColor(extraLightColor)
        }
    }

    @SuppressWarnings
    fun forDrawable(d: Drawable?, tag: String, changeStroke: Boolean = true) {
        if (ignore() || d == null) return

        val color = getColorByTag(tag)
        if (d is ShapeDrawable) {
            d.paint.color = color
        } else if (d is GradientDrawable) {
            d.setColor(color)
            if (changeStroke) {
                d.setStroke(STROKE, Color.WHITE)
            }
        } else if (d is ColorDrawable) {
            d.color = color
        } else if (d is VectorDrawable) {
            d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun forFrame(d: Drawable, tag: String = DARK_TAG) {
        if (ignore()) return
        val color = getColorByTag(tag)

        if (d is GradientDrawable) {
            d.setColor(Color.TRANSPARENT)
            d.setStroke(1, color)
        }
    }

    fun forViewGroupColor(vg: ViewGroup) {
        if (ignore()) return
        val tag = vg.tag
        if (tag == null || tag !is String) return

        vg.setBackgroundColor(getColorByTag(tag))
    }

    @SuppressWarnings
    fun forViewGroup(vg: ViewGroup, changeStroke: Boolean = true) {
        if (ignore()) return
        val tag = vg.tag
        if (tag == null || tag !is String) return

        if (API_20_TAG != tag) {
            val d = vg.background
            forDrawable(d, tag, changeStroke)
            vg.background = d
        }

    }

    fun forTabLayout(tl: TabLayout, tag: String = MAIN_TAG) {
        if (ignore()) return
        tl.setBackgroundColor(getColorByTag(tag))
    }

    fun forImageView(iv: ImageView, tag: String, changeStroke: Boolean = true) {
        if (ignore()) return
        val d = iv.drawable ?: return
        forDrawable(d, tag, changeStroke)
        iv.setImageDrawable(d)
    }

    private fun forImageView(iv: ImageView) {
        if (ignore()) return
        val tag = iv.tag
        if (tag == null || tag !is String) return
        val d = iv.drawable
        forDrawable(d, tag)
        iv.setImageDrawable(d)
    }

    fun forSwitch(s: Switch) {
        if (ignore()) return

        s.thumbDrawable.setColorFilter(mainColor, PorterDuff.Mode.SRC_ATOP)
        s.trackDrawable.setColorFilter(lightColor, PorterDuff.Mode.SRC_ATOP)
    }

    fun forFAB(fab: FloatingActionButton) {
        if (ignore()) return
        fab.backgroundTintList = ColorStateList.valueOf(mainColor)
    }

    fun forProgressBar(pb: ProgressBar) {
        if (ignore()) return

        pb.indeterminateDrawable.setColorFilter(darkColor, PorterDuff.Mode.MULTIPLY)
//        pb.progressTintList = ColorStateList.valueOf(mainColor)
    }

    fun forDialog(dialog: AlertDialog?) {
        if (dialog == null) return
        val context = App.context
        dialog.findViewById<View>(R.id.contentPanel)?.setBackgroundColor(ContextCompat.getColor(context, R.color.popup))
        dialog.findViewById<View>(R.id.buttonPanel)?.setBackgroundColor(ContextCompat.getColor(context, R.color.popup))
        dialog.findViewById<View>(R.id.topPanel)?.setBackgroundColor(ContextCompat.getColor(context, R.color.popup))
        dialog.findViewById<DialogTitle>(R.id.alertTitle)?.setTextColor(ContextCompat.getColor(context, R.color.main_text))
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(context, R.color.main_text))
        dialog.findViewById<Button>(android.R.id.button1)?.setTextColor(ContextCompat.getColor(context, R.color.other_text))
        dialog.findViewById<Button>(android.R.id.button2)?.setTextColor(ContextCompat.getColor(context, R.color.other_text))
    }

    private fun ignore(): Boolean {
        return isDay /*|| !is21Sdk()*/
    }

    fun forAll(vg: ViewGroup?, level: Int = 0) {
        if (ignore()) return
        if (vg == null) return
        forViewGroup(vg)
        for (i in 0 until vg.childCount) {
            val v = vg.getChildAt(i)
            var r = ""
            for (j in 0 until level) {
                r += "- "
            }
            l(r + v)
            when (v) {
                is Switch -> forSwitch(v)
                is FloatingActionButton -> forFAB(v)
                is EditText -> forEditText(v)
                is ImageView -> forImageView(v)
                is Toolbar -> forToolbar(v)
                is TabLayout -> forTabLayout(v)
                is ProgressBar -> forProgressBar(v)
                is ViewGroup -> {
                    forViewGroup(v)
                    forAll(v, level + 1)
                }

            }
        }
    }

}