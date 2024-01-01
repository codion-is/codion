Codion Change Log
==================

## 0.17.23-SNAPSHOT
### is.codion.common.db
- AbstractDatabase, transaction isolation now a final field, instead of using the configuration value directly.
- AbstractDatabase, login timeout now set in a static initializer block, instead of during connection creation.
### is.codion.swing.common.ui
- KeyboardShortcuts.copy() added.
- CalendarPanel.Builder added, replaces factory methods
- CalendarPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
### is.codion.swing.framework.ui
- EntityPanel, EntityTablePanel, keyboard shortcuts can now be configured on an instance basis, via configure().keyStroke().
- TabbedPanelLayout.Builder.keyboardShortcut() added for configuring keyboard shortcuts on an instance basis.
- EntitySearchField.KeyboardShortcut moved to EntitySearchFieldPanel, EntitySearchFieldPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.
- EntityComboBox.KeyboardShortcut moved to EntityComboBoxPanel, EntityComboBoxPanel.Builder.keyStroke() added for configuring keyboard shortcuts on an instance basis.

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
- EntityServerConfiguration, connectionPoolProvider renamed connectionPoolFactory
- ConnectionPoolFactory.createConnectionPoolWrapper() renamed createConnectionPool()
- WaitCursor replaced with Cursors utility class, wait cursor usage reduced and simplified throughout.
- EntityEditPanel.beforeInsert() and beforeUpdate() no longer throw ValidationException, now called outside of try/catch block along with beforeDelete(). EntityTablePanel.beforeDelete() moved outside of try/catch block, delete() added for deleting without confirmation.
- Text.padString() removed along with Text.ALIGNMENT.
- EntityEditPanel.controls() added, public, can be used instead of the protected createControls().