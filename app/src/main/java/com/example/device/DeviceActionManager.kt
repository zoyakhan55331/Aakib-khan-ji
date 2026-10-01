package com.example.device

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat

enum class ActionStatus {
    SUCCESS,
    FAILED,
    PERMISSION_REQUIRED,
    NOT_FOUND,
    AMBIGUOUS,
    CANCELLED
}

data class ContactMatch(
    val name: String,
    val phoneNumber: String
)

data class ActionResult(
    val status: ActionStatus,
    val actionName: String,
    val userFriendlyMessage: String,
    val spokenSummary: String,
    val details: String? = null,
    val permissionNeeded: String? = null
)

class DeviceActionManager(private val context: Context) {

    // Common app package map
    private val wellKnownPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "google" to "com.google.android.googlequicksearchbox",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "gmail" to "com.google.android.gm",
        "play store" to "com.android.vending",
        "camera" to "com.google.android.GoogleCamera",
        "telegram" to "org.telegram.messenger",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "settings" to "com.android.settings"
    )

    fun openApp(appName: String): ActionResult {
        val cleanName = appName.trim().lowercase()
        val pm = context.packageManager

        // 1. Direct package lookup
        val directPkg = wellKnownPackages[cleanName]
        if (directPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult(
                    status = ActionStatus.SUCCESS,
                    actionName = "open_app",
                    userFriendlyMessage = "Opening $appName...",
                    spokenSummary = "$appName khol rahi hoon."
                )
            }
        }

        // 2. Scan installed applications
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label == cleanName || label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        val actualLabel = pm.getApplicationLabel(app).toString()
                        return ActionResult(
                            status = ActionStatus.SUCCESS,
                            actionName = "open_app",
                            userFriendlyMessage = "Opening $actualLabel...",
                            spokenSummary = "$actualLabel open kar diya."
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback for special common apps
        if (cleanName.contains("camera")) {
            val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                return ActionResult(
                    status = ActionStatus.SUCCESS,
                    actionName = "open_app",
                    userFriendlyMessage = "Opening Camera...",
                    spokenSummary = "Camera khol rahi hoon."
                )
            }
        }

        if (cleanName.contains("setting")) {
            return openSettings("general")
        }

        // App not found, offer play store fallback
        return ActionResult(
            status = ActionStatus.NOT_FOUND,
            actionName = "open_app",
            userFriendlyMessage = "$appName not found on device",
            spokenSummary = "Arre, $appName tumhare phone mein nahi mila. Play Store par check karun kya?"
        )
    }

    fun openWebsite(rawUrl: String): ActionResult {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "open_website",
                userFriendlyMessage = "Opening $url",
                spokenSummary = "Website khol rahi hoon."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "open_website",
                userFriendlyMessage = "Could not open $rawUrl: ${e.localizedMessage}",
                spokenSummary = "Website nahi khul payi."
            )
        }
    }

    fun searchWeb(query: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "search_web",
                userFriendlyMessage = "Searching for: $query",
                spokenSummary = "Google par search kar rahi hoon."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "search_web",
                userFriendlyMessage = "Search failed: ${e.localizedMessage}",
                spokenSummary = "Search karne mein dikkat aayi."
            )
        }
    }

    fun openSettings(type: String): ActionResult {
        val cleanType = type.trim().lowercase()
        val intentAction = when {
            cleanType.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            cleanType.contains("wifi") || cleanType.contains("wi-fi") -> Settings.ACTION_WIFI_SETTINGS
            cleanType.contains("location") || cleanType.contains("gps") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            cleanType.contains("notification") -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            cleanType.contains("sound") || cleanType.contains("volume") -> Settings.ACTION_SOUND_SETTINGS
            cleanType.contains("display") || cleanType.contains("brightness") -> Settings.ACTION_DISPLAY_SETTINGS
            cleanType.contains("app") || cleanType.contains("permission") -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "open_settings",
                userFriendlyMessage = "Opening $type settings...",
                spokenSummary = "$type settings open kar rahi hoon."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "open_settings",
                userFriendlyMessage = "Failed to open settings: ${e.localizedMessage}",
                spokenSummary = "Settings kholne mein issue aaya."
            )
        }
    }

    fun searchContacts(query: String): List<ContactMatch> {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val matches = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && matches.size < 5) {
                    val name = it.getString(nameIndex) ?: ""
                    val number = it.getString(numberIndex) ?: ""
                    if (name.isNotBlank() && number.isNotBlank() && matches.none { m -> m.phoneNumber == number }) {
                        matches.add(ContactMatch(name, number))
                    }
                }
            }
        } catch (_: Exception) {}

        return matches
    }

    fun makePhoneCall(target: String): ActionResult {
        val cleanTarget = target.trim()
        val isNumber = cleanTarget.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }

        if (isNumber) {
            return dialOrCallNumber(cleanTarget, cleanTarget)
        }

        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Open dialer with searched name as fallback
            return ActionResult(
                status = ActionStatus.PERMISSION_REQUIRED,
                actionName = "make_phone_call",
                userFriendlyMessage = "Contacts permission needed to call $target",
                spokenSummary = "Mujhe $target ko call karne ke liye Contacts permission chahiye.",
                permissionNeeded = android.Manifest.permission.READ_CONTACTS
            )
        }

        val matches = searchContacts(cleanTarget)
        return when {
            matches.isEmpty() -> {
                ActionResult(
                    status = ActionStatus.NOT_FOUND,
                    actionName = "make_phone_call",
                    userFriendlyMessage = "No contact found for '$cleanTarget'",
                    spokenSummary = "$cleanTarget naam ka koi contact nahi mila phone mein."
                )
            }
            matches.size > 1 -> {
                val names = matches.take(3).joinToString(" ya ") { it.name }
                ActionResult(
                    status = ActionStatus.AMBIGUOUS,
                    actionName = "make_phone_call",
                    userFriendlyMessage = "Multiple contacts found: ${matches.joinToString { it.name }}",
                    spokenSummary = "Multiple contacts mile: $names? Kaunse wale ko call karun?",
                    details = matches.joinToString(";") { "${it.name}:${it.phoneNumber}" }
                )
            }
            else -> {
                val match = matches.first()
                dialOrCallNumber(match.phoneNumber, match.name)
            }
        }
    }

    private fun dialOrCallNumber(number: String, displayName: String): ActionResult {
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val intent = if (hasCallPerm) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "make_phone_call",
                userFriendlyMessage = "Calling $displayName ($number)...",
                spokenSummary = "$displayName ko call mila rahi hoon."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "make_phone_call",
                userFriendlyMessage = "Call failed: ${e.localizedMessage}",
                spokenSummary = "Call lagane mein dikkat aayi."
            )
        }
    }

    fun sendSms(recipient: String, message: String): ActionResult {
        return try {
            val smsUri = Uri.parse("smsto:${Uri.encode(recipient)}")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "send_sms",
                userFriendlyMessage = "SMS ready for $recipient: \"$message\"",
                spokenSummary = "Message prepare kar diya hai, screen par check karo."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "send_sms",
                userFriendlyMessage = "Could not send SMS: ${e.localizedMessage}",
                spokenSummary = "SMS tayyar karne mein error aaya."
            )
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Mahi AI Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minute)
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "set_alarm",
                userFriendlyMessage = "Alarm set for $formattedTime",
                spokenSummary = "$formattedTime baje ka alarm laga diya boss."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "set_alarm",
                userFriendlyMessage = "Failed to set alarm: ${e.localizedMessage}",
                spokenSummary = "Alarm set nahi ho paya."
            )
        }
    }

    fun setTimer(durationSeconds: Int, label: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Mahi AI Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val timeText = if (minutes > 0) "$minutes minute ${if (seconds > 0) "$seconds sec" else ""}" else "$seconds seconds"
            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "set_timer",
                userFriendlyMessage = "Timer set for $timeText",
                spokenSummary = "$timeText ka timer shuru kar diya!"
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "set_timer",
                userFriendlyMessage = "Failed to set timer: ${e.localizedMessage}",
                spokenSummary = "Timer lagane mein error aaya."
            )
        }
    }

    fun controlMedia(action: String): ActionResult {
        val cleanAction = action.lowercase().trim()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        return try {
            val keyCode = when (cleanAction) {
                "play", "resume" -> KeyEvent.KEYCODE_MEDIA_PLAY
                "pause", "stop" -> KeyEvent.KEYCODE_MEDIA_PAUSE
                "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }

            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

            ActionResult(
                status = ActionStatus.SUCCESS,
                actionName = "control_media",
                userFriendlyMessage = "Media: $cleanAction",
                spokenSummary = "Media $cleanAction kar diya."
            )
        } catch (e: Exception) {
            ActionResult(
                status = ActionStatus.FAILED,
                actionName = "control_media",
                userFriendlyMessage = "Media control failed: ${e.localizedMessage}",
                spokenSummary = "Media control nahi ho paya."
            )
        }
    }
}
