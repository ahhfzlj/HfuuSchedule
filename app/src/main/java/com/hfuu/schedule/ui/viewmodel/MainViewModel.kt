package com.hfuu.schedule.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hfuu.schedule.HfuuScheduleApp
import com.hfuu.schedule.data.model.AccountConnectionState
import com.hfuu.schedule.data.model.AgendaEventEntity
import com.hfuu.schedule.data.model.CourseEntity
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.ScheduleMemoEntity
import com.hfuu.schedule.data.model.SemesterConfig
import com.hfuu.schedule.data.model.StudyTaskEntity
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.data.model.UserProfile
import com.hfuu.schedule.ui.theme.AppUiStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as HfuuScheduleApp).repository

    val courses: StateFlow<List<CourseEntity>> = repo.coursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyTasks: StateFlow<List<StudyTaskEntity>> = repo.studyTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeSlots: StateFlow<List<TimeSlot>> = repo.timeSlotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HfuuDefaults.defaultTimeSlots)

    val userProfile: StateFlow<UserProfile> = repo.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val semesterConfig: StateFlow<SemesterConfig> = repo.semesterConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterConfig())

    val accountState: StateFlow<AccountConnectionState> = repo.accountStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountConnectionState())

    val memos: StateFlow<List<ScheduleMemoEntity>> = repo.memosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agendaEvents: StateFlow<List<AgendaEventEntity>> = repo.agendaEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hideWeekends: StateFlow<Boolean> = repo.hideWeekendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val memoEnabled: StateFlow<Boolean> = repo.memoEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiStyle: StateFlow<AppUiStyle> = repo.uiStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUiStyle.DEFAULT)

    val startInAgenda: StateFlow<Boolean> = repo.startInAgendaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repo.getStartInAgendaSync())

    val initialStartInAgenda: Boolean = repo.getStartInAgendaSync()

    private val _selectedWeek = MutableStateFlow(SemesterConfig().currentWeek())
    val selectedWeek: StateFlow<Int> = _selectedWeek.asStateFlow()

    private val _isSyncingChaoxing = MutableStateFlow(false)
    val isSyncingChaoxing: StateFlow<Boolean> = _isSyncingChaoxing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureSeedDataIfEmpty()
        }
        viewModelScope.launch {
            semesterConfig.collect { cfg ->
                _selectedWeek.value = cfg.currentWeek()
            }
        }
    }

    fun selectWeek(week: Int) {
        val total = semesterConfig.value.totalWeeks
        _selectedWeek.value = week.coerceIn(1, total)
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun postToast(message: String) {
        _toastMessage.value = message
    }

    fun saveCourse(course: CourseEntity) {
        viewModelScope.launch {
            repo.addOrUpdateCourse(course)
            _toastMessage.value = "已保存课程：${course.name}"
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repo.deleteCourse(course)
            _toastMessage.value = "已删除课程：${course.name}"
        }
    }

    fun importFromJwxtJson(rawJson: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repo.importFromJwxtJson(rawJson)
            res.onSuccess { imported ->
                val msg = if (imported.courses.isEmpty()) {
                    "当前页面未识别到课表，请先进入教务系统的「个人课表」页面再点击导入"
                } else {
                    "成功从合肥大学教务处导入 ${imported.courses.size} 节课程"
                }
                _toastMessage.value = msg
                onDone(imported.courses.isNotEmpty(), msg)
            }.onFailure { err ->
                val msg = "解析失败：${err.message ?: "请确认已打开课表页面"}"
                _toastMessage.value = msg
                onDone(false, msg)
            }
        }
    }

    fun saveJwxtAccount(username: String, password: String) {
        viewModelScope.launch {
            repo.saveJwxtAccount(username, password)
            _toastMessage.value = "教务账号已加密保存到本机"
        }
    }

    fun getSavedJwxtUsername(): String = repo.getSavedJwxtUsername()

    fun saveChaoxingSession(account: String, cookie: String) {
        viewModelScope.launch {
            repo.saveChaoxingSession(account, cookie)
            _toastMessage.value = "学习通连接成功，正在同步待办..."
            syncChaoxingTasks()
        }
    }

    fun syncChaoxingTasks() {
        if (_isSyncingChaoxing.value) return
        viewModelScope.launch {
            _isSyncingChaoxing.value = true
            val res = repo.syncChaoxingTasks()
            res.onSuccess { sync ->
                _toastMessage.value = sync.message
            }.onFailure { err ->
                _toastMessage.value = "学习通同步失败：${err.message ?: "请重新登录学习通"}"
            }
            _isSyncingChaoxing.value = false
        }
    }

    fun toggleTaskCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repo.toggleStudyTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun disconnectChaoxing() {
        viewModelScope.launch {
            repo.disconnectChaoxing()
            _toastMessage.value = "已断开学习通连接"
        }
    }

    fun disconnectJwxt() {
        viewModelScope.launch {
            repo.disconnectJwxt()
            _toastMessage.value = "已解绑教务账号"
        }
    }

    fun saveTimeSlots(slots: List<TimeSlot>) {
        viewModelScope.launch {
            repo.saveTimeSlots(slots)
            _toastMessage.value = "上课时间已更新"
        }
    }

    fun resetDefaultTimeSlots() {
        viewModelScope.launch {
            repo.resetDefaultTimeSlots()
            _toastMessage.value = "已恢复合肥大学默认作息"
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repo.saveUserProfile(profile)
            _toastMessage.value = "个人信息已保存"
        }
    }

    fun saveSemesterConfig(config: SemesterConfig) {
        viewModelScope.launch {
            repo.saveSemesterConfig(config)
            _selectedWeek.value = config.currentWeek()
            _toastMessage.value = "学期设置已更新，当前为第 ${config.currentWeek()} 周"
        }
    }

    fun setHideWeekends(hide: Boolean) {
        viewModelScope.launch {
            repo.setHideWeekends(hide)
        }
    }

    fun setMemoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setMemoEnabled(enabled)
            _toastMessage.value = if (enabled) {
                "课表备忘录已开启，点击课程或空白时段即可添加备忘"
            } else {
                "课表备忘录已关闭"
            }
        }
    }

    fun setUiStyle(style: AppUiStyle) {
        viewModelScope.launch {
            repo.setUiStyle(style)
            _toastMessage.value = when (style) {
                AppUiStyle.DEFAULT -> "已切换为「拾光极简」默认 UI"
                AppUiStyle.MASSES_TRAVELS -> "已切换为「众生行迹」风格 UI"
            }
        }
    }

    fun setStartInAgenda(enabled: Boolean) {
        viewModelScope.launch {
            repo.setStartInAgenda(enabled)
            _toastMessage.value = if (enabled) {
                "已开启：下次启动 App 将直接进入「日程」"
            } else {
                "已恢复：启动 App 默认进入「课表」"
            }
        }
    }

    fun saveAgendaEvent(event: AgendaEventEntity) {
        viewModelScope.launch {
            repo.saveAgendaEvent(event)
            val suffix = if (event.reminderEnabled || event.countdownEnabled) "（已设置提醒）" else ""
            _toastMessage.value = "已保存日程：${event.title}$suffix"
        }
    }

    fun deleteAgendaEvent(event: AgendaEventEntity) {
        viewModelScope.launch {
            repo.deleteAgendaEvent(event)
            _toastMessage.value = "已删除日程：${event.title}"
        }
    }

    fun toggleAgendaEventCompleted(event: AgendaEventEntity) {
        viewModelScope.launch {
            repo.toggleAgendaEventCompleted(event)
        }
    }

    fun saveMemo(memo: ScheduleMemoEntity) {
        viewModelScope.launch {
            repo.addOrUpdateMemo(memo)
            _toastMessage.value = "已保存备忘：${memo.title}"
        }
    }

    fun deleteMemo(memo: ScheduleMemoEntity) {
        viewModelScope.launch {
            repo.deleteMemo(memo)
            _toastMessage.value = "已删除备忘：${memo.title}"
        }
    }

    fun toggleMemoDone(memo: ScheduleMemoEntity) {
        viewModelScope.launch {
            repo.toggleMemoDone(memo.id, !memo.isDone)
        }
    }

    fun unbindAllAccounts() {
        viewModelScope.launch {
            repo.unbindAllAccounts()
            _toastMessage.value = "已解除全部账号绑定"
        }
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repo.clearLocalCache()
            _toastMessage.value = "已清除本机缓存并重置默认数据"
        }
    }
}
