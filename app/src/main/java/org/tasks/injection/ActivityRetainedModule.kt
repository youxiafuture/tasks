package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.tasks.ui.MainActivityEventBus
import org.tasks.ui.TaskEditEventBus
import org.tasks.ui.TaskListEventBus

/**
 * ActivityRetainedComponent: 注入器面向的对象为ViewModel
 *
 */

@Module
@InstallIn(ActivityRetainedComponent::class)
class ActivityRetainedModule {
    @Provides
    @ActivityRetainedScoped
    fun getTaskListBus(): TaskListEventBus = makeFlow()

    @Provides
    @ActivityRetainedScoped
    fun getMainActivityBus(): MainActivityEventBus = makeFlow()

    @Provides
    @ActivityRetainedScoped
    fun getTaskEditBus(): TaskEditEventBus = makeFlow()

    private fun <T> makeFlow() = MutableSharedFlow<T>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
}