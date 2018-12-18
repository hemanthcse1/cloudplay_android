package com.e_rachit.e_rachitcloudadsplayer.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.e_rachit.e_rachitcloudadsplayer.Utils
import com.e_rachit.e_rachitcloudadsplayer.models.Constants.SCHEDULE_ALARMS_IDS
import com.e_rachit.e_rachitcloudadsplayer.models.Schedule
import com.e_rachit.e_rachitcloudadsplayer.models.ScheduleMeta
import com.e_rachit.e_rachitcloudadsplayer.receivers.ScheduleAlarmReceiver
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


/**
 * Created by rohitranjan on 30/12/17.
 */
class ScheduleManager(val mContext: Context) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd")
    private var fmt = DateTimeFormat.forPattern("HH:mm:ss")

    /**
     * filters the current Schedules
     *
     * @param schedules: the list of all the available schedules
     */
    fun getCurrentSchedule(schedules: List<ScheduleMeta>?): List<ScheduleMeta>? {
        val filteredSchedules = ArrayList<ScheduleMeta>()
        schedules?.let {
            var lastSchedule: ScheduleMeta = ScheduleMeta(-1, DateTime.now().minusDays(1), DateTime.now().minusMinutes(30))
            for (schedule in schedules) {
                if (schedule.fromTime.isBeforeNow && schedule.toTime.isAfterNow) {
                    filteredSchedules.add(schedule)
                } else if (lastSchedule.toTime.isBeforeNow && schedule.fromTime.isAfterNow) {
                    filteredSchedules.add(ScheduleMeta(-1, DateTime.now(), schedule.fromTime))
                }
                lastSchedule = schedule
            }
            if (filteredSchedules.size == 0)
                filteredSchedules.add(ScheduleMeta(-1, DateTime.now(), DateTime.now().plusDays(1)))
        }
        return filteredSchedules
    }

    /**
     * Sort Schedule
     *
     * @param schedules: the schedule that is need to be sorted
     */
    fun sortAndFilterSchedule(schedules: List<Schedule>): List<ScheduleMeta> {
        // expand schedules
        val all_schedules = ArrayList<ScheduleMeta>()

        for (schedule in schedules) {
            val startDate = DateTime(sdf.parse(schedule.start_date)).withTime(fmt.parseLocalTime(schedule.start_time))
            val endDate = DateTime(sdf.parse(schedule.end_date)).withTime(fmt.parseLocalTime(schedule.end_time))

            val days = Days.daysBetween(startDate, endDate).days
            (0..days)
                    .filter { isScheduleEligible(schedule.days, startDate.plusDays(it)) }
                    .forEach { all_schedules.add(ScheduleMeta(schedule.schedule_id, startDate.plusDays(it), startDate.plusDays(it).withTime(endDate.toLocalTime()))) }
        }

        // sort schedules
        val sorted_schedule = all_schedules.sortedWith(CompareObject)
        return sorted_schedule
    }

    /**
     * checks if the schedule is eligible
     */
    private fun isScheduleEligible(days: List<String>, date: DateTime): Boolean {
        var dOW: Int = date.dayOfWeek().get()
        if (dOW == 7)
            dOW = 0
        val abc = days.contains(dOW.toString())
        return days.contains(dOW.toString())
    }

    /**
     * sets the schedule Alarms
     *
     * @param schedule the schedule for which the alarm has to be set
     */
    fun setScheduleEndAlarm(schedule: ScheduleMeta?) {

        val seconds = Seconds.secondsBetween(DateTime.now(), schedule?.toTime).seconds
//        val seconds = 180
        val intent = Intent(mContext, ScheduleAlarmReceiver::class.java)
        intent.setClass(mContext, ScheduleAlarmReceiver::class.java)
        val r = Random()
        val i1 = r.nextInt(999999999 - 100000000) + 100000000

        val pendingIntent = PendingIntent.getBroadcast(mContext, i1, intent, 0)
        val alarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds * 1000, pendingIntent)
        val alarms: HashSet<String> = Utils.Companion.readPreferenceData(mContext, SCHEDULE_ALARMS_IDS, HashSet<String>())
        alarms.add(i1.toString())
        Utils.savePreferenceData(mContext, SCHEDULE_ALARMS_IDS, alarms)
    }

    /**
     * clears all the existing schedule alarms
     */
    fun clearAllAlarms() {
        for (id in Utils.Companion.readPreferenceData(mContext, SCHEDULE_ALARMS_IDS, HashSet<String>())) {
            val alarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val myIntent = Intent(mContext,
                    ScheduleAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                    mContext, id.toInt(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            alarmManager.cancel(pendingIntent)
        }
        Utils.savePreferenceData(mContext, SCHEDULE_ALARMS_IDS, HashSet<String>())
    }

    class CompareObject {
        companion object : Comparator<ScheduleMeta> {
            override fun compare(p0: ScheduleMeta?, p1: ScheduleMeta?): Int {
                if (p0?.fromTime!!.isBefore(p1?.fromTime))
                    return -1
                return 1
            }
        }
    }

}