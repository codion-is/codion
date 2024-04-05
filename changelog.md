Codion Change Log
==================

## 0.17.38-SNAPSHOT
### is.codion.common.core
- Event.listener() and dataListener() removed, no longer necessary.
### is.codion.framework.model
- EntityEditModel.valueEvent() added.
### is.codion.swing.framework.ui
- EntityPanel.icon() added, used when panel is displayed in windows or tabbed panes.
- EntityPanel.Builder.icon() added.
- EntityPanel, caption, description and icon moved to Config, related changes.
- EntityPanel.disposeEditDialogOnEscape moved to Config.
- EntityPanel.updateEditPanelState() refactored, ShowHiddenEditPanel now brings window to front if available, minor related changes.
- EntityPanel.updateEditPanelState() refactoring mistake fixed. 

## 0.17.37
### is.codion.common.core
- MethodLogger now Exception based instead of Throwable.
### is.codion.common.db
- AbstractDatabase.createConnection() bug fixed, did not use errorMessage() for authentication errors.
### is.codion.common.model
- FilteredModel.Refresher.refreshFailedEvent() now Exception based instead of Throwable.
- LoadTest.Result now Exception based instead of Throwable.
### is.codion.dbms.h2
- Localized error messages added for a few error types.
### is.codion.swing.common.model
- ProgressWorker.Builder.onException() now Exception based instead of Throwable.
### is.codion.swing.common.ui
- Control.Builder.onException() added.
- AbstractControl now uses a weak listener on the enabled state in order to prevent memory leaks.
- Control.Builder.onException() now handles Exception instead of Throwable.
- TemporalFieldPanel and TextFieldPanel, button now consistent with other input field related buttons.
- ProgressWorkerDialogBuilder.onException() now Exception based instead of Throwable.
### is.codion.framework.model
- EntityEditModel.revert() added.
- AbstractEntityEditModel.Insert, Update and Delete, refactoring and renaming.
### is.codion.swing.framework.ui
- FrameworkIcons.columns() added, used in EntityTablePanel popup menu control.
- EntityDialogs, edit panel now used as exception handler for the add and edit actions.
- EntityDialogs, lambdas replaced with named classes.
- EntityComboBoxPanel and EntitySearchFieldPanel bug fixed, builder() did not return correct type.
- EntityEditComponentPanel, EntityEditPanel, memory leak fixed, FocusActivationListener and FocusedInputComponentListener now static.
- EntityDialogs.DefaultEditEntityDialogBuilder now reverts all changes before dialog is shown when no entity is provided.
- EntityEditComponentPanel.onException() now Exception based instead of Throwable.
- EntityDialogs, exception handlers now Exception based instead of Throwable.
- EntityPanel.displayException() now Exception based instead of Throwable.
- EntityTablePanel.onException() and displayException() now Exception based instead of Throwable.

## 0.17.36
### is.codion.swing.common.ui
- ComponentBuilder.focusCycleRoot() added.
### is.codion.framework.domain
- AbstractCondition no longer validates that all columns are from the same entity type, custom conditions require more flexibility.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.definitions() now accepts conditions with columns from multiple entity types.
### is.codion.framework.model
- EntityEditModel.delete() now returns the deleted entities.
- EntityTableModel.deleteSelected() now returns the deleted entities.
- EntityEditModel.changeEvent() renamed valueEvent().
### is.codion.swing.framework.ui
- EntityPanel.DetailLayout.layout(), entityPanel parameter removed, now returns Optional, related changes.
- EntityPanel.DetailController reintroduced, related changes.
- WindowDetailLayout.WindowDetailController.select() bug fixed, null check added.
- WindowDetailLayout static factory methods removed.
- EntityPanel bug fixed, minimum panel size restricted detail split pane adjustment.
- EntityPanel.Selector removed.
- EntityPanel.DetailLayout.select() and EntityApplicationPanel.select() renamed activated().
- TabbedDetailLayout and WindowDetailLayout refactored and simplified.
- EntityPanel.detailController() added.
- EntityApplicationPanel applicationLayout constructor parameter now function based, related refactoring.
- EntityPanel.Config.editPanelState() added.
- EntityControls, OK button now only enabled when input is valid, refactoring.
- EntityDialogs.editDialog() renamed editAttributeDialog(), addEntityDialog() and editEntityDialog() added.
- EntityControls now uses EntityDialogs, related refactoring.
- EntityPanel.DetailLayout.NO_LAYOUT renamed NONE.
- EntityEditPanel.insertCommand(), updateCommand() and deleteCommand() added.
- EntityDialogs, minor improvements and refactoring.
- FrameworkMessages.insert() added.
- EntityTablePanel constructor overloaded with edit panel parameter, adds Add and Edit controls when one is provided.
- EntityEditPanel.KEYBOARD_SHORTCUTS added.
- TabbedApplicationLayout.applicationPanel() added.
- EntityTablePanel, add and edit controls added to toolbar.

## 0.17.35
### is.codion.common.model
- TableSelectionModel, add/removeListener methods replaced with methods that return the underlying event observer.
- ColumnConditionModel, add/removeListener methods replaced with methods that return the underlying event observer.
- TableConditionModel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredModel.Refresher, add/removeListener methods replaced with methods that return the underlying event observer.
- LoadTest, add/removeListener methods replaced with methods that return the underlying event observer.
- ColumnSummaryModel.SummaryValueProvider, add/removeListener methods replaced with methods that return the underlying event observer.
### is.codion.framework.db
- EntityConnectionProvider, add/removeListener methods replaced with methods that return the underlying event observer.
### is.codion.framework.model
- EntityEditModel.setDefaults() renamed defaults().
- EntityEditModel.setDefault() renamed defaultValue(), now Value based.
- EntityEditModel.Insert, Update and Delete methods renamed.
- EntityEditModel.Insert, Update and Delete split into separate steps, related refactoring.
- EntityEditModel.refreshEntity() renamed refresh().
- EntityEditModel.referencedEntity() removed.
- EntityEditModel, add/removeListener methods replaced with methods that return the underlying event observer.
### is.codion.swing.common.model
- FilteredTableSortModel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredTableSearchModel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredComboBoxModel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredTableModel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredTableColumnModel, add/removeListener methods replaced with methods that return the underlying event observer.
- NullableToggleButtonModel, add/removeListener method replaced with stateObserver() method.
### is.codion.swing.common.ui
- TemporalField, add/removeListener methods replaced with methods that return the underlying event observer.
- CalendarPanel, add/removeListener methods replaced with methods that return the underlying event observer.
- ColumnConditionPanel, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredTable, add/removeListener methods replaced with methods that return the underlying event observer.
- FilteredTableConditionPanel, add/removeFocusGainedListener() methods replaced focusGainedObserver() method.
- NumberField.addListener() replaced with numberObserver() method.
### is.codion.swing.common.ui.tools
- LoadTestPanel, add/removeSelectedItemListener() methods replaced selectedItemsObserver() method.
### is.codion.swing.framework.ui
- EntityPanel, add/removeListener methods replaced with methods that return the underlying event observer.
- EntityApplicationPanel.addInitializationListener() replaced with initializedObserver().
- EntityApplicationPanel.addExitListener() replaced with exitObserver().
### is.codion.swing.framework.server.monitor
- ConnectionPoolMonitoring, add/removeStatisticsListener() methods replaced statisticsObserver() method.
- HostMonitor, add/removeServerAddedListener() and add/removeServerRemovedListener() methods replaced with serverAddedObserver() and serverRemovedObserver().
### is.codion.*
- Observer replaced with Event as suffix for methods returning EventObserver instances, related changes.

## 0.17.34
### is.codion.common.core
- ValueSetObserver added, related refactoring.
- ValueSetObserver now extends Iterable.
- ValueSetObserver.size() added.
- Primitives removed.
- Rounder removed.
### is.codion.framework.i18n
- FrameworkMessages.confirmDeleteSelected() removed.
### is.codion.swing.framework.model
- EntityModel.activeDetailModels() now returns ValueSetObserver, addActiveDetailModelsListener() and removeActiveDetailModelsListener() removed.
- EntityModel, EntityEditModel, EntityTableModel, EntityApplicationModel.connection() convenience method added.
### is.codion.swing.framework.ui
- EntityPanel.detailLayout() now protected instead of package private.
- EntityPanel.selectDetailPanel() removed for now.
- EntityPanel.containsDetailPanel() removed.
- EntityPanel.detailPanel(entityType) refactored, error message improved.
- EntityPanel bug fixed, editControlTablePanel was not created in case of no table panel, renamed mainPanel, tests added.
- TabbedDetailLayout, static factory methods removed.
- EntityEditPanel, confirmers moved to Config.
- EntityTablePanel, deleteConfirmer moved to Config.
- EntityTablePanel, unknown_dependent_records i18n message moved to EntityEditPanel.
- EntityPanel.DetailLayout.layout() now throws exception in case the panel has already been laid out.
- EntityApplicationPanel.ApplicationLayout.layout() now throws exception in case the panel has already been laid out.
- EntityApplicationPanel.ApplicationLayout.applicationTabPane() renamed tabbedPane().
- EntityTablePanel.StatusPanel.configureLimit() now uses an input validator.
- EntityPanel.DetailLayout.panelState() now throws exception in case the panel has not been laid out.

## 0.17.33
### is.codion.common.core
- ValueSet.addAll() and removeAll() overloaded with Collection parameter.
### is.codion.swing.framework.ui
- EntityPanel, initialization refactored.
- EntityPanel, initialization refactored further, PanelLayout renamed DetailLayout, now only handles laying out entity panels containing one or more detail panels.
- TabbedDetailLayout, bunch of renaming due to recent changes.
- DetailController merged with DetailLayout.
- TabbedDetailLayout resizing bug fixed.
- WindowDetailLayout added, used in SchemaBrowser demo.
- EntityPanel.description() now Value based, for consistency.
- EntityEditPanel.Config.focusActivation added, related refactoring.
- EntityPanel, initialization refactored for easier customization.
- EntityTablePanel, status message now updated dynamically while the selection is being adjusted.
- EntityTablePanel, static configuration values moved to Config.
- EntityPanel, static configuration values moved to Config.
- EntityEditPanel, static configuration values moved to Config.
- EntityTablePanel.Config.SUMMARY_PANEL_VISIBLE configuration value added.
- EntityPanel.editControlPanel() and editControlTablePanel() replaced with mainPanel(), related refactoring.
- EntityEditComponentPanel.excludeComponentFromSelection() replaced with selectableComponents().
- WindowDetailLayout now requests initial focus when the window is initialized.
- EntityEditComponentPanel, minor refactoring + javadoc improvements.

## 0.17.32
### is.codion.common.core
- ValueSet.contains() and containsAll() added.
### is.codion.swing.common.model
- DefaultFilteredTableModel bug fixed, did not trigger dataChanged events when removing items by index.
### is.codion.swing.common.ui
- ComponentBuilder implementations, lambdas replaced with static classes to prevent memory leaks.
- FilteredTableConditionPanel, now handles configuring the horizontal alignment of condition text fields according to the column cell renderer.
- FontSizeSelectionDialogBuilder.selectFontSize() now returns OptionalInt.
### is.codion.framework.i18n
- FrameworkMessages, confirm update and confirm insert messages improved.
- FrameworkMessages, no search results message improved.
### is.codion.framework.db.core
- EntityConnection.Select.limit(), offset() and fetchDepth() now return OptionalInt.
### is.codion.swing.framework.ui
- EntitySearchField.selectorFactory() now Value based.
- EntitySearchField.searchIndicator() now Value based.
- EntityEditComponentPanel.attribute(JComponent) bug fixed, did not take into account that the component is wrapped in a Value.
- EntityEditComponentPanel.setComponentBuilder() bug fixed, did not prevent the creation of a second component if the component had already been built.
- EntityEditComponentPanelTest added.
- EntityEditComponentPanel.excludeComponentsFromSelection() replaced with ValueSet based excludeComponentFromSelection().
- EntityComponents, range validation no longer added to number fields by default.
- EntityDialogs.DefaultEntityEditDialogBuilder.InputValidator now also uses the entity validator when validating input.
- EntityTablePanel refactored.
- EntityTablePanel, settings now configured in a constructor lambda.
- EntityPanel, settings now configured in a constructor lambda.
- EntityTablePanel.Settings.editableAttributes() renamed editable().
- EntityPanel.Settings and EntityTablePanel.Settings renamed Config.
- EntityPanel.configure() removed.
- EntityPanel.addDetailPanel() exceptions improved, test added.
- EntityPanel, EntityTablePanel, configuration instance now defensively copied.
- EntityTablePanel.setEditComponentFactory() and setTableCellEditorFactory() moved to Config.
- EntityTablePanel.referentialIntegrityErrorHandling() moved to Config.
- EntityTablePanel.refreshButtonVisible() moved to Config.
- EntityTablePanel.statusMessage() moved to Config.
- EntityTablePanel.showRefreshProgressBar() moved to Config.
- EntityEditPanel.Config added.

## 0.17.31
### is.codion.common
- DefaultValueSet, synchronization added.
### is.codion.framework.model
- EntityEditModel.Insert, Update and Delete added.
- AbstractEntityEditModel.createInsert(), createUpdate() and createDelete() added, providing support for async CRUD operations.
- EntityEditModel.Insert.validate() and Update.validate() removed, createInsert() and createUpdate() now perform the validation.
- DefaultEntityModel.onMasterSelectionChanged() bug fixed, no longer fetches the active entities in case of no detail models.
### is.codion.swing.common.model
- FilteredTableModel.RemovedRows removed.
- DefaultFilteredTableModel now removes or adds the default RemoveSelectionListener when a JTable is added or removed as a tableModelListener, in order to avoid duplicating the selection removal functionality.
- DefaultFilteredTableModel.removeItems() now calls setValueIsAdjusting() on the selection model to prevent a selection change event on each item removal.
- DefaultFilteredTableModel, dataChangeListener no longer notifies on each removal when removing multiple items, for real this time.
### is.codion.swing.common.ui
- FilteredTableCellRenderer.horizontalAlignment() added.
- ColumnSummaryPanel.columnSummaryPanel() horizontalAlignment parameter added.
### is.codion.swing.framework.ui
- EntityPanel.addKeyEvent() and removeKeyEvent() added, editControlPanel() accessor removed.
- EntityEditPanel, EntityTablePanel, EntityDialogs CRUD operations now performed in a background thread.
- TabbedPanelLayout refactored.
- TabbedPanelLayout.Builder.includeDetailTabPane() renamed includeDetailTabbedPane().
- EntityPanel, edit window location now relative to table panel, if one is available.
- EntityEditPanel bug fixed, did not validate before insert and update.
- EntityTablePanel.createSouthToolBar() renamed createToolBar().
- EntityTablePanel.Settings.includeSummaryPanel() added along with INCLUDE_SUMMARY_PANEL configuration value.
- EntityTablePanel, column summary field horizontal alignment no follows column cell renderer alignment.
- EntityTablePanel.Settings.includeEntityMenu() added.
- EntityTablePanel.INCLUDE_POPUP_MENU configuration value added.
- EntityTablePanel.table now initialized lazily.
- EntityTablePanel.delete(), deleteWithConfirmation() and editSelectedEntities() renamed deleteSelected(), deleteSelectedWithConfirmation() and editSelected() respectively.
- EntityTablePanel.printTable() removed.
- EntityTablePanel.setReferentialIntegrityErrorHandling() replaced with Value based referentialIntegrityErrorHandling().
### is.codion.swing.framework.server.monitor
- ServerMonitor and ServerMonitorPanel, tables migrated from plain JTable to FilteredTable.

## 0.17.30
### is.codion.common
- Text.underscoreToCamelCase() moved to DomainToString, no longer public.
### is.codion.common.model
- LoadTest, refactoring and renaming.
- UsageScenario renamed Scenario and moved to LoadTest.
- AbstractScenario, run counting removed.
- LoadTest.Scenario.maximumTime() removed.
- LoadTest.Scenario.Builder introduced along with LoadTest.Performer, AbstractScenario removed, related refactoring.
- LoadTest.Scenario, couple of scenario() factory methods added.
- LoadTest.title() renamed name(), related refactoring.
- DefaultLoadTest.stop() missing synchronization added, addApplicationBatch() superflous syncronization removed.
### is.codion.common.rmi
- SerializationWhitelist, now allows arrays but checks the component type.
- SerializationWhitelist.DryRun, now excludes arrays and includes the component type.
### is.codion.swing.common.ui
- CalendarPanel.Builder.initialValue() now accepts null.
- DefaultCalendarDialogBuilder refactored.
- CalendarPanel, day buttons now with zero margin insets, month spinner now displays short name.
- CalendarPanel.Builder.includeTodayButton() added.
### is.codion.swing.common.ui.tools
- LoadTestPanel.loadTestPanel() factory method added, constructor now private.
### is.codion.framework.domain
- IdentityKeyGenerator now a singleton.
- EntityInvoker removed.
- AttributeDefinition.beanProperty() removed.
- Entity, ForeignKeyCondition, method arguments simplified due to EntityInvoker removal.
- Entity.mapToPrimaryKey() now throws exception in case a non-unique primary key is encountered.
### is.codion.framework.db.core
- EntityConnection, method arguments simplified due to EntityInvoker removal.
### is.codion.framework.db.local
- LocalEntityConnection, method arguments simplified due to EntityInvoker removal.
### is.codion.framework.db.http
- HttpEntityConnection, method arguments simplified due to EntityInvoker removal.
### is.codion.framework.db.rmi
- RemoteEntityConnection, method arguments simplified due to EntityInvoker removal.
### is.codion.framework.model
- EntityEditModel, method arguments simplified due to EntityInvoker removal.
- EntityEditModel.containsSearchModel() removed.
### is.codion.swing.framework.model
- SwingEntityEditModel.containsComboBoxModel() removed.
### is.codion.swing.framework.ui
- EntityDialogs, method arguments simplified due to EntityInvoker removal.
- EntityTablePanel.beforeDelete() removed.
### is.codion.swing.framework.model.tools
- DomainToString bug fixed, now handles the mapping from an unknown column data type to an object based Column correctly, DatabaseDomain some renaming.
- DomainGeneratorModel, table type column moved.
- EntityLoadTestModel removed.
- LoadTest extracted from LoadTestModel and moved to common.model.loadtest, related refactoring.
- LoadTestModel, refactoring and renaming.
- LoadTestModel no longer extends LoadTest.
- LoadTestModel now handles all counting runs.
- LoadTestModel no longer counts runs exceeding maximum scenario time.
- AbstractEntityPerformer replaced with EntityLoadTestUtil.

## 0.17.29
### is.codion.common.db
- ReferentialIntegrityException.operation() added.
- AbstractDatabase and subclasses, null handling improved.
### is.codion.dbms.postgresql
- PostgreSQLDatabase.errorMessage() refactored, now handles referential integrity errors for update and insert as well as delete.
### is.codion.swing.common.ui
- FilteredTable, column tooltips now enabled when selecting columns.
- DefaultSelectionDialogBuilder bug fixed, did not set locationRelativeTo().
### is.codion.framework.domain
- DualValueColumnCondition bug fixed, null value checking too restrictive, prevented enabling of conditions when no values were specified.
### is.codion.swing.framework.model
- SwingEntityTableModel.setValueAt() bug fixed, no longer tries to update an unmodified entity.
### is.codion.swing.framework.model.tools
- DatabaseExplorerModel, caption based on table name, identity key generator, views readOnly by default.
- DatabaseExplorerModel renamed DomainGeneratorModel, moved to generator package.
- DomainToString, default IntelliJ indentation used.
### is.codion.swing.framework.ui
- EntityEditPanel.onReferentialIntegrityException() now only displays dependencies in case of a DELETE operation.
- EntityTablePanel.selectConditionPanel(), now only displays the condition panel in case one is selected.
- EntityDialogs.DefaultEntityEditDialogBuilder bug fixed, did not set locationRelativeTo().
- EntityDialogs.EntitySelectionDialog bug fixed, did not set locationRelativeTo().
### is.codion.swing.framework.ui.tools
- DatabaseExplorerPanel renamed DomainGeneratorPanel, moved to generator package.

## 0.17.28
### is.codion.common.core
- ValueObserver.equalTo() and notEqualTo() renamed isEqualTo() and isNotEqualTo() respectively, now consistent with isNull() and isNotNull() and less likely to be confused with equals().
### is.codion.common.db
- FunctionType, ProcedureType.execute() removed.
- ReportType.fill() removed.
### is.codion.common.dbms
- Modules h2database and db2database renamed h2 and db2 respectively.
### is.codion.common.model
- FilteredModel.Refresher.asyncRefresh() renamed async().
### is.codion.framework.domain
- DefaultColumn.BooleanConverter, now throws exception in case the values representing true and false are equal.
### is.codion.framework.db.core
- Update.columnValues() renamed values().
- DefaultUpdate.DefaultBuilder.build() now throws exception in case of no values to update.
### is.codion.framework.servlet
- Javalin upgraded to v6.
### is.codion.framework.domain.test
- EntityTestUnit.insertOrSelect() error message now includes entity being inserted.
### is.codion.swing.common.ui
- FilteredTableColumn.toolTipText() and FilteredTableColumn.Builder.toolTipText() added.
- FilteredTable now uses a custom header for displaying column tooltips.
- DefaultLookAndFeelProvider bug fixed, cross platform look and feel now takes precedence over system look and feel.
### is.codion.swing.framework.model
- SwingEntityColumnFactory now uses the attribute descriptions as column tooltips.
### is.codion.swing.framework.model.tools
- DatabaseExplorerModel, now displays the entity table type (view/table).
- DatabaseExplorerModel, now trims table and column comments.
- DatabaseExplorerModel, now escapes double quotes in table and column comments.
- DatabaseExplorerModel, refactoring and renaming of related classes.
### is.codion.swing.framework.ui.tools
- DatabaseExplorerPanel now with FlatLaf available.

## 0.17.27
### is.codion.common.core
- Value, default implementations added for accept() and map().
### is.codion.framework.domain
- DefaultEntityDefinition.validatePrimaryKeyAttributes(), key column indexes now sorted before being displayed in error message.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.delete(keys) bug fixed, maximum number of parameters taken into account.
- DefaultLocalEntityConnection.update() now throws UpdateException in case of unmodified entities.
- DefaultLocalEntityConnection bug fixed, maximum number of parameters did not take composite keys into account.
### is.codion.swing.framework.ui
- EntityTablePanel, popup menu for configuring the limit added to status message panel along with INCLUDE_LIMIT_MENU configuration value, default false.
### is.codion.framework.model
- AbstractEntityEditModel.insert(), update() and delete(), exception order now consistent.
- AbstractEntityEditModel refactored, events, states and related methods moved to separate inner classes.
- AbstractEntityEditModel.modified() now final, modifiedPredicate() should suffice.
- AbstractEntityEditModel.updateModifiedState() added.
- AbstractEntityEditModel.modifiedState() removed.
- AbstractEntityEditModel.modified(entities) removed.
- EntityEditModel.validator() removed.
- AbstractEntityEditModel.validator() now protected and Value based, overloaded constructor removed.
- AbstractEntityEditModel, validator, modifiedPredicate and existsPredicate values now trigger an update in their respective states when changed.
### is.codion.swing.framework.ui
- SwingEntityTableModel.handleEditEvents() renamed editEvents() for consistency.

## 0.17.26
### is.codion.common.core
- EventObserver now accepts data listeners for value type superclasses.
### is.codion.common.rmi
- Clients.resolveTrustStore() now uses the default 'changeit' truststore password when combining truststores instead of throwing an exception in case no truststore password is provided.
### is.codion.swing.common.ui
- Dialogs.createBrowseAction() removed.
- SelectionDialogBuilder.Selector renamed SingleSelector, Dialogs.selector() renamed singleSelector().
- SelectionDialogBuilder.MuliSelector added along with Dialogs.multiSelector().
### is.codion.framework.domain
- ColumnDefinition, ColumnDefinition.Builder.searchColumn() renamed searchable().
- EntityDefinition.Columns.searchColumns() renamed searchable().
- EntityDefinition.Builder.conditionProvider() renamed condition().
### is.codion.swing.framework.ui
- EntityPanel.setDisposeEditDialogOnEscape() replaced with State based disposeEditDialogOnEscape().
- EntityPanel.INCLUDE_DETAIL_PANEL_CONTROLS removed, replaced by TabbedPanelLayout.INCLUDE_DETAIL_CONTROLS.
- EntityDialogs.EditEntityComponentFactory.componentValue(), now sets the columns for text input panels, preventing multi-screen spanning dialogs in case of very long strings.
- EntityApplicationPanel.createRefreshAllControl() removed.

## 0.17.25
### is.codion.common.db
- Database.setConnectionProvider() renamed connectionProvider().
### is.codion.common.model
- DefaultColumnConditionModel, wildcard handling with regex improved.
### is.codion.swing.common.model
- DefaultFilteredTableModel.CombinedIncludeCondition now uses ColumnValueProvider.string() in case of String based conditions.
- DefaultFilteredTableModel.CombinedIncludeCondition bug fixed, now replaces empty string with null before comparing.
- FilteredTableModel.ColumnValueProvider.comparable() default implementation no longer returns a string for non-comparable values.
### is.codion.swing.common.ui
- Icons.setIconColor() renamed iconColor(), FontImageIcon.setColor() renamed color().
- FilteredTable.setAutoStartsEdit() renamed autoStartsEdit().
- SearchHighlighter.setHighlightColor() and setSelectedHighlightColor() renamed highlightColor() and highlightSelectedColor() respectively.
- TemporalFieldPanel, TextFieldPanel.setTransferFocusOnEnter() renamed transferFocusOnEnter().
### is.codion.framework.domain
- EntityDefinition.PrimaryKey.columnDefinitions() renamed definitions().
### is.codion.swing.framework.model
- SwingEntityTableModel.EntityFilterModelFactory now creates String based column condition models for entity, item and non-comparable based columns.
### is.codion.swing.framework.model
- SwingEntityModel.Builder.modelClass(), editModelClass(), tableModelClass(), modelFactory(), editModelFactory(), tableModelFactory() and detailModelBuilder() renamed model(), editModel(), tableModel() and detailModel().
- SwingEntityModel.Builder.buildEditModel() and buildTableModel() removed, buildModel() renamed build().
### is.codion.swing.framework.ui
- TabbedApplicationLayout bug fixed, now applies the border to the application panel, instead of only the tabbed pane.
- EntityTablePanel.DefaultStatusMessage bug fixed, no longer returns an empty message when all rows are filtered.
- EntityTablePanel.DefaultStatusMessage, 'hidden' replaced with 'filtered'.
- EntityPanel, minor refactoring.
- EntityDialogs.EditDialogBuilder.Updater now with generic edit model type parameter.
- EntitySearchField.Selector.setPreferredSize() renamed preferredSize().
- EntitySearchField.setSelectorFactory() renamed selectorFactory().
- EntityPanel.Builder.panelClass(), editPanelClass(), tablePanelClass(), detailPanelBuilder() and panelLayout() renamed panel(), editPanel(), tablePanel(), detailPanel() and layout().
- EntityPanel.Builder.buildEditPanel() and buildTablePanel() removed, buildPanel() renamed build().
- EntityTablePanel.setRefreshButtonVisible() replaced with Value based refreshButtonVisible().

## 0.17.24
### is.codion.common.core
- Text.collate() now returns the sorted list, Text.spaceAwareCollator() renamed collator(). Text.collateSansSpaces() removed.
- AbstractValue, DefaultValueSet and DefaultState fields now initialized lazily.
### is.codion.swing.common.ui
- Utilities.printFocusOwner() bug fixed, no longer keeps adding listeners on subsequent calls. Component string now includes identity hash code in order to distinguish between instances of the same class.
- ControlPanelBuilder, methods for configuring button builders added, related refactoring.
### is.codion.framework.model
- DefaultEntityModel.addDetailModel() bug fixed, no longer calls detailModelLink.selectionChanged() on all detail models each time the active detail model changes, only on the detail model being activated.
### is.codion.swing.framework.ui
- EntityPopupMenu bug fixed, cyclic reference detection was a bit too simplistic, each entity appeared only once, even though referenced via different foreign keys.
- EntityEditComponentPanel.component(attribute) now Value based, setComponent() removed.
- EntityPanel, TabbedPanelLayout, focus handling when navigating between panels improved, jumps less.
- EntityEditComponentPanel.transferFocusOnEnter, defaultTextFieldColumns and useModifiedIndicator now Value based.
- EntityEditComponentPanel.useModifiedIndicator() renamed modifiedIndicator().
- EntityEditPanel.setConfirmer() removed, confirmers now Value based and configured via confirmer().
- EntityTablePanel.setDeleteConfirmer() removed, deleteConfirmer now Values based and configured via deleteConfirmer().
- EntityComboBoxPanel, EntitySearchFieldPanel, add/edit buttons now transfer focus on enter when focusable.
- EntityEditComponentPanel.Defaults added with textFieldColumns() replacing defaultTextFieldColumns(), accessible via defaults().
- EntityEditComponentPanel.Defaults.foreignKeySearchFieldColumns(), comboBoxPreferredWidth(), itemComboBoxPreferredWidth() and foreignKeyComboBoxPreferredWidth() added.
- EntityComboBoxPanel.Builder.comboBoxPreferredWidth() added.
- EntityPanel, edit panel now activated on mouse click, not only when a subcomponent receives focus.

## 0.17.23
### is.codion.common.core
- LocaleDateTimePattern.timePattern() now returns Optional.
### is.codion.common.db
- AbstractDatabase, transaction isolation now a final field, instead of using the configuration value directly.
- AbstractDatabase, login timeout now set in a static initializer block, instead of during connection creation.
- Database.connectionPool(username) now throws IllegalArgumentException in case no connection pool is available for the given username.
- ResultPacker.get() no longer allowed to return null.
### is.codion.common.model
- ColumnConditionModel.setEqualValues() no longer accepts null values.
### is.codion.common.rmi
- SerializationWhitelist.writeToFile() improved a bit, javadocs fixed.
- SerializationWhitelist no longer public, refactored.
### is.codion.framework.domain
- EntityDefinition.orderBy() and selectQuery() now return Optional.
- AttributeDefinition.mnemonic() now primitive based, with 0 replacing null for no mnemonic.
- AbstractAttributeDefinition.validateItems() now returns an unmodifiable copy of the item list, minor refactoring.
### is.codion.framework.model
- EntityTableConditionModel.setEqualConditionValues() no longer accepts null values.
### is.codion.swing.common.model
- DefaultFilteredTableModel.DefaultSummaryValueProvider format no longer nullable.
- FilteredComboBoxModel.setItems() no longer accepts null.
### is.codion.swing.common.ui
- KeyboardShortcuts.copy() added.
- CalendarPanel.Builder added, replaces factory methods
- CalendarPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- TemporalField.Builder.incrementDecrementEnabled() added, for turning off keyboard based increment/decrement of date component under cursor.
- TemporalField.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- TextFieldPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- CalendarPanel, button grid gaps removed for a more compact calendar.
### is.codion.swing.framework.ui
- EntityPanel, EntityTablePanel, keyboard shortcuts can now be configured on an instance basis, via configure().keyStroke().
- TabbedPanelLayout.Builder.keyboardShortcut() added for configuring keyboard shortcuts on an instance basis.
- EntitySearchField.KeyboardShortcut moved to EntitySearchFieldPanel, EntitySearchFieldPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- EntityComboBox.KeyboardShortcut moved to EntityComboBoxPanel, EntityComboBoxPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- TabbedPanelLayout.Builder.keyboardShortcut() renamed keyStroke() for consistency.
- EntityEditComponentPanel.selectInputComponent() bug fixed, now does nothing instead of throwing exception when no input components are selectable.
- TabbedPanelLayout.defaultKeyStroke() bug fixed, incorrect key used for RESIZE_RIGHT.

## 0.17.22
### is.codion.swing.common.core
- Value.map() added.
- Value.mapNull() added.
### is.codion.swing.common.model
- ProgressWorker.Task and ProgressTask.perform() renamed execute().
### is.codion.swing.common.ui
- AbstractButtonBuilder now sets the initial background, foreground and font specified by the action.
- AbstractControl now overrides putValue() and getValue() in order to prevent modification of the 'enabled' property.
- Control.Builder.value() added.
- AbstractControlBuilder added, related refactoring.
- Control and ToggleControl.copyBuilder() added.
- DefaultProgressWorkerDialogBuilder, bug fixed, did not set the locationRelativeTo for the progress dialog, it was always centered on application window. ProgressDialog bug fixed, didn't use the locationRelativeTo value provided by the builder.
- Control and ToggleControl.copyBuilder() renamed copy().
- Control.Command and ActionCommand.perform() renamed execute().
- KeyEvents.builder(KeyStroke) added.
- KeyEvents.Builder.enable() and disable() now with varargs.
- KeyboardShortcut added, first attempt at configurable keyboard shortcuts.
- KeyboardShortcut replaced with KeyboardShortcuts, second iteration of configurable keyboard shortcuts.
- KeyEvents and TransferFocusOnEnter moved to key package.
- KeyboardShortcuts factory method now throws exception in case of a missing default shortcut keyStroke.
### is.codion.framework.domain
- IdentityKeyGenerator now throws exception in case the generated keys result set is empty.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.translateSQLException() renamed databaseException() and moved to Database.
### is.codion.framework.model
- AbstractEntityEditModel, edit event notifications now handled by afterInsert/afterUpdate/afterDelete events.
- AbstractEntityEditModel.insert() no longer calls save() on the entity being inserted, since exists() relies on the original primary key value and is used during validation.
### is.codion.swing.framework.model
- SwingEntityTreeModel.refreshSelect() bug fixed, now handles root nodes correctly.
- SwingEntityTreeModel removed, unused and buggy, may revisit at some point.
### is.codion.swing.framework.ui
- EntityEditPanel now overrides EntityEditComponentPanel.onException() and propagates the exception to the correct exception handler.
- EntityTablePanel.onException() now propagates the exception to the correct exception handler.
- EntityEditPanel.beforeInsert(), beforeUpdate() and beforeDelete() removed.
- EntityTablePanel bug fixed, no longer creates a condition panel if one should not be included.
- EntityEditPanel.confirmInsert(), confirmUpdate() and confirmDelete() now protected, useful when doing custom crud operations.
- EntityTablePanel.confirmDelete() added.
- EntityTablePanel.control() no longer Optional based, containsControl() added.
- EntityEditPanel, EntityTablePanel.setupControls() added for setting up custom controls, called after standard controls have been initialized.
- EntityDialogs.EditDialogBuilder.Updater added for customizing how the actual update is performed when multiple entities are edited, related refactoring.
- EntityTablePanel.editDialogBuilder() added.
- EntityEditPanel, EntityTablePanel.control() now returns a Value containing the control.
- EntityEditPanel.ControlCode and EntityTablePanel.ControlCode renamed EditControl and TableControl respectively.
- EntityEditPanel no longer caches the result of createControls().
- EntityEditPanel, EntityTablePanel no longer overwrite custom controls set before the panel is initialized.
- EntityPanel, EntityTablePanel.Settings added for settings that must be configured before initialization, accessed via configure(), which throws if the panel is initialized.
- EntityTree removed.
- EntityPanel, methods for creating standard controls (refresh, select input field etc.) made protected final.

## 0.17.21
### is.codion.swing.common.model
- FilteredComboBoxModel.allowSelectionPredicate() renamed validSelectionPredicate().
### is.codion.swing.common.ui
- Controls.SEPARATOR added, used instead of null to represent separators in Controls instances.
- HintTextField bug fixed, now adjusts the hint text length to prevent painting outside of bounds.
- FilteredTable search field now has a minimum size instead of columns.
- TextFieldBuilder.DEFAULT_TEXT_FIELD_COLUMNS configuration value removed.
- Utilities.updateComponentTreeUI() added.
- FilteredTableConditionPanel no longer overrides updateUI(), seems unnecessary.
### is.codion.framework.model
- EntitySearchModel.LIMIT renamed DEFAULT_LIMIT.
- EntityTableModel and EntityComboBoxModel.respondToEditEvents() renamed handleEditEvents().
### is.codion.swing.framework.model.tools
- ColumnPacker now creates an Object based column for columns with unknown types, instead of ignoring them.
- MetaDataModel bug fixed, now prevents schemas from being populated more than once.
### is.codion.swing.framework.ui
- EntityPanel bug fixed, table refresh control was always enabled, instead of only when the panel was active.
- EntityPanel.createControls() added, related refactoring.
- EntityPanel.createControlsComponent() renamed createControlComponent().
- EntityPanel.editPanel() and tablePanel() now throw IllegalStateException in case no edit panel or table panel is available.
- EntityTablePanel south panel split pane resize weight no longer specified, for a more consistent initial search field size.
- EntityPanel.INCLUDE_CONTROLS configuration value added.
- EntityEditComponentPanel.DEFAULT_TEXT_FIELD_COLUMNS configuration value added.
- EntityTablePanel no longer overrides updateUI(), calls updateComponentTreeUI() after initialization instead, in case the look and feel has changed since instantiation.

## 0.17.20
### is.codion.common.model
- DefaultColumnConditionModel.accept() no longer returns true in case the condition model is disabled.
- DefaultColumnConditionModel.accept() bug fixed, case insensitivity only worked if a wildcard was present, related refactoring.
- DefaultColumnConditionModel bug fixed, case insensitivity now applies to Character as well as String values.
### is.codion.framework.domain
- SingleValueColumnCondition bug fixed, useLikeOperator now included in equals() and hashCode().
- ColumnCondition.equalToIgnoreCase(Character value) and notEqualToIgnoreCase(Character value) added, related refactoring.
- AbstractAttributeDefinition, Character based attributes now with default maximum length of 1.
- SingleValueColumnCondition refactored to reduce complexity.
- EntityDefinition.staticData() removed.
### is.codion.framework.db.local
- DefaultLocalEntityConnection, optimistic locking now only compares values which are available in the entity being updated, otherwise excluding attributes when selecting entities prevents those entities from being updated, since optimistic locking would always fail due to the missing values.
### is.codion.framework.model
- EntityEditModel.refreshing() removed() along with related functionality in AbstractEntityEditModel.
### is.codion.swing.common.ui
- ColumnConditionPanel bug fixed, case sensitivity control now available for Character based condition panels.
- Components.textField() and textField(Value<String> linkedValue) renamed stringField().
- Components.characterField() and characterField(Value<Character> linkedValue) added.
- ColumnConditionPanel bug fixed, Character added to supported types.
- DefaultComboBoxBuilder now adds a Refresh control to the popup menu by default if the underlying model is a FilteredComboBoxModel, related refactoring.
### is.codion.swing.framework.model
- SwingEntityTableModel.EntityFilterModelFactory now creates String based filter condition models for Entity and Item based columns.
- SwingEntityTableModel.selectAttributes() bug fixed, always returned an empty collection.
- EntityComboBoxModel.staticData() removed along with forceRefresh().
### is.codion.swing.framework.model.tools
- DatabaseDomain, DomainToString, no longer sets the maximum column size for TEXT columns.
- MetaDataModel, DomainGeneratorModel, catalog now available for schemas.
- DatabaseDomain, DomainGeneratorModel, some refactoring.
### is.codion.swing.framework.ui
- EntityTablePanel, table status message now indicates whether the result is limited.
- EntityTablePanel.INCLUDE_CONDITION_PANEL added.
- EntitySearchField bug fixed, now uses the default text input cursor when replacing the wait cursor, instead of Cursors.DEFAULT_CURSOR (arrow).
- EntityConditionPanelFactory bug fixed, Character added to supported types.
- EntityPanel.setIncludeEditControls() renamed setIncludeControls(), setControlPanelConstraints() renamed setControlsComponentConstraints() and createEditControls() renamed createControlsComponent().
- EntityEditPanel.ControlCode.REFRESH removed, EntityPanel.createControlsComponent() now responsible for including the table panel refresh control.
- EntityComboBox.forceRefresh() removed.
### is.codion.swing.framework.ui.tools
- DatabaseExplorerPanel, schema having been populated no longer prevents it from being repopulated, in case of changes, popup menu added to schema table.
- DatabaseExplorerPanel, column selection and auto-resize popup menu controls added.

## 0.17.19
### is.codion.common.db
- AbstractConnectionPoolWrapper, prevent destroyed connection counter being called twice for each closed connection, see https://github.com/brettwooldridge/HikariCP/issues/2152.
### is.codion.framework.domain
- Condition.customCondition() renamed custom().
- BlobColumnDefinition removed, ColumnDefinition.lazy() added. Default behaviour for byte array columns being lazy and hidden removed. Related changes.
- DefaultEntityBuilder, defensive copying of value sets added.
- Entity.loaded(ForeignKey) removed, only used in tests.
### is.codion.framework.db
- EntityConnection.readBlob() and writeBlob() removed.
### is.codion.framework.model
- EntityTableModel.attributes() added, specifying the attributes to include when selecting. Implemented in SwingEntityTableModel, replacing overridable attributes() method. Related refactoring.
### is.codion.swing.framework.model
- SwingEntityTableModel.getColor() renamed toColor().
### is.codion.swing.framework.ui
- EntityPanel.createEditControlPanel() renamed createEditControls().

## 0.17.18
- Text.padString() deprecated for removal along with Text.ALIGNMENT, Text.leftPad() and rightPad() added.
- EntityTable.layoutPanel(), tablePanel parameter replaced with tableComponent, no longer restricted to JPanel.
- Jasper Reports and Apache Commons Logging upgraded.
- Logback and Spotless upgraded.
- EntityServerConfiguration, connectionPoolProvider renamed connectionPoolFactory
- ConnectionPoolFactory.createConnectionPoolWrapper() renamed createConnectionPool()
- WaitCursor replaced with Cursors utility class, wait cursor usage reduced and simplified throughout.
- EntityEditPanel.beforeInsert() and beforeUpdate() no longer throw ValidationException, now called outside of try/catch block along with beforeDelete(). EntityTablePanel.beforeDelete() moved outside of try/catch block, delete() added for deleting without confirmation.
- Commons Logging upgrade to 1.3.0 caused NoClassDefFoundError: org/apache/commons/logging/LogFactory in modular demo applications, reverted back to 1.2.
- Text.padString() removed along with Text.ALIGNMENT.
- EntityEditPanel.controls() added, public, can be used instead of the protected createControls().