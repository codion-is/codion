Codion Change Log
=================

## 0.18.53
### is.codion.common.db
- ResultIterator removed.
### is.codion.framework.db
- DefaultSelect bug fixed, having clause not included in hashCode().
- Select.having() added, related refactoring.
- EntityConnection.iterator() added.
### is.codion.framework.db.local
- LocalEntityConnection.iterator() removed.
### is.codion.framework.db.rmi
- DefaultRemoteEntityConnectionProvider now handles iterator() with RemoteEntityResultIteratorWrapper.
### is.codion.framework.server
- DefaultRemoteEntityConnection, EntityConnection.iterator() implemented with timeouts.
### is.codion.swing.framework.ui
- EntityTableExportPanel refactored, lots of stuff moved to EntityTableExport, which is now called EntityTableExportModel.

## 0.18.52
### is.codion.common.core
- LocaleDateTimePattern.createFormatter() renamed formatter().
### is.codion.swing.common.ui
- Windows.sizeWithinScreenBounds() renamed resizeToFitScreen(), now uses the window graphicsConfiguration instead of the default one.
- ProgressWorkerDialogBuilder.onResult(message), title parameter added.
### is.codion.framework.domain
- ColumnCondition.wildcard() added.
### is.codion.framework.json.domain
- ColumnConditionSerializer and ColumnConditionDeserializer bug fixed, now handle wildcard conditions.
### is.codion.framework.db.http
- HttpEntityConnection.JSON configuration value (codion.client.http.json) no longer defaults to true.
### is.codion.framework.lambda
- LambdaEntityHandler bug fixed, PROCEDURE, DELETE_BY_KEY and SET_QUERY_CACHE_ENABLED added to operations without return value.
### is.codion.framework.model
- AbstractEntityEditModel.configureSearchModel() renamed configure().
### is.codion.swing.framework.model
- SwingEntityEditModel.configureComboBoxModel() renamed configure().
### is.codion.swing.framework.ui
- EntityTablePanel, EntityDialogs, minor i18n message improvements.
- EntityTableExportPanel split from EntityTableExport, related refactoring and minor improvements.
- EntityTableExportPanel configuration now included in user preferences.
- EntityTableExportPanel, export to file added.
- EntityTableExportPanel, alt-click toggles attribute inclusion, help text added, minor improvements.
- EntityTableExportPanel, success messages added, minor fixes.
- EntityTableExportPanel now supports cyclical foreign keys.
- EntityTableExportPanel now automatically expands to show selected attributes when initialized.
- EntityTableExport now cleans up the file if export is cancelled.
- EntityTableExportPanel, added save and open configuration.
- EntityTablePanel.ColumnSelection, AutoResizeModeSelection and EditAttributeSelection enums combined into SelectionMode.
- EntityTablePanel.Config.includeExport() added along with INCLUDE_EXPORT configuration property, default false.
- EntityTablePanel, copy expanded rebranded as export.
- EntityTableExportPanel now uses export instead of copy.
- EntityTablePanel bug fixed, includeExport not set in Config copy constructor, tests fixed.

## 0.18.51
### is.codion.swing.common.ui
- FilterTable.Builder.hiddenColumns() added.
- ProgressDialog.northPanel(), westPanel() and eastPanel() renamed northComponent(), westComponent() and eastComponent(), now take JComponent instead of JPanel.
- DefaultFilterTableCellEditor bug fixed, now sets the border from the renderer, like DefaultCellEditor, checkboxes no longer jump around.
- FilterTableCellRenderer.ColorProvider renamed CellColor, color() renamed get().
### is.codion.framework.domain
- DerivedAttributeDefinition.Builder.SourceAttributesStep added.
- Condition.toString(entityDefinition) renamed string() in order to not be confused with toString().
- ForeignKeyDefinition.Builder.attributes() renamed include().
- EntityDefinition.Attributes.updatable() removed.
### is.codion.framework.domain.db
- SchemaDomain.SchemaSettings.viewSuffix(), auditColumnNames() and hideAuditColumns() added.
### is.codion.framework.model
- EntityEditModel.createInsert(), createUpdate() and createDelete() renamed insertTask(), updateTask() and deleteTask().
- EntityEditModel.Settings added.
### is.codion.swing.framework.ui
- EntitySearchFieldPanel.Builder.preferredSearchFieldWidth() added.
- EntityPanel.createMainComponent() bug fixed, mainPanel() got called twice if a detail panel was present, messing up the controls panel, now supplier based.
- EntityTablePanelPreferences bug fixed, now logs json parsing errors instead of crashing.
- EntityApplicationPanel.handleUnsavedModifications() bug fixed, no longer exits on ESC in warning dialog.
- EntityTablePanel.Config.editableAttributes() added.
### is.codion.plugin.swing.mcp
- SwingMcpServer keyboard input simplified.
### is.codion.tools.generator.domain
- DomainSource refactored and improved concerning read-only entities and columns, tests improved.
### is.codion.tools.generator.model
- DomainGeneratorModel.DOMAIN_PACKAGE now with default 'no.package' instead of empty string.
- DomainGeneratorModel.PopulateTask added.
- DomainGeneratorModel now saves schema settings in user preferences.
- DomainGeneratorModel.PopulateTask bug fixed, did not update table model to reflect changes.
### is.codion.tools.generator.ui
- DomainGeneratorPanel now asks for confirmation before saving files.
- DomainGeneratorPanel, dto entities now configurable.
- DomainGeneratorPanel now provides schema settings configuration.

## 0.18.50
### is.codion.common.core
- ValueCollection.optional() now returns an empty Optional if the collection is empty.
- State.present(value) added.
- State.present() parameter now Observable.
### is.codion.common.model
- DefaultConditionModel.DefaultSetCondition.equals() and hashCode() now throw UnsupportedOperationException, in case equalTo() is confused with equals().
- DefaultConditionModel bug fixed, adding wildards no longer breaks case-insensitivity.
- DefaultConditionModel bug fixed, in now works when case-insensitive.
- DefaultConditionModel bug fixed, now adjusts Temporal precision to match the dateTimePattern.
### is.codion.swing.common.model
- DefaultListSelection.SelectedIndexes and DefaultItems.optional() now return an empty Optional in case of empty selection.
- DefaultListSelection.SelectedIndex no longer non-null, returns null instead of -1 in case of empty selection.
### is.codion.swing.common.ui
- ControlIcon.controlIcon(icon) factory method added, when all you have is a single icon.
- ListBoxBuilder.string() renamed formatter().
- FilterTableColumnModel.ColumnSelection added.
- FilterTable.copyToClipboard() now only includes selected columns if column selection is enabled.
- FilterTable, copyCell control now only enabled when a lead selection is present.
### is.codion.framework.domain
- ColumnDefinition.Builder.hasDatabaseDefault() renamed withDefault().
- Column.ColumnDefiner.column(Function<Column, ColumnDefinition.Builder) added, for shared column definition policies.
- Column.ColumnDefiner.auditColumn() and booleanColumn() removed.
- Column.ColumnTemplate moved up to package level.
- AttributeDefinition.Builder.captionResourceKey() and mnemonicResourceKey() renamed to captionResource() and mnemonicResource().
- AttributeDefinition.Builder.captionResource() and mnemonicResource() overloaded to use a specified resource bundle instead of the entity bundle.
- Column.ColumnDefiner.column() parameter changed from Function to ColumnTemplate.
### is.codion.framework.db
- EntityConnectionProvider.description() now returns Optional.
### is.codion.framework.model
- DefaultEntityTableConditionModel bug fixed, now handles column and dateTimeFormat precision mismatch, by using ranges when precision is lacking.
### is.codion.swing.framework.ui
- EntitySearchField bug fixed, now clears selection when an empty field loses focus, only if the selection isn't empty, selection uses Notify.SET, did not play well with ConditionModel.autoEnable.
- EntityPanel.Builder.icon() now ControlIcon based.
- EntityComboBoxPanel.Builder.comboBoxPreferredWidth() renamed preferredComboBoxWidth().

## 0.18.49
### is.codion.common.core
- AbstractValue bug fixed, get() now guaranteed to return a non-null value if a nullValue is specified, as promised by javadoc.
- Observable.isEqualTo() and isNotEqualTo() renamed is() and isNot(), to not be confused with equals().
### is.codion.common.model
- FilterModel, count() renamed to size().
### is.codion.swing.common.model
- FilterComboBoxModel.Builder.onSelection() added.
- FilterComboBoxModel.Builder.refresh() added, related refactoring.
### is.codion.swing.common.ui
- NumberDocument.NumberRangeValidator bug fixed, values now included correctly in error message.
- NumberField.valueRange(), minimumValue() and maximumValue() removed, accessible via builder, builder validation improved, related refactoring.
- NumberField, setters removed, only used in tests, accessible via builder.
- NumberField.Builder.valueRange(), minimumValue() and maximumValue() renamed range(), minimum() and maximum().
- NumberField.Builder.groupingUsed() renamed grouping().
- Remaining usages of SwingConstants.LEFT and RIGHT replaced with LEADING and TRAILING.
- LabelBuilder.HORIZONTAL_ALIGNMENT removed.
- NumberDocument, value included in range validation exception message.
- NumberDocument, translate single minus sign to -1, in order to prevent minus sign input when negative numbers are not valid.
- ColumnConditionPanel, now displays the selected operator in a tooltip when changed via the keyboard, if the view is SIMPLE and the operator combo box hidden.
- FilterTable.Export moved to FilterTableModel, related refactoring.
- NumberField.Builder.maximumFractionDigits() renamed fractionDigits().
- FilterTableCellRenderer.UISettings.filteredBackground() removed.
- LookAndFeelEnabler.uiDefaults() added.
- ConditionPanel.focusGained() event now with JComponent parameter.
### is.codion.framework.domain
- KeyGenerator.Identity removed.
- AttributeDefinition.Builder.valueRange(), minimumValue() and maximumValue() renamed range(), minimum() and maximum().
- AttributeDefinition.maximumFractionDigits() and decimalRoundingMode() renamed to fractionDigits() and roundingMode().
- AttributeDefinition.Builder.numberFormatGrouping() renamed numberGrouping().
- AttributeDefinition.Builder.localeDateTimePattern() renamed dateTimePattern().
### is.codion.framework.model
- EntityEditModel.EditEvent removed.
### is.codion.swing.framework.model
- DefaultEntityCombBoxModel refactored.
- EntityComboBoxModel.Builder.onSelection() added.
- EntityComboBoxModel.Builder.refresh() added.
### is.codion.swing.framework.ui
- EntityTablePanel, minor refactoring to get rid of table builders after initialization.
### is.codion.plugin.flatlaf.intellij
- Catppuccin themes added.
- VSCode themes added.
- Halcyon theme added.

## 0.18.48
### is.codion.swing.common.model
- FilterTableModel.TableColumns, ColumnValues.string(row, identifier) renamed format().
- FilterTableModel.TableColumns, ColumnValues.format(row, identifier) renamed formatted().
### is.codion.swing.common.ui
- FilterTable.ScrollToSelected bug fixed, no longer jumps to start of selection when a row is deselected.
- NullableCheckBoxBuilder split from CheckBoxBuilder, related refactoring.
- ControlKey.controlClass() removed, unused.
- FilterTableCellRenderer.Builder.string() renamed formatter().
### is.codion.framework.domain
- ConditionString.toString() renamed get().
- AttributeDefinition.string(value) renamed format().
- Entity.string(attribute) renamed format.
- StringFactory renamed EntityFormatter.
- EntityDefinition.Builder.stringFactory() renamed formatter().
- Entity.format(attribute) renamed formatted.
### is.codion.framework.db.core
- EntityConnectionProvider.Builder, domainType.name() now used as clientType if none is specified.
### is.codion.swing.framework.model
- SwingEntityTableModel.EntityTableColumns, improved error message when no attributes are visible.
### is.codion.swing.framework.ui
- EntityApplicationPanelBuilder, domainType.name() now used as application name by default.
- EntityApplicationPanelBuilder.applicationVersion() renamed version().
- EntitySearchField.Builder and EntitySearchFieldPanel.Builder.selectorFactory() renamed selector().
- EntityApplicationPanel.Builder renamed EntityApplication and moved up to package level.
- EntityApplication, builder methods renamed, removing redundant prefixes, related refactoring.
- EntityApplicationPanel.initialize() now returns the panel, consistent with other panels.
- EntitySearchField.Builder.stringFactory() renamed formatter.

## 0.18.47
### is.codion.common.core
- ProxyBuilder no longer an interface.
- TaskScheduler no longer an interface.
- MessageBundle no longer an interface.
- PropertyStore no longer an interface.
- LocaleDateTimePattern no longer an interface.
### is.codion.common.model
- FilterModel.Builder.visiblePredicate() renamed visible().
- FilterModel.visible() and filtered() renamed included() and excluded(), related renaming.
- FilterModel.AbstractRefresher.supplier() renamed items().
- FilterModel.Builder.include() renamed included(), related renaming.
### is.codion.swing.common.model
- ProgressWorker, internal interfaces sealed.
### is.codion.swing.common.ui
- FilterTable.Builder.rowSelection() and columnSelection() added.
- ControlBuilder.icon() parameter now nullable.
- FilterTable.DefaultBuilder refactored, nulls used instead of defaults.
- AbstractComponentBuilder, AbstractButtonBuilder and AbstractTextComponentBuilder refactored, nulls used instead of defaults.
- Layout specific panel builders added.
- DefaultInputDialogBuilder.show(Predicate) bug fixed, no longer closes dialog on ok, regardless of the close predicate.
### is.codion.framework.domain
- DerivedAttribute removed, DerivedValue added instead, related renaming.
- ForeignKeyCondition removed, Factory moved up to package level as ForeignKeyConditionFactory.
- ColumnCondition.Factory moved up to package level as ColumnConditionFactory.
- ConditionProvider renamed ConditionString.
- Interfaces sealed where applicable, related refactoring.
### is.codion.framework.db.local
- MethodTracer.NO_OP_TRACE added, exit() no longer returns null.
### is.codion.framework.server
- EntityServer.setTraceToFile() and isTraceToFile() added along with associated methods in EntityServerAdmin.
### is.codion.framework.model
- EntityEditModel.InsertTask, UpdateTask and DeleteTask merged into EditTask.
### is.codion.swing.framework.ui
- EntityApplicationPanel.TRACING renamed SQL_TRACING.
- EntityEditComponentPanel.EditorComponent.replace() added.
### is.codion.tools.monitor.ui
- ClientUserMonitorPanel, trace to file checkbox added.
### is.codion.tools.loadtest.ui
- LoadTestPanel, user can now be removed, minor ui improvements.

## 0.18.46
### is.codion.common.db
- AbstractConnectionPoolWrapper now logs check out times in microseconds.
### is.codion.swing.common.model
- FilterTableModel.TableColumns.caption() added.
- FilterTableModel.TableColumns.description() added.
- FilterTableModel.Builder.supplier() renamed items().
- FilterModel.Builder.SelectionBuilder and SortBuilder renamed SelectionStep and SortStep.
### is.codion.swing.common.ui
- FilterTable now automatically creates columns, Builder.columns() now allows column builder configuration.
- FilterTableColumn can no longer be instantiated outside FilterTable, Builder.build() removed, related refactoring.
- CalendarPanel bug fixed, clicking day filler labels now sets the year as well as the month.
- Icons.icons() size parameter added, Icons.SIZE removed.
- ControlIcon added along with ControlBuilder.icon(ControlIcon).
- FrameworkIcons now ControlIcon based.
- ComponentValue type parameters reordered.
- ComponentValueBuilder type parameters reordered.
- ListBuilder.Items and SelectedItems.nullable() added.
- CalendarPanel refactored to reduce number of mouse listeners.
- LogViewer added, extracted from ClientInstanceMonitorPanel.
### is.codion.framework.domain
- Entities.primaryKeys() overloaded with Collection.
### is.codion.framework.db.core
- EntityConnectionTracer added.
### is.codion.framework.db.local
- LocalEntityConnectionProvider.tracing() and traces() added, related refactoring.
- LocalEntityConnectionProvider now implements EntityConnectionTracer.
### is.codion.framework.model
- AbstractEntityEditModel.afterInsertUpdateOrDelete() now includes the entities involved, related refactoring.
- EntityEditModel.events() now takes entityType parameter, related refactoring.
### is.codion.swing.framework.ui
- EntityTableColumns removed.
- FrameworkIcons.SIZE added.
- EditComponentFactory type parameters reordered.
- EntityTablePanel.userPreferencesKey() renamed preferencesKey(), EntityPanel.preferencesKey() added.
- EntityEditPanel.writePreference() and applyPreferences() added along with preferencesKey().
- EntityApplicationPanel, log menu now contains controls for tracing, in case of a EntityConnectionTracer enabled connection provider.
- EntityApplicationPanel, Keyboard shortcuts menu caption now Shortcuts.
### is.codion.framework.server
- LocalConnectionHandler.TRACER logger added, handling client method traces.
### is.codion.tools.loadtest.core
- DefaultLoadTest.user() now nullable, defaultUncaughtExceptionHandler set.
### is.codion.tools.loadtest.ui
- LoadTestPanel, multiple user handling improved.
### is.codion.tools.monitor.model
- ClientUserMonitor bug fixed, user history now correctly counts multiple user connections.

## 0.18.45
### is.codion.common.core
- Text.DEFAULT_COLLATOR_LANGUAGE renamed COLLATOR_LANGUAGE.
### is.codion.common.db
- Database.DATABASE_INIT_SCRIPTS renamed INIT_SCRIPTS.
- Database.DATABASE_URL renamed URL.
- ConnectionPoolWrapper, configuration values renamed, DEFAULT prefix removed.
### is.codion.swing.common.ui
- FilterTableColumn.setFixedWidth() added.
- FilterTableColumn.Builder.ModelIndexBuilder renamed ModelIndexStep.
- TabbedPaneBuilder refactored, methods overloaded with Supplier.
- DefaultIcons is now scaling aware.
- Icons.ICON_SIZE and ICON_COLOR renamed SIZE and COLOR.
- Scaler.scale() added.
- FontSizeScaler now uses Scaler.scale().
- Icons.size() and color() added, SIZE and COLOR property values no longer used for dynamic update.
- FontImageIcon.Builder refactored.
- Scaler.RATIO renamed SCALING.
- ScalingSelectionDialogBuilder.createControl() removed, related refactoring.
- FileInputPanel, button caption not used if icon is specified, layout fix.
### is.codion.framework.domain
- ColumnDefinition.get(ResultSet) overloaded without index, fetches value by column name.
- KeyGenerator.returnGeneratedKeys() renamed generatedKeys().
- ForeignKeyDefinition.DEFAULT_FOREIGN_KEY_REFERENCE_DEPTH removed, FOREIGN_KEY_REFERENCE_DEPTH renamed REFERENCE_DEPTH.
### is.codion.framework.db
- DefaultLocalEntityConnection, selecting single values of entities with custom query columns now supported.
- EntityResultPacker now fetches values by column name when the entity query has custom columns, query column order no longer has to match the attribute definition order.
- EntityConnection.Select.queryTimeout() renamed timeout().
- LocalEntityConnection.defaultQueryTimeout() renamed queryTimeout(), QUERY_TIMEOUT_SECONDS renamed QUERY_TIMEOUT.
### is.codion.framework.model
- AbstractEntityEditModel.DefaultUpdateEntities bug fixed, did not respect editor.modified() when updating.
### is.codion.swing.framework.ui
- EntityPanel.USE_KEYBOARD_NAVIGATION renamed KEYBOARD_NAVIGATION, Config.useKeyboardNavigation renamed keyboardNavigation.
- DefaultFrameworkIcons bug fixed, logo color now updated along with other icons, related refactoring.
- FrameworkIcons.logo(size) removed.
- DefaultFrameworkIcons no longer contains a standard sized LOGO, unused.
- FrameworkIcons.refreshRequired() removed.
### is.codion.tools.generator.model
- DomainGeneratorModel.DEFAULT_DOMAIN_PACKAGE and DEFAULT_SOURCE_DIRECTORY renamed DOMAIN_PACKAGE and SOURCE_DIRECTORY.

## 0.18.44
### is.codion.common.core
- State.Combination.add() and remove() removed.
- DefaultStateCombination refactored, no longer locks on read.
### is.codion.common.model
- FilterModel.Items.remove(Predicate) added.
### is.codion.swing.common.ui
- AbstractControlPanelBuilder bug fixed, button builders no longer shared, stack overflow during value linking, same button linked to multiple values.
- ComponentBuilder.visible() and focusable() overloaded with ObservableState.
- AbstractComponentBuilder.configureComponent(), set label for component moved to top, so it's available during component configuration.
- BorderLayoutPanelBuilder.add() added.
- CalendarPanel now displays the last days of previous month and first days of next month.
- ComponentBuilder.label() overloaded with Consumer<LabelBuilder> parameter.
- ComponentBuilder now extends Supplier.
- PanelBuilder and BorderLayoutPanelBuilder now accept Supplier when adding components.
- ComponentDialogBuilder.component() overloaded with Supplier.
- ScrollPaneBuilder.view() overloaded with Supplier.
- CalendarPanel week number labels now styled like day filler labels, as in, disabled.
- ActionDialogBuilder.component() overloaded with Supplier.
- SplitPaneBuilder, component methods overloaded with Supplier.
- DefaultBorderLayoutPanelBuilder.add() bug fixed, BorderLayout.CENTER was missing.
- DefaultBorderLayoutPanelBuilder and DefaultPanelBuilder refactored.
- ComponentValueBuilder split from ComponentBuilder, for components that don't have an associated value, such as panels.
- InputPanelLayout and InputPanelBorderLayoutBuilder added.
- LabelBuilder.builder() overloaded with text parameter.
- BorderLayoutPanelBuilder methods renamed, Component suffix removed.
- Control.Builder now extends Supplier, builder parameters replaced.
- Control.Builder renamed ControlBuilder.
- Control.ControlBuilder and co. moved up to package level.
### is.codion.framework.model
- EntityApplicationModel.refresh() removed.
### is.codion.swing.framework.ui
- EntityEditComponentPanel.createLabel() removed, label(attribute) added, setComponentBuilder() now initializes a default label.
- EntityEditComponentPanel.InputPanelBuilder added.
- EntityEditComponentPanel.createLabel() added for read-only value linking.
- EntityEditComponentPanel.InputPanelBuilder.label() and component() overloaded with Supplier parameter.
- EntityEditComponentPanel.InputPanelBuilder now extends ComponentBuilder.
- EntityEditComponentPanel.InputPanelBuilder.label() overloads with constraints removed.
- EntityEditComponentPanel.InputPanelBuilder moved to package is.codion.swing.common.ui.component.panel.
- EntityEditComponentPanel.label(attribute) removed.
- EntityEditComponentPanel.EditComponent added, components can no longer be replaced once set.
- EntityEditComponentPanel.EditComponent no longer extends Value.
- EntityEditComponentPanel.EditComponent renamed EditorComponent.

## 0.18.43
### is.codion.common.core
- Values renamed ValueCollection.
- TaskScheduler.Builder.IntervalStep added.
- State no longer extends Value, State.is(), toggle() and value() added, ObservableState no longer extends Observable.
- package is.codion.common.observable renamed observer.
- Item.value() renamed back to get(), getOrThrow() added.
- DefaultMethodTrace bug fixed, missing newline.
### is.codion.common.model
- FilterModel.RefreshStrategy removed, no longer necessary after recent refresh related selection improvements.
### is.codion.swing.common.model
- NullableToggleButtonModel.ToggleState removed, get() and set() added, related refactoring.
- FilterListModel.Builder.ItemsStep.items() parameterless overload added.
- DefaultListSelection.SelectedIndexes no longer clears the selection when set, preventing an empty selection event.
- DefaultFilterModelItems.clearAndAdd() no longer triggers an empty selection event if selection before and after overlaps.
### is.codion.swing.common.ui
- FilterTableCellEditor.Builder added, cellEditable now configurable.
- CalendarPanel.Builder.includeWeekNumbers added along with WEEK_NUMBERS configuration value.
- NumberField, minimum and maximum values now Value based.
- NumberDocument.getNumber(), setNumber() and getFormat() renamed get(), set() and format() respectively.
- MenuBuilder.ControlsStep added, simplified, now only Controls based.
- ControlPanelBuilder.ControlsStep added, related refactoring.
- MenuBuilder.menuItemBuilder() and toggleMenuItemBuilder() renamed controlMenuItem() and toggleControlMenuItem(), now take Function instead of builder, actionMenuItem() added.
- ToggleButtonBuilder, ToggleMenuItemBuilder.toggleControl() renamed toggle().
- ColumnConditionPanel bug fixed, changes to wildcards in the model now reflected in the popup menu.
- ListSelectionDialogBuilder.SelectionStep added.
- ToggleButtonBuilder, ToggleMenuItemBuilder and subclasses refactored and simplified.
### is.codion.framework.domain
- EntityDefinition.Attributes.getOrThrow(attributeName) added.
### is.codion.framework.model
- DefaultEntityEditor.DefaultEditorValue no longer nullable in case of non-null boolean attributes.
### is.codion.framework.db
- EntityConnection.Update.set() value parameter now @Nullable.
### is.codion.swing.framework.ui
- EditComponentFactory.component(), value parameter removed.
- EntityDialogs.EntitySelectionDialogBuilder.SelectionStep added.
- EntityDialogs.EntitySelectionDialogBuilder now throws CancelException when selection is cancelled.
### is.codion.framework.server
- LocalConnectionHandler, now uses a single final entityConnection field, related refactoring.

## 0.18.42
### is.codion.common.model
- UserPreferences.file() and delete() no longer accept empty string for filename.
### is.codion.swing.common.model
- JSpecify annotations added.
- NullableToggleButtonModel, constructor now private, factory methods added.
### is.codion.swing.common.ui
- FilterTable.Builder.columnReorderingAllowed() renamed columnReordering().
- FilterTable.Builder.columnResizingAllowed() renamed columnResizing().
- FilterTable.COLUMN_RESIZING configuration value added.
- FilterTable.Builder.cellSelectionEnabled() renamed cellSelection().
- JSpecify annotations added.
- TemporalFieldPanel.getTemporal() and setTemporal() renamed get() and set().
- FrameBuilder, now handles multiple listeners.
- NullableCheckBox, constructor protected, factory methods added.
### is.codion.framework.domain
- DefaultEntity.validateTypes() performance bug fixed, unnecessary HashMap lookup removed, renamed validate().
- DefaultKey.setSerializer() and serializerForDomain() moved to EntitySerializer. 
- SingleColumnKey added, DefaultKey renamed CompositeColumnKey.
- Entity.Builder.key() removed, Entities.key() added.
- Entities.builder() renamed entity().
### is.codion.framework.db.local
- EntityResultPacker, minor optimization to eliminate HashMap resizing.
### is.codion.swing.framework.model
- JSpecify annotations added.
### is.codion.swing.framework.ui
- EntityTablePanelPreferences bug fixed, no longer applies empty preferences.
- EntityApplicationPanel.Builder.frameSupplier() renamed frame().
- JSpecify annotations added.

## 0.18.41
### is.codion
- Builder step suffix now Step instead of Builder.
- Publish config moved from bom build files to root build, revapi plugin removed for now.
### is.codion.common.core
- PropertyStore now accepts null default values.
- DefaultMethodTrace.appendTo(), minor optimization.
### is.codion.common.model
- FilePreferencesFactory removed, UserPreferences.file() added.
- UserPreferences.flush() now flushes all file based preferences as well.
- UserPreferences.delete() added, unit tests now cleanup preferences files.
### is.codion.common.rmi
- Authenticator.type() renamed clientType().
### is.codion.swing.common.model
- NullableToggleButtonModel.toggleState() renamed state().
- ProgressWorker now throws exception in case the task is not of the correct type.
### is.codion.swing.common.ui
- FilterTableCellRenderer.Builder.ColumnClassBuilder added.
- FilterTable.Builder.rowHeight(), rowMargin(), intercellSpacing(), gridColor(), showGrid(), showHorizontalLines(), showVerticalLines() and dragEnabled() added.
- ConditionPanel.focusGainedObserver() renamed focusGained().
- Utilities.propertyObserver() renamed observer().
### is.codion.framework.db.local
- MethodLogger renamed MethodTracer, logger package renamed tracer.
- DefaultLocalEntityConnection.NoOpTracer added, tracer no longer nullable.
### is.codion.framework.server
- LocalConnectionHandler.logExit(), now checks if trace is enabled before calling trace.appendTo().
### is.codion.swing.framework.model
- EntityApplicationModel.USER configuration value added.
- EntityApplicationModel.USERNAME_PREFIX configuration value removed, not really used anymore.
- EntityApplicationModel.preferences and PREFERENCES_KEY configuration value added, file based preferences now application model based.
### is.codion.swing.framework.ui
- EntityApplicationPanel.USER_PREFERENCES_ENABLED renamed USER_PREFERENCES.
- EntityApplicationPanel.preferences removed, USER_PREFERENCES and RESTORE_DEFAULT_PREFERENCES moved to EntityApplicationModel.
- EntityTablePanelPreferences.fromLegacyPreferences() bug fixed, now uses empty json objects.
- EntityTablePanelPreferences.LEGACY_PREFERENCES_ENABLED removed.
- EntityApplicationPanel.WRITE_LEGACY_PREFERENCES renamed LEGACY_PREFERENCES.
- EntityTablePanelPreferences, CONDITIONS_KEY and COLUMNS_KEY only used in legacy preferences, related refactoring.
- EntityApplicationPanel.applyPreferences(Preferences) added.
- EntityApplicationPanel.Builder.setUncaughtExceptionHandler(), includeMainMenu() and displayStartupDialog() renamed uncaughtExceptionHandler(), mainMenu() and startupDialog().
- DefaultEntityApplicationPanelBuilder minor refactoring.
- DefaultEntityApplicationPanelBuilder.applicationName now has a default value.
- EntityApplicationPanel, EntityPanel, EntityTablePanel, savePreferences() renamed writePreferences().
### is.codion.tools.loadtest.core
- LoadTest.Builder.CreateApplicationStep and CloseApplicationStep.
### is.codion.tools.loadtest.model
- QueryLoadTestModel removed.

## 0.18.40
### is.codion
- Builder step class names now consistently end with Builder, with one exception.
### is.codion.common.core
- Unit tests improved.
- DefaultValue no longer exposes Lock interface to subclasses.
- DefaultEvent.observer field now volatile.
- MethodLogger.noOpLogger() added.
- MethodLogger.Entry moved to package level as MethodTrace.
- MethodLogger moved to framework.db.local.logger package, now internal only.
- TaskScheduler.Builder.TaskStep added.
- State, overloaded builder() method removed.
- ProxyBuilder.builder() renamed of().
- DefaultProxyBuilder now thread-safe and reusable.
### is.codion.common.db
- Unit tests improved.
- DatabaseConnection.getMethodLogger() now returns MethodLogger.noOpLogger() in case of no logger.
- DatabaseConnection no longer uses MethodLogger.
### is.codion.common.model
- Unit tests improved.
- FilePreferencesFactory added.
- UserPreferences moved to preferences package.
- SimpleFilePreferences renamed to FilePreferences.
- FilePreferencesFactory and JsonPreferencesStore refactored.
- FilterModel.Builder.RefresherStage added.
- ConditionModel.Builder.ValueClassBuilder added.
### is.codion.common.rmi
- Unit tests improved.
- ClientLog moved to framework.server module.
### is.codion.framework.domain
- Unit tests improved.
### is.codion.framework.db.core
- Unit tests improved.
- EntityConnection.Count.Builder.WhereStage added.
### is.codion.framework.db.http
- HttpEntityConnection.DISCONNECT_ON_CLOSE configuration value added.
### is.codion.framework.db.local
- Unit tests improved.
- LocalEntityConnection.methodLogger() removed, MethodLogger no longer public api.
- MethodLogger.noOpLogger() removed.
### is.codion.framework.model
- Unit tests improved.
- EntitySearchModel.Builder.EntityTypeBuilder and ConnectionProviderBuilder added.
### is.codion.framework.server
- ClientLog removed, was just a wrapper for method traces.
- EntityServerConfiguration.clientLogging() renamed methodTracing(), related renaming.
### is.codion.swing.common.model
- Unit tests improved.
- ProgressWorker.BuilderFactory added, replacing overloaded ProgressWorker.builder() methods.
- FilterTableModel.Builder.Columns added, replacing the columns parameter in FilterTableModel.builder().
- FilterTableModel.RowEditor renamed Editor.
- FilterComboBoxModel.Builder.ItemsBuilder added, replacing overloaded builder() methods.
- FilterListModel.Builder.ItemsBuilder added, replacing overloaded builder() methods.
### is.codion.swing.common.ui
- Unit tests improved.
- HintTextField.hintForegroundColor() bug fixed, returns null if either foreground or background is unspecified.
- AbstractControl bug fixed, default enabledObservable now an ObservableState instance instead of State.
- Utilities.enableActions() and enableComponents() renamed enabled().
- Utilities.linkBoundedRangeModels() renamed link().
- Windows.FrameBuilder.onBuild() added.
- Sizes, set prefix removed from methods.
- Windows.FrameBuilder moved to frame package, Frames added.
- Windows.setSizeWithinScreenBounds() renamed sizeWithinScreenBounds().
- Windows moved to window package.
- Sizes moved to component package.
- Colors moved to color package.
- Cursors moved to cursor package.
- FileTransferHandler moved to transfer package.
- UiManagerDefaults renamed UIManagerDefaults.
- FontSize added. DefaultLookAndFeelEnabler now handles updating the font size when enabling.
- FontSize.updateFontSize() bug fixed, Element Of character got lost and Font instances did not get resized, now uses deriveFont() for FontUIResource instead of instantiation.
- TextFieldBuilder.SELECT_ALL_ON_FOCUS_GAINED configuration value added.
- DefaultIcons now reacts to changes in Icons.ICON_SIZE, replacing each icon with a resized instance.
- FrameBuilder.component() added.
- ComponentDialogBuilder.component() added.
- Dialogs.componentDialog() renamed dialog().
- ActionDialogBuilder.component() added.
- Dialogs, Dialog suffix removed from method names.
- SelectionDialogBuilderFactory added.
- Windows.resizeWindow() renamed resize().
- FontSize replaced with Scaler, default implementation font size based while the FlatLaf one is based on the flatlaf.uiScale property.
- FilterTable.Builder.Columns added, replacing the columns parameter in FilterTable.builder().
- FilterTable.Builder.Model added, replacing the tableModel parameter in FilterTable.builder().
- FileSelectionDialogBuilder.fileFilter() renamed filter().
- ProgressWorkerDialogBuilder.BuilderFactory added, replacing overloaded methods in Dialogs.
- InputDialogBuilder.ComponentStep added, replacing overloaded methods in Dialogs.
- AbstractComponentBuilder, Value constructor parameter removed.
- KeyEvents, overloaded builder() methods removed.
- FontImageIcon.Builder.IkonBuilder added.
- TextFieldBuilder.ValueClassBuilder added.
- TemporalField.Builder.TemporalClassBuilder added.
- TemporalFieldPanel.Builder.TemporalClassBuilder added.
- NumberField.Builder.NumberClassBuilder added.
- ButtonBuilder, overloaded builder() methods removed.
- ButtonPanelBuilder, overloaded builder() methods removed.
- MenuBuilder, overloaded builder() methods removed.
- MenuItemBuilder, overloaded builder() methods removed.
- ToolBarBuilder, overloaded builder() methods removed.
- LabelBuilder, overloaded builder() methods removed.
- BorderLayoutPanelBuilder, overloaded builder() methods removed.
- PanelBuilder, overloaded builder() methods removed.
- ProgressBarBuilder, overloaded builder() methods removed.
- ScrollPaneBuilder, overloaded builder() methods removed.
- ColumnConditionPanel.Builder.ModelBuilder.added.
- DefaultMenuBuilder refactoring bug fixed, captions were missing.
- NumberSpinnerBuilder.NumberClassBuilder added.
- SpinnerBuilder.model() added.
- SearchHighlighter.Builder.component() added.
- FilterTableColumn.Builder.IdentifierBuilder and ModelIndexBuilder added.
- DefaultFilterListModel.DefaultItemsBuilder.items() refactoring bug fixed, now allows null.
- ListBuilder.ModelBuilder added.
- SliderBuilder.ModelBuilder added.
- ComboBoxBuilder.ModelBuilder added.
- ItemComboBoxBuilder.BuilderFactory added.
- Components.temporalField() parameter removed.
- ListBoxBuilder.ItemValueBuilder and LinkedValueBuilder added.
### is.codion.swing.framework.model
- EntityComboBoxModel.Builder.EntityTypeBuilder and ConnectionProviderBuilder added.
### is.codion.swing.framework.ui
- ApplicationPreferences.fromString() bug fixed, FRAME_MAXIMIZED_KEY now used.
- EntityComboBoxPanel.Builder.EditPanel added, replacing the editPanel parameter in EntityComboBoxPanel.builder().
- EntitySearchFieldPanel.Builder.EditPanel added, replacing the editPanel parameter in EntitySearchFieldPanel.builder().
- EntityComboBox.Builder.ModelBuilder added.
- EntitySearchField.Builder.ModelBuilder added.
- EntitySearchFieldPanel.Builder.ModelBuilder added.
- EntityComboBoxPanel.Builder.ModelBuilder added.
- TabbedDetailLayout.Builder.PanelBuilder.added.
- WindowDetailLayout.Builder.PanelBuilder.added.
- EntityPanel.Builder.EntityTypeBuilder and PanelBuilder added.
### is.codion.framework.lambda
- LambdaEntityHandler refactored.

## 0.18.39
### is.codion.common.core
- DefaultPropertyStore, synchronization added.
- Configuration, error handling and javadocs improved.
- ValueLink.updatingLinked and updatingOriginal now volatile, javadocs improved.
- DefaultMessageBundle now provides a fallback resource indicating a missing key instead of throwing exception.
- Configuration, security validation added for paths.
### is.codion.common.db
- AbstractConnectionPoolWrapper.closeStatisticsCollection() added.
- SLF4J dependency added, DefaultDatabaseConnection logging improved.
- ConnectionPoolWrapper.DEFAULT_CHECK_OUT_TIMEOUT added.
- ConnectionPoolWrapper.VALIDATE_CONNECTIONS_ON_CHECKOUT added.
### is.codion.common.model
- DefaultFilterModelItems.filterIncremental() added.
### is.codion.common.i18n
- Messages, mnemonic bounds checking added.
- Messages, icelandic Clear mnemonic fixed.
### is.codion.common.rmi
- SerializationFilter, minor refactoring.
- ConnectionRequest.id() and type() renamed back to clientId() and clientType().
### is.codion.dbms.h2
- H2Database, security validation for the H2 database script injection.
### is.codion.swing.common.ui
- KeyEvents.keyStroke() overloaded with String parameter.
### is.codion.plugin.swing.mcp
- is.codion.plugin.swing.mcp module added.
- SwingMcpServer, unnecessary title bar click removed, refactoring.
- SwingMcpServer.takeApplicationScreenshot() reimplemented, bypassing the robot, window no longer has to be in front.
- SwingMcpServer, window listing tool added and window screenshot functionality improved, refactoring.
- SwingMcpPlugin.startMcpServer() removed, mcpServer() returning State added.
- SwingMcpPlugin, major overhaul, refactoring, cleanup and bugfixes.
### is.codion.swing.common.model
- FilterTableModel.Builder.filterModelFactory() renamed filters().
- ProgressWorker.Builder.maximumProgress() renamed maximum, related renaming.
- FilterTableModel.Builder.asyncRefresh() renamed async(), related renaming.
### is.codion.swing.common.ui
- FilterTable.Builder.sortingEnabled() renamed sortable().
### is.codion.framework.domain
- ColumnDefinition.columnHasDefaultValue() renamed hasDatabaseDefault().
- DerivedAttributeDefinition.sourceAttributes() renamed sources().
- EntityDefinition.selectTableName() renamed selectTable().
- EntityDefinition.tableName() renamed table().
- ColumnDefinition.primaryKeyIndex() renamed keyIndex().
- ColumnDefinition.Builder.columnClass() renamed converter().
- DerivedAttributeDefinition.valueProvider() renamed provider().
### is.codion.framework.db.core
- EntityConnectionProvider.Builder.domainType() renamed domain().
- EntityConnection.setQueryCacheEnabled() and isQueryCacheEnabled() renamed queryCache().
- EntityConnection.MAXIMUM_BATCH_SIZE added.
### is.codion.framework.db.local
- LocalEntityConnection, getter/setter combos renamed for consistency.
- DefaultLocalEntityConnection, resource handling improved.
### is.codion.framework.model
- EntityQueryModel bug fixed, did support including non-selected columns.
- EntityTableModel.orderQueryBySortOrder() renamed orderQuery().
- AbstractEntityTableModel.orderByFromSortModel() renamed orderBy().
- EntitySearchModel.handleEditEvents() renamed editEvents().
- EntityEditModel.postEditEvents() renamed editEvents(), editEvents() renamed events().
### is.codion.swing.framework.model
- EntityComboBoxModel.handleEditEvents() renamed editEvents().
### is.codion.swing.framework.ui
- EntityApplicationPanel.Builder.domainType() renamed domain().
- EntityApplicationPanel.exitObserver() renamed exiting().
### is.codion.framework.bom
- BOM module added.
### is.codion.common.bom
- Common BOM module added.
### is.codion.framework.lambda
- Add codion-framework-lambda module for serverless deployment
- LambdaEntityHandler, removed env variables, now configured with EntityServer system properties.

## 0.18.38
### is.codion
- module-version added to module-info.class files.
### is.codion.common.core
- Value.Notify.WHEN_CHANGED and WHEN_SET renamed CHANGED and SET respectively.
- MethodLogger.Entry.getChildren() renamed children(), enterMessage() renamed message(), ArgumentToString renamed ArgumentFormatter.
- PropertyValue.propertyName() renamed name().
- Item.itemI18n() renamed i18n().
- Text.parseCommaSeparatedText() renamed parseCSV().
### is.codion.common.db
- Database.URL_SCOPED_INSTANCE configuration value added.
- ConnectionPoolStatistics.averageGetTime(), minimumCheckOutTime() and maximumCheckOutTime() renamed averageTime(), minimumTime() and maximumTime() respectively.
- ConnectionPoolWrapper.getIdleConnectionTimeout() and setIdleConnectionTimeout() renamed getIdleTimeout() and setIdleTimeout().
- Database.maximumNumberOfParameters() renamed maximumParameters().
### is.codion.common.model
- UserPreferences, UserPreference suffix removed from method names.
- ConditionModel.equalWildcards() renamed equalWithWildcards().
- SummaryModel.Summary.summary() renamed get().
### is.codion.common.rmi
- ServerInformation, server prefix removed from methods.
- AuxiliaryServer.startServer() and stopServer() renamed start() and stop().
- ConnectionRequest, client prefix renamed from methods.
- Authenticator.clientType() renamed type().
### is.codion.swing.common.model
- FilterComboBoxModel.createSelectorValue() renamed createSelector().
### is.codion.swing.common.ui
- FilterTable.ScrollToSelected bug fixed, rowVisible() now based on viewport.getViewRect().
- FlexibleGridLayout re-implemented.
- FlexibleGridLayout edge case for zero rows/columns bug fixed, test added.
### is.codion.framework.domain
- fetchDepth renamed referenceDepth.
- ForeignKeyDefinition.Builder.referenceDepth() added, ForeignKeyDefiner.foreignKey(referenceDepth) removed.
- TransientAttribute.modifiesEntity() and Builder.modifiesEntity() renamed modifies().
- SelectQuery renamed EntitySelectQuery.
### is.codion.framework.db.local
- SelectQueriesTest improved.
### is.codion.framework.model
- EntitySearchModel.singleSelection() removed.
### is.codion.swing.framework.ui
- EntityApplicationPanel, support table now known as lookup table.
- NullableCheckBox, FlatLaf hack removed.
- QueryInspector added, available with Ctrl-Alt-Q in EntityTablePanel, when using a local connection.
- EntityTableExport added.
- EntityEditPanel, query inspector available with Ctrl-Alt-Q, when using a local connection, QueryInspector split into SelectQueryInspector and InsertUpdateQueryInspector.
- SelectQueryInspector.BasicFormatterImpl bug fixed, now handles single quoted values.
- SelectQueryInspector.BasicFormatterImpl now inserts line breaks in insert column and value lists.
- EntityEditPanel, EntityTablePanel.updateUI() now includes queryInspector.
- EntityApplicationPanel, log file/dir controls now with icons.
- EntitySearchField.singleSelection() added.
### is.codion.swing.framework.ui.test
- is.codion.swing.framework.ui.test module removed

## 0.18.34
### is.codion.common.core
- DefaultValue.DefaultBuilder now adds listeners in the order they were added to the builder.
- Version.parse() input validation improved.
- Value.map() parameter now UnaryOperator instead of Function.
### is.codion.common.model
- FilterModel.VisibleItems and FilteredItems.get() annotated as NonNull.
- FilterModel.Sorter renamed Sort, sorter() renamed sort().
- FilterModel renamed FilterListModel and moved to list package.
- FilterListModel renamed back to FilterModel and moved to filter package.
- FilterListModel.Items.Builder added along with ItemsListener.
- DefaultFilterListItems added, extracted from DefaultFilterTableItems, related refactoring.
- FilterModel.Sort now extends Comparator, comparator() removed.
- FilterModel.AbstractRefresher no longer refreshes if no supplier is available.
- FilterModel.AbstractRefresher, async constructor parameter added.
### is.codion.common.rmi
- ServerAdmin, removed a few public accessors for values included in ServerStatistics, related cleanup.
### is.codion.framework.domain
- Column.Getter and Setter renamed GetValue and SetParameter respectively.
- Entity.put() renamed set().
- DerivedAttributeDefinition.Builder.ProviderStage added, related refactoring.
### is.codion.framework.domain.test
- DefaultEntityFactory.value() now handles UUIDs.
### is.codion.framework.model
- EntityQueryModel.conditions() renamed condition().
- EntityEditModel.EntityEditor.isNull(attribute) replaced with EditorValue.present().
- DefaultForeignKeyModelLink bug fixed, clearing the condition did not change it if the entities involved had been deleted, problem masked by referential integrity.
### is.codion.swing.common.model
- FilterTableModel.TableSelection moved to list package and renamed FilterListSelection, related refactoring.
- FilterTableSort.sorted() moved to FilterListModel.Sort.
- DefaultFilterTableItems.DefaultVisibleItems no longer adds a table model listener, changes to visible items notified explicitly.
- FilterListModel and FilterListSort added.
- FilterComboBoxModel.Builder.asyncRefresh() added.
- FilterListModel, FilterTableModel.Builder.visible() added.
- ProgressWorker.Task and ProgressTask added, related changes.
- ProgressWorker refactored.
- ProgressWorker.ProgressTask and ResultProgressTask.maximumProgress() added.
### is.codion.swing.common.ui
- LookAndFeelSelectionDialogBuilder.createControl() overloaded without parameter.
- BackgroundColorValidIndicator bug fixed, did not refresh state when colors changed during updateUI().
- LookAndFeelSelectionDialogBuilder.INCLUDE_PLATFORM_LOOK_AND_FEELS moved to LookAndFeelComboBox.
- LookAndFeelComboBox.Builder added.
- TextPaneBuilder added.
- FilterTable.ScrollToAdded.scrollToAddedRow() no longer calls moveLeadSelectionIndex(), caused a selection event.
- Control.Builder.name() renamed caption().
- Utilities.linkToEnabledState() renamed enableActions() and enableComponents().
- ValidIndicatorFactory.instance() now returns Optional.
- AbstractComponentBuilder no longer throws exception if the specified ValidIndicatorFactory is not found.
- ValidIndicatorFactory.instance() overloaded with factoryClassName parameter.
- DefaultFileSelectionDialogBuilder.selectFileToSave() and selectFilesOrDirectories() bug fixed, incorrect class used for synchronization.
- SelectionDialogBuilder split into ListSelectionDialogBuilder and ComboBoxSelectionDialogBuilder.
- Dialogs.selectionDialog() renamed listSelectionDialog() and comboBoxSelectionDialog() added.
- ListSelectionDialogBuilder.SingleSelector and MultiSelector removed.
- Dialogs.singleSelector() and multiSelector() removed.
- TextFieldBuilder.selector() removed.
- DefaultButtonPanelBuilder.enableTransferFocusOnEnter() implemented.
- ControlPanelBuilder, Builder suffix removed from a few methods.
- DefaultButtonPanelBuilder.enableTransferFocusOnEnter() bug fixed, was restricted to JButton.
- ButtonPanelBuilder.fixedButtonSize() and buttonGroup() added.
- SelectionDialogBuilder.defaultSelection() moved to sub-interfaces.
- ProgressDialogBuilder.onPublish() added.
- DefaultComboBoxSelectionDialogBuilder bug fixed, did not validate default selection item.
- TemporalFieldPanel and TextFieldPanel, minor button related improvements.
- FilterList added, ListBuilder now FilterList based.
- ListSelectionDialogBuilder.comparator() added.
- Dialogs.inputDialog() overloaded with ComponentBuilder.
- FilterList now scrolls to the selected item.
- FilterTableColumn.Builder.fixedWidth() added.
- FilterTable.doubleClick() renamed doubleClicked().
- FilterTable.Builder.doubleClickAction() renamed doubleClick().
- LookAndFeelComboBox.Builder.onSelection() added.
- LookAndFeelPanel no longer prints to err when encountering an IllegalAccessException while loading defaults, LookAndFeel not exported from module.
- LookAndFeelEnabler.platform() is now installed(), PlatformLookAndFeelEnabler renamed InstalledLookAndFeelEnabler, related refactoring.
### is.codion.swing.framework.ui
- EntityApplicationPanel.Builder.applicationModelFactory() and applicationPanelFactory() renamed applicationModel() and applicationPanel().
- EntityApplicationPanel.createLogControls() now protected final.
- EntityTablePanel.conditions() renamed condition().
- EntityPanel.createEditControlPanel() no longer adds an empty border.
- EntityApplicationPanel no longer abstract.
- EntityEditPanel constructors now protected.
- EntityEditPanel.insertCommandBuilder(), updateCommandBuilder() and deleteCommandBuilder() renamed insertCommand(), updateCommand() and deleteCommand().
- EntityComponentFactory.componentValue() renamed component().
### is.codion.plugin.flatlaf.intellij
- IntelliJThemeProvider, disable FlatLaf logging due to FlatLaf #990.
- material.Dracula theme renamed DraculaMaterial, conflicted with the other Dracula theme.

## 0.18.33
### is.codion.swing.common.ui
- Icons.icon() renamed get().
- ActionDialogBuilder.onShown() now supports multiple consumers.
- ComponentDialogBuilder.onShown(), onOpened() and onClosed() now support multiple consumers.
### is.codion.framework.db.core
- JSpecify annotations added.
### is.codion.framework.db.local
- JSpecify annotations added.
### is.codion.framework.model
- AbstractEntityEditModel.configureSearchModel() added, for separating the configuration of a search model for use in the edit model from search model creation.
- EntityEditModel.apply() added.
- EntityEditModel.apply() renamed applyEdit().
### is.codion.swing.framework.model
- DefaultEntityComboBoxModel.DefaultForeignKeyFilter.link() bug fixed, combo box model used its own selection instead of the one from the filter model when initializing the link
- SwingEntityEditModel.configureComboBoxModel() added, for separating the configuration of a combo box model for use in the edit model from combo box model creation.
- SwingEntityTableModel.setValueAt() now uses EntityEditModel.apply().
- EntityComboBoxModel.Filter.builder() removed.
### is.codion.swing.framework.ui
- EntityEditPanel, exception handling and logging now consistent for insert, update and delete.
- EditComponentFactory.caption() AttributeDefinition parameter added.
- DefaultEditAttributeDialogBuilder now displays no caption if EditComponentFactory.caption() returns nothing.
- EntityDialogs.EditAttributeDialogBuilder.applier() removed, EntityEditModel.apply() used instead.
- EntityTablePanel, edit panel dialog location no longer follows mouse when displayed on double click.
- EntityDialogs.addEntityDialog() and editEntityDialog() editPanel parameter no longer Supplier.
- EntityDialogs.AddEntityDialogBuilder and EditEntityDialogBuilder.onShown() added.
- EntityEditPanel, insert, update and delete commands refactored.
- EntityPanel.requestInitialFocus() now uses requestFocusInWindow() instead of requestFocus(), otherwise focus can escape the application when toggling the edit panel state.
- EntityEditComponentPanel.Defaults removed.

## 0.18.32
### is.codion.common.model
- UserPreferences.getUserPreference(key, defaultValue) now requires a non-null default value, can no longer return null.
- MultiSelection.Indexes and Items override get() annotated with @NonNull.
- DefaultConditionModel.isEqualWildcard() bug fixed, bypassed the automic wildcards.
### is.codion.swing.common.ui
- FilterTable bug fixed, doubleClickAction no longer triggered if disabled.
- ComponentBuilder.toolTipText(Observable<String>) added for a dynamic tool tip.
- ComponentBuilder.validIndicator() added along with ValidIndicatorFactory.
- ComponentBuilder.modifiedIndicator() added along with ModifiedIndicatorFactory.
- DefaultInputDialogBuilder.show(closeDialog) bug fixed, now uses onOk() instead of okAction(), which bypassed the valid state.
- ComponentBuilder.validIndicator(validator) argument now Predicate instead of Consumer.
- DefaultFilterTableColumnModel.hideColumn() bug fixed, event triggered too early.
- Icons.ICON_COLOR and iconColor() removed, enableIconColorConsumer() and disableIconColorConsumer() removed.
- Utilities.propertyChangeEvent() renamed propertyObserver().
- SearchHighlighter.updateHighlights() bug fixed, now only tries to update highlight if a matching position is found, failed on look and feel change with text selected.
- SearchHighlighter, search no longer case-sensitive by default.
- TransferFocusOnEnter now an enum.
- ComponentBuilder.transferFocusOnEnter(TransferFocusOnEnter) added, related refactoring.
### is.codion.framework.domain
- ValidationException now extends IllegalArgumentException.
- EntityType.resourceBundleName() now returns Optional.
- AttributeDefinition.minimumValue() and maximumValue() now return Optional.
- AttributeDefinition.format(), dateTimePattern() and dateTimeFormatter() now return Optional.
- AttributeDefinition.description() now returns Optional.
- Entity.isNotNull() removed.
- Entity.Key.isNotNull() removed.
- EntityDefinition.entity() factory methods no longer accept null for values and originalValues, empty maps expected instead.
- JSpecify annotations added.
### is.codion.framework.model
- AbstractEntityEditModel.refresh() bug fixed, now handles the case when a primary key attribute has been modified.
- AbstractEntityTableModel.refresh(keys) bug fixed, now includes the same attributes as the query model, when selecting.
- EntityEditModel.EntityEditor.EditorValue.message() added.
- EntityQueryModel.SelectAttributes added, returned by attributes().
- EntityQueryModel.createSelect() added, DefaultEntityQueryModel refactored.
- EntityQueryModel.query() renamed dataSource().
- EntityQueryModel no longer implements Supplier, get() renamed query().
- EntityQueryModel.createSelect() renamed select().
- EntityEditModel.EntityEditor.isNotNull() removed.
### is.codion.swing.framework.ui
- EntityDialogs.selectionDialog() parameter now EntityTablePanel.
- EntitySelectionDialogBuilder.dialogSize() and configureTablePanel() removed, includeSearchButton() added.
- EntityDialogs.EditAttributeDialogBuilder.defaultValue() and applier() added.
- EntityPanel.Builder no longer implements equals() and hashCode() based on entityType, restricts entity panel caching to a single panel per entity type.
- EntityEditComponentPanel, toolTip validation message split from ComponentValidator.
- EntitySearchField.Builder.selectionToolTip() added, enabled by default for multi-selection instances.
- EntityTablePanel bug fixed, both constructors now call initializeConditionsAndFilters().
- EntityTablePanel.updateUI() bug fixed, now includes editPanel.
- EntityTablePanel, redundant disabling of filters on column hidden removed, handled by FilterTable.
- EntityTablePanel.Config.QUERY_HIDDEN_COLUMNS replaced with EXCLUDE_HIDDEN_COLUMNS, EntityTablePanel.queryHiddenColumns() removed.
- EntitySearchField, special handling for transferring focus on enter removed.
- EntityDialogs.selectionDialog() bug fixed, now initializes the EntityTablePanel.
### is.codion.framework.servlet
- EntityService.USE_VIRTUAL_THREADS configuration value added.
- EntityService now configures handlers before starting.

## 0.18.31
### is.codion
- Build with JDK21, target 17.
- Javadoc, replace multiline @code sections with @snippet.
### is.codion.common.model
- FilterModel.Items.replace() overloaded with a Map parameter.
- FilterModel.VisibleItems.set() no longer sorts.
### is.codion.swing.common.model
- DefaultFilterComboBoxModel.replace() bug fixed, added the replacement item even if it did not contain the item being replaced, did not replace the selected item.
- DefaultFilterTableItems.replace() improved a bit, DefaultFilteredItems.items now a LinkedHashSet.
- DefaultFilterTableItems.DefaultVisibleItems.set() now sorts.
- FilterModel.FilteredItems no longer extends Observable.
- DefaultFilterTableItems.merge() refactored.
- DefaultFilterTableItems.remove() refactored.
- FilterTableModel.RowEditor.set(), rowIndex parameter added.
### is.codion.framework.domain
- Entity.Builder.originalPrimaryKey() added.
- AttributeDefinition.trim() added along with TRIM_STRINGS configuration value.
### is.codion.framework.model
- AbstractEntityEditModel now handles edit events, EntityEditModel.add(), remove() and replace() removed, AbstractEntityEditModel.replaceForeignKey() removed, inserted(), updated() and deleted() added.
- EntityTableModel.handleEditEvents() removed.
- EntitySearchModel.Builder.handleEditEvents() added.
- EntityTableModel.editModel() inferred return type removed.
- AbstractEntityTableModel.replaceEntitiesByKey() refactored and renamed.
- EntityEditEvents removed, EntityEditModel.EditEvents added along with editEvents().
- EntitySearchModel.Builder.condition()  now accepts a null argument.
- DefaultEntitySearchModel, improved exception message when refreshing in case the condition supplier returns null or a condition for the incorrect type.
### is.codion.swing.common.ui
- DefaultFilterTableColumnModel, validation for unique model indexes and identifiers added.
- FilterTableColumn.setModelIndex() now throws UnsupportedOperationException.
- MOUSE_WHEEL_SCROLLING configuration property added to ComboBoxBuilder, SpinnerBuilder and SliderBuilder.
### is.codion.swing.framework.model
- EntityComboBoxModel.Builder.condition() now accepts a null argument.
- DefaultEntityComboBoxModel, improved exception message when refreshing in case the condition supplier returns null or a condition for the incorrect type.
### is.codion.plugin.flatlaf.intellij.themes
- HighContrast theme now based on the on from IntelliJ, as in FlatLaf.

## 0.18.30
### is.codion.common.model
- FilterModel.SortModel added along with sort().
- FilterModel.VisibleItems.sort() renamed sortItems().
- FilterModel.SortModel renamed Sorter.
- FilterModel.VisibleItems.sortItems() renamed back to sort().
- DefaultConditionModel bug fixed, now disables on clear, instead of relying on auto-enable, which fails for non-nullable conditions, since the equal operand doesn't become null when cleared.
### is.codion.swing.common.model
- FilterTableModel.ColumnValues.get() and selected() now returns List instead of Collection.
- FilterTableSortModel renamed FilterTableSorter.
- LookAndFeelComboBox, major memory usage reduction, now only holds on the required defaults.
### is.codion.swing.common.ui
- CompletionDocument bug fixed, prevented combo boxes from selecting a null value, if the model did not contain null.
- ComboBoxMouseWheelListener bug fixed, now selects first item instead of second on down scroll with null selected, if the model did not contain null.
- CompletionDocument.remove() bug fixed, did not handle empty combo box.
### is.codion.framework.model
- EntityTableModel.orderQueryBySortOrder() added.
- AbstractEntityTableModel.orderByFromSortModel() added.
- EntityEditModel.EntityEditor.ValueEditor renamed EditorValue.
- DefaultEntityApplicationModel, entityModels now initialized via constructor parameter, related refactoring.
- EntityQueryModel.LIMIT configuration value added.
### is.codion.swing.framework.model
- SwingEntityModel.Builder removed.
### is.codion.swing.framework.ui
- EntityTablePanel.orderQueryBySortOrder() moved to model.
- EntityPanel.Builder, major simplification and related refactoring.
- EntityApplicationPanel.createEntityPanels() and createSupportEntityPanelBuilders() removed, constructor parameters added.

## 0.18.29
### is.codion.common.model
- ConditionModel.wildcard() moved to Operands, Operands.equalWildcards() added.
### is.codion.swing.common.ui
- FilterTable.ScrollToAdded bug fixed, now stops scrolling after the first row.
- FilterTable.Builder.surrendersFocusOnKeystroke() added.
- DefaultFilterTableCellEditor now sets JComboBox.isTableCellEditor client property for combo boxes.
- FilterTable bug fixed, updateUI() now updates cell editor UIs.
- FilterTable.ControlKeys.TOGGLE_NEXT_SORT_ORDER, TOGGLE_PREVIOUS_SORT_ORDER, TOGGLE_NEXT_SORT_ORDER_ADD and TOGGLE_PREVIOUS_SORT_ORDER_ADD added, replacing TOGGLE_SORT_ORDER and TOGGLE_SORT_ORDER_ADD.
- ColumnConditionPanel bug fixed, incorrect operator caption for NOT_BETWEEN and NOT_BETWEEN_EXCLUSIVE.
- DefaultListBoxBuilder bug fixed, did not populate combo box model with initial values.
- Controls.empty(), notEmpty() and get(index) removed, related refactoring.
- Controls.Builder no longer adds empty Controls instances, skips leading and trailing separators, related refactoring.
- FilterTable.Builder.startEditing(keyStroke) added.
- DefaultFilterTableCellEditor now configures JCheckBox background and text field horizontal alignment according to the table cell renderer.
### is.codion.framework.domain
- DualValueColumnCondition now throws exception if a bound value is null.
- SingleValueColumnCondition now throws exception if a required bound value is null.
- DualValueColumnCondition bug fixed, incorrect operators used for NOT_BETWEEN and NOT_BETWEEN_EXCLUSIVE.
### is.codion.framework.domain.db
- MetaDataSchema now supports databases without schemas, as in, SQLite.
### is.codion.framework.db
- EntityConnectionProvider.connection() inferred return type removed.
- AbstractEntityConnectionProvider.validateConnection() replaced with validConnection(), connection() no longer final.
### is.codion.framework.db.local
- LocalEntityConnectionProvider now overrides connection() with LocalEntityConnection return type.
### is.codion.framework.model
- DefaultEntityTableConditionModel bug fixed, inCondition() and notInCondition() now return isNull() and isNotNull() respectively when no operands are specified.
- DefaultEntityTableConditionModel now returns an appropriate greaterThan or lessThan condition when either the lower or upper bound is missing for between conditions.
### is.codion.swing.framework.model
- SwingEntityTableModel.isCellEditable() now final, protected editable() added.
### is.codion.swing.framework.ui
- EntitySearchField bug fixed, did not initialize correctly when instantiated with a search model with non-empty selection.
- EntityTablePanel.ControlKeys.DECREMENT_SELECTION and INCREMENT_SELECTION keys changed from ALT-SHIFT to CTRL-SHIFT.
- EntityApplicationPanel.Builder.LoginProvider removed, loginProvider() replaced with userSupplier().
- EntityApplicationPanel.Builder.ConnectionProviderFactory removed, Builder.connectionProvider() added, related refactoring.
- EntityApplicationPanel.Builder.defaultLoginUser(), automaticLoginUser() and userSupplier() renamed defaultUser(), user() and user() respectively.
- EntityApplicationPanel.Builder.loginRequired() removed.
- EntityApplicationPanel.Builder.connectionProvider(EntityConnectionProvider) and user(User) added, connectionProvider(EntityConnectionProvider.Builder) replaced with connectionProvider(Function<User, EntityConnectionProvider>).
- EntityPanelBuilder.createPanel() bug fixed, no longer validates model types when constructor parameter type is the base model class.
- DefaultEditComponentFactory bug fixed, text input panel not initialized with the current value.
- DefaultEditComponentFactory.MAXIMUM_TEXT_FIELD_LENGTH replaced with DEFAULT_TEXT_FIELD_COLUMNS, back to text input panels for all strings, with the number of columns depending on the value and maximum length.
### is.codion.tools.generator.model
- DomainGeneratorModel now supports databases without schemas and users, as in, SQLite.
### is.codion.tools.generator.ui
- DomainGeneratorPanel.USER_REQUIRED configuration property added.

## 0.18.28
### is.codion.common.model
- FilterModel.VisibleItems.added() added.
- FilterModel.Items.replace() added.
### is.codion.swing.common.model
- DefaultFilterTableItems.DefaultVisibleItems now always sorts when items are added, even when added at a specific index.
- FilterTableModel.RowEditor added.
### is.codion.swing.common.ui
- FilterTable.scrollToAddedItem() added.
- FilterTable.scrollToCoordinate() renamed scrollToRowColumn().
### is.codion.framework.domain
- ColumnDefinition.lazy() and selectable() replaced with selected().
- MultiValueColumnCondition bug fixed, now rejects empty values for IN and NOT_IN.
- Entity.Key.get() renamed value().
- Entity.clearPrimaryKey() moved to Builder.
- Entities.keyBuilder() moved to Entity.Builder.key().
### is.codion.framework.model
- EntityModel.DetailModels.get() now returns a Map, related refactoring.
- Remove type inference return values, convenient but unsafe.
- ModelLink, model type parameters added, removed from static builder factory methods.
- EntityEditModel.Insert, Update and Delete renamed InsertEntities, UpdateEntities and DeleteEntities.
- EntityEditModel.afterUpdate() map keys are now an immutable copy of the entity before update, instead of only the original primary key.
- EntityTableModel.OnInsert.ADD_TOP and ADD_TOP_SORTED replaced with PREPEND and ADD_BOTTOM and ADD_BOTTOM_SORTED with APPEND.
- EntityEditModel.beforeUpdate() observer data now a Collection instead of a Map.
### is.codion.swing.framework.ui
- Remove type inference return values, convenient but unsafe.
- EntityPanelBuilder.createPanel(), createEditPanel() and createTablePanel() error message on incorrect model type improved.
- EntitySearchField.Builder.selectorFactory() function input now EntitySearchField instead of EntitySearchModel.
- EntitySearchField.DefaultListSelector now uses the search field stringFactory.
- EntitySearchField.Selector.select(), dialogOwner parameter removed, search field used.

## 0.18.27
### is.codion.common.core
- Value.set() and map() return value removed.
- AbstractValue now only compares the values when set if the notification policy requires it.
- Configuration property value methods now statically imported.
### is.codion.common.db
- Database.subqueryRequiresAlias() removed, related refactoring.
- Database.subqueryRequiresAlias() reintroduced.
### is.codion.common.model
- DefaultConditionModel.DefaultSetCondition refactored.
- MultiSelection.Indexes now extends Value.
- MultiSelection.Items now extends Value.
- SingleSelection.Item replaced with Value.
- ConditionModel.Builder.operands() added, InitialOperands removed.
- MultiSelection.Indexes.set(Collection) removed.
### is.codion.common.rmi
- SerializationWhitelist renamed SerializationFilter, whitelist implementation removed, now based on the built in pattern based filter.
- SerializationFilterFactory, configuration properties renamed, some cleanup, docs improved.
### is.codion.swing.common.ui
- ColumnConditionPanel.ComponentFactory, component() methods replaced with equal(), upper(), lower() and in().
- LookAndFeelEnabler.enableLookAndFeel() now fallbacks to the defaultLookAndFeel, if the user preference one is not available.
- DialogBuilder.onBuild() added.
- CalendarPanel.doubleClicked() added.
- DefaultCalendarDialogBuilder now accepts the selected date when the day selection panel is double clicked.
- FilterTable.Export.replaceNewline(String) added, defaults to a single space, DefaultExport now automatically trims strings.
- FilterTable.Builder.cellSelectionEnabled().
- FilterTable, column moving, resizing and sorting now based on lead selection index instead of selected column.
### is.codion.framework.db.rmi
- LocalConnectionHandler, method logging improved, fetch/return connection no longer logged during an open transaction.
- LocalConnectionHandler, local connection creation logged.
### is.codion.framework.domain
- Entity.entityType() renamed type().
- EntityDefinition.entityType() renamed type().
- Entity.Key.entityType() renamed type().
- Column.Setter added.
- Column.Fetcher renamed Getter for consistency.
- DefaultColumnDefinitionBuilder bug fixed, columnClass() did not replace the default setter.
### is.codion.framework.model
- DefaultForeignKeyConditionModel now links search models to their respective operands.
- ForeignKeyDetailModelLink, configuration methods renamed along with associated properties.
- DefaultEntityModel, type parameters now refer to EntityModel and EntityEditModel instead of DefaultEntityModel and AbstractEntityEditModel.
- DefaultEntityModel now abstract, renamed AbstractEntityModel.
- DetailModelLink and ForeignKeyDetailModelLink now Builder based, implementations package private and final.
- DetailModelLink and ForeignKeyDetailModelLink renamed ModelLink and ForeignKeyModelLink.
- EntityModel.DetailModels.linked() renamed active().
- EntityModel.DetailModels.link() replaced with active().
- ModelLink, type parameters removed.
- EntityModel, M type parameter removed.
- EntityModel.link() added, now throws exception in case multiple fitting foreign keys are found.
- EntityModel.DetailModels.contains(EntityType) and contains(Class) removed.
- AbstractEntityModel bug fixed, now initializes a detail model link if active when added.
- AbstractEntityModel now calls onSelection() for all detail model links, not just active ones.
- DefaultModelLink.onSelection() does nothing unless link is active.
- EntityEditModel.value() removed.
- EntityEditModel.PERSIST_FOREIGN_KEYS moved to EntityEditor, some docs fixes and minor cleanup.
- ForeignKeyModelLink.Builder now extends ModelLink.Builder.
- EntityModel, M type parameter reintroduced.
### is.codion.swing.framework.model
- DefaultSwingForeignKeyConditionModel now links search and combo box models to their respective operands.
### is.codion.swing.framework.ui
- EntityConditionComponentFactory no longer links foreign key components to operands.
- DefaultEntityApplicationPanelBuilder now adds the framework version to system properties along with the application version.
- EntityPanel.DetailPanels.linked() renamed active().
### is.codion.tools.monitor.ui
- Demo server monitor split into separate demo-server-monitor module.
### is.codion.tools.generator.ui
- Demo domain generator split into separate demo-domain-generator module.

## 0.18.26
### is.codion.common.model
- TableConditionModel.ConditionModelFactory removed, related refactoring.
- ConditionModel.SetCondition added along with set().
### is.codion.swing.common.model
- FilterComboBoxModel.Builder.filterSelected() added, now final, ComboBoxSelection removed.
### is.codion.swing.common.ui
- ColumnConditionPanel.ControlKeys.CLEAR added.
- ComponentBuilder.TRANSFER_FOCUS_ON_ENTER removed.
- DefaultControlsBuilder.separatorAt() bug fixed, appended instead of inserting at index.
- DefaultControlsBuilder, now prevents multiple consecutive separators from being added via actions().
- DefaultFilterTableSearchModel optimized, searchResults no longer ValueList based.
- DefaultFilterTableSearchModel now clears the search text when columns are modified, which also clears the result.
### is.codion.framework.server
- Demo server split into separate demo-server module.
### is.codion.framework.model
- DefaultEntityQueryModel.conditionChanged() bug fixed, no longer active when limit, orderBy or attributes have changed.
- EntityQueryModel.resetConditionChanged() removed.
- EntitySearchModel.Builder.condition() added.
- ForeignKeyConditionModel.equalSearchModel() and inSearchModel() moved to ForeignKeyOperands.
- EntityConditionModel.attribute() replaced with column() and foreignKey().
- ForeignKeyConditionModel now an interface.
- EntityConditionModel renamed EntityTableConditionModel.
- AttributeConditionModelFactory renamed EntityConditionModelFactory.
- ForeignKeyConditionModel.ForeignKeyOperands removed.
- EntityConditionModelFactory.conditionModel(ForeignKey) now returns ForeignKeyConditionModel.
- EntitySearchModel.Builder.columns() renamed searchColumns().
- EntityTableConditionModel no longer extends TableConditionModel.
- EntityTableConditionModel.setEqualOperand() and setInOperands() removed.
- DefaultEntityQueryModel bug fixed, refactored in order to prevent ConcurrentModificationException, no longer List based.
### is.codion.swing.framework.model
- SwingForeignKeyConditionModel.equalComboBoxModel() and inSearchModel() moved to SwingForeignKeyOperands.
### is.codion.swing.framework.ui
- EntityTablePanel.SHOW_REFRESH_PROGRESS_BAR renamed REFRESH_PROGRESS_BAR, now true by default.
- EntitySearchField bug fixed, no longer updates the background color when disabled.
- EntitySearchField.searchOnFocusLost now final, searchOnFocusLost() removed.
- EntityEditComponentPanel.InputFocus.transferOnEnter() removed.
- SwingEntityTableModel.EntityColumnFilterFactory bug fixed, did not include foreign key filters.
- SwingForeignKeyConditionModel now an interface, extends ForeignKeyConditionModel.
- SwingAttributeConditionModelFactory renamed SwingEntityConditionModelFactory.
- SwingForeignKeyConditionModel.SwingForeignKeyOperands removed.
- EntitySearchField.selectionToolTip() now performs some basic html escaping.
- EntitySearchField bug fixed, no longer selects all and moves the cursor when returning from the selection dialog.
- EntityEditComponentPanel.InputFocus.transferOnEnter() reintroduced.
- DefaultEditComponentFactory, EntitySearchField no longer performs search on focus lost.
- EntitySearchField.Builder, method return type fixed.
- EntityComponents.textField() bug fixed, did not set the format and tooltip text for numerical and temporal fields.
- EntityEditComponentPanel.InputFocus.AfterInsert bug fixed, component supplier initialized incorrectly, always returned null.

## 0.18.25
### is.codion.common.db
- ConnectionFactory.createConnection() and ConnectionProvider.connection() overload without User parameter added.
### is.codion.common.model
- FilterModel.Refresher.success() and failure() renamed result() and exception() respectively, related renaming.
- FilterModel.Items no longer extends Observable.
- FilterModel.Items.add() and remove() now void.
- ConditionModel.Builder.operands() added along with InitialOperands.
### is.codion.framework.domain
- DefaultEntityValidator.valid() now final.
- AttributeDefinition.Builder.mnemonicResourceKey() added.
- ForeignKeyDefiner.softForeignKey() replaced with ForeignKeyDefinition.Builder.soft().
- EntityDefinition.cacheToString() added, redundant comment cleanup.
- Entity.Key.entityDefinition() renamed definition().
- Entity.Key.primaryKey() renamed primary().
- ObservableValues.empty() renamed isEmpty(), notEmpty() removed.
- Observable.isNotNull() removed.
### is.codion.framework.model
- EntitySearchModel.Builder.attributes() added.
- EntitySearchModel.Builder.orderBy() added.
- EntitySearchModel.entityType() replaced with entityDefinition().
- EntitySearchModel.stringFunction() now final and no longer Value based.
- EntitySearchModel.stringFunction() renamed stringFactory().
- EntitySearchModel.separator() now final and no longer Value based.
- EntitySearchModel.description() removed.
- EntitySearchModel.reset(), searchStringModified(), stringFactory() and separator() removed.
- EntitySearchModel.Selection.string() and strings() added.
- EntitySearchModel.Selection.string() and strings() now sorted.
- EntitySearchModel.Search added, related refactoring and renaming.
- EntitySearchModel.Search.text() replaced with strings(), Builder.separator() and stringFactory() removed.
- AttributeConditionModelFactory bug fixed, now initializes the equal operand of non-null boolean condition models.
### is.codion.swing.common.model
- FilterComboBoxModel.ItemComboBoxModelBuilder.selected() added.
### is.codion.swing.common.ui
- FilterTableCellRenderer.UISettings.selectionForeground() added.
- DefaultFilterTableCellRenderer uses UISettings.selectionForeground().
- LookAndFeelEnabler.platform() added.
- PlatformLookAndFeelProvider added, related refactoring.
- LookAndFeelSelectionDialogBuilder.includePlatformLookAndFeels() added along with INCLUDE_PLATFORM_LOOK_AND_FEELS, for excluding platform look and feels from the selection if auxiliary ones are available.
- Components.textField() no longer forwards call to NumberField in case of numerical values.
- ColumnConditionPanel.FieldFactory.createLowerField() and createUpperField() now with default implementations.
- TransferFocusOnEnter.forwardAction() and backwardAction() removed, transfer focus actions now singletons.
- ColumnConditionPanel.FieldFactory refactored and renamed ComponentFactory, related renaming and refactoring.
- ConditionPanel.condition() renamed model(), related renaming.
### is.codion.swing.framework.model
- EntityComboBoxModel.entityType() replaced with entityDefinition().
### is.codion.swing.framework.ui
- EntityComponents.textField() now forwards call to NumberField in case of numerical attributes.
- EntityComponents.textField() now creates an non-focusable, read only text field for item based attributes.
- EntitySearchField.selectorFactory now final, selectorFactory() removed.
- EntitySearchField.searchIndicator now final, searchIndicator() removed.
- EntityComponents.supports() removed, unused.
- EntitySearchField now displays the selected entities in a multiline tooltip instead of the model description.
- EntitySearchField refactored.
- EntitySearchField.Build.separator() and stringFactory() added, related refactoring.
- EntitySearchField.Builder.selectAllOnFocusGained() removed, related refactoring.
- EntitySearchField, EntitySearchFieldPanel.MultiSelectionBuilder and SingleSelectionBuilder added along with Factory, related refactoring.
- EntityComponentFactory renamed EditComponentFactory.
- EntityComponents.MAXIMUM_TEXT_FIELD_LENGTH configuration value added.
- EntitySearchField.Builder.editable() added.
- EntityComponents.MAXIMUM_TEXT_FIELD_LENGTH moved to DefaultEditComponentFactory.
- EntityTablePanel.orderByFromSortModel() empty order by clause bug fixed, now excludes non-column based attributes correctly.
### is.codion.tools.generator.ui
- DomainGeneratorPanel bug fixed, did not display exceptions happening during async table model refresh.

## 0.18.24
### is.codion.common.core
- DefaultObserver bug fixed, did not work correctly when a consumer implemented both Runnable and Consumer, such as Event, then it was notified only as Runnable.
### is.codion.swing.common.model
- ItemComboBoxModel.builder() moved to FilterComboBoxModel.
- ItemComboBoxModel removed, methods moved to FilterComboBoxModel.
- DefaultItemComboBoxModelBuilder null handling now nullItem() based.
### is.codion.swing.common.ui
- LookAndFeelPanel.initializeLookAndFeelDefaults() error message improved.
- ExceptionDialogBuilder.WRAPPER_EXCEPTIONS, RuntimeException re-added, but unwrapping now unwraps according to the exact class, not isAssignableFrom, RemoteException removed.
- LookAndFeelProviders added, ServiceLoader based, related refactoring.
- LookAndFeelProvider.CROSS_PLATFORM and SYSTEM configuration values removed.
- LookAndFeelComboBox now populates the first item with the system or cross platform look and feels, depending on LookAndFeelComboBox.DEFAULT_LOOK_AND_FEEL.
- LookAndFeelProvider renamed LookAndFeelEnabler, LookAndFeelProviders renamed LookAndFeelProvider, related refactoring.
- LookAndFeelEnabler.enableLookAndFeel() added, related refactoring.
### is.codion.swing.framework.ui 
- DefaultEntityApplicationPanelBuilder no longer validates that the look and feel exists on the classpath, should not prevent application start.
- EntityApplicationPanel.Builder.defaultLookAndFeelClassName() and lookAndFeelClassName() renamed defaultLookAndFeel() and lookAndFeel() respectively, overloaded with Class parameter. 
### is.codion.plugin.flatlaf.intellij.themes
- Added a bunch of IntelliJ theme based look and feels along with all of those available in com.formdev.flatlaf-intellij-themes.
- IntelliJThemes now implements LookAndFeelProviders.
- Module is.codion.plugin.intellij.themes renamed is.codion.plugin.flatlaf.intellij.themes.
- is.codion.plugin.flatlaf.intellij.themes.material.ArcDark renamed ArcDarkMaterial, was being used instead of non-material ArcDark.
### is.codion.plugin.flatlaf
- FlatLookAndFeelProviders in module is.codion.plugin.flatlaf added.

## 0.18.23
### is.codion.common
- JSpecify annotations applied to common modules.
## is.codion.common.core
- ValueObserver.getOrThrow() added.
- ValueObserver merged with Observable, related refactoring and renaming.
- Mutable removed.
- is.codion.common.observer package renamed observable.
- Value no longer extends Consumer.
- Observable.getOrThrow() happy path optimized.
- DefaultObserver refactored, improved and optimized.
- Value.nullable() renamed isNullable(), Value.value() factory method renamed nullable(), Value.nonNull() factory method added.
- Values.builder() removed.
## is.codion.common.db
- Database.connectionProvider() no longer accepts null.
- ResultPacker.pack() no longer skips null instances returned from get(), caller must handle any null values encountered.
### is.codion.common.model
- ConditionModel.dateTimePattern() now returns Optional.
- ConditionModel.wildcard() now back to being Value based instead of Mutable.
- TableConditionModel.persist() added, for excluding conditions when clearing the model.
- MultiItemSelection.Index added.
- FilterModel.Refresher.observable() renamed active().
- ConditionModel.Operands.lowerBound() and upperBound() shortened to lower() and upper().
- FilterModel.refresh() and related methods moved to Items, related refactoring.
- TableSummaryModel.summaryModel() renamed get().
- FilterModel.Items.addItem(), addItems(), removeItem() and removeItems() renamed add() and remove().
- FilterMode.VisibleItems.itemAt() renamed get(), addItemAt() and addItemsAt() renamed add(), setItemAt() renamed set(), removeItemAt() and removeItemsAt() renamed remove().
- SingleItemSelection and MultiItemSelection renamed SingleSelection and MultiSelection.
- FilterModel.VisibleItems.comparator() added.
### is.codion.common.rmi
- ServerConfiguration.serverName() now throws exception if the server name supplier returns null or an empty string.
- Server.serverLoad() removed, related refactoring.
- ServerConfiguration.Builder.rmiClientSocketFactory() and rmiServerSocketFactory() removed.
- ServerConfiguration.builderFromSystemProperties() bug fixed, failed if no AUXILIARY_SERVER_FACTORY_CLASS_NAMES were specified.
### is.codion.dbms.h2
- H2DatabaseFactory.runScript() static methods replaced with ScriptRunner.
### is.codion.swing.common.model
- FilterComboBoxModel.ComboBoxSelection.value() and nullSelected() removed.
- DefaultFilterTableSelection bug fixed, back to inheriting from DefaultListSelectionModel since some JTable keyboard actions rely on it.
- DefaultFilterComboBoxModel.DefaultComboBoxItems.removeItem() now sets the selection to null if the selected item is removed, addItems() and removeItems() implemented.
- DefaultFilterComboBoxModel events fixed, related refactoring.
- DefaultFilterTableModel synchronized.
- DefaultFilterComboBoxModel synchronized.
- FilterTableSortModel.sortingEnabled() replaced with locked().
- FilterTableSortModel.Sort added along with sort(), replacing setSortOrder() and addSortOrder().
- FilterTableSortModel.ColumnSort added along with columnSort(), replacing columnSortOrder(identifier) and columnSortOrder().
- FilterTableSortModel.locked() moved to Sort.
- FilterTableModel.FilterTableItems added, refreshStrategy() moved.
- DefaultFilterTableItems refactored from DefaultFilterTableModel.
- FilterTableModel.Columns renamed TableColumns.
- FilterTableSortModel.Sort and sort() renamed Order and order() respectively.
- FilterTableSortModel.columnSort() renamed columns(), sorted() added.
- FilterTableModel.sorter() renamed sort().
- FilterTableSortModel.ascending() and descending() added.
- FilterTableModel.ColumnValues added along with values(), getStringAt(), values() and selectedValues() moved to ColumnValues as string(), get() and selected() respectively.
- DefaultFilterTableItems.add(item) and add(items) now sort the visible items after adding.
### is.codion.swing.common.ui
- TableConditionPanel bug fixed, clear control did not use TableConditionModel.clear().
- SearchHighlighter refactored.
- TemporalField.setTemporal(), getTemporal() and temporalValue() renamed set(), get() and observable() respectively.
- NumberField.number() replaced with set(), get() and observable(), optional() added.
- ProgressWorker.Builder.onCancelled() handler now gets called in case the background task throws CancelException.
- DefaultProgressWorkerDialogBuilder, CancelException handling removed, some refactoring.
- FilterTable bug fixed, sorting actions no longer enabled for columns with sorting locked in the sort model.
- ComboBoxMouseWheelListener and SpinnerMouseWheelListener bug fixed, no longer enabled when component is disabled.
- CalendarPanel.Builder.enabled() added.
- FilterTableSearchModel.Results added along with results(), related refactoring and renaming.
- FilterTable.DefaultBuilder now validates column model indexes.
- CalendarPanel.CalendarDate and CalendarDateTime added along with date() and dateTime(), related refactoring.
- FilterTable.searchModel() and summaryModel() renamed search() and summaries() respectively.
- ExceptionDialogBuilder.WRAPPER_EXCEPTIONS no longer contains RuntimeException, now that we've stopped using checked exceptions.
- LookAndFeelComboBox bug fixed, now handles the case when the current look and feel is not available in the combo box model.
### is.codion.framework.domain
- EntityDefinition.description() now returns Optional.
- DefaultEntityDefinition, source attributes validated for denormalized attributes.
- DenormalizedValueProvider now validates the denormalized attribute in case the source is a foreign key.
- Entity.groupByValue() can now handle null values.
- AbstractCondition now throws exception if the number of condition columns does not match the number of values.
- Condition.custom() moved to ConditionType.get().
- ConditionType.get() overloaded with a single column and value and no columns.
### is.codion.framework.db.core
- EntityConnectionProvider.clientType() and clientVersion() now return Optional.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.createValueString() bug fixed, no longer crashes when the number of condition columns does not match the number of values.
### is.codion.framework.model
- DefaultForeignKeyDetailModelLink now uses TableConditionModel.persist() to exclude the associated foreign key condition from being cleared.
- EntityEditModel.EditableEntity and EditableValue renamed EntityEditor and ValueEditor respectively.
- EntityEditModel.validate() methods moved to EntityEditor.
- AbstractEntityEditModel.DefaultEntityEditor moved up to package level.
- EntityEditModel.EntityEditor.refresh() moved to EntityEditModel along with entityConnectionProvider.
- EntityEditModel.entity() renamed editor().
- EntityEditModel.EntityEditor.Modified now only active when the entity exists, consistent with ValueEditor.modified(), EntityEditor.edited() removed.
- EntityModel.DetailModels added, related refactoring and renaming.
- EntityApplicationModel.EntityModels added, related renaming.
- EntityEditModel.createForeignKeySearchModel() and foreignKeySearchModel() renamed createSearchModel() and searchModel() respectively.
### is.codion.swing.framework.model
- SwingEntityEditModel.createForeignKeyComboBoxModel() and foreignKeyComboBoxModelModel() renamed createComboBoxModel() and comboBoxModel() respectively.
### is.codion.swing.framework.ui
- TabbedApplicationLayout.applicationPanel() removed.
- EntityApplicationPanel.displayEntityPanelFrame() and displayEntityPanelDialog() now initialize the focus by activating the panel when opened.
- EntityPanel.activateEvent() renamed activated().
- EntityApplicationPanel.initializedEvent() renamed initialized().
- EntityApplicationPanel.requestInitialFocus() added, called by DefaultEntityApplicationBuilder on application start.
- EntityApplicationPanel.extit(), unsaved modification handling improved, related refactoring.
- EntityPanel.activated() replaced with requestDisplay() and displayRequested(), ApplicationLayout.activate() and DetailController.activate() renamed display().
- EntityPanel.DetailPanels added, related refactoring.
- EntityPanel.builder(SwingEntityModel) removed
- EntityApplicationPanel.displayEntityPanel() renamed displayEntityPanelWindow().
- EntityPanel.Display added.
- EntityPanel.activate() bug fixed, requests display first, to ensure correct initial focus.
- EntityPanel.Display change reverted, activate() and activated() reintroduced, needs polishing.
- EntityComponents.foreignKeyComboBox(), foreignKeyComboBoxPanel(), foreignKeySearchField(), foreignKeySearchFieldPanel(), foreignKeyTextField() and foreignKeyLabel() renamed comboBox(), comboBoxPanel(), searchField(), searchFieldPanel(), textField() and entityLabel() respectively.
- EntityEditComponentPanel.createForeignKeyComboBox(), createForeignKeyComboBoxPanel(), createForeignKeySearchField(), createForeignKeySearchFieldPanel(), createForeignKeyTextField() and createForeignKeyLabel() renamed createComboBox(), createComboBoxPanel(), createSearchField(), createSearchFieldPanel(), createTextField() and createEntityLabel() respectively.
- EntityComponents.component() now returns a entity text field for foreign keys.
- EntityComponents.entityLabel(), EntityEditComponentPanel.createEntityLabel() removed.
- EntityComponents.list() removed.
- EntityEditComponentPanel.InputFocus added, related refactoring.
- EntityEditComponentPanel.selectInputComponent() moved to EntityEditPanel, selectableComponents() replaced with EntityEditPanel.Config.excludeFromSelection().
- DefaultEntityApplicationPanelBuilder now validates that the look and feel exists on the classpath.

## 0.18.22
### is.codion.common.core
- PropertyStore.systemProperties() now uses a DefaultSystemPropertyFormatter, which formats class and module paths as one item per line.
### is.codion.common.rmi
- ConnectionRequest.clientTypeId() renamed clientType().
### is.codion.common.model
- MultiItemSelection.Indexes.moveDown() and moveUp() renamed increment() and decrement() respectively.
- MultiItemSelection.singleSelectionMode() renamed singleSelection().
### is.codion.swing.common.ui
- Completion.NORMALIZE configuration value added.
- Completion.COMBO_BOX_COMPLETION_MODE renamed COMPLETION_MODE.
- ComboBoxBuilder.normalize() added.
- ItemComboBoxBuilder.normalize() added.
- Completion, CompletionDocument refactored to support accented character input in Linux.
- ComboBoxBuilder and ItemComboBoxBuilder.normalize() parameter now boolean.
- Completion.Builder added, Normalize removed, related refactoring.
- ComponentBuilder.name() added.
- TableConditionPanel.get() and get(identifier) renamed panels() and panel() respectively.
- FilterTable.createSingleSelectionModeControl() renamed createSingleSelectionControl().
- TextComponentBuilder.initialCaretPosition() renamed caretPosition(), InitialCaretPosition renamed CaretPosition.
- LookAndFeelProvider.lookAndFeel() no longer throws Exception.
### is.codion.framework.domain
- DerivedAttribute.SourceValues.optional() default implementation removed.
- DefaultEntity.DefaultSourceValues implements optional(), no longer throws exception in case the attribute is not a source attribute.
### is.codion.swing.framework.ui
- EntityEditComponentPanel, create component methods now assign a name to the component, using attribute.toString().
- EntityTablePanel.ControlKeys.MOVE_SELECTION_DOWN and MOVE_SELECTION_UP renamed INCREMENT_SELECTION and DECREMENT_SELECTION respectively, related renaming.
- EntityTablePanel.ControlKeys.SELECTION_MODE renamed SINGLE_SELECTION, Config.includeSelectionModeControl() renamed includeSingleSelectionControl().
- EntityTablePanel.Config.POPUP_MENU_LAYOUT and TOOLBAR_LAYOUT configuration values added.
- EntityApplicationPanel, button for displaying system properties added to about panel, DISPLAY_SYSTEM_PROPERTIES configuration value added.

## 0.18.21
### is.codion.common.db
- DatabaseException now extends RuntimeException, no longer checked.
- ReportException now extends RuntimeException, no longer checked.
### is.codion.swing.common.ui
- ExceptionDialogBuilder.unwrap() added.
### is.codion.framework.domain
- AuditColumn removed, unused.
- AbstractAttributeDefinition, Item based attributes now use a custom comparator, sorting by caption.
- AttributeDefinition.definitionComparator() removed.
- Entity.groupKeysByType() moved to Entity.Key and renamed groupByType().
- Entity.Key.copyBuilder() renamed copy().
- Entity.Copy.immutable() removed.
- ValidationException now extends RuntimeException, no longer checked.
### is.codion.framework.db
- EntityConnection.transaction() now catches Throwable.
- EntityConnection.Transactional and TransactionalResult.execute() now throw Exception.
### is.codion.swing.framework.ui
- EntityTableCellRenderer, Item based cells now use the default horizontal alignment instead of the one based on the the item value type.
- DefaultEntityApplicationPanelBuilder bug fixed, exception handling during application start improved.
### is.codion.plugin.imagepanel
- NavigableImagePanel.isWithinImage() bug fixed, now returns false if image is null.

## 0.18.20
### is.codion.common.model
- FilterModel.VisibleItems.comparator() removed.
### is.codion.framework.domain
- DefaultAttribute.DefaultType.validateType() bug fixed, error message now contains the attribute.
- DefaultEntityDefinition, improved error message on duplicate attribute names.
### is.codion.swing.common.model
- DefaultFilterComboBoxModel.DefaultComparator bug fixed, did not use collator to sort strings.
- FilterComboBoxModel.NullItem removed.
- FilterComboBoxModel.Builder added, related refactoring.
- FilterComboBoxModel.ComboBoxSelection.translator() removed, Builder.translator() added.
- ItemComboBoxModel.SelectedItemTranslator refactored.
### is.codion.swing.common.ui
- FilterTableSortModel.sortingChanged() renamed observer, now propagates the sorting state instead of a column identifier.
- FilterTableSortModel.sortOrder() replaced with columnSortOrder(), sortPriority() removed, ColumnSortOrder.priority() added.
- FilterTableSortModel.sorted() removed.
- FilterTableSortModel.setSortingEnabled() and isSortingEnabled() replaced with State based sortingEnabled().
- FilterTableSortModel, refactoring mistake fixed, sortOrder() no longer returns Optional.
- FilterTableSortModel moved back to swing.common.model.
### is.codion.swing.framework.model
- EntityComboBoxModel.Filter and ForeignKeyFilter added.
- DefaultEntityComboBoxModel bug fixed, now uses the comparator from the entity it is based on.
- EntityComboBoxModel.ForeignKeyFilter.set() overloaded for a single Key.
- EntityComboBoxModel.Builder.comparator() added.
- DefaultEntityComboBoxModel.SelectedItemTranslator removed.
- EntityComboBoxModel.ForeignKeyFilter.get() added.
### is.codion.swing.framework.ui
- EntityApplicationPanel now disposes all windows on exit.
- EntityTablePanel, row limit configuration field now selects all on focus gained.
- EntityTablePanel, row limit configuration now accessible by double clicking the status panel.
### is.codion.tools.generator.domain
- DomainSource refactored.
- DomainSource can now generate DTOs.

## 0.18.19
### is.codion.common.model
- ConditionModel.WILDCARD default now PREFIX_AND_POSTFIX.
- FilterModel.Refresher.supplier() removed.
### is.codion.swing.common.model
- DefaultFilterComboBoxModel.DefaultComboBoxItems.visiblePredicate bug fixed, now Notify.WHEN_SET.
- DefaultFilterTableModel.VisiblePredicate.predicate bug fixed, now Notify.WHEN_SET.
- ItemComboBoxModel no longer extends DefaultFilterComboBoxModel, now a factory class.
- ItemComboBoxModel.Builder along with builder() added, replacing factory methods.
- FilterComboBoxModel.ComboBoxSelection.validPredicate() removed.
- FilterComboBoxModel.ComboBoxItems.validator() removed.
### is.codion.swing.common.ui
- DefaultFilterTableCellRenderer.DefaultBuilder bug fixed, useBooleanRenderer now initialized before settings, in order for a correct horizontal alignment for boolean columns.
- FilterTableCellRenderer.Factory.create() T type parameter removed.
- FilterTableColumnModel.identifiers() added.
- DefaultFilterTableCellRenderer bug fixed, cell specific background color now included in alternate row coloring, some refactoring and renaming.
### is.codion.framework.db
- DefaultLocalEntityConnection.delete() bug fixed, database exception now with the correct Operation.
### is.codion.swing.framework.model
- EntityComboBoxModel, foreign key filtering and linking improved.
- EntityComboBoxModel.foreignKeyVisiblePredicate() renamed foreignKeyFilterPredicate().
- EntityComboBoxModel.ForeignKeyFilters added, related refactoring.
- EntityComboBoxModel now an interface.
- EntityComboBoxModel.ForeignKeyFilters renamed ForeignKeyFilter.
- EntityComboBoxModel.ForeignKeyComboBoxModelFactory and ForeignKeyComboBoxModelLinker removed, ForeignKeyFilter.link() added.
- EntityComboBoxModel.Builder added, related refactoring.
- EntityComboBoxModel.ForeignKeyFilter.builder() added.
- EntityComboBoxModel.ForeignKeyFilter.builder() now sets includeNull() to true if the foreign key is nullable.
- EntityComboBoxModel.foreignKeyFilter() renamed filter().
### is.codion.swing.framework.ui
- EntityTablePanel.Config.cellRenderer() and cellEditor() added.
- EntityComboBox.model() added for consistency.
- EntityComboBox.createForeignKeyFilterControl() removed.
- EntityTablePanel, no longer removes tooltips from south toolbar buttons, seem to only block mouse cursor in MetalLookAndFeel.
- EntityTableCellRenderer bug fixed, create(Attribute, SwingEntityTableModel) never got called.
- EntityTable.Config.cellEditorFactory() and cellRendererFactory() added.
- EntityTableCellRenderer.Factory.create() T type parameter removed.
- EntityDependenciesPanel.displayDependenciesDialog() removed, EntityTablePanel and EntityEditPanel refactored accordingly.
- EntityTablePanel no longer applies preferences when constructed, applyPreferences() now public.
- EntityPanel.applyPreferences() now public.
- EntityApplicationPanel now handles applying preferences for its entity panels.
- EntityTablePanel now remembers and reuses the dependencies dialog size.
- EntityTablePanel now remembers and reapplies column user preferences for dependency panels, related refactoring.
- EntityApplicationPanel, cached entity panels displayed via displayEntityPanel() now remember their size next time they are opened.
- EntityPanelBuilder bug fixed, no longer overrides the panel condition and filter view, unless explicitly set in the builder.
- EntityApplicationPanel.USE_CLIENT_PREFERENCES renamed USER_PREFERENCES_ENABLED and RESTORE_DEFAULT_PREFERENCES added.
- EntityApplicationPanel.CALL_SYSTEM_EXIT added.
- EntityComboBox.foreignKeyComboBox() and ForeignKeyComboBoxFactory removed.

## 0.18.18
### is.codion.common.db
- DatabaseFactory.createDatabase() renamed create().
- DatabaseConnection.rollbackTransaction() and commitTransaction() no longer ignore exceptions, now throw SQLException.
### is.codion.common.model
- ColumnConditions renamed TableConditionModel.
- TableConditionModel.ColumnConditionFactory renamed ConditionModelFactory.
### is.codion.swing.common.ui
- FilterColumnConditionPanel.Builder.operatorCaptions() added.
- ColumnConditionPanel.identifier() removed along with type parameter.
- FilterColumnConditionPanel column identifier type parameter removed, related refactoring.
- FilterTableCellEditorFactory.tableCellEditor() renamed create() for consistency.
- FilterTableCellRendererFactory.tableCellRenderer() renamed create() for consistency.
- ColumnConditionsPanel.selectableConditionPanels() back to returning Collection instead of Map.
- ColumnConditionPanels.conditionPanels(), selectableConditionPanels() and selectConditionPanel() renamed panels(), selectablePanels() and selectPanel() respectively.
- FilterTable.filterPanel() renamed filters().
- FilterTableCellRendererFactory moved to FilterTableCellRenderer and renamed Factory.
- FilterTableCellEditorFactory moved to FilterTableCellEditor and renamed Factory.
- ColumnConditionPanels.selectablePanels() and selectPanel() renamed selectable() and select() respectively.
- FilterTable.Builder.conditionState() renamed filterState().
- DefaultToolBarBuilder bug fixed, now cleans up separators like DefaultMenuBuilder, related refactoring.
- ColumnConditionPanel.caption() removed, related refactoring.
- ColumnConditionsPanel captions function added as constructor parameter.
- ColumnConditionPanel renamed ConditionPanel.
- ColumnConditionsPanel.panels() and panel() renamed get().
- ColumnConditionsPanel renamed TableConditionPanel.
- ConditionPanel.ConditionState renamed ConditionView.
- FilterColumnConditionPanel.Fields added.
- FilterTableColumnModel.Visible and Hidden added, related refactoring.
- FilterTableColumnModel.containsColumn() and resetColumns() renamed contains() and reset() respectively.
- FilterColumnConditionPanel bug fixed, did not link all components to locked state.
- TableConditionPanel bug fixed, incorrect baseBundleName in MessageBundle.
- FilterColumnConditionPanel renamed ColumnConditionPanel.
- ConditionPanel.conditionView() and TableConditionPanel.conditionView() renamed view().
- FilterTableCellRenderer.CellColors replaced with ColorProvider, Builder.foreground() and background() added, related refactoring.
- FilterTableCellRenderer.Builder, column identifier type parameter removed.
- FilterTableCellRenderer.Factory.create() parameter now column identifier instead of column.
- FilterTableCellRenderer.Builder, column value type parameter added.
- FilterTableCellEditor.Factory.create() parameter now column identifier instead of column.
- FilterTableCellRenderer.UISettings added, DefaultFilterTableCellRenderer refactored.
- FilterTable.Builder.cellEditor() and cellRenderer() added, related refactoring.
- FilterTableCellRenderer.Color.Provider.color() column parameter added.
- FilterTableCellRenderer.Builder row and column identifier type parameters added.
- FilterTable.Builder.cellEditor() and cellRenderer() now long Supplier based.
- FilterTableCellRenderer, renaming and refactoring.
- FilterTableCellRenderer.ColorProvider.color() row and column index parameters replaced with row item and column identifier.
- FilterTableCellRenderer column type parameter added, related refactoring.
### is.codion.framework.domain
- Entity.copy(), copyBuilder() and deepCopy() moved to Entity.Copy, renamed mutable(), builder() and deep() respectively, immutable() added.
- ImmutableEntity bug fixed, StackOverflowError when creating an immutable entity with cyclical foreign key dependencies, some refactoring.
- Entity.Copy.deep() removed.
- EntityDefinition.backgroundColorProvider() and foregroundColorProvider() removed.
- EntityDefinition.conditionProvider() renamed condition().
### is.codion.framework.db
- EntityConnection.rollbackTransaction() and commitTransaction() no longer ignore exceptions, now throw DatabaseException.
- EntityConnection.transaction() added along with Transactional and TransactionalResult.
- EntityConnection.Transactional and TransactionalResult.execute() now throw Exception instead of DatabaseException.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.valueMissingOrModified() bug fixed, could not discern a missing value from a null original value, since the incorrent entity was being used when checking, test case improved.
### is.codion.framework.model
- EntityConditions.AdditionalCondition moved to EntityQueryModel, additionalWhere() and additionalHaving() renamed where() and having() respectively.
- EntityConditions renamed EntityConditionModel.
- EntityColumnConditionFactory renamed AttributeConditionModelFactory.
- EntityTableModel.backgroundColor() and foregroundColor() removed.
### is.codion.swing.framework.model
- SwingEntityColumnConditionFactory renamed SwingAttributeConditionModelFactory
### is.codion.swing.framework.ui
- EntityApplicationPanel.Builder.ConnectionProviderFactory.createConnectionProvider() renamed create().
- EntityTablePanel.conditionPanel() renamed conditions().
- EntityTablePanel.Config.includeFilterPanel(), includeConditionPanel() and includeSummaryPanel() renamed includeFilters(), includeConditions() and includeSummary() respectively.
- EntityDependenciesPanel, dependencies dialog no longer modal.
- EntityTablePanel.Config.conditionState() and filterState() added.
- EntityDialogs.EntitySelectionDialogBuilder.configureTablePanel() added.
- EntityTableCellRendererBuilder removed.
- EntityTablePanel.Config.configureTable() renamed table().
- EntityTableCellRendererFactory replaced with EntityTableCellRenderer.
- EntityTable.Config.configureTable() renamed table().
- EntityTableCellRenderer.EntityColorProvider and Factory added.

## 0.18.17
### is.codion.common.core
- Value.Builder.initialValue() renamed value.
### is.codion.common.model
- Unused slf4j dependency removed.
- ConditionModel.identifier() removed, related refactoring.
- ConditionModel.AutomaticWildcard renamed Wildcard.
- ConditionModel.wildcard() now Mutable instead of Value.
### is.codion.swing.common.model
- DefaultFilterTableModel, null item rejection improved.
- DefaultFilterTableSelection, null item rejection improved.
- FilterTableModel.conditions() renamed filters().
- TableConditionModel.conditions() renamed get().
- TableConditionModel renamed TableConditions.
- ConditionModelFactory renamed ColumnConditionFactory.
- ColumnConditionFactory.createColumnCondition() renamed create().
- TableConditions renamed ColumnConditions.
### is.codion.swing.common.ui
- AbstractComponentBuilder.setInitialValue() removed, no longer necessary now that the component value is always created, which is now used to set the initial value.
- SpinnerNumberValue now non-null and validates the value according to the model minimum and maximum, if specified.
- FilterTableCellRenderer.backgroundColor() tableModel parameter replaced with conditionModel
- DefaultFilterTableCellRenderer condition model cache removed.
- ComponentBuilder.initialValue() renamed value().
- AbstractComponentBuilder bug fixed, now only sets the componentOrientation if one has been specified, since it can affect the horizontalAlignment of components.
- AbstractSpinnerBuilder hack added to preserve the horizontal editor alignment through UI changes.
- FileInputPanel.ByteArrayInputPanelValue.setComponentValue() no longer throws UnsupportedOperationException.
- FileInputPanel improved, related refactoring.
- FilterTableCellRenderer.Builder.condition() renamed filter().
- TableConditionPanel.conditionModel() renamed conditions().
- FilterTable.Builder.resizeRowToFitEditor() added along with RESIZE_ROW_TO_FIT_EDITOR configuration value.
- FilterColumnConditionPanel.FieldFactory, type parameter added to create methods.
- NumberField.getNumber(), setNumber() and value() replaced with Mutable number().
### is.codion.framework.model
- DefaultEntityConditionModel, no longer wraps a single condition in a condition combination.
- EntityConditionModel.AdditionalCondition added, related refactoring.
- EntityTableConditions renamed EntityConditions.
### is.codion.swing.framework.ui
- EntityTableCellEditorFactory.editModel() added.
- EntityTableCellRenderer removed.
- EntityTableCellEditorFactory.builder() now final, tableCellRenderer() no longer final.
- EntityTableCellRendererBuilder, query condition model cache removed.
- EntityEditComponentPanel.requestComponentFocus() now uses requestFocusInWindow() instead of requestFocus().

## 0.18.16
### is.codion.common.core
- AbstractValue.link/unlink(ValueObserver) bug fixed, did not support multiple links.
### is.codion.common.model
- SingleSelectionModel and SelectionModel renamed SingleSelection and Selection respectively.
- FilterModel.clear() moved to Items.
- FilterModel.Refresher.refresh() removed, refresh() now protected in AbstractRefresher.
- ColumnSummaryModel renamed SummaryModel.
- SingleSelection and Selection renamed SingleItemSelection and MultiItemSelection respectively.
- FilterModel.Items.Visible and Filtered moved to FilterModel and renamed VisibleItems and FilteredItems respectively.
- common.model.loadtest and randomizer packages moved to codion-loadtest-core module.
- Item no longer extends Comparable.
### is.codion.swing.common.model
- FilterComboBoxModel.includeNull() and nullItem() moved to NullItem.
- FilterComboBoxModel.replace() moved to FilterComboBoxModelItems.
- FilterComboBoxModel.validator() moved to FilterComboBoxModelItems.
- FilterComboBoxModel.cleared() moved to FilterComboBoxModelItems.
- FilterComboBoxModel.selectedItemTranslator() moved to FilterComboBoxSelectionModel and renamed translator().
- FilterComboBoxModel.FilterComboBoxModelItems renamed FilterComboBoxItems.
- FilterComboBoxModel.validSelectionPredicate() moved to FilterComboBoxSelectionModel and renamed validPredicate().
- FilterComboBoxModel.filterSelectedItem() moved to FilterComboBoxSelectionModel and renamed filterSelected().
- FilterTableSelectionModel moved to FilterTableModel and renamed TableSelection.
- FilterComboBoxModel.FilterComboBoxSelectionModel renamed ComboBoxSelection.
- FilterComboBoxModel.FilterComboBoxItems renamed ComboBoxItems.
- DefaultFilterComboBoxModel.Selected bug fixed, selection empty state now set before fireContentsChanged() is called.
### is.codion.swing.common.ui
- FilterTableSearchModel.searchPredicate() renamed predicate().
- FileInputPanel.Builder now Path based.
- FileInputPanel.BuilderFactory added, now Path or byte[] based.
- DefaultFilterTableCellEditor.isCellEditable() now returns true for events other than double click, in order to respect the default cell editor table action.
- AbstractSpinnerBuilder, editable spinner now overridden in order for the editor to receive the focus when used as a cell editor.
### is.codion.framework.model
- EntityComboBoxModel.linkForeignKeyFilterComboBoxModel() and linkForeignKeyConditionComboBoxModel() replaced with foreignKeyComboBoxModelLinker() returning a ForeignKeyComboBoxModelLinker.
- EntityTableModel.rowCount() removed.
- EntityEditModel.EditableEntity.changed() renamed valueChanged().
### is.codion.swing.framework.model
- SwingEntityTableModel bug fixed, constructor used refresh, which could be async, instead of adding items directly to the model.
### is.codion.swing.framework.ui
- EntityComponentFactory.caption() added, related refactoring.
- EntityComponentFactory, Attribute type parameter removed, DefaultEntityComponentFactory refactored accordingly.
- EntityComponents now supports byte[].

## 0.18.15
### is.codion.common.core
- StateObserver.isNull(), isNotNull() and nullable() now have default implementations.
### is.codion.common.model
- FilterModel.Items.visiblePredicate() moved to Items.Visible and renamed predicate().
- FilterModel, methods for adding and removing items now return true if the items are/were visible.
- FilterModel, addSorted() methods removed.
- FilterModel, index based add and remove methods moved to Items.Visible.
- FilterModel.comparator() and sort() moved to Items.Visible.
- FilterModel.Refresher.items() renamed supplier().
- FilterModel.refreshThen() and Refresher.refreshThen() renamed refresh().
- FilterModel.addItem(), addItems(), removeItem() and removeItems() moved to FilterModel.Items.
### is.codion.swing.common.ui
- FilterTableCellRenderer now caches the filter condition models to reduce memory churn during repaint.
### is.codion.framework.model
- ForeignKeyDetailModelLink, configuration values renamed for clarity, a couple more added along with tests.
- EntityEditModel.EditableEntity and EditableValue added, related refactoring.
- AbstractEntityEditModel.validate(Entity) now final.
- AbstractEntityEditModel bug fixed, inserting, updating and deleting modified entities no longer trigger modification warnings.
- EntityEditModel.EditableEntity.clear() overridden, now clears all values from the entity as opposed to setting defaults.
- EntityComboBoxModel.createForeignKeyFilterComboBoxModel() and createForeignKeyConditionComboBoxModel() replaced with foreignKeyComboBoxModel() returning a ForeignKeyComboBoxModelFactory.
### is.codion.swing.framework.ui
- EntityPanel.Config.description now initialized with entity description.
- EntityTableCellRenderer now caches the query condition models to reduce memory churn during repaint.
- EntityComboBox.createForeignKeyFilterComboBox() replaced with foreignKeyComboBox() returning a ForeignKeyComboBoxFactory.
### is.codion.tools.generator.domain
- DomainSource.apiSearchString() bug fixed, space added after interface name, since table names can have common prefixes.

## 0.18.14
### is.codion.common.core
- PropertyStore, property access now based on predicates instead of key prefixes.
- ValueObserver and ValueSupplier no longer extend Supplier.
- EventObserver renamed Observer and moved to observable package.
- observable package renamed observer.
- DefaultObserver.removeWeakListener() and removeWeakConsumer() bug fixed, could possibly return true even if the listener was not removed, due to a null weak reference being removed.
- AbstractItem collator now ThreadLocal based.
- Mutable split from Observable.
- MethodLogger.Entry.hasChildEntries() removed.
- Item no longer Serializable, only the implementations.
- Mutable.clear() added.
### is.codion.common.model
- FilterModel.items() now Observable based, related refactoring.
- FilterModel.Items added, related refactoring.
- FilterModel.containsItem(), visible() and filtered() moved to Items, containsItem() renamed contains().
- FilterModel.filterItems() moved to Items, renamed filter().
- FilterModel.Refresher.success() now includes the refresh result.
- FilterModel.includeCondition() renamed visiblePredicate().
- TableSelectionModel moved to FilterModel and split into SingleSelectionModel and SelectionModel.
- SingleSelectionModel.selectionNotEmpty() removed.
- FilterModel.Items.VisibleItems and FilteredItems added, Items.visible(), filtered(), indexOf() and itemAt() moved.
- FilterModel.visiblePredicate() moved to Items.
- FilterModel.VisibleItems and FilteredItems.size() added.
- FilterModel.SelectionModel.isSelected() removed, SelectedItems and SelectedIndexes.contains() added.
- FilterModel.SingleSelectionModel and SelectionModel moved to common.model.selection package.
- FilterModel.selectionModel() renamed selection().
- SelectionModel, most methods renamed.
- SelectionModel.clear() moved to SingleSelectionModel.
- FilterModel.sortItems() renamed sort().
- FilterModel.Items.count() added, Items.Visible and Filtered.size() renamed count().
- TableConditionModel and ColumnConditionModel moved to condition package.
- TableSummaryModel and ColumnSummaryModel moved to package summary.
- TableConditionModel.conditionModel() renamed condition().
- ColumnConditionModel.columnClass() renamed valueClass().
- ColumnConditionModel renamed ConditionModel.
- TableConditionModel and ConditionModel.conditionChanged() renamed changed().
- TableConditionModel.conditions() and enabled(identifier) removed, condition() renamed get(), optional() added.
### is.codion.swing.common.model
- DefaultFilterComboBoxModel refactored.
- FilterTableModel.dataChanged() removed.
- FilterTableModel, a few methods moved to FilterModel.
- FilterTableModel.indexOf() and itemAt() moved to FilterModel.Items.
- FilterTableModel, methods for adding and removing items moved to FilterModel.
- DefaultFilterComboBoxModel.Selected bug fixed, refactoring mistake.
- FilterModel.Items.VisibleItems and FilteredItems renamed Visible and Filtered respectively.
- FilterTableModel.filterModel() renamed conditionModel().
- FilterTableModel.conditionModel() renamed conditions().
### is.codion.swing.common.ui
- FilterTable, now propagates the associated MouseEvent as the source of the ActionEvent when doubleClickAction is performed.
- MenuBuilder no longer extends MenuItemBuilder.
- ComponentDialogBuilder.closeEvent() renamed closeObserver().
- ColumnConditionPanel.focusGainedEvent() renamed focusGainedObserver().
- DefaultMenuBuilder bug fixed, now trims and prevents multiple separators.
- Control.commandControl(), actionControl() and toggleControl() renamed command(), action() and toggle() respectively.
- FilterTable.filterPanel() renamed conditionPanel().
- ColumnConditionPanel.conditionModel() renamed condition().
### is.codion.framework.model
- EntityEditModel.valueEdited() renamed edited().
- EntityEditModel.entity() now Mutable based.
- EntityTableConditionModel.enabled() now StateObserver based.
- EntityQueryModel split from EntityTableModel.
- AbstractEntityEditModel.DefaultUpdate.UpdateResult.handle() bug fixed, no longer triggers EntityEditPanel.beforeEntity().
- EntityTableConditionModel renamed EntityConditionModel.
- EntityConditionModelFactory renamed EntityColumnConditionModelFactory.
- EntityQueryModel.query() added for overriding the default query mechanism.
- EntityTableModel.selectionChanged() removed.
- EntitySearchModel.Selection added.
- AbstractEntityTableModel.find() bug fixed, now searches through all items, not just visible.
- AbstractEntityTableModel.indexOf() now loop based.
- EntityTableModel.find() and indexOf() removed.
- AbstractEntityTableModel.tableModel() renamed filterModel().
- EntityEditEvents.addListener() methods replaced with observer() methods.
- EntityConditionModel.attributeCondition() renamed attribute() added.
- EntityQueryModel.conditionModel() renamed conditions().
- EntityColumnConditionModelFactory renamed back to EntityConditionModelFactory.
### is.codion.swing.framework.model
- EntityComboBoxModel.setForeignKeyFilterKeys() renamed filterByForeignKey(), getForeignKeyFilterKeys() renamed foreignKeyFilterKeys().
- AbstractEntityTableModel extracted from SwingEntityTableModel.
- SwingEntityTableModel.tableModel() factory method replaced with constructor overloads, related refactoring.
- SwingEntityConditionModelFactory renamed SwingEntityColumnConditionModelFactory.
- SwingEntityColumnConditionModelFactory renamed back to SwingEntityConditionModelFactory
### is.codion.swing.framework.ui
- EntityTablePanel bug fixed, Config.SHOW_REFRESH_PROGRESS_BAR now used for default config value.
- EntityDialogs bug fixed, now respects the location if specified.
- EntityTablePanel, edit panel dialog location now follows mouse when displayed on double click.
- EntityApplicationPanel.exitEvent() renamed exitObserver().
- EntityDependenciesPanel no longer public, needs a better api.
### is.codion.framework.domain
- DefaultKey.isNull(column) now throws exception in case the column is not part of the key.
### is.codion.framework.server
- AbstractRemoteEntityConnection.closedEvent() renamed closedObserver().
### is.codion.tools.monitor.model
- ConnectionPoolMonitor.statisticsEvent() renamed statisticsObserver().
- ConnectionPoolMonitor.statisticsObserver() renamed statisticsUpdated().
### is.codion.tools.loadtest.ui
- ItemRandomizerPanel.selectedItems() now Observable based.

## 0.18.13
### is.codion
- Event suffix removed from methods returning EventObserver, related renaming.
### is.codion.common.core
- Observable added.
- Item.get() renamed value(), no longer extends Supplier.
- Observable no longer extends Supplier.
### is.codion.common.rmi
- DefaultRemoteClient.withDatabaseUser() bug fixed, did not copy client host and creationTime. 
### is.codion.swing.common.model
- TableSelectionModel, selectedIndex, selectedIndexes, selectedItem and selectedItems now Observable based.
- TableSelectionModel.SelectedIndexes added.
- TableSelectionModel.SelectedItems added.
- TableSelectionModel.moveSelectionDown() and moveSelectionUp() moved to SelectedIndexes and renamed moveDown() and moveUp() respectively.
- NullableToggleButtonModel.toggleState() now Observable based.
- TableSelectionModel.SelectedIndexes.set(Collection<Integer>) added.
- TableSelectionModel.SelectedItems.set(Collection<R>) added.
### is.codion.swing.common.model
- FilterComboBoxModel.selectionChanged() removed, Observable based selectedItem() added.
### is.codion.swing.common.ui
- ComponentBuilder memory leaks prevented by replacing lambdas and anonymous inner classes with static classes.
- NullableCheckBox.getNullableModel() renamed model(), getState() removed.
### is.codion.framework.domain
- Domain.configureConnection() and configureDatabase() renamed configure().
### is.codion.framework.model
- EntityEditModel.valueEvent(attribute) removed.
- EntityEditModel.put(), get(), remove() and optional() removed.
- EntityEditModel.insertUpdateOrDelete() renamed afterInsertUpdateOrDelete().
### is.codion.swing.framework.model
- SwingEntityEditModel.createComboBoxModel(), nullItem caption now static.
### is.codion.swing.framework.ui
- EntityPanel.Config.entityPanel(), EntityTablePanel.Config.entityTablePanel() type parameter added.
### is.codion.tools.loadtest.ui
- ItemRandomizerPanel refactored.

## 0.18.12
### is.codion.swing.common.ui
- AbstractComponentBuilder bug fixed, now sets the initial component value only if an initialValue has been set, otherwise model based components (List, ComboBox etc.) clear the initial model state.
- ComponentBuilder.clear() removed, AbstractComponentBuilder now always creates the associated ComponentValue, related refactoring.
- AbstractComponentBuilder now adds the associated ComponentValue as a client property on the resulting component.
- FilterTableCellRenderer.Settings.selectionBackgroundColor() and backgroundShaded() bug fixed alternate row coloring now correct.
- FilterTableCellRenderer.Settings.alternateRowColor() renamed alternateRow(), now static.
### is.codion.swing.framework.ui
- EntityPanel.createControlPanel() toggle button type now the default BUTTON.
- EntityPanel, overridable controlComponent() replaced with Config.controlComponent().
- EntityPanel, overridable editBasePanel() replaced with Config.editBasePanel().
- EntityTableCellRendererBuilder.EntitySettings.backgroundColorShaded() bug fixed, alternate row coloring now correct.

## 0.18.11
### is.codion.common.model
- ColumnConditionModel, bound value getters and setters removed.
- ColumnConditionModel.operand() added.
- DefaultColumnConditionModel refactored.
- ColumnConditionModel.Operand renamed Operands.
### is.codion.framework.domain.db
- Column.AuditColumnDefiner added, related refactoring.
### is.codion.swing.common.model
- DefaultFilterComboBoxModel.filterSelectedItem() now false by default, less confusing.
- FilterComboBoxModel.ItemFinder.findItem() now returns Optional.
### is.codion.swing.common.ui
- CalendarPanel.Builder.locale() added.
- CalendarPanel now takes the first day of week according to locale into account.
- CalendarPanel now uses labels instead of buttons to represent days.
- CalendarPanel, ALT modifier removed from day/week keyboard shortcut.
- CalendarPanel bug fixed, day and month labels used default locale instead of the selected one.
- CalendarPanel.Builder.firstDayOfWeek() added, related refactoring.
- DefaultProgressBarBuilder bug fixed, no longer uses a null model for indeterminate progress bars, Look & Feels may require it.
- FilterTableCellRenderer.Settings.backgroundColor() bug fixed, no longer returns alternate row color when alternate row coloring is disabled.
- FilterTableCellRenderer bug fixed, now respects Table.alternateRowColor if set, even if alternate row coloring is disabled.
### is.codion.framework.model
- EntityTableConditionModel.setEqualConditionValue() and setInConditionValues() renamed setEqualOperand() and setInOperands() respectively.
### is.codion.swing.framework.ui
- EntityTablePanel.TablePanel.createColumnSummaryPanels() bug fixed, no longer assumes FilterTableCellRenderer when determining the horizontal alignment.
- EntityTablePanel.setConditionStateHidden(), focus flicker when hiding focused condition/filter panel fixed.

## 0.18.10
### is.codion.common.core
- DatabaseException.statement() and sqlState() now return Optional.
### is.codion.common.model
- TableSelectionModel.selectedItems() and selectedIndexes() added for consistency.
### is.codion.swing.common.ui
- FilterTable.filterPanel() now returns TableConditionPanel.
- FilterTable.Builder.filterPanelFactory added.
- FilterColumnConditionPanel.Builder added, now configures the horizontal alignment of components according to the associated table column.
- InputDialog.show() with closeDialog Predicate parameter added.
- CalendarPanel bug fixed, no more focus flicker when day panel is reset when navigating between months by week, focus now follows day when toggled.
- SelectionDialogBuilder.dialogSize() added.
### is.codion.framework.domain.db
- Module is.codion.framework.domain.db split from is.codion.tools.generator.domain.
- DatabaseDomain renamed SchemaDomain.
- SchemaDomain.SchemaSettings added, audit column names can now be specified.
- SchemaDomain.SchemaSettings.primaryKeyColumnSuffix() added for improved foreign key names.
### is.codion.framework.server
- LocalConnectionHandler now logs exceptions occurring when returning connection to pool.
### is.codion.tools.generator.domain
- DomainSource, domain implementation now in base source package with the API in a separate api package.
### is.codion.tools.generator.model
- DomainGeneratorModel no longer keeps a live Connection instance, now creates short lived connections on demand, related refactoring.
### is.codion.swing.framework.ui
- EntityDialogs.DefaultEditAttributeDialogBuilder, bug fixed, retrying a failed update now working again.
- EntityDialogs.EditAttributeDialogBuilder.Updater removed.
- EntityDialogs.SelectionDialogBuilder.preferredSize() replaced with dialogSize().
- EntityDialogs.SelectionDialogBuilder renamed EntitySelectionDialogBuilder, implementation refactored, now uses Dialogs.actionDialog().
### is.codion.swing.framework.ui.test
- EntityEditPanelTestUnit, createEditPanel() removed, edit panel initializer function parameter added.
- EntityApplicationPanelTestUnit now initializes all entity panels.
- EntityApplicationPanelTestUnit, domainType constructor parameter added, related refactoring.

## 0.18.9
### is.codion
- columnIdentifier renamed identifier where applicable.
### is.codion.common.core
- Memory utility class removed.
- Separators utility class removed.
- State.Builder now extends Value.Builder.
### is.codion.common.rmi
- ServerConfiguration.Builder.connectionMaintenanceIntervalMs() renamed to connectionMaintenanceInterval().
- ServerAdmin, ServerStatistics.allocatedMemory() renamed totalMemory().
- RemoteClient.Builder added, replacing static factory methods.
### is.codion.swing.common.model
- DefaultFilterTableSelectionModel, inheritance replaced with composition.
### is.codion.swing.common.ui
- MemoryUsageField removed.
- TableConditionPanel.Factory.create(), onPanelInitialized parameter added.
- FilterTableConditionPanel.initializedEvent() removed, onPanelInitialized parameter added to factory method.
- TemporalField, TemporalFieldPanel, dateTimePattern parameter moved from factory method to builder, related refactoring.
### is.codion.swing.framework.model
- SwingEntityTableModel, bug fixed, three methods that should have been final are now final.
- SwingEntityTableModel, color cache now static.
### is.codion.swing.framework.ui
- EntityEditComponentPanel.component() now protected, attributes() and component(attribute) removed, unused.
- EntityEditComponentPanel.initialFocusComponent(), initialFocusAttribute(), afterInsertFocusComponent(), afterInsertFocusAttribute() and selectableComponents() now protected.
- EntityEditPanel.control() and controls() now protected.

## 0.18.8
### is.codion.common.core
- Value.BuilderFactory added, nullable() and nonNull() methods replaced with builder() returning a BuilderFactory.
- AbstractValue.get() now implemented and final, protected getValue() added, clearValue() removed.
- ValueObserver.isNullable() renamed back to nullable(), no factory method conflict any more.
- Text.WILDCARD_CHARACTER removed, wildcards no longer configurable.
- Value.value() factory method overloaded with initialValue.
- PropertyStore.propertyStore() and writeToFile() parameters changed from File to Path.
### is.codion.common.rmi
- ServerConfiguration.connectionLimit() added.
### is.codion.swing.common.ui
- FilterColumnConditionPanel, minor focus related refactoring.
- TextComponents.selectAllOnFocusGained() and selectNoneOnFocusGained() removed.
### is.codion.framework.server
- EntityServerConfiguration.connectionLimit() moved up to ServerConfiguration.
- EntityServerConfiguration.LOG removed, related refactoring.
### is.codion.swing.framework.model
- SwingForeignKeyConditionModel bug fixed, IN operator only selected by default if entity is searchable.
### is.codion.swing.framework.ui
- EntityTablePanel, EDIT control used as double click action if available.
- TabbedDetailLayout bug fixed, detail panel no longer loses focus when embedded, focus flicker reduced by clearing the focus.
- EntityDialogs.DefaultEditEntityDialogBuilder bug fixed, now reverts any edits on cancel.
### is.codion.plugin.jasperreports
- Dependencies fixed, org.apache.xmlgraphics no longer excluded, automatic module info added.

## 0.18.7
### is.codion.common.core
- DatabaseConnection.getConnection() now throws IllegalStateException if the connection has been closed, valid() added.
- State.Builder.validator() type parameter widened, now consistent with Value.addValidator().
### is.codion.common.model
- ColumnConditionModel.Builder.operator() added for specifying the default condition operator.
### is.codion.swing.common.ui
- SearchHighlighter.Builder added, scrollYRatio and scrollXRatio added.
- TextComponentBuilder.initialCaretPosition() added along with InitialCaretPosition enum.
- TextComponentBuilder.caretUpdatePolicy() added.
- SearchHighlighter now updates its color scheme dynamically when the look and feel is changed.
### is.codion.framework.domain
- DomainModel.add(EntityDefinition.Builder... builders) removed.
- DomainModel, DefaultEntities.setStrictForeignKeys() renamed validateForeignKeys().
- AttributeDefinition.prepareValue() removed, functionality moved to DefaultEntity, decimal fraction digits coming from database no longer modified.
- AbstractAttributeDefinitionBuilder.attribute no longer protected, public accessor available.
- DerivedAttributeDefinition caching can now be disabled, cached() added, default true, cached values can be removed via remove() and are included when entities are serialized.
- AttributeDefinition.denormalized() removed, no longer used.
- KeyGenerator.Identity added.
- DefaultColumnDefinition.BigDecimalFetcher now strips trailing zeros from values coming from the database.
### is.codion.framework.model
- ForeignKeyConditionModel now defaults to Operator.IN when available.
- DefaultForeignKeyDetailModelLink.setForeignKeyCondition() now always uses Operator.IN.
- EntityTableModel.getRowCount() renamed rowCount().
### is.codion.swing.framework.ui
- EntityTablePanel, bug fixed, three methods that should have been final are now final.
- SwingForeignKeyConditionModel now defaults to Operator.IN when available.
### is.codion.tools.generator.domain
- DomainSource no longer adds KeyGenerator.identity() import unless it is actually used.
### is.codion.plugin.jasperreports
- JasperReports automatic module names updated, hopefully the ones they end up using.

## 0.18.6
### is.codion
- Dependency cleanup using dependency-analysis-gradle-plugin.
- BuildReportsPlugin replaced with io.github.f-cramer.jasperreports.
### is.codion.common.core
- ProxyBuilder.DefaultHandler.invoke() bug fixed, now unwraps the InvocationTargetException and rethrows the target exception.
- ValueLink, automatically reverting a value change on exception functionality removed, far from bulletproof especially when dealing with component values.
### is.codion.swing.common.ui
- FilterTable.createAutoResizeModeControl() renamed createSelectAutoResizeModeControl()
- Controls.Builder.controls() parameter now Collection<? extends Control>.
- FilterTable.createSelectAutoResizeModeControl() bug fixed, was only enabled when column model was unlocked.
- FilterTable.createToggleAutoResizeModelControls() added.
### is.codion.framework.domain
- Entity.mapToPrimaryKey() renamed primaryKeyMap().
- Entity.entitiesByValue() removed.
- DefaultEntity bug fixed, no longer caches values of derived attributes without source attributes.
### is.codion.framework.domain.test
- EntityFactory.populateForeignKeys() removed, related refactoring.
- DomainTest now prevents transactions from being closed on the active connection during a test.
### is.codion.framework.db.core
- DefaultLocalEntityConnection.createValueString() bug fixed, now catches Exception instead of just SQLException, since creating a log message should never fail.
### is.codion.plugin.jasperreports
- JasperReports upgraded to 7.0.0.
### is.codion.swing.framework.ui
- EntityTablePanel.ControlKeys.COLUMN_AUTO_RESIZE_MODE renamed SELECT_AUTO_RESIZE_MODE.
- EntityTablePanel.ControlKeys.TOGGLE_AUTO_RESIZE_MODE_CONTROLS added along with AutoResizeModeSelection and Config.autoResizeModeSelection().
- EntityTablePanel.setupStandardControls() bug fixed, no longer overwrites previously configured controls.
### is.codion.tools.loadtest.ui
- LoadTestPanel now displays an indeterminate progress bar while shutting down.

## 0.18.5
### is.codion.common.core
- DefaultProxyBuilder.MethodKey now caches the hashCode.
- ProxyBuilder.ProxyMethod.invoke() now throws Throwable.
### is.codion.common.db
- AbstractConnectionPoolWrapper now uses ProxyBuilder.
### is.codion.framework.domain.test
- EntityTestUnit renamed DomainTest.
- DomainTest.EntityFactory refactored.
- DomainTest.EntityFactory.foreignKey() and createValue() renamed entity() and value().
### is.codion.plugin.imagepanel
- NavigableImagePanel bug fixed, now creates the navigation image when it is enabled for the first time.
### is.codion.tools.generator.model
- Module is.codion.tools.generator.domain split from model.
### is.codion.tools.monitor.ui
- HostMonitorPanel, refresh toolbar removed.
### is.codion.swing.common.ui
- DefaultFilterTableSearchModel bug fixed, no longer throws ArrayIndexOutOfBoundsException when searching through a table with one or more hidden columns.
- FilterTable.Builder.cellEditorFactory() added along with FilterTableCellEditor, FilterTableCellEditorFactory and DefaultFilterTableCellEditor.
### is.codion.swing.framework.ui
- EntityTablePanel bug fixed, refresh progress bar did not updateUI() when switching look and feel.
- EntityApplicationPanel.exit() now catches Throwable when trying to save preferences, logged as error, otherwise it can prevent application exit.
- EntityTablePanel.Config.cellEditorComponentFactories removed, functionality moved to FilterTable.
- EntityTableCellEditorFactory added.

## 0.18.4
### is.codion
- Load test, generator and server monitor modules moved to tools directory.
- Load test, generator and server monitor modules renamed.
- Load test, generator and server monitor module packages renamed to reflect module names.
### is.codion.swing.common.tools.model
- Module renamed to is.codion.swing.common.loadtest.model.
### is.codion.swing.common.tools.ui
- Module renamed to is.codion.swing.common.loadtest.ui.
### is.codion.framework.domain.entity.test
- EntityTestUtil replaced with EntityTestUnit.EntityFactory, related refactoring.
### is.codion.swing.framework.model.tools
- EntityLoadTestUtil removed.
- Module renamed to is.codion.swing.framework.generator.model.
### is.codion.swing.framework.ui.tools
- Module renamed to is.codion.swing.framework.generator.ui.
### is.codion.swing.framework.ui
- DefaultEntityApplicationPanelBuilder bug fixed, no longer tries to create a default login user when no default username is available in preferences.

## 0.18.3
### is.codion.common.db
- ConnectionPoolWrapper.poolDataSource() removed, getPool() renamed connectionPool(), related refactoring.
- AbstractConnectionPoolWrapper.connectionPool now final.
### is.codion.common.rmi
- RemoteClient.clientHost() no longer nullable, RemoteClient.UNKNOWN_HOST added.
- ServerConfiguration.rmiClientSocketFactory(), rmiServerSocketFactory() and objectInputFilterFactoryClassName() now return Optional.
### is.codion.common.model
- DefaultLoadTest.DefaultBuilder.user() null check added.
- ColumnConditionModel.format() now returns Optional.
### is.codion.swing.common.ui
- Controls.Config.standard() renamed control().
- NumberField.numberValue() renamed value().
- KeyEvents.Builder.enable() and disable() overloaded with Collection parameter.
- ControlKey.defaultKeyStroke() now Value based and configurable.
- Controls.Config.create(), ControlMap parameter added, related refactoring.
- Controls.Config renamed Layout, standard controls layouts can now be configured, related refactoring.
- ControlKeyStrokes removed, instance methods moved to ControlMap, static helper methods moved to KeyEvents, related refactoring.
- Controls.Layout no longer extends ControlsKey.
- ControlKey.name() added.
- Controls.Builder.controls(Collection<Control> controls) added, related cleanup.
- FilterTableColumn.toolTipText() now returns Optional.
- DefaultControlMap.control() and controls(), control key not found error message now contains the control key name, test improved.
### is.codion.swing.framework.ui
- EntityTablePanel bug fixed, summary panel toggle control displayed only if a summary panel is available.
- EntityApplicationPanel.createMainMenuControls() and related methods now Optional based.
- EntityApplicationPanel, control for opening log folder added to help menu, related refactoring.
- EntityPanel, EntityEditPanel, EntityTablePanel.Config.keyStroke() now consumer based providing access to the current keyStroke.
### is.codion.swing.framework.server.monitor
- ServerMonitorPanel, ClientUserMonitorPanel, shutdown and disconnect confirmation changed from OK/Cancel to Yes/No.

## 0.18.2
### is.codion.swing.common.model
- DefaultFilterTableModel now validates column identifier uniqueness.
### is.codion.swing.common.ui
- Control.control() renamed commandControl().
- Control.BuilderFactory.actionCommand() renamed action().
- FilterTable now validates column identifier uniqueness.
- FilterTableColumn.setIdentifier() now throws UnsupportedOperationException instead of IllegalStateException.
- ControlSet renamed ControlMap, ControlId renamed ControlKey.
- NumberDocument, NumberField.setRethrowValidationException() replaced with setSilentValidation().
### is.codion.framework.domain
- DefaultColumnDefinition.groupBy, aggregate, selectable and lazy no longer transient.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.createValueString() bug fixed, did not respect Column.Converter.handlesNull().
### is.codion.framework.model
- EntityConditionModelFactory, now creates condition models for all attributes, whether selectable or not.
### is.codion.swing.framework.ui
- EntityPanel.Config.editPanelConstraints() and EDIT_PANEL_CONSTRAINTS configuration value added.
- EntityPanel.Navigate bug fixed, navigating down always activated the first detail panel, instead of the currently linked one.

## 0.18.1
### is.codion.swing.common.ui
- TableConditionPanel.initializedEvent() added.
- FilterTable bug fixed, now configures the filter condition panels correctly.
- FilterTableCellRenderer, minor renaming of configuration value keys.
- FilterTableCellRendererFactory.tableCellRenderer() now returns FilterTableCellRenderer instead of TableCellRenderer.
- FilterColumnConditionPanel bug fixed, GREATER_THAN_OR_EQUAL was missing from LOWER_BOUND_OPERATORS.
- FilterTable, mouse click modifier for adding a sort column changed from Ctrl to Alt.
- Control now immutable, related refactoring.
- Controls now immutable, related refactoring.
- Control.BuilderFactory added, related refactoring.
- FilterTableColumn.identifier() added.
- FilterTable.model() added.
- Control related renaming and refactoring.
- ControlId, ControlShortcuts and ControlSet added, KeyboardShortcuts removed, related refactoring.
- KeyEvents, special handling added for JSpinner.
- AbstractSpinnerBuilder, now makes the spinner non-focusable only if editable, editor no longer focusable if not editable, no longer overrides enableTransferFocusOnEnter().
- AbstractControl.Enabler now acts on the EDT.
- AbstractComponentBuilder now always sets the initial value even if it is null, related refactoring.
- ControlShortcuts renamed ControlKeyStrokes.
### is.codion.swing.framework.model
- SwingEntityTableModel.EntityTableColumns, problematic override of comparable() removed.
### is.codion.swing.framework.model.tools
- DomainGeneratorModel bug fixed, sourceDirectorySpecified and domainPackageSpecified states now initialized properly.
- DomainGeneratorModel bug fixed, save now enabled only when a populated schema is selected.
### is.codion.swing.framework.ui
- EntityConditionFieldFactory.createInForeignKeyField() bug fixed, did not handle SwingForeignKeyConditionModel.
- DefaultEntityApplicationPanelBuilder bug fixed, did not close initialization frame on exception.
- EntityTablePanel no longer initializes the table condition panel lazily, now that FilterColumnConditionPanel is initialized lazily.
- EntityApplicationPanel.preferences removed.
- EntityTablePanel.conditionPanelStateChanged() bug fixed, did not initialize the condition scroll pane.
- EntityTablePanel.configureColumnConditionComponent() bug fixed, did not handle combo boxes.
- EntityTablePanel, minor optimization, now uses a single ScrollToColumn instance.
- EntityEditPanel bug fixed, modified warning no longer triggered when the active entity is replaced with an updated version of itself.
- EntityApplicationPanel.createHelpMenuControls() bug fixed, trailing separator removed.
- EntityDialogs.AddEntityDialogBuilder, EditEntityDialogBuilder.confirm() added, default false.
- EntityComboBox, EntityComboBoxPanel.Builder, EntitySearchField, EntitySearchFieldPanel.Builder.confirmAdd() and confirmEdit() added.
- EntityEditComponentPanel.onException() bug fixed, no longer displays cancel exceptions.
- EntityPanel, empty border added around edit panel when displayed in a window.

## 0.18.0
### is.codion
- ServiceConfigurationError caught, cause unwrapped and rethrown.
### is codion.common.core
- Text.SpaceAwareCollator bug fixed, now treats null values as an empty string.
- Nulls utility class added.
- MessageBundle added, Messages service provides a way to override default localization messages.
- is.codion.common.resources renamed resource.
- Value.mapNull() removed, no longer used.
- Messages renamed Resources. DefaultMessageBundle now throws correct exception in case of a missing resource.
- Resources.get() renamed getString(), consistent with ResourceBundle.
- Operator.IN and NOT_IN added.
- Operator, reordered to group single value operators.
- State.Group no longer uses weak references for member states.
- State.Group now enables the previously enabled state when the currently enabled state is disabled, instead of leaving all states disabled.
- State.Group bug fixed, now enables the next state when the currently enabled state is disabled and a previously active state is not available.
- Value.addValidator(), removeValidator(), validator type now ? super T.
### is codion.common.db
- Database.databaseException() renamed exception().
- Database.createConnectionPool() now returns the pool.
- ResultIterator now implementes Iterable.
- Database.COUNT_QUERIES configuration value added.
### is codion.common.model
- ColumnSummaryModel.SummaryValues removed, related refactoring.
- ColumnSummaryModel.SummaryValueProvider renamed SummaryValues, related renaming.
- FilteredModel.Refresher.itemSupplier() renamed items(), related renaming.
- FilteredModel renamed FilterModel.
- ColumnConditionModel.equalValues() replaced with equalValue() and inValues().
- ColumnConditionModel.Factory.supports() renamed includes().
- ColumnConditionModel.Factory.includes() removed, createConditionModel() now returns Optional.
### is.codion.swing.common.model
- FilteredComboBoxModel.ItemFinder.createPredicate() renamed predicate().
- FilteredComboBoxModel.sortVisibleItems() renamed sortItems(), now public.
- FilteredComboBoxModel.sortVisibleItems() bug fixed, now takes the null item into account.
- Controls.Builder.defaults(stopAt) now includes the specified stopAt control instead of stopping before it.
- DialogBuilder, FrameBuilder.titleProvider() renamed title().
- LookAndFeelProvider.addLookAndFeelProvider() renamed addLookAndFeel().
- FilteredTableCellRenderer.Builder.columnShadingEnabled() renamed columnShading().
- FilteredTableModel.Builder.itemValidator() renamed validator(), related renaming.
- Major surgery, FilteredTableModel.sortModel(), searchModel(), summaryModel() and columnModel() moved to FilteredTable, related refactoring.
- FilteredTableModel renamed FilterTableModel, related renaming.
- FilteredComboBoxModel renamed FilterComboBoxModel.
- FilteredTableModel.ColumnValueProvider renamed ColumnValues, related renaming.
- FilterComboBoxModel now interface, DefaultFilterComboBoxModel added.
### is.codion.swing.common.ui
- ProgressDialog.Builder, ProgressWorkerDialogBuilder.controls() replaced with control().
- ProgressDialog bug fixed, no longer adds an empty south panel when no controls are specified.
- KeyboardShortcut.Shortcut.keyStroke() now returns Optional.
- Control keyboard shortcut handling refactored.
- AbstractComponentBuilder bug fixed, now able to set size values to zero, validates size values.
- FilteredTable.MoveResizeColumnKeyListener bug fixed, now longer triggers when modifiers are added, restricted to the correct keys.
- FilteredTable, move and resize column controls added, default key stroke now configurable.
- FilteredTable, key strokes now configurable.
- FilteredTableCellRenderer.CollColorProvider renamed CellColors.
- FilteredTableCellRenderer.Builder.displayValueProvider() renamed values().
- LookAndFeelProvider.CROSS_PLATFORM and SYSTEM configuration values added.
- Control.copy() added, copying the control with the underlying command.
- EntityPanel.Config.editPanelState() and editPanelStates() renamed initialEditState() and enabledEditStates() respectively.
- TabbedDetailLayout.Builder.panelState() and panelStates() renamed initialDetailState() and enabledDetailStates() respectively.
- FilteredTable renamed FilterTable, related renaming.
- SearchHighlighter.nextSearchPosition() and previousSearchPosition() now public.
- SearchHighlighter default colors now based on the selection color of the component.
- ComponentBuilder.onBuildValue() added, related refactoring.
- LookAndFeelProvider.defaultLookAndFeelName() overloaded with a defaultLookAndFeel parameter.
- FilterTable.copyToClipBoard() bug fixed, always exported the whole table instead of the selected rows.
- ColumnConditionPanel now supports the IN operator.
- ListBoxBuilder added.
- ListBoxBuilder.string() added, providing the string values to render in the list.
- FilterTableCellRenderer.Builder.values() renamed string().
- AbstractColumnConditionPanel and AbstractFilterTableConditionPanel added, related refactoring.
- AbstractColumnConditionPanel, AbstractFilterTableConditionPanel.focusGainedEvent() removed.
- AbstractColumnConditionPanel and AbstractFilterTableConditionPanel replaced with interfaces FilterTable.ColumnConditionPanel and FilterTable.TableConditionPanel, related refactoring.
- TableConditionPanel.conditionPanel() now returns Optional.
- FilterTableConditionPanel.conditionPanels() bug fixed, now actually returns the panels.
- PersistMenuCheckBoxMenuItem and PersistMenuRadioButtonMenuItem bug fixed, responded to mouse events when disabled.
- ColumnConditionPanel.ConditionState and state() added, FilterTableConditionPanel.state() added, related refactoring.
- FilterTable.Builder.filterState() added.
- FilterColumnConditionPanel.columnConditionPanel() renamed filterColumnConditionPanel().
- TableConditionPanel.Factory added.
- FilterTableConditionPanel bug fixed, now overrides updateUI() in order to update hidden components.
- NumberDocument, NumberField.setRethrowValidationException() added, can now be turned off for the field to prevent invalid input without displaying the exception.
- TableConditionPanel.conditionPanel() no longer returns Optional.
- TableConditionPanel.selectCondition() added.
- TableConditionPanel and ColumnConditionPanel now abstract classes.
- ColumnConditionPanel.caption() added.
- TableConditionPanel.selectCondition() renamed selectConditionPanel() and implemented, related refactoring.
- ColumnConditionPanel.state() no longer abstract.
- ComponentBuilder.listener() and consumer() added.
- FilterTable bug fixed, sorting, column reordering and resizing key events were enabled even though the functionality had been disabled.
- TableConditionPanel.focusEvent() added.
- FilterColumnConditionPanel now initialized lazily.
- FilterTableConditionPanel.focusGainedEvent() moved to ColumnConditionPanel, related refactoring.
- TableConditionPanel.state() now final, controls() implemented, related refactoring.
- DefaultFilterTableColumnModel.setVisibleColumns() bug fixed, now resets the selected column to zero after the columns have been configured.
- LookAndFeelSelectionDialogBuilder, FontSizeSelectionDialogBuilder, selection method consumer parameter added, no longer return the selected value.
### is.codion.framework.domain
- DefaultDomain renamed DomainModel.
- Entity.valuesEqual() instance methods renamed equalValues(), static ones removed.
- Entity.Builder.withDefaultValues() renamed withDefaults().
- DomainModel.addAll() renamed add().
- AbstractQueriedKeyGenerator, IdentityKeyGenerator now remove any previous value from the entity before setting the key value, removing original value related memory churn.
- Entity.valuesAsString() removed.
- Entity.mapToValue(), mapToType() and mapKeysToType() renamed groupByValue(), groupByType() and groupKeysByType() respectively.
- DefaultEntity.createPrimaryKey() now creates a pseudo primary key, containing all column values, for entities without a defined primary key.
### is.codion.framework.json.domain
- EntityObjectMapper.serializeEntities() and serializeKeys() removed.
- ColumnConditionSerializer bug fixed, did not deserialize case insensitive IN conditions correctly.
- EntityDeserializer, EntityKeyDeserializer, minor backwards incompatible changes reverted.
### is.codion.framework.db.local
- DefaultLocalEntityConnection.report() exception handling improved, now throws DatabaseException in case of an SQLException.
- DefaultLocalEntityConnection exception handling refactored in order to guarantee rollbacks in case of RuntimeExceptions.
### is.codion.framework.model
- EntityEditModel.editEvents() renamed postEditEvents, EntityTableModel.editEvents() renamed handleEditEvents().
- EntityEditEvents.notifyInserted(), notifyUpdated() and notifyDelete() renamed inserted(), updated() and deleted() respectively.
- EntityEditEvents, consumers now called listeners.
- EntityTableModel.HANDLE_EDIT_EVENTS configuration value added.
- EntityModel.activeDetailModels() renamed linkedDetailModels().
- EntitySearchConditionModel replaced with ForeignKeyConditionModel.
- EntityTableConditionModel type parameter removed, connectionProvider() added.
- DefaultEntityConditionModel.createConditionModels() bug fixed, did not respect Factory.include().
- EntityConditionModelFactory no longer excludes hidden attributes.
- AbstractForeignKeyConditionModel column identifier type now Attribute<Entity> instead of ForeignKey.
- ForeignKeyConditionModel.builder() added, replacing static factory method, available operators now configurable.
- ForeignKeyConditionModel.condition() added, a shorthand for adding a condition to both the IN and EQUAL search/combobox models.
- EntityTableConditionModel.setEqualConditionValue() added.
- DefaultForeignKeyDetailModelLink now uses the EQUAL operator in case of a single value when setting the foreign key condition.
- ForeignKeyConditionModel renamed DefaultForeignKeyConditionModel.
- AbstractForeignKeyConditionModel renamed ForeignKeyConditionModel.
- EntityApplicationModel.warnAboutUnsavedData() and containsUnsavedData() removed.
- EntityEditModel.WARN_ABOUT_UNSAVED_DATA removed, beforeEntityEvent() added.
- EntityComboBoxModel, inheritance replaced with composition.
- DefaultForeignKeyConditionModel renamed ForeignKeyConditionModel, abstract base class removed, inheritance replaced with composition. 
### is.codion.framework.servlet
- EntityService refactored.
### is.codion.swing.framework.model
- SwingEntityTableModel.UpdateListener now replaces updated entities.
- SwingEntityTableModel.replaceEntitiesByKey() now uses a key index map when looking up entities to replace, orders of magnitude faster.
- EntityComboBoxModel.HANDLE_EDIT_EVENTS configuration value added.
- EntityComboBoxConditionModel replaced with SwingForeignKeyConditionModel.
- SwingEntityTableModel EntityConditionModelFactory parameter replaced with EntityTableConditionModel.
- SwingForeignKeyConditionModel.builder() added, replacing static factory method, available operators now configurable.
- SwingEntityTableModel bug fixed, now uses a SwingEntityConditionModelFactory by default.
- EntityComboBoxModel now final.
- SwingForeignKeyConditionModel, inheritance replaced with composition.
### is.codion.swing.framework.model.tools
- DomainGenerator now uses com.squareup.javapoet to generate the source and generates a full domain implementation class.
- DomainToString now handles views correctly, refactoring.
- DomainToString now splits source up into api and implementation.
- DomainToString now highlights the selected entity definition in the source.
- DomainGeneratorModel.DEFAULT_DOMAIN_PACKAGE configuration value added.
- DomainGeneratorModel now orders class elements according to the entity dependency graph and disables strict foreign keys in case of cyclical dependencies.
- DomainToString, definition ordering improved.
### is.codion.swing.framework.ui
- EntityEditPanel, control configuration refactored, now consistent with EntityTablePanel.
- EntityTablePanel.PopupMenuConfig and ToolBarConfig removed, quite unnecessary.
- ControlConfig.control() overloaded for Control.Builder.
- ControlConfig now Controls.Config, related refactoring.
- EntityEditComponentPanel bug fixed, modified indicator now also added to labels of components set directly via component().set().
- EntityEditComponentPanel bug fixed, createLabel() no longer prevents subsequent component creation.
- EntityTablePanel, selection controls removed from default toolbar.
- EntityTablePanel, keyboard shortcuts added for moving the selection.
- EntityTablePanel.TableControl and EntityEditPanel.EditControl renamed EntityTablePanelControl and EntityEditPanelControl, merged with KeyboardShortcut, related refactoring.
- KeyboardShortcutPanel bug fixed, now displays the correct Add mnemonic.
- EntityEditPanel.configureControls() renamed setupControls() to prevent confusion. EntityTablePanel.configureControls() renamed setupControls() as well, for consistency.
- EntityEditPanel, EntityTablePanel, EntityPanel, control setup refactored.
- TabbedDetailLayout, keyboard shortcuts added for expanding the detail panel.
- TabbedDetailLayout, now shows the detail panel if hidden when activated.
- EntityTablePanel.createControls() bug fixed, now only relies on configuration.
- EntityTablePanel.Config bug fixed, now initializes includeSummaryPanel to false if no summary models are available.
- TabbedDetailLayout, now shows the parent panel if hidden when activated.
- TabbedDetailLayout.TabbedDetailLayoutControl.EXPAND_RIGHT and EXPAND_LEFT renamed COLLAPSE and EXPAND respectively, CTRL added to key event button combination, otherwise the table seemed to consume the key events.
- EntityTablePanel.Config.configureTable() added, createTableCellRenderer() removed.
- TabbedDetailLayout, expand/collapse improved, now stops in the middle when expanding/collapsing from the opposite extreme.
- TabbedDetailLayout now activates parent panel when detail panel is collapsed.
- EntityEditPanel, crud operation builders added, EntityDialogs refactored.
- EntityTableCellRendererFactory added, related refactoring.
- EntityTablePanel.StatusPanel.configureLimit() bug fixed, dialog now centered on table.
- EntityTablePanel.editPanel() added.
- EntityPanel control configuration refactored, now consistent with EntityEditPanel and EntityTablePanel.
- EntityPanel, TabbedDetailLayout, now possible to configure the available panel states.
- EntityComponents now final, constructor private, factory method added.
- EntityComponents parameter removed from EntityEditComponentPanel constructor.
- EntityTablePanel.savePreferences() no longer final.
- EntityTablePanel.selectConditionPanel() bug fixed, now sets locationRelativeTo() the table parent, otherwise the scrolling state seems to interfere with the dialog location calculation.
- EntityPanel.activeDetailPanels() renamed linkedDetailPanels().
- EntityPanel, can now navigate to hidden detail panels, TabbedDetailLayout shows a hidden detail panel when activated.
- EntityTablePanel.StatusPanel refactored to not use CardLayout, otherwise the progress bar keeps updating even when invisible.
- EntityTablePanel, refresh keystroke Alt-R added.
- EntityConditionPanelFactory now sets the string provider for the in values list box.
- EntityTablePanel now removes the condition and filter panels when they are hidden, related refactoring.
- EntityTablePanel.createConditionPanel() replaced with Config.tableConditionPanelFactory(), conditionPanel no longer initialized lazily.
- EntityTablePanel, condition panel field configuration moved to EntityFieldFactory.
- TabbedDetailLayout, detail model link now deactivated when the detail panel is hidden.
- EntityTablePanel bug fixed, now overrides updateUI() in order to update hidden components.
- EntityDialogs, add button caption now Add instead of Insert.
- EntityFieldFactory renamed EntityConditionFieldFactory, now public.
- EntityComboBox now displays a wait cursor while the model is refreshing.
- EntityTablePanel bug fixed, refresh toolbar visibility now initialized correctly.
- EntityTablePanel, table condition panel initialized lazily.
- EntityApplicationPanel, frame size and maximized state now saved in user preferences, Builder.defaultFrameSize() added.
- DefaultEntityApplicationPanelBuilder, now displays an empty frame while the application panel is being initialized.
- EntityApplicationPanel, user preferences functionality improved, related refactoring.
- EntityEditPanel.Config.modifiedWarning() added, related refactoring.
- EntityTablePanel.userPreferencesKey() now protected.
### is.codion.swing.framework.ui.tools
- DomainGeneratorPanel.DEFAULT_USERNAME configuration value added.
- DomainGeneratorPanel.DEFAULT_USERNAME renamed DEFAULT_USER, can include password.
- DomainGeneratorPanel, save implemented.
- DomainGeneratorPanel bug fixed, directory creation improved.

## 0.17.43
### is.codion.common.core
- Text.delimitedString() bug fixed, no header caused a line break.
- Text.randomString() removed.
- Text.textFileContents() removed.
- Text.delimitedString() removed.
- NullOrEmpty removed, nullOrEmpty(String) moved to Text.
### is.codion.common.rmi
- ConnectionRequest.clientVersion() now returns Optional.
### is.codion.swing.common.model
- FilteredTableModel.refreshStrategy() added, replacing mergeOnRefresh().
- FilteredTableModel.rowsAsDelimitedString() replaced with export().
- TableSelectionModel.beforeSelectionChangeEvent() renamed selectionChangingEvent().
### is.codion.swing.common.ui
- FilteredTable.copyRowsAsTabDelimitedString() replaced with copyToClipboard() and copySelectedToClipboard().
- Controls.empty() now ignores separators.
### is.codion.framework.domain
- Entity.referencedEntity() and referencedKey() renamed entity() and key().
- Attribute.denormalized() added.
- DefaultEntity now caches derived values.
- EntityDefinition.STRICT_FOREIGN_KEY moved to Entities.
- Domain.domains() now unwraps the ServiceConfigurationError thrown for a clearer error message.
- Unused module dependency on slf4j removed.
- DefaultEntityDefinition.EntityAttributes.foreignKeyColumnDefinition() error message now contains the foreign key.
### is.codion.swing.framework.model
- SwingEntityTableModel.replace() now triggers multiple targeted table row updated events, instead of one for all rows.
- SwingEntityTableModel.editEvents() removed, UpdateListener duplicated detail model link functionality by replacing foreign key values.
- SwingEntityTableModel.editEvents() reintroduced.
- DefaultForeignKeyDetailModelLink.onUpdate() now only replaces table model foreign keys if editEvents() are disabled.
### is.codion.swing.framework.ui
- EntityTablePanel.TableControls.COPY_TABLE_DATA renamed COPY, COPY_CELL and COPY_ROWS added.
- EntityTablePanel.TableControls.COPY, EDIT_ATTRIBUTE, CONFIGURE_COLUMNS renamed COPY_CONTROLS, EDIT_ATTRIBUTE_CONTROLS and COLUMN_CONTROLS respectively, PRINT_CONTROLS, CONDITION_CONTROLS and FILTER_CONTROLS added.
- EntityTablePanel controls configuration refactored.
- EntityTablePanel.configurePopupMenu() and configureToolBar() added.
- EntityTablePanel bug fixed, Config.popupMenuEditAttributeControl() added.
- EntityTablePanel bug fixed, now excludes the edit attribute control from the toolbar if an editPanel is available.
- EntityEditPanel, EntityTablePanel.setupControls() renamed configureControls().
- MenuConfig renamed ControlConfig, create() renamed createControls(), now public.
- EntityTablePanel.PopupMenuConfig and ToolBarConfig added, related refactoring.
- EntityTablePanel, popup menu and toolbar configuration refactored, overriding no longer required.
- EntityTablePanel.PopupMenuConfig and ToolBarConfig now protected.

## 0.17.42
### is.codion.common.core
- Values added, extended by ValueSet, related refactoring.
- ValueList added.
- Values now behaves like Collection in regards to remove() and removeAll(), related refactoring and javadoc fixes.
- Values.set(Collection<T>) now returns true if the value changed.
### is.codion.common.rmi
- WhitelistObjectInputFilterFactory renamed WhitelistInputFilterFactory.
### is.codion.framework.domain
- DefaultEntity refactored.
- TypeReference now throws exception if used to specify a non-parameterized type.
- DefaultDomain.add() now varargs based, related refactoring.
- Derived attributes no longer require source attributes.
### is.codion.framework.db.core
- EntityConnection.Insert renamed BatchInsert, Copy.Builder.conditions() added.
- EntityConnection.insertEntities() and copyEntities() renamed batchInsert() and batchCopy() respectively.
- EntityConnection.Copy renamed BatchCopy.
### is.codion.swing.common.ui
- ListBuilder migrated from using ValueSet to ValueList.
- AbstractComponentBuilder now supports multiple linked values.
- KeyEvents bug fixed, no longer uses the action name as action map key, always generates a unique one.
### is.codion.swing.framework.ui
- EntityDialogs.DefaultEditAttributeDialogBuilder bug fixed, didn't take null into account when counting distinct values.
- EntityTablePanel, TableControl.EDIT_SELECTED renamed EDIT_VALUE and now associated with an action for editing the selected items.
- EntityTablePanel.Config.editAttributeSelection() and includeEditValueControl() added.
- EntityTablePanel.TableControl.DELETE_SELECTED renamed DELETE, related renaming.
- EntityTablePanel, edit value control added to toolbar.
- EntityTablePanel, TableControl.EDIT_VALUE renamed EDIT_ATTRIBUTE, EDIT_SELECTED_ATTRIBUTE added, related renaming.
- EntityEditPanel.Config.keyStrokes() added.

## 0.17.41
### is.codion.common.core
- Value.clear() added, PropertyValue.clear() renamed remove() in order to not clash with method from supertype.
- Value.Builder, methods for adding listeners and consumers added.
- ValueSet.Builder, methods for adding listeners and consumers added.
- State.Builder added.
- ValueObserver.nullable() renamed isNullable(), now consistent with other method names.
- Value.nullable() factory method overloaded with no parameter.
### is.codion.common.rmi
- ObjectInputFilterFactory added, configured for ServiceLoader for pluggable object input filters, related refactoring.
### is.codion.framework.domain
- OrderBy.Builder, NullOrder now parameter.
- OrderBy.OrderByColumn.ignoreCase() added.
### is.codion.swing.common.model
- ProgressWorker.Task and ProgressTask renamed ResultTask and ProgressResultTask respectively.
- ProgressWorker refactored, no longer wraps ResultTask in a ProgressResultTask.
### is.codion.swing.common.ui
- InputDialogBuilder.inputValid() and inputValidator() renamed valid() and validator() respectively.
- DefaultProgressWorkerDialogBuilder refactored.
### is.codion.swing.framework.ui
- EntitySearchFieldPanel.Builder.includeAddButton() and includeEditButton() bug fixed, exception now thrown as per javadoc.
- EntityEditPanel.Config.editPanel() added.
### is.codion.swing.framework.server.monitor
- ConnectionPoolMonitor, pool cleanup interval now milliseconds based.

## 0.17.40
### is.codion.common.core
- EventObserver.addDataListener() renamed addConsumer(), related renaming.
- Value.Builder added, replaces a couple of static factory methods.
- Value.Builder.link() added.
- ValueSet.Builder added, replaces a couple of static factory methods.
### is.codion.framework.domain
- QueryKeyGenerator, SequenceKeyGenerator, now overwrite a current value instead of silently skipping it.
- AbstractQueriedKeyGenerator.selectAndPopulate() primary key column definition parameter removed.
### is.codion.swing.common.ui
- ListBuilder overhaul, ListBuilder.Factory added, provides list builders depending on whether the value should be represented by the list items or selected item(s).
- ComponentBuilder.linkedValue() renamed link().
### is.codion.swing.fraework.ui
- EntitySearchField.searchControl() added.
- EntitySearchFieldPanel, search button added.
- EntityPanel, EntityEditPanel, EntityTablePanel.Config now provides access the the panel being configured.
- EntityEditPanel.Confirmer.NONE added.
- EntityComboBoxPanel, EntitySearchFieldPanel.Builder.add() and edit() renamed addButton() and editButton().
- EntityComboBox.Builder added.
- EntityComboBox, EntitySearchField, add and edit actions moved from respective panel classes, Builder.editPanelSupplier() added.
- EntityComboBoxPanel, EntitySearchFieldPanel.Builder.addButton() and editButton() renamed includeAddButton() and includeEditButton().

## 0.17.39
### is.codion.common.core
- Version.metadata() now returns Optional.
- Version.build() added, parsing no longer supports space as metadata delimiter.
- Version, static factory methods replaced with a builder.
- Version.parsePropertiesFile() renamed parse().
- Configuration.ConfigurationFileNotFoundException now thrown instead of RuntimeException when a required configuration file is not found.
### is.codion.common.db
- DatabaseConnection.beginTransaction() renamed startTransaction().
### is.codion.framework.domain
- TypeReference added.
- Attribute.attribute(), Column.column(), EntityType.attribute() and column() overloaded with TypeReference.
### is.codion.framework.domain
- Column.Converter.handlesNull() added, default implementation returns false.
- DefaultColumnDefinition.get() now only uses the Converter to convert null values in case it handles null.
- DefaultColumn.BooleanConverter now throws exception if it comes across an unrecognized value.
### is.codion.framework.domain.test
- EntityTestUnit.testInsert() now asserts all inserted values.
### is.codion.framework.db.core
- EntityConnection.beginTransaction() renamed startTransaction().
### is.codion.framework.db.local
- DefaultLocalEntityConnection.setParameterValue() now only uses the Converter to convert null values in case it handles null.
### is.codion.swing.common.model
- DefaultFilteredTableColumnModel.setVisibleColumns() bug fixed, now throws IllegalArgumentException in case a column is not found, instead of NullPointerException.
### is.codion.swing.common.ui
- KeyboardShortcuts.Shortcut added, with defaultKeystroke(), keystroke enums must implement it.
### is.codion.swing.framework.ui
- EntityTablePanel, mnemonic removed from deleteSelected control.

## 0.17.38
### is.codion.common.core
- Event.listener() and dataListener() removed, no longer necessary.
### is.codion.framework.model
- EntityEditModel.valueEvent() added.
- EntitySearchModel.Settings.spaceAsWildcard() added.
### is.codion.swing.common.ui
- KeyboardShortcuts, default keystroke added to javadoc.
- FilteredTable, Metal Look and Feel related bug fixed, searchField minimum width also used as preferred width.
- HintTextField.hintText() renamed hint(), refactoring with minor improvements.
- TextFieldBuilder.hintText() renamed hint().
- DefaultListBuilder bug fixed, visibleRowCount was 0 by default.
### is.codion.swing.framework.ui
- EntityPanel.icon() added, used when panel is displayed in windows or tabbed panes.
- EntityPanel.Builder.icon() added.
- EntityPanel, caption, description and icon moved to Config, related changes.
- EntityPanel.disposeEditDialogOnEscape moved to Config.
- EntityPanel.updateEditPanelState() refactored, ShowHiddenEditPanel now brings window to front if available, minor related changes.
- EntityPanel.updateEditPanelState() refactoring mistake fixed. 
- DefaultEntityApplicationPanelBuilder, lambdas replaced with classes.
- EntityComponentValidators merged with EntityEditComponentPanel, FormattedTextValidator removed.
- EntityEditPanel bug fixed, insert, update and delete control exception handlers set.
- EntityEditPanel.KeyboardShortcut.DISPLAY_ENTITY_MENU added.
- EntityEditPanel.Config.includeEntityMenu added.
- EntityTablePanel.KeyboardShortcut.DISPLAY_ENTITY_MENU added.
- WindowDetailLayout.WindowType moved to EntityPanel, EntityPanel.Config.USE_FRAME_PANEL_DISPLAY replaced with WINDOW_TYPE.
- TabbedDetailLayout, WindowDetailLayout, windowType now defaults to the one specified by the EntityPanel.
- KeyboardShortcuts, default keystroke added to javadoc.
- EntityTablePanel.Config.includeToolBar added.
- EntityEditComponentPanel.addValidator() overloaded for combo boxes, now protected, validator added to all combo boxes.
- EntitySearchField.DefaultTableSelector refactored.
- EntitySearchField.DefaultListSelector refactored.
- EntityTablePanel constructor now throws exception if the edit models don't match.
- EntityTablePanel no longer creates Add and Edit controls if the edit model doesn't allow insert or update.
- EntitySearchField, spaceAsWildcard() setting added to settings panel.

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