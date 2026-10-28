package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class BreakScheduleMethod(val title: String, val description: String) {
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
    val isLongBreak: Boolean = false,
    val label: String = "Micro-Break"
) {
    fun toJsonObject(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("triggerElapsedMinute", triggerElapsedMinute)
        o.put("breakDurationMinutes", breakDurationMinutes)
        o.put("isLongBreak", isLongBreak)
        o.put("label", label)
        return o
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): BreakPoint {
            return BreakPoint(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                triggerElapsedMinute = obj.getInt("triggerElapsedMinute"),
                breakDurationMinutes = obj.optInt("breakDurationMinutes", 5),
                isLongBreak = obj.optBoolean("isLongBreak", false),
                label = obj.optString("label", "Micro-Break")
            )
        }
    }
}

data class StudyProtocol(
    val version: Int = 1,
    val totalFocusMinutes: Int = 50,
    val scheduleMethod: BreakScheduleMethod = BreakScheduleMethod.INTERVAL,
    val intervalMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
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
     * - Be strictly less than totalFocusMinutes
     * - Have unique timestamps (duplicates resolved)
     * - Be sorted in ascending order
     */
    fun resolvedBreakPoints(): List<BreakPoint> {
        val maxMinute = totalFocusMinutes
        if (maxMinute <= 1) return emptyList()

        return when (scheduleMethod) {
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
                            isLongBreak = isLong,
                            label = label
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
                    .filter { it.triggerElapsedMinute in 1 until maxMinute && it.breakDurationMinutes > 0 }
                    .distinctBy { it.triggerElapsedMinute }
                    .sortedBy { it.triggerElapsedMinute }
            }
        }
    }

    fun validate(): String? {
        if (totalFocusMinutes < 1) return "Total focus duration must be at least 1 minute."
        if (scheduleMethod == BreakScheduleMethod.INTERVAL) {
            if (intervalMinutes < 1) return "Interval must be at least 1 minute."
            if (intervalMinutes >= totalFocusMinutes) return "Interval must be shorter than total focus duration."
            if (defaultBreakMinutes < 1) return "Break duration must be at least 1 minute."
        } else {
            val invalidPoints = customBreakPoints.filter { it.triggerElapsedMinute <= 0 || it.triggerElapsedMinute >= totalFocusMinutes }
            if (invalidPoints.isNotEmpty()) return "Break points must be between 1 and ${totalFocusMinutes - 1} minutes."
            val durations = customBreakPoints.filter { it.breakDurationMinutes <= 0 }
            if (durations.isNotEmpty()) return "Break duration must be at least 1 minute."
        }
        return null
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("totalFocusMinutes", totalFocusMinutes)
        root.put("scheduleMethod", scheduleMethod.name)
        root.put("intervalMinutes", intervalMinutes)
        root.put("defaultBreakMinutes", defaultBreakMinutes)
        root.put("longBreakMinutes", longBreakMinutes)
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
                    BreakScheduleMethod.valueOf(root.optString("scheduleMethod", BreakScheduleMethod.INTERVAL.name))
                } catch (_: Exception) {
                    BreakScheduleMethod.INTERVAL
                }

                val points = mutableListOf<BreakPoint>()
                val arr = root.optJSONArray("customBreakPoints")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        points.add(BreakPoint.fromJsonObject(arr.getJSONObject(i)))
                    }
                }

                StudyProtocol(
                    version = root.optInt("version", 1),
                    totalFocusMinutes = root.optInt("totalFocusMinutes", 50),
                    scheduleMethod = method,
                    intervalMinutes = root.optInt("intervalMinutes", 25),
                    defaultBreakMinutes = root.optInt("defaultBreakMinutes", 5),
                    longBreakMinutes = root.optInt("longBreakMinutes", 15),
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
