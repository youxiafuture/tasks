package org.tasks.widget

import android.app.Activity
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.FilterSelectionActivity
import org.tasks.databinding.ActivityWidgetShortcutLayoutBinding
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.DrawableUtil
import org.tasks.themes.ThemeColor
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutConfigActivity : ThemedInjectingAppCompatActivity(), ColorPalettePicker.ColorPickedCallback {
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider

    private lateinit var toolbar: Toolbar
    private lateinit var shortcutList: TextInputEditText
    private lateinit var shortcutName: TextInputEditText
    private lateinit var colorIcon: TextView
    private lateinit var clear: View

    private var selectedFilter: Filter? = null
    private var selectedTheme = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWidgetShortcutLayoutBinding.inflate(layoutInflater)
        binding.let {
            toolbar = it.toolbar.toolbar
            shortcutList = it.body.shortcutList.apply {
                setOnClickListener { showListPicker() }
                setOnFocusChangeListener { _, hasFocus -> onListFocusChange(hasFocus) }
            }
            shortcutName = it.body.shortcutName
            colorIcon = it.body.color.color
            clear = it.body.color.clear.clear
            it.body.color.colorRow.setOnClickListener { showThemePicker() }
        }
        setContentView(binding.root)

        toolbar.setTitle(R.string.FSA_label)
        toolbar.navigationIcon = getDrawable(R.drawable.ic_outline_save_24px)
        toolbar.setNavigationOnClickListener { save() }
        if (savedInstanceState == null) {
            selectedFilter = defaultFilterProvider.startupFilter
            selectedTheme = 7
        } else {
            selectedFilter = savedInstanceState.getParcelable(EXTRA_FILTER)
            selectedTheme = savedInstanceState.getInt(EXTRA_THEME)
        }
        updateFilterAndTheme()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                if (selectedFilter != null && selectedFilter!!.listingTitle == getShortcutName()) {
                    shortcutName.text = null
                }
                selectedFilter = data!!.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER)
                updateFilterAndTheme()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_FILTER, selectedFilter)
        outState.putInt(EXTRA_THEME, selectedTheme)
    }

    private fun onListFocusChange(focused: Boolean) {
        if (focused) {
            shortcutList.clearFocus()
            showListPicker()
        }
    }

    private fun showListPicker() {
        val intent = Intent(this, FilterSelectionActivity::class.java)
        intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, selectedFilter)
        intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true)
        startActivityForResult(intent, REQUEST_FILTER)
    }

    private fun showThemePicker() {
        newColorPalette(null, 0, Palette.LAUNCHERS)
                .show(supportFragmentManager, FRAG_TAG_COLOR_PICKER)
    }

    private fun updateFilterAndTheme() {
        if (isNullOrEmpty(getShortcutName()) && selectedFilter != null) {
            shortcutName.setText(selectedFilter!!.listingTitle)
        }
        if (selectedFilter != null) {
            shortcutList.setText(selectedFilter!!.listingTitle)
        }
        updateTheme()
    }

    private fun updateTheme() {
        clear.visibility = View.GONE
        val color = ThemeColor.getLauncherColor(this, themeIndex)
        DrawableUtil.setLeftDrawable(this, colorIcon, R.drawable.color_picker)
        DrawableUtil.setTint(DrawableUtil.getLeftDrawable(colorIcon), color.primaryColor)
        color.applyToNavigationBar(this)
    }

    private val themeIndex: Int
        get() = if (selectedTheme >= 0 && selectedTheme < ThemeColor.LAUNCHER_COLORS.size) selectedTheme else 7

    private fun getShortcutName(): String = shortcutName.text.toString().trim { it <= ' ' }

    private fun save() {
        val filterId = defaultFilterProvider.getFilterPreferenceValue(selectedFilter!!)
        val shortcutIntent = TaskIntents.getTaskListByIdIntent(this, filterId)
        val icon: Parcelable = ShortcutIconResource.fromContext(this, ThemeColor.ICONS[themeIndex])
        val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName())
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onColorPicked(index: Int) {
        selectedTheme = index
        updateTheme()
    }

    companion object {
        private const val EXTRA_FILTER = "extra_filter"
        private const val EXTRA_THEME = "extra_theme"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        private const val REQUEST_FILTER = 1019
    }
}