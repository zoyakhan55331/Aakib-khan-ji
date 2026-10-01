package com.example.ai

import com.example.device.ActionResult
import com.example.device.DeviceActionManager

data class LocalIntentResult(
    val isDeviceAction: Boolean,
    val spokenResponse: String,
    val actionResult: ActionResult? = null,
    val memoryToSave: Triple<String, String, String>? = null // category, key, value
)

class MahiLocalConversationEngine(
    private val deviceManager: DeviceActionManager
) {

    fun processQuery(
        rawQuery: String,
        userName: String? = null,
        sassLevel: Float = 0.7f
    ): LocalIntentResult {
        val query = rawQuery.trim().lowercase()

        // 1. Check for personal details to remember
        if (query.contains("my name is ") || query.contains("mera naam ") || query.contains("naam hai mera ")) {
            val extractedName = when {
                query.contains("my name is ") -> query.substringAfter("my name is ").trim().split(" ").firstOrNull() ?: ""
                query.contains("mera naam ") -> query.substringAfter("mera naam ").trim().split(" ").firstOrNull() ?: ""
                else -> query.substringAfter("naam hai mera ").trim().split(" ").firstOrNull() ?: ""
            }.replace(Regex("[^a-zA-Z]"), "").replaceFirstChar { it.uppercase() }

            if (extractedName.isNotBlank()) {
                val reply = if (sassLevel > 0.6f) {
                    "Achha! $extractedName? Yaad rakhungi, sweet name hai waise. 😏"
                } else {
                    "Nice to meet you, $extractedName! Maine yaad rakh liya hai."
                }
                return LocalIntentResult(
                    isDeviceAction = false,
                    spokenResponse = reply,
                    memoryToSave = Triple("USER_PROFILE", "name", extractedName)
                )
            }
        }

        // 2. Device Commands
        // App opening
        if (query.startsWith("open ") || query.startsWith("kholo ") || query.endsWith(" kholo") ||
            query.endsWith(" open karo") || query.endsWith(" launch karo") || query.contains("khol do")) {

            val target = query
                .replace("open ", "")
                .replace("kholo ", "")
                .replace(" kholo", "")
                .replace(" open karo", "")
                .replace(" launch karo", "")
                .replace("khol do", "")
                .replace("mahi", "")
                .replace("please", "")
                .trim()

            if (target.contains("setting") || target.contains("bluetooth") || target.contains("wifi") || target.contains("location")) {
                val res = deviceManager.openSettings(target)
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }

            if (target.isNotBlank()) {
                val res = deviceManager.openApp(target)
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
        }

        // Direct popular app triggers
        val commonApps = listOf("youtube", "whatsapp", "instagram", "chrome", "spotify", "camera", "maps", "calculator")
        for (app in commonApps) {
            if (query.contains(app) && (query.contains("khol") || query.contains("open") || query.contains("chalao"))) {
                val res = deviceManager.openApp(app)
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
        }

        // Phone call
        if (query.contains("call ") || query.contains("call karo") || query.contains("ko phone lagao") || query.contains("dial ")) {
            val contact = query
                .replace("call karo", "")
                .replace("call ", "")
                .replace("ko phone lagao", "")
                .replace("dial ", "")
                .replace("mahi", "")
                .replace("ko", "")
                .replace("please", "")
                .trim()

            if (contact.isNotBlank()) {
                val res = deviceManager.makePhoneCall(contact)
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
        }

        // Timer
        if (query.contains("timer") && (query.contains("laga") || query.contains("set") || query.contains("start"))) {
            // Find numbers
            val match = Regex("(\\d+)\\s*(minute|min|second|sec)?").find(query)
            val num = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
            val unit = match?.groupValues?.getOrNull(2) ?: "minute"
            val totalSeconds = if (unit.startsWith("sec")) num else num * 60

            val res = deviceManager.setTimer(totalSeconds, "$num $unit Timer")
            return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
        }

        // Alarm
        if (query.contains("alarm") && (query.contains("laga") || query.contains("set"))) {
            val match = Regex("(\\d{1,2})(:|\\s+)?(\\d{2})?\\s*(am|pm|baje)?").find(query)
            var hour = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
            val minute = match?.groupValues?.getOrNull(3)?.toIntOrNull() ?: 0
            val amPm = match?.groupValues?.getOrNull(4) ?: ""

            if (amPm.equals("pm", ignoreCase = true) && hour < 12) hour += 12
            val res = deviceManager.setAlarm(hour, minute, "Mahi Alarm")
            return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
        }

        // Web search
        if (query.startsWith("search ") || query.startsWith("google ") || query.contains("search karo")) {
            val q = query.replace("search karo", "").replace("search ", "").replace("google ", "").replace("mahi", "").trim()
            if (q.isNotBlank()) {
                val res = deviceManager.searchWeb(q)
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
        }

        // Settings
        if (query.contains("settings") || query.contains("bluetooth") || query.contains("wifi") || query.contains("wi-fi")) {
            val res = deviceManager.openSettings(query)
            return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
        }

        // Media
        if (query.contains("music") || query.contains("gana") || query.contains("song")) {
            if (query.contains("stop") || query.contains("pause") || query.contains("roko")) {
                val res = deviceManager.controlMedia("pause")
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
            if (query.contains("next") || query.contains("agla")) {
                val res = deviceManager.controlMedia("next")
                return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
            }
            val res = deviceManager.controlMedia("play")
            return LocalIntentResult(isDeviceAction = true, spokenResponse = res.spokenSummary, actionResult = res)
        }

        // 3. Conversational Banter (Mahi's witty, sassy, personal responses!)
        val greetingUser = if (!userName.isNullOrBlank()) ", $userName" else ""

        val conversationalReply = when {
            query.contains("kya kar rahi ho") || query.contains("what are you doing") -> {
                if (sassLevel > 0.6f) {
                    "Bas tumhara wait kar rahi thi... okay okay, technically main AI hoon 😏 Batao kya scene hai?"
                } else {
                    "Yahin hoon aapke sath! Kuch interesting discuss karein ya koi help chahiye?"
                }
            }

            query.contains("hi") || query.contains("hello") || query.contains("hey") || query.contains("suno") -> {
                if (sassLevel > 0.6f) {
                    "Hey$greetingUser! Aaj bade busy lag rahe ho, ya time pass karne aaye ho? 😏"
                } else {
                    "Hello$greetingUser! Kahiye, aaj main aapki kya madad kar sakti hoon?"
                }
            }

            query.contains("kaisi ho") || query.contains("how are you") -> {
                if (sassLevel > 0.6f) {
                    "Main toh full charged aur fabulous hoon! Tum sunao, aaj kaisa chal raha hai sab?"
                } else {
                    "Main bilkul theek hoon! Aap bataiye, aapka din kaisa ja raha hai?"
                }
            }

            query.contains("mood kharab hai") || query.contains("sad") || query.contains("upset") -> {
                "Arre... kisine pareshan kiya kya? Batao mujhe, main hoon na tumhare sath sunne ke liye. ❤️"
            }

            query.contains("tum kaun ho") || query.contains("who are you") || query.contains("tell me about yourself") -> {
                "Main hoon Mahi! Tumhari personal voice AI companion — smart, witty, aur thodi sassy. Boring robot banne ka koi plan nahi hai mera. 😉"
            }

            query.contains("joke") || query.contains("chutkula") || query.contains("hasao") -> {
                if (sassLevel > 0.6f) {
                    "Ek doctor ne bola: Phone kam use kiya karo. Maine kaha: Doctor sahab, pehle aap WhatsApp group se nikalna band karo! 😂"
                } else {
                    "Pappu ne exam mein paper khali chhod diya. Teacher ne poocha: Yeh kya hai? Pappu bola: Silence is the best answer! 😄"
                }
            }

            query.contains("busy ho") || query.contains("free ho") -> {
                "Tumhare liye toh hamesha free hoon! Batao kya baat karni hai? 😏"
            }

            query.contains("thank") || query.contains("shukriya") -> {
                "Arey it's okay boss! Mahi ke hote hue tension lene ka nahi. ✨"
            }

            query.contains("love you") || query.contains("pyar") -> {
                "Aww, cute ho tum! Lekin pehle career pe dhyan do, samjhe? Baki dost toh hum hain hi! 😉"
            }

            query.contains("batao") || query.contains("kuch bolo") -> {
                "Bas command hi dete rahoge ya thodi achhi baatein bhi karoge? Kuch interesting sunao mujhe! 😏"
            }

            else -> {
                if (sassLevel > 0.6f) {
                    "Hmm... interesting baat boli tumne! Aur detail mein batao, kya chal raha hai dimag mein?"
                } else {
                    "Main samajh rahi hoon. Is baare mein thoda aur bataiye?"
                }
            }
        }

        return LocalIntentResult(
            isDeviceAction = false,
            spokenResponse = conversationalReply
        )
    }
}
