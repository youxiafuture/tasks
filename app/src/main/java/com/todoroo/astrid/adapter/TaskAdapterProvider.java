package com.todoroo.astrid.adapter;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import javax.inject.Inject;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

public class TaskAdapterProvider {

  private final Context context;
  private final Preferences preferences;
  private final TagDataDao tagDataDao;
  private final TaskListMetadataDao taskListMetadataDao;
  private final TaskDao taskDao;
  private final GtasksListService gtasksListService;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final SubtasksHelper subtasksHelper;

  @Inject
  public TaskAdapterProvider(
      @ForApplication Context context,
      Preferences preferences,
      TagDataDao tagDataDao,
      TaskListMetadataDao taskListMetadataDao,
      TaskDao taskDao,
      GtasksListService gtasksListService,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      SubtasksHelper subtasksHelper) {
    this.context = context;
    this.preferences = preferences;
    this.tagDataDao = tagDataDao;
    this.taskListMetadataDao = taskListMetadataDao;
    this.taskDao = taskDao;
    this.gtasksListService = gtasksListService;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.subtasksHelper = subtasksHelper;
  }

  public TaskAdapter createTaskAdapter(Filter filter) {
    if (filter instanceof TagFilter) {
      TagFilter tagFilter = (TagFilter) filter;
      TagData tagData = tagDataDao.getByUuid(tagFilter.getUuid());
      if (tagData != null && preferences.isManualSort()) {
        return createManualTagTaskAdapter(tagFilter);
      }
    } else if (filter instanceof GtasksFilter) {
      GtasksFilter gtasksFilter = (GtasksFilter) filter;
      GoogleTaskList list = gtasksListService.getList(gtasksFilter.getStoreId());
      if (list != null) {
        return preferences.isManualSort()
            ? new GoogleTaskManualSortAdapter(taskDao, googleTaskDao)
            : new GoogleTaskAdapter(taskDao, googleTaskDao, preferences.addTasksToTop());
      }
    } else if (filter instanceof CaldavFilter) {
      CaldavFilter caldavFilter = (CaldavFilter) filter;
      CaldavCalendar calendar = caldavDao.getCalendarByUuid(caldavFilter.getUuid());
      if (calendar != null) {
        return preferences.isManualSort()
            ? new CaldavManualSortTaskAdapter(taskDao, caldavDao)
            : new CaldavTaskAdapter(taskDao, caldavDao, preferences.addTasksToTop());
      }
    } else if (subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)) {
      return createManualFilterTaskAdapter(filter);
    }
    return new TaskAdapter();
  }

  private TaskAdapter createManualTagTaskAdapter(TagFilter filter) {
    TagData tagData = filter.getTagData();
    String tdId = tagData.getRemoteId();
    TaskListMetadata list = taskListMetadataDao.fetchByTagOrFilter(tagData.getRemoteId());
    if (list == null && !Task.isUuidEmpty(tdId)) {
      list = new TaskListMetadata();
      list.setTagUuid(tdId);
      taskListMetadataDao.createNew(list);
    }
    SubtasksFilterUpdater updater = new SubtasksFilterUpdater(taskListMetadataDao, taskDao);
    updater.initialize(list, filter);
    return new AstridTaskAdapter(list, filter, updater, taskDao);
  }

  private TaskAdapter createManualFilterTaskAdapter(Filter filter) {
    String filterId = null;
    String prefId = null;
    if (BuiltInFilterExposer.isInbox(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_ALL;
      prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER;
    } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_TODAY;
      prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER;
    }
    if (isNullOrEmpty(filterId)) {
      return null;
    }
    TaskListMetadata list = taskListMetadataDao.fetchByTagOrFilter(filterId);
    if (list == null) {
      String defaultOrder = preferences.getStringValue(prefId);
      if (isNullOrEmpty(defaultOrder)) {
        defaultOrder = "[]"; // $NON-NLS-1$
      }
      defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskDao, defaultOrder);
      list = new TaskListMetadata();
      list.setFilter(filterId);
      list.setTaskIds(defaultOrder);
      taskListMetadataDao.createNew(list);
    }
    SubtasksFilterUpdater updater = new SubtasksFilterUpdater(taskListMetadataDao, taskDao);
    updater.initialize(list, filter);
    return new AstridTaskAdapter(list, filter, updater, taskDao);
  }
}
