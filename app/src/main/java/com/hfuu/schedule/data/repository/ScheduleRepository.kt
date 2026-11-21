package com.hfuu.schedule.data.repository

import android.content.Context
import com.hfuu.schedule.data.local.AppDatabase
import com.hfuu.schedule.data.local.PreferencesManager
import com.hfuu.schedule.data.local.SecureAccountStore
import com.hfuu.schedule.data.model.AccountConnectionState
import com.hfuu.schedule.data.model.AgendaEventEntity
import com.hfuu.schedule.data.model.CourseEntity
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.ScheduleMemoEntity
import com.hfuu.schedule.data.model.SemesterConfig
import com.hfuu.schedule.data.model.StudyTaskEntity
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.data.model.UserProfile
import com.hfuu.schedule.data.remote.ChaoxingApiClient
import com.hfuu.schedule.data.remote.ChaoxingSyncResult
import com.hfuu.schedule.data.remote.JwxtCourseParser
import com.hfuu.schedule.data.remote.JwxtImportResult
import com.hfuu.schedule.reminder.AgendaReminderScheduler
import com.hfuu.schedule.ui.theme.AppUiStyle
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val courseDao = db.courseDao()
    private val studyDao = db.studyDao()
    private val memoDao = db.memoDao()
    private val agendaDao = db.agendaDao()
    private val prefs = PreferencesManager(context)
    private val secureStore = SecureAccountStore(context)
    private val chaoxingClient = ChaoxingApiClient()

    val coursesFlow: Flow<List<CourseEntity>> = courseDao.observeAllCourses()
    val studyTasksFlow: Flow<List<StudyTaskEntity>> = studyDao.observeAllTasks()
    val memosFlow: Flow<List<ScheduleMemoEntity>> = memoDao.observeAllMemos()
    val agendaEventsFlow: Flow<List<AgendaEventEntity>> = agendaDao.observeAllEvents()
    val timeSlotsFlow: Flow<List<TimeSlot>> = prefs.timeSlotsFlow
    val userProfileFlow: Flow<UserProfile> = prefs.userProfileFlow
    val semesterConfigFlow: Flow<SemesterConfig> = prefs.semesterConfigFlow
    val accountStateFlow: Flow<AccountConnectionState> = prefs.accountStateFlow
    val hideWeekendsFlow: Flow<Boolean> = prefs.hideWeekendsFlow
    val memoEnabledFlow: Flow<Boolean> = prefs.memoEnabledFlow
    val uiStyleFlow: Flow<AppUiStyle> = prefs.uiStyleFlow
    val startInAgendaFlow: Flow<Boolean> = prefs.startInAgendaFlow

    fun getStartInAgendaSync(): Boolean = prefs.getStartInAgendaSync()

    suspend fun ensureSeedDataIfEmpty() {
        if (courseDao.count() == 0) {
            courseDao.insertAll(HfuuDefaults.demoCourses)
        }
        if (studyDao.count() == 0) {
            studyDao.insertAll(HfuuDefaults.demoStudyTasks())
        }
    }

    suspend fun addOrUpdateCourse(course: CourseEntity) {
        if (course.id == 0L) {
            courseDao.insert(course)
        } else {
            courseDao.update(course)
        }
    }

    suspend fun deleteCourse(course: CourseEntity) {
        courseDao.delete(course)
    }

    suspend fun importFromJwxtJson(rawJson: String): Result<JwxtImportResult> {
        val parsed = JwxtCourseParser.parseFromBridgeJson(rawJson)
        parsed.onSuccess { result ->
            if (result.courses.isNotEmpty()) {
                courseDao.replaceAll(result.courses)
            }
            val now = System.currentTimeMillis()
            prefs.updateAccountState {
                it.copy(
                    jwxtConnected = true,
                    jwxtUsername = result.studentInfo.studentId.ifBlank { it.jwxtUsername },
                    jwxtLastSyncMillis = now
                )
            }
            if (result.studentInfo.name.isNotBlank() || result.studentInfo.studentId.isNotBlank()) {
                prefs.saveUserProfile(
                    UserProfile(
                        name = result.studentInfo.name.ifBlank { "合大同学" },
                        studentId = result.studentInfo.studentId.ifBlank { "2025010130" },
                        className = result.studentInfo.className,
                        major = result.studentInfo.major
                    )
                )
            }
        }
        return parsed
    }

    suspend fun saveJwxtAccount(username: String, password: String) {
        secureStore.saveJwxtCredentials(username, password)
        prefs.updateAccountState {
            it.copy(
                jwxtConnected = username.isNotBlank(),
                jwxtUsername = username,
                jwxtLastSyncMillis = if (it.jwxtLastSyncMillis == 0L) System.currentTimeMillis() else it.jwxtLastSyncMillis
            )
        }
    }

    fun getSavedJwxtUsername(): String = secureStore.getJwxtUsername()

    suspend fun saveChaoxingSession(account: String, cookie: String) {
        secureStore.saveChaoxingSession(account, cookie)
        prefs.updateAccountState {
            it.copy(
                chaoxingConnected = true,
                chaoxingAccount = account.ifBlank { "学习通已授权" },
                chaoxingLastSyncMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun syncChaoxingTasks(): Result<ChaoxingSyncResult> {
        val cookie = secureStore.getChaoxingCookie()
        if (cookie.isBlank()) {
            // 未绑定真实学习通 Cookie 时刷新演示数据时间戳，方便体验完整交互
            val demo = HfuuDefaults.demoStudyTasks()
            studyDao.replaceAll(demo)
            prefs.updateAccountState {
                it.copy(
                    chaoxingCourseCount = 12,
                    chaoxingLastSyncMillis = System.currentTimeMillis()
                )
            }
            return Result.success(
                ChaoxingSyncResult(
                    courseCount = 12,
                    tasks = demo,
                    message = "当前为演示模式；连接学习通账号后可同步真实待办"
                )
            )
        }

        val result = chaoxingClient.syncAll(cookie)
        result.onSuccess { sync ->
            studyDao.replaceAll(sync.tasks)
            prefs.updateAccountState {
                it.copy(
                    chaoxingConnected = true,
                    chaoxingCourseCount = sync.courseCount,
                    chaoxingLastSyncMillis = System.currentTimeMillis()
                )
            }
        }
        return result
    }

    suspend fun toggleStudyTaskCompleted(taskId: String, completed: Boolean) {
        studyDao.updateCompleted(taskId, completed)
    }

    suspend fun disconnectChaoxing() {
        secureStore.clearChaoxingSession()
        prefs.updateAccountState {
            it.copy(
                chaoxingConnected = false,
                chaoxingAccount = "",
                chaoxingCourseCount = 0
            )
        }
    }

    suspend fun disconnectJwxt() {
        secureStore.clearJwxtCredentials()
        prefs.updateAccountState {
            it.copy(
                jwxtConnected = false,
                jwxtUsername = ""
            )
        }
    }

    suspend fun saveTimeSlots(slots: List<TimeSlot>) {
        prefs.saveTimeSlots(slots)
    }

    suspend fun resetDefaultTimeSlots() {
        prefs.saveTimeSlots(HfuuDefaults.defaultTimeSlots)
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        prefs.saveUserProfile(profile)
    }

    suspend fun saveSemesterConfig(config: SemesterConfig) {
        prefs.saveSemesterConfig(config)
    }

    suspend fun setHideWeekends(hide: Boolean) {
        prefs.setHideWeekends(hide)
    }

    suspend fun setMemoEnabled(enabled: Boolean) {
        prefs.setMemoEnabled(enabled)
    }

    suspend fun setUiStyle(style: AppUiStyle) {
        prefs.setUiStyle(style)
    }

    suspend fun setStartInAgenda(enabled: Boolean) {
        prefs.setStartInAgenda(enabled)
    }

    suspend fun saveAgendaEvent(event: AgendaEventEntity): Long {
        val savedId = if (event.id == 0L) {
            agendaDao.insert(event)
        } else {
            agendaDao.update(event)
            event.id
        }
        AgendaReminderScheduler.scheduleOrCancel(appContext, event.copy(id = savedId))
        return savedId
    }

    suspend fun deleteAgendaEvent(event: AgendaEventEntity) {
        agendaDao.delete(event)
        AgendaReminderScheduler.cancel(appContext, event.id)
    }

    suspend fun toggleAgendaEventCompleted(event: AgendaEventEntity) {
        val nextDone = !event.isCompleted
        agendaDao.updateCompleted(event.id, nextDone)
        AgendaReminderScheduler.scheduleOrCancel(appContext, event.copy(isCompleted = nextDone))
    }

    suspend fun addOrUpdateMemo(memo: ScheduleMemoEntity) {
        if (memo.id == 0L) {
            memoDao.insert(memo)
        } else {
            memoDao.update(memo)
        }
    }

    suspend fun deleteMemo(memo: ScheduleMemoEntity) {
        memoDao.delete(memo)
    }

    suspend fun toggleMemoDone(memoId: Long, done: Boolean) {
        memoDao.updateDone(memoId, done)
    }

    suspend fun unbindAllAccounts() {
        secureStore.clearAll()
        prefs.updateAccountState {
            AccountConnectionState(localAccountSignedIn = false)
        }
    }

    suspend fun clearLocalCache() {
        courseDao.deleteAll()
        studyDao.deleteAll()
        memoDao.deleteAll()
        agendaDao.deleteAll()
        prefs.clearAll()
        ensureSeedDataIfEmpty()
    }
}
