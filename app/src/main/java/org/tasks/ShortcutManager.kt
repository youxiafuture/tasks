package org.tasks

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
class ShortcutManager {

    private lateinit var instance : ShortcutManager

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun getInstance(context: Context): ShortcutManager? {
        return context.getSystemService(ShortcutManager::class.java)
    }

    fun reportShortcutUsed(shortcutId: String) {
        if (AndroidUtilities.atLeastNougatMR1()) {
            instance?.reportShortcutUsed(shortcutId)
        }
    }

    companion object {
        const val SHORTCUT_NEW_TASK = "static_new_task"
    }
}