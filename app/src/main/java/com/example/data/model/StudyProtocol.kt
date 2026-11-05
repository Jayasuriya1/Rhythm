package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class BreakScheduleMethod(val title: String, val description: String) {
    MULTI_SESSION("Multi-Session & Sprints", "Structured block (e.g. 3h) split into sessions with micro-breaks and long section breaks"),
    INTERVAL("Repeating Interval", "Automatically schedule a break every X minutes"),
    CUSTOM_TIMELINE("Custom Timeline", "Define specific elapsed-time points for breaks")
}

enum class TimerPhase(val displayName: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break"),
    COOLDOWN("Session Cooldown"),
    COMPLETED("Completed")
}

data class BreakPoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val triggerElapsedMinute: Int, // Minute into focus session when break triggers
    val breakDurationMinutes: Int = 5,
    val breakDurationSeconds: Int = breakDurationMinutes * 60,
    val isLongBreak: Boolean = false,
    val label: String = "Micro-Break",
    val effectStyle: String = "BREATHING_GUIDE" // "BREATHING_GUIDE", "AMBIENT_RIPPLE", "ZEN_GLOW", "MINIMAL"
) {
    fun toJsonObject(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("triggerElapsedMinute", triggerElapsedMinute)
        o.put("breakDurationMinutes", breakDurationMinutes)
        o.put("breakDurationSeconds", breakDurationSeconds)
        o.put("isLongBreak", isLongBreak)
        o.put("label", label)
        o.put("effectStyle", effectStyle)
        return o
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): BreakPoint {
            val mins = obj.optInt("breakDurationMinutes", 5)
            val secs = obj.optInt("breakDurationSeconds", mins * 60)
            return BreakPoint(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                triggerElapsedMinute = obj.getInt("triggerElapsedMinute"),
                breakDurationMinutes = if (secs >= 60) secs / 60 else 1,
                breakDurationSeconds = maxOf(10, secs),
                isLongBreak = obj.optBoolean("isLongBreak", false),
                label = obj.optString("label", "Micro-Break"),
                effectStyle = obj.optString("effectStyle", "BREATHING_GUIDE")
            )
        }
    }
}

data class StudyProtocol(
    val version: Int = 2,
    val totalFocusMinutes: Int = 50,
    val scheduleMethod: BreakScheduleMethod = BreakScheduleMethod.MULTI_SESSION,

    // Multi-Session & Sprints Parameters
    val sessionCount: Int = 3,
    val sessionDurationMinutes: Int = 50, // e.g. 3 x 50m = 150m (2.5h) focus
    val microBreakIntervalMinutes: Int = 20, // micro-break every 20m of focus
    val microBreakDurationSeconds: Int = 30, // 30s micro-break
    val microBreakLabel: String = "20-20-20 Eye Rest & Breathe",
    val longBreakMinutes: Int = 15,
    val longBreakLabel: String = "Section Recharge Break",
    val breakEffectStyle: String = "BREATHING_GUIDE", // "BREATHING_GUIDE", "AMBIENT_RIPPLE", "ZEN_GLOW", "MINIMAL"

    // Interval & Custom Timeline
    val intervalMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val longBreakIntervalCount: Int = 4, // e.g. every 4th break is a long break
    val customBreakPoints: List<BreakPoint> = emptyList(),
    val hasCooldownPeriod: Boolean = false,
    val cooldownMinutes: Int = 5,
    val breaksCountTowardTotal: Boolean = false, // false: breaks pause focus countdown; true: breaks within overall time window
    val startSoundEnabled: Boolean = true,
    val breakStartSoundEnabled: Boolean = true,
    val breakEndSoundEnabled: Boolean = true,
    val sessionEndSoundEnabled: Boolean = true
) {

    /**
     * Resolves all active break points deterministically sorted by elapsed minute.
     * Guaranteed to:
     * - Contain only positive minutes
     * - Be strictly less than or equal to totalFocusMinutes
     * - Have unique timestamps (duplicates resolved)
     * - Be sorted in ascending order
     */
    fun resolvedBreakPoints(): List<BreakPoint> {
        val maxMinute = totalFocusMinutes
        if (maxMinute <= 1) return emptyList()

        return when (scheduleMethod) {
            BreakScheduleMethod.MULTI_SESSION -> {
                val list = mutableListOf<BreakPoint>()
                val count = maxOf(1, sessionCount)
                val sDuration = maxOf(5, sessionDurationMinutes)

                for (s in 0 until count) {
                    val sStart = s * sDuration
                    val sEnd = (s + 1) * sDuration

                    // Micro-breaks inside this session
                    if (microBreakIntervalMinutes > 0 && microBreakIntervalMinutes < sDuration) {
                        var microMinute = sStart + microBreakIntervalMinutes
                        var microIndex = 1
                        while (microMinute < sEnd && microMinute < maxMinute) {
                            list.add(
                                BreakPoint(
                                    id = "micro_${s + 1}_$microMinute",
                                    triggerElapsedMinute = microMinute,
                                    breakDurationMinutes = maxOf(1, microBreakDurationSeconds / 60),
                                    breakDurationSeconds = maxOf(10, microBreakDurationSeconds),
                                    isLongBreak = false,
                                    label = "$microBreakLabel (#${microIndex})",
                                    effectStyle = breakEffectStyle
                                )
                            )
                            microMinute += microBreakIntervalMinutes
                            microIndex++
                        }
                    }

                    // Long section break at session boundary (except after the final session)
                    if (s < count - 1 && sEnd < maxMinute && longBreakMinutes > 0) {
                        list.add(
                            BreakPoint(
                                id = "long_section_${s + 1}_$sEnd",
                                triggerElapsedMinute = sEnd,
                                breakDurationMinutes = longBreakMinutes,
                                breakDurationSeconds = longBreakMinutes * 60,
                                isLongBreak = true,
                                label = "$longBreakLabel (Session ${s + 1} of $count Complete)",
                                effectStyle = breakEffectStyle
                            )
                        )
                    }
                }
                list.distinctBy { it.triggerElapsedMinute }.sortedBy { it.triggerElapsedMinute }
            }

            BreakScheduleMethod.INTERVAL -> {
                if (intervalMinutes <= 0) return emptyList()
                val list = mutableListOf<BreakPoint>()
                var minute = intervalMinutes
                var breakIndex = 1

                while (minute < maxMinute) {
                    val isLong = (breakIndex % longBreakIntervalCount == 0)
                    val duration = if (isLong) longBreakMinutes else defaultBreakMinutes
                    val label = if (isLong) "Long Break #$breakIndex" else "Micro-Break #$breakIndex"

                    list.add(
                        BreakPoint(
                            id = "interval_break_$minute",
                            triggerElapsedMinute = minute,
                            breakDurationMinutes = maxOf(1, duration),
                            breakDurationSeconds = maxOf(1, duration) * 60,
                            isLongBreak = isLong,
                            label = label,
                            effectStyle = breakEffectStyle
                        )
                    )
                    minute += intervalMinutes
                    breakIndex++
                }
                list
            }

            BreakScheduleMethod.CUSTOM_TIMELINE -> {
                // Deduplicate by triggerElapsedMinute and sort ascending
                customBreakPoints
                    .filter { it.triggerElapsedMinute in 1 until maxMinute && (it.breakDurationSeconds > 0 || it.breakDurationMinutes > 0) }
                    .distinctBy { it.triggerElapsedMinute }
                    .sortedBy { it.triggerElapsedMinute }
            }
        }
    }

    fun validate(): String? {
        if (totalFocusMinutes < 1) return "Total focus duration must be at least 1 minute."
        when (scheduleMethod) {
            BreakScheduleMethod.MULTI_SESSION -> {
                if (sessionCount < 1) return "Session count must be at least 1."
                if (sessionDurationMinutes < 5) return "Session duration must be at least 5 minutes."
                if (microBreakIntervalMinutes > 0 && microBreakIntervalMinutes >= sessionDurationMinutes) {
                    return "Micro-break interval must be shorter than session duration."
                }
            }
            BreakScheduleMethod.INTERVAL -> {
                if (intervalMinutes < 1) return "Interval must be at least 1 minute."
                if (intervalMinutes >= totalFocusMinutes) return "Interval must be shorter than total focus duration."
                if (defaultBreakMinutes < 1) return "Break duration must be at least 1 minute."
            }
            BreakScheduleMethod.CUSTOM_TIMELINE -> {
                val invalidPoints = customBreakPoints.filter { it.triggerElapsedMinute <= 0 || it.triggerElapsedMinute >= totalFocusMinutes }
                if (invalidPoints.isNotEmpty()) return "Break points must be between 1 and ${totalFocusMinutes - 1} minutes."
                val durations = customBreakPoints.filter { it.breakDurationSeconds <= 0 && it.breakDurationMinutes <= 0 }
                if (durations.isNotEmpty()) return "Break duration must be at least 10 seconds."
            }
        }
        return null
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("totalFocusMinutes", totalFocusMinutes)
        root.put("scheduleMethod", scheduleMethod.name)
        root.put("sessionCount", sessionCount)
        root.put("sessionDurationMinutes", sessionDurationMinutes)
        root.put("microBreakIntervalMinutes", microBreakIntervalMinutes)
        root.put("microBreakDurationSeconds", microBreakDurationSeconds)
        root.put("microBreakLabel", microBreakLabel)
        root.put("longBreakMinutes", longBreakMinutes)
        root.put("longBreakLabel", longBreakLabel)
        root.put("breakEffectStyle", breakEffectStyle)

        root.put("intervalMinutes", intervalMinutes)
        root.put("defaultBreakMinutes", defaultBreakMinutes)
        root.put("longBreakIntervalCount", longBreakIntervalCount)
        root.put("hasCooldownPeriod", hasCooldownPeriod)
        root.put("cooldownMinutes", cooldownMinutes)
        root.put("breaksCountTowardTotal", breaksCountTowardTotal)
        root.put("startSoundEnabled", startSoundEnabled)
        root.put("breakStartSoundEnabled", breakStartSoundEnabled)
        root.put("breakEndSoundEnabled", breakEndSoundEnabled)
        root.put("sessionEndSoundEnabled", sessionEndSoundEnabled)

        val arr = JSONArray()
        customBreakPoints.forEach { arr.put(it.toJsonObject()) }
        root.put("customBreakPoints", arr)

        return root.toString()
    }

    companion object {
        fun fromJson(json: String?): StudyProtocol {
            if (json.isNullOrBlank()) return StudyProtocol()
            return try {
                val root = JSONObject(json)
                val method = try {
                    BreakScheduleMethod.valueOf(root.optString("scheduleMethod", BreakScheduleMethod.MULTI_SESSION.name))
                } catch (_: Exception) {
                    BreakScheduleMethod.MULTI_SESSION
                }

                val points = mutableListOf<BreakPoint>()
                val arr = root.optJSONArray("customBreakPoints")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        points.add(BreakPoint.fromJsonObject(arr.getJSONObject(i)))
                    }
                }

                val sessionCount = root.optInt("sessionCount", 3)
                val sessionDur = root.optInt("sessionDurationMinutes", 50)
                val defaultTotal = if (method == BreakScheduleMethod.MULTI_SESSION) sessionCount * sessionDur else 50
                val totalFocus = root.optInt("totalFocusMinutes", defaultTotal)

                StudyProtocol(
                    version = root.optInt("version", 2),
                    totalFocusMinutes = totalFocus,
                    scheduleMethod = method,
                    sessionCount = sessionCount,
                    sessionDurationMinutes = sessionDur,
                    microBreakIntervalMinutes = root.optInt("microBreakIntervalMinutes", 20),
                    microBreakDurationSeconds = root.optInt("microBreakDurationSeconds", 30),
                    microBreakLabel = root.optString("microBreakLabel", "20-20-20 Eye Rest & Breathe"),
                    longBreakMinutes = root.optInt("longBreakMinutes", 15),
                    longBreakLabel = root.optString("longBreakLabel", "Section Recharge Break"),
                    breakEffectStyle = root.optString("breakEffectStyle", "BREATHING_GUIDE"),

                    intervalMinutes = root.optInt("intervalMinutes", 25),
                    defaultBreakMinutes = root.optInt("defaultBreakMinutes", 5),
                    longBreakIntervalCount = root.optInt("longBreakIntervalCount", 4),
                    customBreakPoints = points,
                    hasCooldownPeriod = root.optBoolean("hasCooldownPeriod", false),
                    cooldownMinutes = root.optInt("cooldownMinutes", 5),
                    breaksCountTowardTotal = root.optBoolean("breaksCountTowardTotal", false),
                    startSoundEnabled = root.optBoolean("startSoundEnabled", true),
                    breakStartSoundEnabled = root.optBoolean("breakStartSoundEnabled", true),
                    breakEndSoundEnabled = root.optBoolean("breakEndSoundEnabled", true),
                    sessionEndSoundEnabled = root.optBoolean("sessionEndSoundEnabled", true)
                )
            } catch (_: Exception) {
                StudyProtocol()
            }
        }
    }
}
