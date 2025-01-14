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

package com.twoeightnine.root.xvii.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.background.longpoll.receivers.RestarterBroadcastReceiver
import com.twoeightnine.root.xvii.background.longpoll.services.NotificationService
import com.twoeightnine.root.xvii.chatowner.ChatOwnerFactory
import com.twoeightnine.root.xvii.chats.messages.chat.usual.ChatActivity
import com.twoeightnine.root.xvii.crypto.md5
import com.twoeightnine.root.xvii.lg.L
import com.twoeightnine.root.xvii.main.MainActivity
import global.msnthrp.xvii.uikit.extensions.SimpleBitmapTarget
import global.msnthrp.xvii.uikit.extensions.load
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.ResponseBody
import java.io.*
import java.text.DecimalFormat
import java.util.regex.Pattern


private const val REGEX_MENTION = "(\\[id\\d{1,9}\\|[^\\]]+\\]|#+[a-zA-Z0-9а-яА-Я(_)]{1,})"

fun isOnline(): Boolean {
    val cm = App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isOnline = cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
    L.tag("is online").log("$isOnline")
    return isOnline
}

fun showToast(context: Context?, message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (context == null) return

    val toast = Toast.makeText(context, message, duration)
    toast.view?.findViewById<TextView>(android.R.id.message)?.apply {
        typeface = SANS_SERIF_LIGHT
    }
    toast.show()
}

fun showToast(context: Context?, @StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) {
    showToast(context, context?.getString(text) ?: "", duration)
}

fun showError(context: Context?, text: String?) {
    showAlert(context, text)
}

fun rate(context: Context) {
    val uri = Uri.parse("market://details?id=${context.packageName}")
    val goToMarket = Intent(Intent.ACTION_VIEW, uri)
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    try {
        context.startActivity(goToMarket)
    } catch (e: ActivityNotFoundException) {
        BrowsingUtils.openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
    }

}

fun getBatteryLevel(context: Context): String {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batLevel: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return when {
        batLevel < 10 -> "0$batLevel"
        else -> batLevel.toString()
    }
}

fun showError(context: Context?, @StringRes text: Int) {
    showError(context, context?.getString(text))
}

fun showAlert(context: Context?, text: String?, onOkPressed: (() -> Unit)? = null) {
    if (context == null) return

    val dialog = AlertDialog.Builder(context)
            .setMessage(text)
            .setPositiveButton(R.string.ok) { _, _ ->
                onOkPressed?.invoke()
            }
            .create()
    dialog.show()
    dialog.stylize()
}

fun showConfirm(context: Context?, text: String, callback: (Boolean) -> Unit) {
    if (context == null) return

    val dialog = AlertDialog.Builder(context)
            .setMessage(text)
            .setPositiveButton(R.string.ok) { _, _ -> callback.invoke(true) }
            .setNegativeButton(R.string.cancel) { _, _ -> callback.invoke(false) }
            .create()
    dialog.show()
    dialog.stylize()
}

fun showWarnConfirm(context: Context?, text: String, positiveButton: String, callback: (Boolean) -> Unit) {
    if (context == null) return

    val dialog = AlertDialog.Builder(context)
            .setMessage(text)
            .setPositiveButton(positiveButton) { _, _ -> callback.invoke(true) }
            .setNegativeButton(R.string.cancel) { _, _ -> callback.invoke(false) }
            .create()
    dialog.show()
    dialog.stylize(warnPositive = true)
}

fun startNotificationService(context: Context) {
    try {
        NotificationService.launch(context)
    } catch (e: Exception) {
        L.tag("longpoll")
                .throwable(e)
                .log("unable to start service")
    }
}

fun startNotificationAlarm(context: Context) {
    val intent = Intent(context, RestarterBroadcastReceiver::class.java).apply {
        action = RestarterBroadcastReceiver.RESTART_ACTION
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val tenMinutes = 60 * 1000L * 10
    alarms.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + tenMinutes,
            tenMinutes, pendingIntent
    )
    L.tag("longpoll").log("alarm started")
}

inline fun launchActivity(context: Context?,
                          activityClass: Class<out AppCompatActivity>,
                          intentBlock: Intent.() -> Unit = {}) {
    context?.startActivity(Intent(context, activityClass).apply {
        intentBlock()
    })
}

fun copyToClip(text: String) {
    val clipboard = App.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.text = text
}

fun getSize(resources: Resources, bytes: Int): String {
    val twoDecimalForm = DecimalFormat("#.##")
    return when {
        bytes < 1024 -> {
            resources.getString(R.string.bytes, bytes)
        }
        bytes < 1048576 -> {
            resources.getString(R.string.kilobytes, twoDecimalForm.format(bytes.toDouble() / 1024))
        }
        else -> {
            resources.getString(R.string.megabytes, twoDecimalForm.format(bytes.toDouble() / 1048576))
        }
    }
}

fun streamToBytes(input: InputStream): ByteArray {
    try {
        val byteBuffer = ByteArrayOutputStream()
        val buffer = ByteArray(32768)
        var len: Int
        do {
            len = input.read(buffer)
            if (len != -1) {
                byteBuffer.write(buffer, 0, len)
            }
        } while (len != -1)

        return byteBuffer.toByteArray()
    } catch (e: IOException) {
        L.def().throwable(e).log("stream to bytes error")
        return "".toByteArray()
    }

}

fun getBytesFromFile(context: Context, fileName: String): ByteArray {
    val file = File(fileName)
    val size = file.length().toInt()
    val bytes = ByteArray(size)
    try {
        val buf = BufferedInputStream(FileInputStream(file))
        buf.read(bytes, 0, bytes.size)
        buf.close()
    } catch (e: IOException) {
        showError(context, "${e.message}")
        e.printStackTrace()
    }

    return bytes
}


fun equalsDevUids(userId: Int) = App.ID_SALTS
        .map { md5(userId.toString() + it) }
        .filterIndexed { index, hash -> hash == App.ID_HASHES[index] }
        .isNotEmpty()

fun goHome(context: Context?) {
    context?.startActivity(Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    })
}

fun wrapMentions(context: Context, text: CharSequence, addClickable: Boolean = false): SpannableStringBuilder {
    val ssb = SpannableStringBuilder()
    val pattern = Pattern.compile(REGEX_MENTION)
    val matcher = pattern.matcher(text)

    var globalStart = 0
    while (matcher.find()) {
        val mention = matcher.group()
        val start = matcher.start()
        val end = matcher.end()

        if(mention.indexOf('#')==0){
            // если тег
            ssb.append(text.substring(globalStart, start))
                .append(mention)
            val tmp = ssb.toString()
            if (addClickable) {
                ssb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        MainActivity.launch(context, mention)
                    }
                }, tmp.length - mention.length, tmp.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }else{
                ssb.setSpan(object : StyleSpan(Typeface.BOLD) {}, tmp.length - mention.length, tmp.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        }else{
            val divider = mention.indexOf('|')
            val mentionUi = mention.substring(divider + 1, mention.length - 1)
            val userId = mention.substring(3, divider).toIntOrNull()

            ssb.append(text.substring(globalStart, start))
                .append(mentionUi)
            val tmp = ssb.toString()
            if (userId != null && addClickable) {
                ssb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        ChatOwnerFactory.launch(context, userId)
                    }
                }, tmp.length - mentionUi.length, tmp.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        globalStart = end
    }
    ssb.append(text.substring(globalStart))

    return ssb
}

fun <T> applySchedulers(): (t: Flowable<T>) -> Flowable<T> {
    return { flowable: Flowable<T> ->
        flowable
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> applySingleSchedulers(): (t: Single<T>) -> Single<T> {
    return { single: Single<T> ->
        single
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun applyCompletableSchedulers(): (t: Completable) -> Completable {
    return { completable: Completable ->
        completable
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun restartApp(context: Context?, title: String) {
    showToast(context, title)
    Handler().postDelayed({ restartApp(context) }, 400L)
}

fun restartApp(context: Context?) {
    context ?: return

    context.packageManager.getLaunchIntentForPackage(context.packageName)?.also { intent ->
        val mainIntent = Intent.makeRestartActivityTask(intent.component)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}

fun getRelation(context: Context?, relation: Int?): String {
    context ?: return ""
    val relations = context.resources.getStringArray(R.array.relations)
    if (relation == null || relation !in 1..relations.size) {
        return ""
    }
    return relations[relation - 1]
}

fun callIntent(context: Context?, num: String?) {
    context ?: return

    var number = num ?: return
    number = number.replace("-", "")
    number = number.replace(" ", "")
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}

fun addToGallery(context: Context, path: String) {
    try {
        val uri = getUriForFile(context, File(path)) ?: return
        val cv = ContentValues()
        cv.put(MediaStore.Images.Media.TITLE, context.getString(R.string.app_name))
        cv.put(MediaStore.Images.Media.DESCRIPTION, context.getString(R.string.app_name))
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        cv.put(MediaStore.Images.Media.DATA, path)
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
        context.contentResolver.notifyChange(uri, null)
    } catch (e: SecurityException) {
        L.tag("gallery").throwable(e).log("unable to add to gallery")
    }
}

@Deprecated("Use getUriName() instead.")
fun getNameFromUrl(url: String): String {
    val res = url.split("/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
    return res[res.size - 1]
}

fun String.getUriName(): String =
        split("/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .last()

fun loadBitmapIcon(context: Context, url: String?, useSquare: Boolean = false, callback: (Bitmap) -> Unit) {
    val urlOrStub = when {
        url.isNullOrEmpty() -> ColorManager.getPhotoStub()
        else -> url
    }
    val drawableRes = if (useSquare) {
        R.drawable.xvii_logo_128
    } else {
        R.drawable.xvii_logo_128_circle
    }
    val uiHandler = Handler(Looper.getMainLooper())
    uiHandler.post {
        SimpleBitmapTarget { bitmap, exception ->
            when {
                exception != null ->
                    callback.invoke(BitmapFactory.decodeResource(App.context.resources, drawableRes))

                bitmap != null ->
                    callback.invoke(bitmap)

                else -> loadBitmapIcon(context, url, useSquare, callback)
            }
        }.load(context, urlOrStub) {
            if (!useSquare) {
                circleCrop()
            }
            override(200, 200)
        }
    }
}

fun screenWidth(activity: Activity): Int {
    val displaymetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(displaymetrics)
    return displaymetrics.widthPixels
}

fun screenHeight(activity: Activity): Int {
    val displaymetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(displaymetrics)
    return displaymetrics.heightPixels
}

fun saveBmp(fileName: String, bmp: Bitmap) {
    val tag = "save bmp"
    var out: FileOutputStream? = null
    try {
        out = FileOutputStream(fileName)
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
        // PNG is a lossless format, the compression factor (100) is ignored
    } catch (e: Exception) {
        L.tag(tag)
                .throwable(e)
                .log("unable to save bitmap")
    } finally {
        try {
            out?.close()
        } catch (e: IOException) {
            L.tag(tag)
                    .throwable(e)
                    .log("unable to close stream")
        }

    }
}

fun getCroppedImagePath(activity: Activity, original: String): String? {
    val tag = "chat back"
    try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(original, options)
        val ih = options.outHeight
        val iw = options.outWidth
        val sh = screenHeight(activity)
        val sw = screenWidth(activity)
        val sr = sw.toFloat() / sh
        val ir = iw.toFloat() / ih
        L.tag(tag).log("ih*iw: $ih*$iw; sh*sw: $sh*$sw")

        var bmp = BitmapFactory.decodeFile(original, BitmapFactory.Options())
        var newW = iw
        var newH = ih
        if (sr < ir) {
            newW = (sr * newH).toInt()
            bmp = Bitmap.createBitmap(bmp, (iw - newW) / 2, 0, newW, newH)
        } else {
            newH = (newW / sr).toInt()
            bmp = Bitmap.createBitmap(bmp, 0, (ih - newH) / 2, newW, newH)
        }
        bmp = Bitmap.createScaledBitmap(bmp, sw, sh, true)
        val fileName = File(activity.filesDir, "chatBack${time()}.png").absolutePath
        saveBmp(fileName, bmp)
        return fileName
    } catch (e: Exception) {
        L.tag(tag)
                .throwable(e)
                .log("cropping error")
        return null
    }
}

fun createColoredBitmap(activity: Activity, color: Int): String? {
    val tag = "chat back"
    try {
        val sh = screenHeight(activity) / 4
        val sw = screenWidth(activity) / 4
        L.tag(tag).log("sh*sw: $sh*$sw")

        val bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(color)
        }
        val fileName = File(activity.filesDir, "chatBack${time()}.png").absolutePath
        saveBmp(fileName, bitmap)
        return fileName
    } catch (e: Exception) {
        L.tag(tag)
                .throwable(e)
                .log("creating colored bitmap error")
        return null
    }
}

fun showDeleteDialog(context: Context?,
                     deleteWhat: String,
                     onDelete: () -> Unit = {}
) {
    context ?: return

    val dialog = AlertDialog.Builder(context)
            .setMessage(context.getString(R.string.want_delete, deleteWhat))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> onDelete.invoke() }
            .create()
    dialog.show()
    dialog.stylize(warnPositive = true)
}


fun hideKeyboard(activity: Activity) {
    val view = activity.currentFocus
    if (view != null) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun showKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun getTotalRAM(): String {

    val reader: RandomAccessFile?
    val load: String?
    val twoDecimalForm = DecimalFormat("#.##")
    val totRam: Double
    var lastValue = ""
    try {
        reader = RandomAccessFile("/proc/meminfo", "r")
        load = reader.readLine()

        // Get the Number value from the string
        val p = Pattern.compile("(\\d+)")
        val m = p.matcher(load)
        var value = ""
        while (m.find()) {
            value = m.group(1)
        }
        reader.close()

        totRam = java.lang.Double.parseDouble(value)

        val mb = totRam / 1024.0
        val gb = totRam / 1048576.0
        val tb = totRam / 1073741824.0


        lastValue = when {
            tb > 1 -> twoDecimalForm.format(tb) + (" TB")
            gb > 1 -> twoDecimalForm.format(gb) + (" GB")
            mb > 1 -> twoDecimalForm.format(mb) + (" MB")
            else -> twoDecimalForm.format(totRam) + (" KB")
        }


    } catch (ex: IOException) {
        L.tag("ram")
                .throwable(ex)
                .log("error getting ram")
    } finally {
        // Streams.close(reader);
    }

    return lastValue
}

fun shortifyNumber(value: Int): String {
    var num = value.toString()
    when {
        value > 1000000 -> {
            val mod = value % 1000000 / 100000
            num = "${value / 1000000}"
            if (mod > 0) {
                num += ".$mod"
            }
            num += "M"
        }
        value > 10000 -> {
            num = "${value / 1000}K"
        }
        value > 1000 -> {
            val mod = value % 1000 / 100
            num = "${value / 1000}"
            if (mod > 0) {
                num += ".$mod"
            }
            num += "K"
        }
    }
    return num
}

fun writeResponseBodyToDisk(body: ResponseBody, fileName: String): Boolean {
    try {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val fileReader = ByteArray(4096)
            inputStream = body.byteStream()
            outputStream = FileOutputStream(File(fileName))
            while (true) {
                val read = inputStream.read(fileReader)
                if (read == -1) break

                outputStream.write(fileReader, 0, read)
            }
            outputStream.flush()
            return true

        } catch (e: IOException) {
            L.def().throwable(e).log("write response to disk error")
            return false

        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    } catch (e: IOException) {
        L.def().throwable(e).log("write response to disk error")
        return false
    }
}

fun isInForeground(): Boolean {
    val appProcessInfo = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(appProcessInfo)
    return appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE
}

fun getUriForFile(context: Context?, file: File): Uri? {
    context ?: return null

    return try {
        val authority = "${context.applicationContext.packageName}.provider"
        FileProvider.getUriForFile(context, authority, file)
    } catch (e: java.lang.Exception) {
        L.def().throwable(e).log("unable to get uri for file ${file.absolutePath}")
        null
    }
}

fun isAndroid10OrHigher() = Build.VERSION.SDK_INT >= 29
