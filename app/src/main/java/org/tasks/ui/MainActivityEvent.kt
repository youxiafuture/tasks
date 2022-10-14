package org.tasks.ui

import com.todoroo.astrid.data.Task
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * typealias: 等价别名
 * MutableSharedFlow:
 */
typealias MainActivityEventBus = MutableSharedFlow<MainActivityEvent>

/**
 * sealed interface: 密封类，限制不可被外部使用
 * data class: 数据类，含构造参数
 * object:
 */
sealed interface MainActivityEvent {
    data class OpenTask(val task: Task) : MainActivityEvent
    object RequestRating : MainActivityEvent
    object ClearTaskEditFragment : MainActivityEvent
}

