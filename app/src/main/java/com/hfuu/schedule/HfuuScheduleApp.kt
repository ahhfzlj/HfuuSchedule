package com.hfuu.schedule

import android.app.Application
import com.hfuu.schedule.data.repository.ScheduleRepository

class HfuuScheduleApp : Application() {
    lateinit var repository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ScheduleRepository(this)
    }
}
