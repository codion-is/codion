/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilteredTableCellRendererFactory;
import is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel;
import is.codion.swing.common.ui.component.table.FilteredTableConditionPanel;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel.Confirmer;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableConditionPanel.filteredTableConditionPanel;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * The EntityTablePanel is a UI class based on the EntityTableModel class.
 * It consists of a JTable as well as filtering/searching and summary panels.
 * The default layout is as follows
 * <pre>
 *  ____________________________________________________
 * |                conditionPanel                      |
 * |____________________________________________________|
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                entityTable (FilteredTable)         |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |____________________________________________________|
 * |                summaryPanel                        |
 * |____________________________________________________|
 * |                southPanel                          |
 * |____________________________________________________|
 * </pre>
 * The condition and summary panels can be hidden
 * Note that {@link #initialize()} must be called to initialize this panel before displaying it.
 * @see EntityTableModel
 */
public class EntityTablePanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());

  /**
   * Specifies whether table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CONDITION_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.conditionPanelVisible", false);

  /**
   * Specifies whether table filter panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> FILTER_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.filterPanelVisible", false);

  /**
   * Specifies whether to include a {@link EntityPopupMenu} on this table, triggered with CTRL-ALT-V.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeEntityMenu", true);

  /**
   * Specifies whether to include a 'Clear' control in the popup menu.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> INCLUDE_CLEAR_CONTROL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeClearControl", false);

  /**
   * Specifies whether to include a condition panel.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_CONDITION_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeConditionPanel", true);

  /**
   * Specifies whether to include a filter panel.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_FILTER_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeFilterPanel", false);

  /**
   * Specifies whether to show an indeterminate progress bar while the model is refreshing.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SHOW_REFRESH_PROGRESS_BAR =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.showRefreshProgressBar", false);

  /**
   * Specifies whether the refresh button should always be visible or only when the condition panel is visible<br>
   * Value type: Boolean<br>
   * Default value: {@link RefreshButtonVisible#WHEN_CONDITION_PANEL_IS_VISIBLE}
   */
  public static final PropertyValue<RefreshButtonVisible> REFRESH_BUTTON_VISIBLE =
          Configuration.enumValue("is.codion.swing.framework.ui.EntityTablePanel.refreshButtonVisible",
                  RefreshButtonVisible.class, RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE);

  /**
   * Specifies how column selection is presented to the user.<br>
   * Value type: {@link ColumnSelection}<br>
   * Default value: {@link ColumnSelection#DIALOG}
   */
  public static final PropertyValue<ColumnSelection> COLUMN_SELECTION =
          Configuration.enumValue("is.codion.swing.framework.ui.EntityTablePanel.columnSelection", ColumnSelection.class, ColumnSelection.DIALOG);

  /**
   * The standard controls available
   */
  public enum ControlCode {
    PRINT,
    DELETE_SELECTED,
    VIEW_DEPENDENCIES,
    EDIT_SELECTED,
    SELECT_COLUMNS,
    RESET_COLUMNS,
    COLUMN_AUTO_RESIZE_MODE,
    SELECTION_MODE,
    CLEAR,
    REFRESH,
    TOGGLE_SUMMARY_PANEL,
    TOGGLE_CONDITION_PANEL,
    CONDITION_PANEL_VISIBLE,
    TOGGLE_FILTER_PANEL,
    FILTER_PANEL_VISIBLE,
    CLEAR_SELECTION,
    MOVE_SELECTION_UP,
    MOVE_SELECTION_DOWN,
    COPY_TABLE_DATA,
    REQUEST_TABLE_FOCUS,
    SELECT_CONDITION_PANEL,
    SELECT_FILTER_PANEL,
    CONFIGURE_COLUMNS
  }

  /**
   * Specifies the refresh button visibility.
   */
  public enum RefreshButtonVisible {
    /**
     * Refresh button should always be visible
     */
    ALWAYS,
    /**
     * Refresh button should only be visible when the table condition panel is visible
     */
    WHEN_CONDITION_PANEL_IS_VISIBLE
  }

  /**
   * Specifies how column selection is presented.
   */
  public enum ColumnSelection {
    /**
     * Display a dialog.
     */
    DIALOG,
    /**
     * Display toggle controls directly in the menu.
     */
    MENU
  }

  private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;
  private static final Control NULL_CONTROL = Control.control(() -> {});
  private static final Function<SwingEntityTableModel, String> DEFAULT_STATUS_MESSAGE = new DefaultStatusMessage();

  private final State conditionPanelVisibleState = State.state(CONDITION_PANEL_VISIBLE.get());
  private final State filterPanelVisibleState = State.state(FILTER_PANEL_VISIBLE.get());
  private final State summaryPanelVisibleState = State.state();

  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> editComponentFactories = new HashMap<>();
  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> cellEditorComponentFactories = new HashMap<>();

  private final List<Controls> additionalPopupControls = new ArrayList<>();
  private final List<Controls> additionalToolBarControls = new ArrayList<>();
  private final ValueSet<Attribute<?>> editableAttributes;

  private final SwingEntityTableModel tableModel;
  private final EntityConditionPanelFactory conditionPanelFactory;
  private final FilteredTable<Entity, Attribute<?>> table;
  private final StatusPanel statusPanel;
  private final JPanel southPanel = new JPanel(new BorderLayout());

  private Confirmer deleteConfirmer = new DeleteConfirmer();
  private JScrollPane tableScrollPane;
  private FilteredTableConditionPanel<Attribute<?>> conditionPanel;
  private JScrollPane conditionPanelScrollPane;
  private FilteredTableConditionPanel<Attribute<?>> filterPanel;
  private JScrollPane filterPanelScrollPane;
  private FilteredTableColumnComponentPanel<Attribute<?>, JPanel> summaryPanel;
  private JScrollPane summaryPanelScrollPane;
  private JPanel tablePanel;
  private JToolBar refreshButtonToolBar;
  private Control conditionRefreshControl;
  private RefreshButtonVisible refreshButtonVisible = REFRESH_BUTTON_VISIBLE.get();
  private boolean includeSouthPanel = true;
  private boolean includeConditionPanel = INCLUDE_CONDITION_PANEL.get();
  private boolean includeFilterPanel = INCLUDE_FILTER_PANEL.get();
  private boolean includeClearControl = INCLUDE_CLEAR_CONTROL.get();
  private boolean includeSelectionModeControl = false;
  private ColumnSelection columnSelection = COLUMN_SELECTION.get();
  private boolean includePopupMenu = true;
  private boolean initialized = false;

  /**
   * The action to take when a referential integrity error occurs on delete
   */
  private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
          ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   */
  public EntityTablePanel(SwingEntityTableModel tableModel) {
    this(requireNonNull(tableModel), new EntityConditionPanelFactory(tableModel.entityDefinition()));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   * @param conditionPanelFactory the condition panel factory, if any
   */
  public EntityTablePanel(SwingEntityTableModel tableModel, EntityConditionPanelFactory conditionPanelFactory) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.conditionPanelFactory = conditionPanelFactory;
    this.editableAttributes = valueSet(tableModel.entityDefinition().attributes().updatable().stream()
            .map(AttributeDefinition::attribute)
            .collect(toSet()));
    this.editableAttributes.addValidator(new EditMenuAttributeValidator());
    this.table = createTable();
    this.statusPanel = new StatusPanel();
  }

  /**
   * @return the table
   */
  public final FilteredTable<Entity, Attribute<?>> table() {
    return table;
  }

  /**
   * @param <T> the table model type
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final <T extends SwingEntityTableModel> T tableModel() {
    return (T) tableModel;
  }

  /**
   * @return the condition panel
   * @throws IllegalStateException in case no condition panel is available
   */
  public final FilteredTableConditionPanel<Attribute<?>> conditionPanel() {
    if (conditionPanel == null) {
      conditionPanel = createConditionPanel(conditionPanelFactory);
      if (conditionPanel == null) {
        throw new IllegalStateException("No condition panel is available");
      }
    }

    return conditionPanel;
  }

  /**
   * Specifies the attributes that should be editable in this table panel, such as via the edit selected entities menu.
   * Modifying this {@link ValueSet} after the panel has been initialized will result in a {@link IllegalStateException} being thrown
   * @return the attributes that should be editable via this table panel
   */
  public final ValueSet<Attribute<?>> editableAttributes() {
    return editableAttributes;
  }

  /**
   * @param additionalPopupMenuControls a set of controls to add to the table popup menu
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void addPopupMenuControls(Controls additionalPopupMenuControls) {
    throwIfInitialized();
    this.additionalPopupControls.add(requireNonNull(additionalPopupMenuControls));
  }

  /**
   * @param additionalToolBarControls a set of controls to add to the table toolbar menu
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void addToolBarControls(Controls additionalToolBarControls) {
    throwIfInitialized();
    this.additionalToolBarControls.add(requireNonNull(additionalToolBarControls));
  }

  /**
   * @param includeSouthPanel true if the south panel should be included
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initializeSouthPanel()
   * @see #initialize()
   */
  public final void setIncludeSouthPanel(boolean includeSouthPanel) {
    throwIfInitialized();
    this.includeSouthPanel = includeSouthPanel;
  }

  /**
   * @param includeConditionPanel true if the condition panel should be included
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void setIncludeConditionPanel(boolean includeConditionPanel) {
    throwIfInitialized();
    this.includeConditionPanel = includeConditionPanel;
  }

  /**
   * @param includeFilterPanel true if the filter panel should be included
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void setIncludeFilterPanel(boolean includeFilterPanel) {
    throwIfInitialized();
    this.includeFilterPanel = includeFilterPanel;
  }

  /**
   * @param includePopupMenu true if a popup menu should be included
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void setIncludePopupMenu(boolean includePopupMenu) {
    throwIfInitialized();
    this.includePopupMenu = includePopupMenu;
  }

  /**
   * @param includeClearControl true if a 'Clear' control should be included in the popup menu
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void setIncludeClearControl(boolean includeClearControl) {
    throwIfInitialized();
    this.includeClearControl = includeClearControl;
  }

  /**
   * @param includeSelectionModeControl true if a 'Single Selection' control should be included in the popup menu
   * @throws IllegalStateException in case the panel has already been initialized
   * @see #initialize()
   */
  public final void setIncludeSelectionModeControl(boolean includeSelectionModeControl) {
    throwIfInitialized();
    this.includeSelectionModeControl = includeSelectionModeControl;
  }

  /**
   * @param columnSelection specifies how columns are selected
   */
  public final void setColumnSelection(ColumnSelection columnSelection) {
    throwIfInitialized();
    this.columnSelection = requireNonNull(columnSelection);
  }

  /**
   * @return the refresh button visible setting
   */
  public final RefreshButtonVisible getRefreshButtonVisible() {
    return refreshButtonVisible;
  }

  /**
   * @param refreshButtonVisible the refresh button visible setting
   */
  public final void setRefreshButtonVisible(RefreshButtonVisible refreshButtonVisible) {
    this.refreshButtonVisible = requireNonNull(refreshButtonVisible);
    if (refreshButtonToolBar == null) {
      refreshButtonToolBar = createRefreshButtonToolBar();
    }
    refreshButtonToolBar.setVisible(refreshButtonVisible == RefreshButtonVisible.ALWAYS || conditionPanelVisibleState.get());
  }

  /**
   * @return the state controlling whether the condition panel is visible
   */
  public final State conditionPanelVisible() {
    return conditionPanelVisibleState;
  }

  /**
   * @return the state controlling whether the filter panel is visible
   */
  public final State filterPanelVisible() {
    return filterPanelVisibleState;
  }

  /**
   * @return the state controlling whether the summary panel is visible
   */
  public final State summaryPanelVisible() {
    return summaryPanelVisibleState;
  }

  /**
   * @return the state controlling whether an indeterminate progress bar should be shown while the model is refreshing
   * @see #SHOW_REFRESH_PROGRESS_BAR
   */
  public final State showRefreshProgressBar() {
    return statusPanel.showRefreshProgressBar;
  }

  /**
   * @return the value containing the function for creating the table status message
   * @see #statusMessage()
   */
  public final Value<Function<SwingEntityTableModel, String>> statusMessage() {
    return statusPanel.statusMessageFunction;
  }

  /**
   * Sets the component factory for the given attribute, used when editing entities via {@link #editSelectedEntities(Attribute)}.
   * @param attribute the attribute
   * @param componentFactory the component factory
   * @param <T> the value type
   * @param <A> the attribute type
   * @param <C> the component type
   */
  public final <T, A extends Attribute<T>, C extends JComponent> void setEditComponentFactory(A attribute,
                                                                                              EntityComponentFactory<T, A, C> componentFactory) {
    tableModel().entityDefinition().attributes().definition(attribute);
    editComponentFactories.put(attribute, requireNonNull(componentFactory));
  }

  /**
   * Sets the table cell editor component factory for the given attribute.
   * @param attribute the attribute
   * @param componentFactory the component factory
   * @param <T> the value type
   * @param <A> the attribute type
   * @param <C> the component type
   */
  public final <T, A extends Attribute<T>, C extends JComponent> void setTableCellEditorFactory(A attribute,
                                                                                                EntityComponentFactory<T, A, C> componentFactory) {
    tableModel().entityDefinition().attributes().definition(attribute);
    cellEditorComponentFactories.put(attribute, requireNonNull(componentFactory));
  }

  /**
   * Toggles the condition panel through the states hidden, visible and advanced
   */
  public final void toggleConditionPanel() {
    if (conditionPanel != null) {
      toggleConditionPanel(conditionPanelScrollPane, conditionPanel.advanced(), conditionPanelVisibleState);
    }
  }

  /**
   * Toggles the filter panel through the states hidden, visible and advanced
   */
  public final void toggleFilterPanel() {
    if (filterPanel != null) {
      toggleConditionPanel(filterPanelScrollPane, filterPanel.advanced(), filterPanelVisibleState);
    }
  }

  /**
   * Allows the user to select one of the available search condition panels
   */
  public final void selectConditionPanel() {
    if (includeConditionPanel) {
      selectConditionPanel(conditionPanel, conditionPanelScrollPane, conditionPanelVisibleState,
              tableModel, this, FrameworkMessages.selectSearchField());
    }
  }

  /**
   * Allows the user to select one of the available filter condition panels
   */
  public final void selectFilterPanel() {
    if (includeFilterPanel) {
      selectConditionPanel(filterPanel, filterPanelScrollPane, filterPanelVisibleState,
              tableModel, this, FrameworkMessages.selectFilterField());
    }
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error during delete
   */
  public final void setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
  }

  /**
   * @param deleteConfirmer the delete confirmer, null for the default one
   */
  public final void setDeleteConfirmer(Confirmer deleteConfirmer) {
    this.deleteConfirmer = deleteConfirmer == null ? new DeleteConfirmer() : deleteConfirmer;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.entityType();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode} or an empty Optional if no control is available
   */
  public final Optional<Control> control(ControlCode controlCode) {
    Control control = controls.get(requireNonNull(controlCode));

    return control == NULL_CONTROL ? Optional.empty() : Optional.ofNullable(control);
  }

  /**
   * Retrieves a new value via input dialog and performs an update on the selected entities
   * assigning the value to the attribute
   * @param attributeToEdit the attribute which value to edit
   * @param <T> the attribute value type
   * @see #setEditComponentFactory(Attribute, EntityComponentFactory)
   */
  public final <T> void editSelectedEntities(Attribute<T> attributeToEdit) {
    requireNonNull(attributeToEdit);
    if (!tableModel.selectionModel().isSelectionEmpty()) {
      EntityDialogs.editDialog(tableModel.editModel(), attributeToEdit)
              .owner(this)
              .componentFactory((EntityComponentFactory<T, Attribute<T>, ?>) editComponentFactories.get(attributeToEdit))
              .edit(tableModel.selectionModel().getSelectedItems());
    }
  }

  /**
   * Displays a dialog containing tables of entities depending on the selected entities via non-soft foreign keys
   */
  public final void viewDependencies() {
    if (!tableModel.selectionModel().isSelectionEmpty()) {
      displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(), this);
    }
  }

  /**
   * Deletes the entities selected in the underlying table model after asking for confirmation using
   * the {@link Confirmer} set via {@link #setDeleteConfirmer(Confirmer)}.
   * @return true if the delete operation was successful
   * @see #setDeleteConfirmer(EntityEditPanel.Confirmer)
   * @see #beforeDelete()
   */
  public final boolean deleteWithConfirmation() {
    if (deleteConfirmer.confirm(this)) {
      return delete();
    }

    return false;
  }

  /**
   * Deletes the entities selected in the underlying table model without asking for confirmation.
   * @return true if the delete operation was successful
   * @see #beforeDelete()
   */
  public final boolean delete() {
    beforeDelete();
    try {
      tableModel.deleteSelected();

      return true;
    }
    catch (ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onException(e);
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }

    return false;
  }

  /**
   * Prints the table
   * @throws java.awt.print.PrinterException in case of a print exception
   * @see JTable#print()
   */
  public final void printTable() throws PrinterException {
    table.print();
  }

  /**
   * Initializes the UI, while presenting a wait cursor to the user.
   * Note that calling this method more than once has no effect.
   * @return this EntityTablePanel instance
   */
  public final EntityTablePanel initialize() {
    if (!initialized) {
      try {
        setupComponents();
        setupControls();
        setupTable();
        layoutPanel(tablePanel, includeSouthPanel ? initializeSouthPanel() : null);
        setConditionPanelVisibleInternal(conditionPanelVisibleState.get());
        setFilterPanelVisibleInternal(filterPanelVisibleState.get());
        setSummaryPanelVisibleInternal(summaryPanelVisibleState.get());
        bindEvents();
        setupKeyboardActions();
        updateComponentTreeUI(this);
      }
      finally {
        initialized = true;
      }
    }

    return this;
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * Not called if the south panel has been disabled via {@link #setIncludeSouthPanel(boolean)}.
   * @return the south panel, or null if no south panel should be included
   */
  protected JPanel initializeSouthPanel() {
    JPanel searchFieldPanel = Components.panel(new GridBagLayout())
            .add(table.searchField(), createHorizontalFillConstraints())
            .build();
    JSplitPane southPanelSplitPane = Components.splitPane()
            .continuousLayout(true)
            .leftComponent(searchFieldPanel)
            .rightComponent(statusPanel)
            .build();
    southPanel.add(southPanelSplitPane, BorderLayout.CENTER);
    southPanel.add(refreshButtonToolBar, BorderLayout.WEST);
    JToolBar southToolBar = createSouthToolBar();
    if (southToolBar != null) {
      southPanel.add(southToolBar, BorderLayout.EAST);
    }

    return southPanel;
  }

  /**
   * Sets up the default keyboard actions.
   */
  protected void setupKeyboardActions() {
    control(ControlCode.REQUEST_TABLE_FOCUS).ifPresent(control ->
            KeyEvents.builder(VK_T)
                    .modifiers(CTRL_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.SELECT_CONDITION_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_S)
                    .modifiers(CTRL_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.TOGGLE_CONDITION_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_S)
                    .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.TOGGLE_FILTER_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_F)
                    .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.SELECT_FILTER_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_F)
                    .modifiers(CTRL_DOWN_MASK | SHIFT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(EntityTablePanel.ControlCode.PRINT).ifPresent(control ->
            KeyEvents.builder(VK_P)
                    .modifiers(CTRL_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}, null for none
   * @throws IllegalStateException in case the panel has already been initialized
   */
  protected final void setControl(ControlCode controlCode, Control control) {
    throwIfInitialized();
    controls.put(requireNonNull(controlCode), control == null ? NULL_CONTROL : control);
  }

  protected Controls createToolBarControls(List<Controls> additionalToolBarControls) {
    requireNonNull(additionalToolBarControls);
    Controls toolbarControls = Controls.controls();
    control(ControlCode.TOGGLE_SUMMARY_PANEL).ifPresent(toolbarControls::add);
    Control toggleConditionPanelControl = control(ControlCode.TOGGLE_CONDITION_PANEL).orElse(null);
    Control toggleFilterPanelControl = control(ControlCode.TOGGLE_FILTER_PANEL).orElse(null);
    if (toggleConditionPanelControl != null || toggleFilterPanelControl != null) {
      if (toggleConditionPanelControl != null) {
        toolbarControls.add(toggleConditionPanelControl);
      }
      if (toggleFilterPanelControl != null) {
        toolbarControls.add(toggleFilterPanelControl);
      }
      toolbarControls.addSeparator();
    }
    control(ControlCode.DELETE_SELECTED).ifPresent(toolbarControls::add);
    control(ControlCode.PRINT).ifPresent(toolbarControls::add);
    control(ControlCode.CLEAR_SELECTION).ifPresent(control -> {
      toolbarControls.add(control);
      toolbarControls.addSeparator();
    });
    control(ControlCode.MOVE_SELECTION_UP).ifPresent(toolbarControls::add);
    control(ControlCode.MOVE_SELECTION_DOWN).ifPresent(toolbarControls::add);
    additionalToolBarControls.forEach(additionalControls -> {
      toolbarControls.addSeparator();
      additionalControls.actions().forEach(toolbarControls::add);
    });

    return toolbarControls;
  }

  /**
   * Creates a Controls instance containing the controls to include in the table popup menu.
   * Returns null or an empty Controls instance to indicate that no popup menu should be included.
   * @param additionalPopupMenuControls any additional controls to include in the popup menu
   * @return Controls on which to base the table popup menu, null or an empty Controls instance
   * if no popup menu should be included
   */
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    requireNonNull(additionalPopupMenuControls);
    Controls popupControls = Controls.controls();
    control(ControlCode.REFRESH).ifPresent(popupControls::add);
    control(ControlCode.CLEAR).ifPresent(popupControls::add);
    if (popupControls.notEmpty()) {
      popupControls.addSeparator();
    }
    addAdditionalControls(popupControls, additionalPopupMenuControls);
    State separatorRequired = State.state();
    control(ControlCode.EDIT_SELECTED).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    control(ControlCode.DELETE_SELECTED).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    control(ControlCode.VIEW_DEPENDENCIES).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    Controls printControls = createPrintMenuControls();
    if (printControls != null && printControls.notEmpty()) {
      popupControls.add(printControls);
      separatorRequired.set(true);
    }
    Controls columnControls = createColumnControls();
    if (columnControls.notEmpty()) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(columnControls);
      separatorRequired.set(true);
    }
    control(ControlCode.SELECTION_MODE).ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (includeConditionPanel && conditionPanel != null) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addConditionControls(popupControls);
      separatorRequired.set(true);
    }
    if (includeFilterPanel && filterPanel != null) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addFilterControls(popupControls);
      separatorRequired.set(true);
    }
    control(ControlCode.COPY_TABLE_DATA).ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
    });

    return popupControls;
  }

  /**
   * By default this method returns a {@link Controls} instance containing
   * the {@link Control} associated with {@link ControlCode#PRINT}.
   * If no {@link Control} has been assigned to that control key,
   * an empty {@link Controls} instance is returned.
   * Override to add print actions, which will then appear in the table popup menu.
   * @return the print controls to display in the table popup menu
   */
  protected Controls createPrintMenuControls() {
    Controls.Builder builder = Controls.builder()
            .name(Messages.print())
            .mnemonic(Messages.printMnemonic())
            .smallIcon(FrameworkIcons.instance().print());
    control(ControlCode.PRINT).ifPresent(builder::control);

    return builder.build();
  }

  /**
   * Creates a TableCellRenderer to use for the given attribute in this EntityTablePanel
   * @param attribute the attribute
   * @return the TableCellRenderer for the given attribute
   */
  protected TableCellRenderer createTableCellRenderer(Attribute<?> attribute) {
    return EntityTableCellRenderer.builder(tableModel, attribute).build();
  }

  /**
   * Creates a TableCellEditor for the given attribute, returns null if no editor is available,
   * such as for non-updatable attributes.
   * @param attribute the attribute
   * @return a TableCellEditor for the given attribute, null in case none is available
   */
  protected TableCellEditor createTableCellEditor(Attribute<?> attribute) {
    AttributeDefinition<?> attributeDefinition = tableModel.entityDefinition().attributes().definition(attribute);
    if (attribute instanceof ColumnDefinition && !((ColumnDefinition<?>) attributeDefinition).updatable()) {
      return null;
    }
    if (nonUpdatableForeignKey(attribute)) {
      return null;
    }

    return new EntityTableCellEditor<>(() -> cellEditorComponentValue(attribute, null));
  }

  /**
   * This method simply adds {@code tablePanel} at location BorderLayout.CENTER and,
   * if non-null, the given {@code southPanel} to the {@code BorderLayout.SOUTH} location.
   * By overriding this method you can override the default layout.
   * @param tableComponent the component containing the table, condition and summary panel
   * @param southPanel the south toolbar panel, null if not required
   * @see #initializeSouthPanel()
   */
  protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
    requireNonNull(tableComponent, "tableComponent");
    setLayout(new BorderLayout());
    add(tableComponent, BorderLayout.CENTER);
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * Creates the south panel toolbar, by default based on {@link #createToolBarControls(List)}
   * @return the toolbar to add to the south panel, null if none should be included
   */
  protected JToolBar createSouthToolBar() {
    Controls toolbarControls = createToolBarControls(additionalToolBarControls);
    if (toolbarControls == null || toolbarControls.empty()) {
      return null;
    }

    return toolBar()
            .controls(toolbarControls)
            .focusable(false)
            .floatable(false)
            .rollover(true)
            .build(toolBar -> Arrays.stream(toolBar.getComponents())
                    .map(JComponent.class::cast)
                    .forEach(component -> component.setToolTipText(null)));
  }

  /**
   * Called before delete is performed on the selected entities.
   * To cancel the delete throw a {@link is.codion.common.model.CancelException}.
   */
  protected void beforeDelete() {}

  /**
   * Propagates the exception to {@link #onValidationException(ValidationException)} or
   * {@link #onReferentialIntegrityException(ReferentialIntegrityException)} depending on type,
   * otherwise displays the exception.
   * @param exception the exception to handle
   * @see #displayException(Throwable)
   */
  protected void onException(Throwable exception) {
    if (exception instanceof ValidationException) {
      onValidationException((ValidationException) exception);
    }
    else if (exception instanceof ReferentialIntegrityException) {
      onReferentialIntegrityException((ReferentialIntegrityException) exception);
    }
    else {
      displayException(exception);
    }
  }

  /**
   * Called when a {@link ReferentialIntegrityException} occurs during a delete operation on the selected entities.
   * If the referential error handling is {@link ReferentialIntegrityErrorHandling#DISPLAY_DEPENDENCIES},
   * the dependencies of the entities involved are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
    requireNonNull(exception);
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
      displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(),
              this, MESSAGES.getString("unknown_dependent_records"));
    }
    else {
      displayException(exception);
    }
  }

  /**
   * Displays the exception message.
   * @param exception the exception
   */
  protected void onValidationException(ValidationException exception) {
    requireNonNull(exception);
    String title = tableModel.entities()
            .definition(exception.attribute().entityType())
            .attributes().definition(exception.attribute())
            .caption();
    JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays the exception in a dialog, with the dialog owner as the current focus owner
   * or this panel if none is available.
   * @param exception the exception to display
   */
  protected final void displayException(Throwable exception) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      focusOwner = EntityTablePanel.this;
    }
    Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
  }

  /**
   * Creates a {@link Controls} containing controls for editing the value of a single attribute
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#updateEnabled()} is enabled.
   * @return the edit controls
   * @see #excludeFromEditMenu(Attribute)
   * @see EntityEditModel#updateEnabled()
   */
  private Controls createEditSelectedControls() {
    StateObserver selectionNotEmpty = tableModel.selectionModel().selectionNotEmpty();
    StateObserver updateEnabled = tableModel.editModel().updateEnabled();
    StateObserver updateMultipleEnabledOrSingleSelection =
            State.or(tableModel.editModel().updateMultipleEnabled(),
                    tableModel.selectionModel().singleSelection());
    StateObserver enabledState = State.and(selectionNotEmpty, updateEnabled, updateMultipleEnabledOrSingleSelection);
    Controls editControls = Controls.builder()
            .name(FrameworkMessages.edit())
            .enabled(enabledState)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(FrameworkMessages.editSelectedTip())
            .build();
    editableAttributes.get().stream()
            .map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
            .sorted(AttributeDefinition.definitionComparator())
            .forEach(attributeDefinition -> editControls.add(Control.builder(() -> editSelectedEntities(attributeDefinition.attribute()))
                    .name(attributeDefinition.caption() == null ? attributeDefinition.attribute().name() : attributeDefinition.caption())
                    .enabled(enabledState)
                    .build()));

    return editControls;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  private Control createViewDependenciesControl() {
    return Control.builder(this::viewDependencies)
            .name(FrameworkMessages.dependencies())
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .description(FrameworkMessages.dependenciesTip())
            .smallIcon(FrameworkIcons.instance().dependencies())
            .build();
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
   */
  private Control createDeleteSelectedControl() {
    return Control.builder(this::deleteWithConfirmation)
            .name(FrameworkMessages.delete())
            .enabled(State.and(
                    tableModel.editModel().deleteEnabled(),
                    tableModel.selectionModel().selectionNotEmpty()))
            .description(FrameworkMessages.deleteSelectedTip())
            .smallIcon(FrameworkIcons.instance().delete())
            .build();
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  private Control createRefreshControl() {
    return Control.builder(tableModel::refresh)
            .name(Messages.refresh())
            .description(Messages.refreshTip())
            .mnemonic(Messages.refreshMnemonic())
            .smallIcon(FrameworkIcons.instance().refresh())
            .enabled(tableModel.refresher().observer().not())
            .build();
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  private Control createClearControl() {
    return Control.builder(tableModel::clear)
            .name(Messages.clear())
            .description(Messages.clearTip())
            .mnemonic(Messages.clearMnemonic())
            .smallIcon(FrameworkIcons.instance().clear())
            .build();
  }

  private Control createToggleConditionPanelControl() {
    return Control.builder(this::toggleConditionPanel)
            .smallIcon(FrameworkIcons.instance().search())
            .description(MESSAGES.getString("show_condition_panel"))
            .build();
  }

  private Control createToggleFilterPanelControl() {
    return Control.builder(this::toggleFilterPanel)
            .smallIcon(FrameworkIcons.instance().filter())
            .description(MESSAGES.getString("show_filter_panel"))
            .build();
  }

  private Control createToggleSummaryPanelControl() {
    return ToggleControl.builder(summaryPanelVisibleState)
            .smallIcon(FrameworkIcons.instance().summary())
            .description(MESSAGES.getString("toggle_summary_tip"))
            .build();
  }

  private Control createClearSelectionControl() {
    return Control.builder(tableModel.selectionModel()::clearSelection)
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .smallIcon(FrameworkIcons.instance().clearSelection())
            .description(MESSAGES.getString("clear_selection_tip"))
            .build();
  }

  private Control createMoveSelectionDownControl() {
    return Control.builder(tableModel.selectionModel()::moveSelectionDown)
            .smallIcon(FrameworkIcons.instance().down())
            .description(MESSAGES.getString("selection_down_tip"))
            .build();
  }

  private Control createMoveSelectionUpControl() {
    return Control.builder(tableModel.selectionModel()::moveSelectionUp)
            .smallIcon(FrameworkIcons.instance().up())
            .description(MESSAGES.getString("selection_up_tip"))
            .build();
  }

  private Controls createColumnControls() {
    Controls.Builder builder = Controls.builder()
            .name(MESSAGES.getString("columns"));
    control(ControlCode.SELECT_COLUMNS).ifPresent(builder::control);
    control(ControlCode.RESET_COLUMNS).ifPresent(builder::control);
    control(ControlCode.COLUMN_AUTO_RESIZE_MODE).ifPresent(builder::control);

    return builder.build();
  }

  private Control createConditionPanelControl() {
    return ToggleControl.builder(conditionPanelVisibleState)
            .name(FrameworkMessages.show())
            .build();
  }

  private Control createFilterPanelControl() {
    return ToggleControl.builder(filterPanelVisibleState)
            .name(FrameworkMessages.show())
            .build();
  }

  private Controls createCopyControls() {
    return Controls.builder()
            .name(Messages.copy())
            .smallIcon(FrameworkIcons.instance().copy())
            .controls(createCopyCellControl(), createCopyTableRowsWithHeaderControl())
            .build();
  }

  private Control createCopyCellControl() {
    return Control.builder(table::copySelectedCell)
            .name(FrameworkMessages.copyCell())
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .build();
  }

  private Control createCopyTableRowsWithHeaderControl() {
    return Control.builder(table::copyRowsAsTabDelimitedString)
            .name(FrameworkMessages.copyTableWithHeader())
            .build();
  }

  private boolean includeEditSelectedControls() {
    return !editableAttributes.empty() &&
            !tableModel.editModel().readOnly().get() &&
            tableModel.editModel().updateEnabled().get();
  }

  private boolean includeDeleteSelectedControl() {
    return !tableModel.editModel().readOnly().get() && tableModel.editModel().deleteEnabled().get();
  }

  private FilteredTable<Entity, Attribute<?>> createTable() {
    return FilteredTable.builder(tableModel)
            .cellRendererFactory(new EntityTableCellRendererFactory())
            .onBuild(filteredTable -> filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT))
            .build();
  }

  private Control createConditionRefreshControl() {
    return Control.builder(tableModel::refresh)
            .enabled(tableModel.conditionChanged())
            .smallIcon(FrameworkIcons.instance().refreshRequired())
            .build();
  }

  private <T> ComponentValue<T, ? extends JComponent> cellEditorComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) cellEditorComponentFactories.computeIfAbsent(attribute, a ->
            new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).componentValue(attribute, tableModel.editModel(), initialValue);
  }

  private JToolBar createRefreshButtonToolBar() {
    if (conditionRefreshControl == null) {
      conditionRefreshControl = createConditionRefreshControl();
    }
    KeyEvents.builder(VK_F5)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(conditionRefreshControl)
            .enable(this);

    return toolBar()
            .action(conditionRefreshControl)
            .focusable(false)
            .floatable(false)
            .rollover(false)
            .visible(false)//made visible when condition panel is visible
            .onBuild(toolBar -> toolBar.getComponentAtIndex(0).setFocusable(false))
            .build();
  }

  private FilteredTableConditionPanel<Attribute<?>> createConditionPanel(ColumnConditionPanel.Factory<Attribute<?>> conditionPanelFactory) {
    return conditionPanelFactory == null || !includeConditionPanel ? null : filteredTableConditionPanel(tableModel.conditionModel(), tableModel.columnModel(), conditionPanelFactory);
  }

  private JScrollPane createConditionPanelScrollPane() {
    return conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
  }

  private JScrollPane createFilterPanelScrollPane() {
    return filterPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, filterPanel);
  }

  private FilteredTableColumnComponentPanel<Attribute<?>, JPanel> createSummaryPanel() {
    Map<Attribute<?>, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
    if (columnSummaryPanels.isEmpty()) {
      return null;
    }

    return filteredTableColumnComponentPanel(tableModel.columnModel(), columnSummaryPanels);
  }

  private JScrollPane createSummaryPanelScrollPane() {
    if (summaryPanel == null) {
      return null;
    }

    return createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
  }

  private JPanel createTablePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tableScrollPane, BorderLayout.CENTER);
    if (conditionPanelScrollPane != null) {
      panel.add(conditionPanelScrollPane, BorderLayout.NORTH);
    }
    JPanel tableSouthPanel = new JPanel(new BorderLayout());
    if (summaryPanelScrollPane != null) {
      tableSouthPanel.add(summaryPanelScrollPane, BorderLayout.NORTH);
    }
    if (filterPanelScrollPane != null) {
      tableSouthPanel.add(filterPanelScrollPane, BorderLayout.CENTER);
    }
    panel.add(tableSouthPanel, BorderLayout.SOUTH);

    return panel;
  }

  private void bindEvents() {
    if (INCLUDE_ENTITY_MENU.get()) {
      KeyEvents.builder(VK_V)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .action(Control.control(this::showEntityMenu))
              .enable(table);
    }
    control(ControlCode.DELETE_SELECTED).ifPresent(control ->
            KeyEvents.builder(VK_DELETE)
                    .action(control)
                    .enable(table));
    conditionPanelVisibleState.addDataListener(this::setConditionPanelVisibleInternal);
    filterPanelVisibleState.addDataListener(this::setFilterPanelVisibleInternal);
    summaryPanelVisibleState.addDataListener(this::setSummaryPanelVisibleInternal);
    tableModel.conditionModel().addChangeListener(this::onConditionChanged);
    tableModel.refresher().observer().addDataListener(this::onRefreshingChanged);
    tableModel.refresher().addRefreshFailedListener(this::onException);
    tableModel.editModel().addInsertUpdateOrDeleteListener(table::repaint);
    if (conditionPanel != null) {
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(conditionRefreshControl)
              .enable(conditionPanel);
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
      addRefreshOnEnterControl(tableModel.columnModel().columns(), conditionPanel, conditionRefreshControl);
      conditionPanel.advanced().addDataListener(advanced -> {
        if (conditionPanelVisibleState.get()) {
          revalidate();
        }
      });
    }
    if (filterPanel != null) {
      filterPanel.addFocusGainedListener(table::scrollToColumn);
      filterPanel.advanced().addDataListener(advanced -> {
        if (filterPanelVisibleState.get()) {
          revalidate();
        }
      });
    }
  }

  private void setConditionPanelVisibleInternal(boolean visible) {
    if (conditionPanelScrollPane != null) {
      conditionPanelScrollPane.setVisible(visible);
      refreshButtonToolBar.setVisible(refreshButtonVisible == RefreshButtonVisible.ALWAYS || visible);
      revalidate();
    }
  }

  private void setFilterPanelVisibleInternal(boolean visible) {
    if (filterPanelScrollPane != null) {
      filterPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void setSummaryPanelVisibleInternal(boolean visible) {
    if (summaryPanelScrollPane != null) {
      summaryPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void setupComponents() {
    if (conditionRefreshControl == null) {
      conditionRefreshControl = createConditionRefreshControl();
    }
    if (conditionPanel == null) {
      conditionPanel = configureHorizontalAlignment(createConditionPanel(conditionPanelFactory));
    }
    tableScrollPane = new JScrollPane(table);
    conditionPanelScrollPane = createConditionPanelScrollPane();
    if (includeFilterPanel) {
      filterPanel = configureHorizontalAlignment(table.filterPanel());
      filterPanelScrollPane = createFilterPanelScrollPane();
    }
    summaryPanel = createSummaryPanel();
    summaryPanelScrollPane = createSummaryPanelScrollPane();
    tablePanel = createTablePanel();
    if (refreshButtonToolBar == null) {
      refreshButtonToolBar = createRefreshButtonToolBar();
    }
    conditionPanelVisibleState.addValidator(new PanelAvailableValidator(conditionPanel, "condition"));
    filterPanelVisibleState.addValidator(new PanelAvailableValidator(filterPanel, "filter"));
    summaryPanelVisibleState.addValidator(new PanelAvailableValidator(summaryPanel, "summary"));
  }

  private void setupControls() {
    if (includeDeleteSelectedControl()) {
      controls.putIfAbsent(ControlCode.DELETE_SELECTED, createDeleteSelectedControl());
    }
    if (includeEditSelectedControls()) {
      controls.putIfAbsent(ControlCode.EDIT_SELECTED, createEditSelectedControls());
    }
    if (includeClearControl) {
      controls.putIfAbsent(ControlCode.CLEAR, createClearControl());
    }
    controls.putIfAbsent(ControlCode.REFRESH, createRefreshControl());
    controls.putIfAbsent(ControlCode.SELECT_COLUMNS, columnSelection == ColumnSelection.DIALOG ?
            table.createSelectColumnsControl() : table.createToggleColumnsControls());
    controls.putIfAbsent(ControlCode.RESET_COLUMNS, table.createResetColumnsControl());
    controls.putIfAbsent(ControlCode.COLUMN_AUTO_RESIZE_MODE, table.createAutoResizeModeControl());
    if (includeViewDependenciesControl()) {
      controls.putIfAbsent(ControlCode.VIEW_DEPENDENCIES, createViewDependenciesControl());
    }
    if (summaryPanelScrollPane != null) {
      controls.putIfAbsent(ControlCode.TOGGLE_SUMMARY_PANEL, createToggleSummaryPanelControl());
    }
    if (includeConditionPanel && conditionPanel != null) {
      controls.putIfAbsent(ControlCode.CONDITION_PANEL_VISIBLE, createConditionPanelControl());
      controls.putIfAbsent(ControlCode.TOGGLE_CONDITION_PANEL, createToggleConditionPanelControl());
      controls.put(ControlCode.SELECT_CONDITION_PANEL, Control.control(this::selectConditionPanel));
    }
    if (includeFilterPanel && filterPanel != null) {
      controls.putIfAbsent(ControlCode.FILTER_PANEL_VISIBLE, createFilterPanelControl());
      controls.putIfAbsent(ControlCode.TOGGLE_FILTER_PANEL, createToggleFilterPanelControl());
      controls.put(ControlCode.SELECT_FILTER_PANEL, Control.control(this::selectFilterPanel));
    }
    controls.putIfAbsent(ControlCode.CLEAR_SELECTION, createClearSelectionControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_UP, createMoveSelectionDownControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_DOWN, createMoveSelectionUpControl());
    controls.putIfAbsent(ControlCode.COPY_TABLE_DATA, createCopyControls());
    if (includeSelectionModeControl) {
      controls.putIfAbsent(ControlCode.SELECTION_MODE, table.createSingleSelectionModeControl());
    }
    controls.put(ControlCode.REQUEST_TABLE_FOCUS, Control.control(table()::requestFocus));
    controls.put(ControlCode.CONFIGURE_COLUMNS, createColumnControls());
  }

  private boolean includeViewDependenciesControl() {
    return tableModel.entities().definitions().stream()
            .flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
            .filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
            .anyMatch(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(tableModel.entityType()));
  }

  private void setupTable() {
    tableModel.columnModel().columns().forEach(this::configureColumn);
    if (includePopupMenu) {
      addTablePopupMenu();
    }
  }

  private void configureColumn(FilteredTableColumn<Attribute<?>> column) {
    TableCellEditor tableCellEditor = createTableCellEditor(column.getIdentifier());
    if (tableCellEditor != null) {
      column.setCellEditor(tableCellEditor);
    }
    column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
  }

  private void addTablePopupMenu() {
    Controls popupControls = createPopupMenuControls(additionalPopupControls);
    if (popupControls == null || popupControls.empty()) {
      return;
    }

    JPopupMenu popupMenu = menu(popupControls).createPopupMenu();
    table.setComponentPopupMenu(popupMenu);
    tableScrollPane.setComponentPopupMenu(popupMenu);
    KeyEvents.builder(VK_G)
            .modifiers(CTRL_DOWN_MASK)
            .action(Control.control(() -> {
              Point location = popupLocation(table);
              popupMenu.show(table, location.x, location.y);
            }))
            .enable(table);
  }

  private void throwIfInitialized() {
    if (initialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private void addConditionControls(Controls popupControls) {
    Controls conditionControls = Controls.builder()
            .name(FrameworkMessages.search())
            .smallIcon(FrameworkIcons.instance().search())
            .build();
    control(ControlCode.CONDITION_PANEL_VISIBLE).ifPresent(conditionControls::add);
    Controls conditionPanelControls = conditionPanel.controls();
    if (conditionPanelControls.notEmpty()) {
      conditionControls.addAll(conditionPanelControls);
      conditionControls.addSeparator();
    }
    conditionControls.add(ToggleControl.builder(tableModel.conditionRequired())
            .name(MESSAGES.getString("require_query_condition"))
            .description(MESSAGES.getString("require_query_condition_description"))
            .build());
    if (conditionControls.notEmpty()) {
      popupControls.add(conditionControls);
    }
  }

  private void addFilterControls(Controls popupControls) {
    Controls filterControls = Controls.builder()
            .name(FrameworkMessages.filter())
            .smallIcon(FrameworkIcons.instance().filter())
            .build();
    control(ControlCode.FILTER_PANEL_VISIBLE).ifPresent(filterControls::add);
    Controls filterPanelControls = filterPanel.controls();
    if (filterPanelControls.notEmpty()) {
      filterControls.addAll(filterPanelControls);
    }
    if (filterControls.notEmpty()) {
      popupControls.add(filterControls);
    }
  }

  private void showEntityMenu() {
    Point location = popupLocation(table);
    tableModel.selectionModel().selectedItem().ifPresent(selected ->
            new EntityPopupMenu(selected.copy(), tableModel.connectionProvider().connection()).show(table, location.x, location.y));
  }

  private void onConditionChanged() {
    table.getTableHeader().repaint();
    table.repaint();
  }

  private void onRefreshingChanged(boolean refreshing) {
    if (refreshing) {
      setCursor(Cursors.WAIT);
    }
    else {
      setCursor(Cursors.DEFAULT);
    }
  }

  private void toggleConditionPanel(JScrollPane scrollPane, State advancedState, State visibleState) {
    if (scrollPane != null && scrollPane.isVisible()) {
      if (advancedState.get()) {
        boolean parentOfFocusOwner = parentOfType(JScrollPane.class,
                getCurrentKeyboardFocusManager().getFocusOwner()) == scrollPane;
        visibleState.set(false);
        if (parentOfFocusOwner) {
          table.requestFocusInWindow();
        }
      }
      else {
        advancedState.set(true);
      }
    }
    else {
      advancedState.set(false);
      visibleState.set(true);
    }
  }

  private boolean nonUpdatableForeignKey(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;

      return foreignKey.references().stream()
              .map(ForeignKey.Reference::column)
              .map(referenceAttribute -> tableModel.entityDefinition().columns().definition(referenceAttribute))
              .filter(ColumnDefinition.class::isInstance)
              .map(ColumnDefinition.class::cast)
              .noneMatch(ColumnDefinition::updatable);
    }

    return false;
  }

  private FilteredTableConditionPanel<Attribute<?>> configureHorizontalAlignment(FilteredTableConditionPanel<Attribute<?>> tableConditionPanel) {
    if (tableConditionPanel != null) {
      tableConditionPanel.conditionPanels().forEach(this::configureHorizontalAlignment);
    }

    return tableConditionPanel;
  }

  private void configureHorizontalAlignment(ColumnConditionPanel<Attribute<?>, ?> columnConditionPanel) {
    configureHorizontalAlignment(columnConditionPanel,
            tableModel.columnModel().column(columnConditionPanel.model().columnIdentifier()).getCellRenderer());
  }

  private static void configureHorizontalAlignment(ColumnConditionPanel<Attribute<?>, ?> columnConditionPanel,
                                                   TableCellRenderer cellRenderer) {
    if (cellRenderer instanceof DefaultTableCellRenderer) {
      int horizontalAlignment = ((DefaultTableCellRenderer) cellRenderer).getHorizontalAlignment();
      JComponent component = columnConditionPanel.equalField();
      if (component instanceof JTextField) {
        ((JTextField) component).setHorizontalAlignment(horizontalAlignment);
      }
      component = columnConditionPanel.lowerBoundField();
      if (component instanceof JTextField) {
        ((JTextField) component).setHorizontalAlignment(horizontalAlignment);
      }
      component = columnConditionPanel.upperBoundField();
      if (component instanceof JTextField) {
        ((JTextField) component).setHorizontalAlignment(horizontalAlignment);
      }
    }
  }

  private static final void selectConditionPanel(FilteredTableConditionPanel<Attribute<?>> tableConditionPanel,
                                                 JScrollPane conditionPanelScrollPane, State conditionPanelVisibleState,
                                                 SwingEntityTableModel tableModel, JComponent dialogOwner, String dialogTitle) {
    if (tableConditionPanel != null) {
      if (!(conditionPanelScrollPane != null && conditionPanelScrollPane.isVisible())) {
        conditionPanelVisibleState.set(true);
      }
      List<AttributeDefinition<?>> attributeDefinitions = tableConditionPanel.conditionPanels().stream()
              .filter(panel -> tableModel.columnModel().visible(panel.model().columnIdentifier()).get())
              .map(panel -> tableModel.entityDefinition().attributes().definition(panel.model().columnIdentifier()))
              .sorted(AttributeDefinition.definitionComparator())
              .collect(toList());
      if (attributeDefinitions.size() == 1) {
        tableConditionPanel.conditionPanel(attributeDefinitions.get(0).attribute())
                .ifPresent(ColumnConditionPanel::requestInputFocus);
      }
      else if (!attributeDefinitions.isEmpty()) {
        Dialogs.selectionDialog(attributeDefinitions)
                .owner(dialogOwner)
                .title(dialogTitle)
                .selectSingle()
                .flatMap(attributeDefinition -> tableConditionPanel.conditionPanel(attributeDefinition.attribute()))
                .ifPresent(ColumnConditionPanel::requestInputFocus);
      }
    }
  }

  private static GridBagConstraints createHorizontalFillConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;

    return constraints;
  }

  private static void addRefreshOnEnterControl(Collection<FilteredTableColumn<Attribute<?>>> columns,
                                               FilteredTableConditionPanel<Attribute<?>> tableConditionPanel,
                                               Control refreshControl) {
    columns.stream()
            .map(column -> tableConditionPanel.conditionPanel(column.getIdentifier()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(columnConditionPanel -> {
              enableRefreshOnEnterControl(columnConditionPanel.operatorComboBox(), refreshControl);
              enableRefreshOnEnterControl(columnConditionPanel.equalField(), refreshControl);
              enableRefreshOnEnterControl(columnConditionPanel.lowerBoundField(), refreshControl);
              enableRefreshOnEnterControl(columnConditionPanel.upperBoundField(), refreshControl);
            });
  }

  private static void enableRefreshOnEnterControl(JComponent component, Control refreshControl) {
    if (component instanceof JComboBox) {
      new ComboBoxEnterPressedAction((JComboBox<?>) component, refreshControl);
    }
    else if (component instanceof TemporalField) {
      ((TemporalField<?>) component).addActionListener(refreshControl);
    }
  }

  private static void addAdditionalControls(Controls popupControls, List<Controls> additionalPopupControls) {
    additionalPopupControls.forEach(controls -> {
      if (nullOrEmpty(controls.getName())) {
        popupControls.addAll(controls);
      }
      else {
        popupControls.add(controls);
      }
      popupControls.addSeparator();
    });
  }

  private static Map<Attribute<?>, JPanel> createColumnSummaryPanels(FilteredTableModel<?, Attribute<?>> tableModel) {
    Map<Attribute<?>, JPanel> components = new HashMap<>();
    tableModel.columnModel().columns().forEach(column ->
            tableModel.summaryModel().summaryModel(column.getIdentifier())
                    .ifPresent(columnSummaryModel ->
                            components.put(column.getIdentifier(), columnSummaryPanel(columnSummaryModel))));

    return components;
  }

  private static JScrollPane createHiddenLinkedScrollPane(JScrollPane parentScrollPane, JPanel panelToScroll) {
    return Components.scrollPane(panelToScroll)
            .horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
            .verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
            .visible(false)
            .onBuild(scrollPane -> linkBoundedRangeModels(
                    parentScrollPane.getHorizontalScrollBar().getModel(),
                    scrollPane.getHorizontalScrollBar().getModel()))
            .build();
  }

  private static Point popupLocation(JTable table) {
    Rectangle visibleRect = table.getVisibleRect();
    int x = visibleRect.x + visibleRect.width / 2;
    int y = table.getSelectionModel().isSelectionEmpty() ?
            visibleRect.y + visibleRect.height / 2 :
            table.getCellRect(table.getSelectedRow(), table.getSelectedColumn(), true).y;

    return new Point(x, y + table.getRowHeight() / 2);
  }

  private final class EntityTableCellRendererFactory implements FilteredTableCellRendererFactory<Attribute<?>> {

    @Override
    public TableCellRenderer tableCellRenderer(FilteredTableColumn<Attribute<?>> column) {
      return createTableCellRenderer(column.getIdentifier());
    }
  }

  private final class HeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer wrappedRenderer;

    private HeaderRenderer(TableCellRenderer wrappedRenderer) {
      this.wrappedRenderer = wrappedRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      Component component = wrappedRenderer == null ?
              table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) :
              wrappedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      FilteredTableColumn<Attribute<?>> tableColumn = tableModel.columnModel().getColumn(column);
      TableCellRenderer renderer = tableColumn.getCellRenderer();
      boolean useBoldFont = renderer instanceof FilteredTableCellRenderer
              && ((FilteredTableCellRenderer) renderer).columnShadingEnabled()
              && tableModel.conditionModel().enabled(tableColumn.getIdentifier());
      Font defaultFont = component.getFont();
      component.setFont(useBoldFont ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

      return component;
    }
  }

  private final class DeleteConfirmer implements Confirmer {

    @Override
    public boolean confirm(JComponent dialogOwner) {
      return confirm(dialogOwner, FrameworkMessages.confirmDeleteSelected(
              tableModel.selectionModel().selectionCount()), FrameworkMessages.delete());
    }
  }

  private final class EditMenuAttributeValidator implements Value.Validator<Set<Attribute<?>>> {

    @Override
    public void validate(Set<Attribute<?>> attributes) {
      throwIfInitialized();
      //validate that the attributes exists
      attributes.forEach(attribute -> tableModel.entityDefinition().attributes().definition(attribute));
    }
  }

  private static final class PanelAvailableValidator implements Value.Validator<Boolean> {

    private final JPanel panel;
    private final String panelType;

    private PanelAvailableValidator(JPanel panel, String panelType) {
      this.panel = panel;
      this.panelType = panelType;
    }

    @Override
    public void validate(Boolean visible) throws IllegalArgumentException {
      if (visible && panel == null) {
        throw new IllegalArgumentException("No " + panelType + " panel available");
      }
    }
  }

  private static final class DefaultStatusMessage implements Function<SwingEntityTableModel, String> {

    private static final NumberFormat STATUS_MESSAGE_NUMBER_FORMAT = NumberFormat.getIntegerInstance();

    @Override
    public String apply(SwingEntityTableModel tableModel) {
      int rowCount = tableModel.getRowCount();
      if (rowCount == 0) {
        return "";
      }
      int filteredCount = tableModel.filteredCount();
      int selectionCount = tableModel.selectionModel().selectionCount();
      StringBuilder builder = new StringBuilder();
      if (tableModel.limit().equalTo(tableModel.getRowCount())) {
        builder.append(MESSAGES.getString("limited_to")).append(" ");
      }
      builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(rowCount));
      if (selectionCount > 0 || filteredCount > 0) {
        builder.append(" (");
        if (selectionCount > 0) {
          builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(selectionCount)).append(" ").append(MESSAGES.getString("selected"));
        }
        if (filteredCount > 0) {
          if (selectionCount > 0) {
            builder.append(" - ");
          }
          builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(filteredCount)).append(" ").append(MESSAGES.getString("hidden"));
        }
        builder.append(")");
      }

      return builder.toString();
    }
  }

  private final class StatusPanel extends JPanel {

    private static final String STATUS = "status";
    private static final String REFRESHING = "refreshing";

    private final Value<String> statusMessage = Value.value("", "");
    private final Value<Function<SwingEntityTableModel, String>> statusMessageFunction =
            Value.value(DEFAULT_STATUS_MESSAGE, DEFAULT_STATUS_MESSAGE);
    private final State showRefreshProgressBar = State.state(SHOW_REFRESH_PROGRESS_BAR.get());

    private StatusPanel() {
      super(new CardLayout());
      add(Components.label(statusMessage)
              .horizontalAlignment(SwingConstants.CENTER)
              .build(), STATUS);
      add(createRefreshingProgressPanel(), REFRESHING);
      CardLayout layout = (CardLayout) getLayout();
      tableModel.refresher().observer().addDataListener(isRefreshing -> {
        if (showRefreshProgressBar.get()) {
          layout.show(this, isRefreshing ? REFRESHING : STATUS);
        }
      });
      Runnable statusListener = this::updateStatusMessage;
      statusMessageFunction.addListener(statusListener);
      tableModel.selectionModel().addSelectionListener(statusListener);
      tableModel.addDataChangedListener(statusListener);
      updateStatusMessage();
    }

    private JPanel createRefreshingProgressPanel() {
      return Components.panel(new GridBagLayout())
              .add(Components.progressBar()
                      .indeterminate(true)
                      .string(MESSAGES.getString(REFRESHING))
                      .stringPainted(true)
                      .build(), createHorizontalFillConstraints())
              .build();
    }

    private void updateStatusMessage() {
      statusMessage.set(statusMessageFunction.get().apply(tableModel));
    }
  }
}
