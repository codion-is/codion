/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.summary.SummaryModel.SummaryValues;
import is.codion.common.model.summary.TableSummaryModel;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableSelection;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.FieldFactory;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.component.table.FilterTableSortModel.ColumnSortOrder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.item.Item.item;
import static is.codion.common.model.summary.TableSummaryModel.tableSummaryModel;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.itemComboBox;
import static is.codion.swing.common.ui.component.table.FilterTable.ControlKeys.*;
import static is.codion.swing.common.ui.component.table.FilterTableSortModel.nextSortOrder;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.KeyStroke.getKeyStrokeForEvent;

/**
 * A JTable implementation for {@link FilterTableModel}.
 * Note that for the table header to display you must add this table to a JScrollPane.
 * For instances use the builder {@link #builder(FilterTableModel, List)}
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @see #builder(FilterTableModel, List)
 */
public final class FilterTable<R, C> extends JTable {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterTable.class, getBundle(FilterTable.class.getName()));

	/**
	 * Specifies the default table column resize mode for tables in the application
	 * <ul>
	 * <li>Value type: Integer (JTable.AUTO_RESIZE_*)
	 * <li>Default value: {@link JTable#AUTO_RESIZE_OFF}
	 * </ul>
	 */
	public static final PropertyValue<Integer> AUTO_RESIZE_MODE =
					Configuration.integerValue(FilterTable.class.getName() + ".autoResizeMode", AUTO_RESIZE_OFF);

	/**
	 * Specifies whether columns can be rearranged in tables
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING =
					Configuration.booleanValue(FilterTable.class.getName() + ".allowColumnReordering", true);

	/**
	 * Specifies whether the table resizes the row being edited to fit the editor component. Only applicable to {@link FilterTableCellEditor}.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> RESIZE_ROW_TO_FIT_EDITOR =
					Configuration.booleanValue(FilterTable.class.getName() + ".resizeRowToFitEditor", true);

	/**
	 * The controls.
	 */
	public static final class ControlKeys {

		/**
		 * Moves the selected column to the left.<br>
		 * Default key stroke: CTRL-SHIFT-LEFT
		 */
		public static final ControlKey<CommandControl> MOVE_COLUMN_LEFT = CommandControl.key("moveColumnLeft", keyStroke(VK_LEFT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Moves the selected column to the right.<br>
		 * Default key stroke: CTRL-SHIFT-RIGHT
		 */
		public static final ControlKey<CommandControl> MOVE_COLUMN_RIGHT = CommandControl.key("moveColumnRight", keyStroke(VK_RIGHT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Decreases the size of the selected column.<br>
		 * Default key stroke: CTRL-SUBTRACT
		 */
		public static final ControlKey<CommandControl> DECREASE_COLUMN_SIZE = CommandControl.key("decreaseColumnSize", keyStroke(VK_SUBTRACT, CTRL_DOWN_MASK));
		/**
		 * Increases the size of the selected column.<br>
		 * Default key stroke: CTRL-ADD
		 */
		public static final ControlKey<CommandControl> INCREASE_COLUMN_SIZE = CommandControl.key("increaseColumnSize", keyStroke(VK_ADD, CTRL_DOWN_MASK));
		/**
		 * Copy the selected cell contents to the clipboard.<br>
		 * Default key stroke: CTRL-ALT-C
		 */
		public static final ControlKey<CommandControl> COPY_CELL = CommandControl.key("copyCell", keyStroke(VK_C, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Toggles the sort on the selected column.<br>
		 * Default key stroke: ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_SORT_COLUMN = CommandControl.key("toggleSortColumn", keyStroke(VK_DOWN, ALT_DOWN_MASK));
		/**
		 * Toggles the sort on the selected column adding it to any already sorted columns.<br>
		 * Default key stroke: ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_SORT_COLUMN_ADD = CommandControl.key("toggleSortColumnAdd", keyStroke(VK_UP, ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	/**
	 * Specifies whether to center the scrolled to row and or column.
	 */
	public enum CenterOnScroll {
		/**
		 * Centers the selected column, if possible.
		 */
		COLUMN,
		/**
		 * Centers the selected row, if possible.
		 */
		ROW,
		/**
		 * Centers both the selected column and row, if possible.
		 */
		BOTH,
		/**
		 * Centers neither the selected column nor row.
		 */
		NEITHER
	}

	private static final String SELECT = "select";
	private static final String SELECT_COLUMNS = "select_columns";
	private static final String RESET = "reset";
	private static final String RESET_COLUMNS_DESCRIPTION = "reset_columns_description";
	private static final String SINGLE_SELECTION_MODE = "single_selection_mode";
	private static final String AUTO_RESIZE = "auto_resize";
	private static final String TABLE_CELL_EDITOR = "tableCellEditor";
	private static final List<Item<Integer>> AUTO_RESIZE_MODES = asList(
					item(AUTO_RESIZE_OFF, MESSAGES.getString("resize_off")),
					item(AUTO_RESIZE_NEXT_COLUMN, MESSAGES.getString("resize_next_column")),
					item(AUTO_RESIZE_SUBSEQUENT_COLUMNS, MESSAGES.getString("resize_subsequent_columns")),
					item(AUTO_RESIZE_LAST_COLUMN, MESSAGES.getString("resize_last_column")),
					item(AUTO_RESIZE_ALL_COLUMNS, MESSAGES.getString("resize_all_columns")));
	private static final int SEARCH_FIELD_MINIMUM_WIDTH = 100;
	private static final int COLUMN_RESIZE_AMOUNT = 10;

	private final FilterTableModel<R, C> tableModel;
	private final FilterTableSearchModel searchModel;
	private final FilterTableSortModel<R, C> sortModel;
	private final TableSummaryModel<C> summaryModel;

	private final TableConditionPanel.Factory<C> filterPanelFactory;
	private final FieldFactory filterFieldFactory;
	private final Event<MouseEvent> doubleClick = Event.event();
	private final Value<Action> doubleClickAction;
	private final State sortingEnabled;
	private final State scrollToSelectedItem;
	private final Value<CenterOnScroll> centerOnScroll;

	private final ControlMap controlMap;

	private TableConditionPanel<C> filterPanel;
	private JTextField searchField;

	private FilterTable(DefaultBuilder<R, C> builder) {
		super(builder.tableModel, new DefaultFilterTableColumnModel<>(builder.columns),
						builder.tableModel.selection());
		this.tableModel = builder.tableModel;
		this.sortModel = new DefaultFilterTableSortModel<>(tableModel.columns());
		this.searchModel = new DefaultFilterTableSearchModel<>(tableModel, columnModel());
		this.summaryModel = tableSummaryModel(builder.summaryValuesFactory == null ?
						new DefaultSummaryValuesFactory() : builder.summaryValuesFactory);
		this.filterPanelFactory = builder.filterPanelFactory;
		this.filterFieldFactory = builder.filterFieldFactory;
		this.centerOnScroll = Value.builder()
						.nonNull(CenterOnScroll.NEITHER)
						.value(builder.centerOnScroll)
						.build();
		this.doubleClickAction = Value.value(builder.doubleClickAction);
		this.scrollToSelectedItem = State.state(builder.scrollToSelectedItem);
		this.sortingEnabled = State.state(builder.sortingEnabled);
		this.controlMap = builder.controlMap;
		this.controlMap.control(COPY_CELL).set(createCopyCellControl());
		this.controlMap.control(TOGGLE_SORT_COLUMN).set(createToggleSortColumnControl());
		this.controlMap.control(TOGGLE_SORT_COLUMN_ADD).set(createToggleSortColumnAddControl());
		filters().view().set(builder.filterView);
		autoStartsEdit(builder.autoStartsEdit);
		setSelectionMode(builder.selectionMode);
		setAutoResizeMode(builder.autoResizeMode);
		configureColumns(builder.cellRenderers, builder.cellRendererFactory, builder.cellEditors, builder.cellEditorFactory);
		configureTableHeader(builder.columnReorderingAllowed, builder.columnResizingAllowed);
		bindEvents(builder.columnReorderingAllowed, builder.columnResizingAllowed);
		if (builder.resizeRowToFitEditor) {
			addPropertyChangeListener(TABLE_CELL_EDITOR, new ResizeRowToFitEditor());
		}
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(getTableHeader(), searchField, filterPanel);
		Utilities.updateUI(columnModel().hidden().columns().stream()
						.flatMap(FilterTable::columnComponents)
						.collect(toList()));
	}

	@Override
	public FilterTableModel<R, C> getModel() {
		return (FilterTableModel<R, C>) super.getModel();
	}

	/**
	 * @return the table model
	 */
	public FilterTableModel<R, C> model() {
		return getModel();
	}

	@Override
	public FilterTableColumnModel<C> getColumnModel() {
		return (FilterTableColumnModel<C>) super.getColumnModel();
	}

	/**
	 * @return the column model
	 */
	public FilterTableColumnModel<C> columnModel() {
		return getColumnModel();
	}

	@Override
	public void setModel(TableModel dataModel) {
		if (this.tableModel != null) {
			throw new IllegalStateException("Table model has already been set");
		}
		if (!(dataModel instanceof FilterTableModel)) {
			throw new IllegalArgumentException("FilterTable model must be a FilterTableModel instance");
		}
		List<R> selection = ((FilterTableModel<R, C>) dataModel).selection().items().get();
		super.setModel(dataModel);
		if (!selection.isEmpty()) {
			((FilterTableModel<R, C>) dataModel).selection().items().set(selection);
		}
	}

	@Override
	public void setColumnModel(TableColumnModel columnModel) {
		if (this.columnModel != null) {
			throw new IllegalStateException("Column model has already been set");
		}
		if (!(columnModel instanceof FilterTableColumnModel)) {
			throw new IllegalArgumentException("FilterTable column model must be a FilterTableColumnModel instance");
		}
		super.setColumnModel(columnModel);
	}

	@Override
	public void setSelectionModel(ListSelectionModel selectionModel) {
		if (this.selectionModel != null) {
			throw new IllegalStateException("Selection model has already been set");
		}
		if (!(selectionModel instanceof TableSelection)) {
			throw new IllegalArgumentException("FilterTable selection model must be a TableSelection instance");
		}
		super.setSelectionModel(selectionModel);
	}

	/**
	 * @return the filter {@link TableConditionPanel}
	 */
	public TableConditionPanel<C> filters() {
		if (filterPanel == null) {
			filterPanel = filterPanelFactory.create(tableModel.filters(), createFilterPanels(),
							columnModel(), this::configureFilterConditionPanel);
		}

		return filterPanel;
	}

	/**
	 * @return the search model
	 */
	public FilterTableSearchModel searchModel() {
		return searchModel;
	}

	/**
	 * @return the sorting model
	 */
	public FilterTableSortModel<R, C> sortModel() {
		return sortModel;
	}

	/**
	 * @return the summary model
	 */
	public TableSummaryModel<C> summaryModel() {
		return summaryModel;
	}

	/**
	 * @return the search field
	 */
	public JTextField searchField() {
		if (searchField == null) {
			searchField = createSearchField();
		}

		return searchField;
	}

	/**
	 * The {@link ActionEvent} propagated when this action is performed, contains the associated {@link MouseEvent} as source.
	 * <pre>
	 * {@code
	 *   public void actionPerformed(ActionEvent event) {
	 *       MouseEvent mouseEvent = (MouseEvent) event.getSource();
	 *       Point location = mouseEvent.getLocationOnScreen();
	 *       // ...
	 *   }
	 * }
	 * </pre>
	 * @return the {@link Value} controlling the action to perform when a double click is performed on the table
	 */
	public Value<Action> doubleClickAction() {
		return doubleClickAction;
	}

	/**
	 * @return the {@link State} controlling whether sorting via the table header is enabled
	 */
	public State sortingEnabled() {
		return sortingEnabled;
	}

	/**
	 * @return the {@link State} controlling whether the JTable instance scrolls automatically to the coordinate
	 * of the item selected in the underlying table model
	 */
	public State scrollToSelectedItem() {
		return scrollToSelectedItem;
	}

	/**
	 * @return the {@link Value} controlling the scrolling behaviour when scrolling to the selected row/column
	 */
	public Value<CenterOnScroll> centerOnScroll() {
		return centerOnScroll;
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		tableModel.selection().setSelectionMode(selectionMode);
	}

	/**
	 * Shows a dialog for selecting which columns to display
	 */
	public void selectColumns() {
		ColumnSelectionPanel<C> columnSelectionPanel = new ColumnSelectionPanel<>(columnModel());
		Dialogs.okCancelDialog(columnSelectionPanel)
						.owner(getParent())
						.title(MESSAGES.getString(SELECT_COLUMNS))
						.onShown(dialog -> columnSelectionPanel.requestColumnPanelFocus())
						.onOk(columnSelectionPanel::applyChanges)
						.show();
	}

	/**
	 * Displays a dialog for selecting the column auto-resize mode
	 */
	public void selectAutoResizeMode() {
		FilterComboBoxModel<Item<Integer>> autoResizeComboBoxModel = createAutoResizeModeComboBoxModel();
		Dialogs.okCancelDialog(borderLayoutPanel()
										.centerComponent(itemComboBox(autoResizeComboBoxModel).build())
										.border(Borders.emptyBorder())
										.build())
						.owner(getParent())
						.title(MESSAGES.getString(AUTO_RESIZE))
						.onOk(() -> setAutoResizeMode(autoResizeComboBoxModel.selection().value().value()))
						.show();
	}

	/**
	 * Returns true if the given cell is visible.
	 * @param row the row
	 * @param column the column
	 * @return true if this table is contained in a scrollpanel and the cell with the given coordinates is visible.
	 */
	public boolean cellVisible(int row, int column) {
		JViewport viewport = Utilities.parentOfType(JViewport.class, this);

		return viewport != null && cellVisible(viewport, row, column);
	}

	/**
	 * Scrolls horizontally so that the column identified by the given identifier becomes visible.
	 * Has no effect if this table is not contained in a scrollpanel.
	 * @param identifier the column identifier
	 */
	public void scrollToColumn(C identifier) {
		requireNonNull(identifier);
		JViewport viewport = Utilities.parentOfType(JViewport.class, this);
		if (viewport != null) {
			scrollToRowColumn(viewport, rowAtPoint(viewport.getViewPosition()),
							columnModel().getColumnIndex(identifier), CenterOnScroll.NEITHER);
		}
	}

	/**
	 * Scrolls to the given coordinate. Has no effect if this table is not contained in a scrollpanel.
	 * @param row the row
	 * @param column the column
	 * @param centerOnScroll specifies whether to center the selected row and or column
	 */
	public void scrollToCoordinate(int row, int column, CenterOnScroll centerOnScroll) {
		requireNonNull(centerOnScroll);
		JViewport viewport = Utilities.parentOfType(JViewport.class, this);
		if (viewport != null) {
			scrollToRowColumn(viewport, row, column, centerOnScroll);
		}
	}

	/**
	 * Copies the contents of the selected cell to the clipboard.
	 */
	public void copySelectedCell() {
		int selectedRow = getSelectedRow();
		int selectedColumn = getSelectedColumn();
		if (selectedRow >= 0 && selectedColumn >= 0) {
			FilterTableColumn<C> column = columnModel().getColumn(selectedColumn);
			Utilities.setClipboard(model().getStringAt(selectedRow, column.identifier()));
		}
	}

	/**
	 * Copies the table data as a TAB delimited string, with header, to the clipboard.
	 * If the selection is empty, all rows are included, otherwise only selected ones.
	 */
	public void copyToClipboard() {
		Utilities.setClipboard(export()
						.delimiter('\t')
						.selected(!selectionModel.isSelectionEmpty())
						.get());
	}

	/**
	 * Copies the selected table rows as a TAB delimited string, with header, to the clipboard.
	 */
	public void copySelectedToClipboard() {
		Utilities.setClipboard(export()
						.delimiter('\t')
						.selected(true)
						.get());
	}

	/**
	 * @return a {@link Export} instance for exporting the table model data
	 */
	public Export export() {
		return new DefaultExport();
	}

	/**
	 * @return a control for showing the column selection dialog
	 */
	public CommandControl createSelectColumnsControl() {
		return Control.builder()
						.command(this::selectColumns)
						.name(MESSAGES.getString(SELECT) + "...")
						.enabled(columnModel().locked().not())
						.description(MESSAGES.getString(SELECT_COLUMNS))
						.build();
	}

	/**
	 * @return Controls containing {@link ToggleControl}s for showing/hiding columns.
	 */
	public Controls createToggleColumnsControls() {
		return Controls.builder()
						.name(MESSAGES.getString(SELECT))
						.enabled(columnModel().locked().not())
						.controls(columnModel().columns().stream()
										.sorted(new ColumnComparator())
										.map(this::createToggleColumnControl)
										.collect(toList()))
						.build();
	}

	/**
	 * @return a Control for resetting the columns to their original location and visibility
	 */
	public CommandControl createResetColumnsControl() {
		return Control.builder()
						.command(columnModel()::reset)
						.name(MESSAGES.getString(RESET))
						.enabled(columnModel().locked().not())
						.description(MESSAGES.getString(RESET_COLUMNS_DESCRIPTION))
						.build();
	}

	/**
	 * @return a Control for selecting the auto-resize mode
	 */
	public CommandControl createSelectAutoResizeModeControl() {
		return Control.builder()
						.command(this::selectAutoResizeMode)
						.name(MESSAGES.getString(AUTO_RESIZE) + "...")
						.build();
	}

	/**
	 * @return Controls containing {@link ToggleControl}s for choosing the auto resize mode.
	 */
	public Controls createToggleAutoResizeModeControls() {
		return Controls.builder()
						.name(MESSAGES.getString(AUTO_RESIZE))
						.controls(createAutoResizeModeControls())
						.build();
	}

	/**
	 * @return a ToggleControl for toggling the table selection mode (single or multiple)
	 */
	public ToggleControl createSingleSelectionModeControl() {
		return Control.builder()
						.toggle(tableModel.selection().singleSelectionMode())
						.name(MESSAGES.getString(SINGLE_SELECTION_MODE))
						.build();
	}

	/**
	 * @return a Control for copying the contents of the selected cell
	 */
	public CommandControl createCopyCellControl() {
		return Control.builder()
						.command(this::copySelectedCell)
						.name(MESSAGES.getString("copy_cell"))
						.enabled(tableModel.selection().empty().not())
						.build();
	}

	/**
	 * A convenience method for setting the client property 'JTable.autoStartsEdit'.
	 * @param autoStartsEdit the value
	 */
	public void autoStartsEdit(boolean autoStartsEdit) {
		putClientProperty("JTable.autoStartsEdit", autoStartsEdit);
	}

	/**
	 * @return an observer notified each time the table is double-clicked
	 */
	public Observer<MouseEvent> doubleClick() {
		return doubleClick.observer();
	}

	/**
	 * Instantiates a new {@link SummaryValues} instance.
	 * @param identifier the column identifier
	 * @param tableModel the table model
	 * @param format the format
	 * @param <T> the column value type
	 * @param <C> the column identifier type
	 * @return a new {@link SummaryValues} instance
	 */
	public static <T extends Number, C> SummaryValues<T> summaryValues(C identifier, FilterTableModel<?, C> tableModel, Format format) {
		return new DefaultSummaryValues<>(identifier, tableModel, format);
	}

	/**
	 * Instantiates a new {@link FilterTable.Builder} using the given model
	 * @param tableModel the table model
	 * @param columns the columns
	 * @param <R> the type representing rows
	 * @param <C> the type used to identify columns
	 * @return a new {@link FilterTable.Builder} instance
	 * @throws IllegalArgumentException in case the column identifiers are not unique
	 */
	public static <R, C> Builder<R, C> builder(FilterTableModel<R, C> tableModel,
																						 List<FilterTableColumn<C>> columns) {
		return new DefaultBuilder<>(requireNonNull(tableModel), requireNonNull(columns));
	}

	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new FilterTableHeader(columnModel);
	}

	/**
	 * Creates a JTextField for searching through this table.
	 * @return a search field
	 */
	private JTextField createSearchField() {
		Control nextResult = command(() -> selectSearchResult(false, true));
		Control selectNextResult = command(() -> selectSearchResult(true, true));
		Control previousResult = command(() -> selectSearchResult(false, false));
		Control selectPreviousResult = command(() -> selectSearchResult(true, false));
		Control requestTableFocus = command(this::requestFocusInWindow);

		return Components.stringField(searchModel.searchString())
						.minimumWidth(SEARCH_FIELD_MINIMUM_WIDTH)
						.preferredWidth(SEARCH_FIELD_MINIMUM_WIDTH)
						.selectAllOnFocusGained(true)
						.keyEvent(KeyEvents.builder(VK_ENTER)
										.action(nextResult))
						.keyEvent(KeyEvents.builder(VK_ENTER)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectNextResult))
						.keyEvent(KeyEvents.builder(VK_DOWN)
										.action(nextResult))
						.keyEvent(KeyEvents.builder(VK_DOWN)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectNextResult))
						.keyEvent(KeyEvents.builder(VK_UP)
										.action(previousResult))
						.keyEvent(KeyEvents.builder(VK_UP)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectPreviousResult))
						.keyEvent(KeyEvents.builder(VK_ESCAPE)
										.action(requestTableFocus))
						.popupMenuControls(textField -> searchFieldPopupMenuControls())
						.hint(Messages.find() + "...")
						.onTextChanged(this::onSearchTextChanged)
						.onBuild(field -> KeyEvents.builder(VK_F)
										.modifiers(CTRL_DOWN_MASK)
										.action(command(field::requestFocusInWindow))
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this))
						.build();
	}

	private void selectSearchResult(boolean addToSelection, boolean next) {
		searchResult(addToSelection, next).ifPresent(rowColumn -> {
			if (!addToSelection) {
				setColumnSelectionInterval(rowColumn.column(), rowColumn.column());
			}
			scrollToCoordinate(rowColumn.row(), rowColumn.column(), centerOnScroll.get());
		});
	}

	private Optional<RowColumn> searchResult(boolean addToSelection, boolean next) {
		if (next) {
			return addToSelection ? searchModel.selectNextResult() : searchModel.nextResult();
		}

		return addToSelection ? searchModel.selectPreviousResult() : searchModel.previousResult();
	}

	private void onSearchTextChanged(String searchText) {
		if (!searchText.isEmpty()) {
			searchModel.nextResult();
		}
	}

	private void toggleColumnSorting(int selectedColumn, boolean add) {
		if (sortingEnabled.get() && selectedColumn != -1) {
			ColumnSortOrder<C> columnSortOrder = sortModel.columnSortOrder(columnModel().getColumn(selectedColumn).identifier());
			if (add) {
				sortModel.addSortOrder(columnSortOrder.identifier(), nextSortOrder(columnSortOrder.sortOrder()));
			}
			else {
				sortModel.setSortOrder(columnSortOrder.identifier(), nextSortOrder(columnSortOrder.sortOrder()));
			}
		}
	}

	private Controls searchFieldPopupMenuControls() {
		return Controls.builder()
						.control(Control.builder()
										.toggle(searchModel.caseSensitive())
										.name(MESSAGES.getString("case_sensitive_search")))
						.control(Control.builder()
										.toggle(searchModel.regularExpression())
										.name(MESSAGES.getString("regular_expression_search")))
						.build();
	}

	private boolean cellVisible(JViewport viewport, int row, int column) {
		Rectangle cellRect = getCellRect(row, column, true);
		Point viewPosition = viewport.getViewPosition();
		cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

		return new Rectangle(viewport.getExtentSize()).contains(cellRect);
	}

	private void scrollToRowColumn(JViewport viewport, int row, int column, CenterOnScroll centerOnScroll) {
		Rectangle cellRectangle = getCellRect(row, column, true);
		Rectangle viewRectangle = viewport.getViewRect();
		cellRectangle.setLocation(cellRectangle.x - viewRectangle.x, cellRectangle.y - viewRectangle.y);
		int x = cellRectangle.x;
		int y = cellRectangle.y;
		if (centerOnScroll != CenterOnScroll.NEITHER) {
			if (centerOnScroll == CenterOnScroll.COLUMN || centerOnScroll == CenterOnScroll.BOTH) {
				x = (viewRectangle.width - cellRectangle.width) / 2;
				if (cellRectangle.x < x) {
					x = -x;
				}
			}
			if (centerOnScroll == CenterOnScroll.ROW || centerOnScroll == CenterOnScroll.BOTH) {
				y = (viewRectangle.height - cellRectangle.height) / 2;
				if (cellRectangle.y < y) {
					y = -y;
				}
			}
			cellRectangle.translate(x, y);
		}
		viewport.scrollRectToVisible(cellRectangle);
	}

	private ToggleControl createToggleColumnControl(FilterTableColumn<C> column) {
		return Control.builder()
						.toggle(columnModel().visible(column.identifier()))
						.name(String.valueOf(column.getHeaderValue()))
						.description(column.toolTipText().orElse(null))
						.build();
	}

	private void configureColumns(Map<C, FilterTableCellRenderer<?>> cellRenderers,
																FilterTableCellRenderer.Factory<R, C> cellRendererFactory,
																Map<C, FilterTableCellEditor<?>> cellEditors,
																FilterTableCellEditor.Factory<C> cellEditorFactory) {
		columnModel().columns().stream()
						.filter(column -> column.getCellRenderer() == null)
						.forEach(column -> column.setCellRenderer(cellRenderers.getOrDefault(column.identifier(),
										cellRendererFactory.create(column.identifier(), tableModel))));
		columnModel().columns().stream()
						.filter(column -> column.getHeaderRenderer() == null)
						.forEach(column -> column.setHeaderRenderer(new FilterTableHeaderRenderer<>(this, column)));
		columnModel().columns().stream()
						.filter(column -> column.getCellEditor() == null)
						.forEach(column -> {
							FilterTableCellEditor<?> cellEditor = cellEditors.get(column.identifier());
							if (cellEditor != null) {
								column.setCellEditor(cellEditor);
							}
							else if (cellEditorFactory != null) {
								cellEditorFactory.create(column.identifier()).ifPresent(column::setCellEditor);
							}
						});
	}

	private void configureTableHeader(boolean reorderingAllowed, boolean columnResizingAllowed) {
		JTableHeader header = getTableHeader();
		header.setFocusable(false);
		header.setReorderingAllowed(reorderingAllowed);
		header.setResizingAllowed(columnResizingAllowed);
		header.setAutoscrolls(true);
		header.addMouseMotionListener(new ColumnDragMouseHandler());
		header.addMouseListener(new MouseSortHandler());
	}

	private FilterComboBoxModel<Item<Integer>> createAutoResizeModeComboBoxModel() {
		FilterComboBoxModel<Item<Integer>> autoResizeComboBoxModel = ItemComboBoxModel.builder(AUTO_RESIZE_MODES).build();
		autoResizeComboBoxModel.setSelectedItem(AUTO_RESIZE_MODES.get(getAutoResizeMode()));

		return autoResizeComboBoxModel;
	}

	private List<ToggleControl> createAutoResizeModeControls() {
		List<ToggleControl> controls = new ArrayList<>();
		State.Group group = State.group();
		for (Item<Integer> resizeMode : AUTO_RESIZE_MODES) {
			State state = State.state(resizeMode.value().equals(getAutoResizeMode()));
			group.add(state);
			state.addConsumer(enabled -> {
				if (enabled) {
					setAutoResizeMode(resizeMode.value());
				}
			});
			controls.add(Control.builder()
							.toggle(state)
							.name(resizeMode.caption())
							.build());
		}
		addPropertyChangeListener("autoResizeMode", changeEvent ->
						controls.get((Integer) changeEvent.getNewValue()).value().set(true));

		return controls;
	}

	private void bindEvents(boolean columnReorderingAllowed,
													boolean columnResizingAllowed) {
		columnModel().columnHidden().addConsumer(this::onColumnHidden);
		tableModel.selection().indexes().addConsumer(new ScrollToSelected());
		tableModel.filters().changed().addListener(getTableHeader()::repaint);
		searchModel.currentResult().addListener(this::repaint);
		sortModel.observer().addListener(getTableHeader()::repaint);
		sortModel.observer().addConsumer(sorted ->
						tableModel.items().visible().comparator().set(sorted ? sortModel.comparator() : null));
		addMouseListener(new FilterTableMouseListener());
		addKeyListener(new MoveResizeColumnKeyListener(columnReorderingAllowed, columnResizingAllowed));
		controlMap.keyEvent(COPY_CELL).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_SORT_COLUMN_ADD).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_SORT_COLUMN).ifPresent(keyEvent -> keyEvent.enable(this));
	}

	private void onColumnHidden(C columnIdentifier) {
		//disable the filter model for the column to be hidden, to prevent confusion
		tableModel.filters().optional(columnIdentifier)
						.ifPresent(condition -> condition.enabled().set(false));
	}

	private CommandControl createToggleSortColumnAddControl() {
		return command(() -> toggleColumnSorting(getSelectedColumn(), true));
	}

	private CommandControl createToggleSortColumnControl() {
		return command(() -> toggleColumnSorting(getSelectedColumn(), false));
	}

	private static Stream<JComponent> columnComponents(FilterTableColumn<?> column) {
		Collection<JComponent> components = new ArrayList<>(3);
		addIfComponent(components, column.getCellRenderer());
		addIfComponent(components, column.getCellEditor());
		addIfComponent(components, column.getHeaderRenderer());

		return components.stream();
	}

	private Map<C, ConditionPanel<?>> createFilterPanels() {
		Map<C, ConditionPanel<?>> conditionPanels = new HashMap<>();
		for (Map.Entry<C, ConditionModel<?>> entry : tableModel.filters().get().entrySet()) {
			ConditionModel<?> condition = entry.getValue();
			C identifier = entry.getKey();
			if (columnModel().contains(identifier) && filterFieldFactory.supportsType(condition.valueClass())) {
				conditionPanels.put(identifier, ColumnConditionPanel.builder(condition)
								.fieldFactory(filterFieldFactory)
								.tableColumn(columnModel().column(identifier))
								.build());
			}
		}

		return conditionPanels;
	}

	private void configureFilterConditionPanel(TableConditionPanel<C> filterConditionPanel) {
		filterConditionPanel.get().forEach(this::configureFilterPanel);
	}

	private void configureFilterPanel(C identifier, ConditionPanel<?> filterPanel) {
		filterPanel.focusGainedObserver().ifPresent(focusGainedObserver ->
						focusGainedObserver.addListener(() -> scrollToColumn(identifier)));
	}

	private static void addIfComponent(Collection<JComponent> components, Object object) {
		if (object instanceof JComponent) {
			components.add((JComponent) object);
		}
	}

	/**
	 * A MouseListener for handling double click, which invokes the action returned by {@link #getDoubleClickAction()}
	 * with the associated MouseEvent as the ActionEvent source as well as triggering the {@link #addDoubleClickListener(Consumer)} event.
	 * @see #getDoubleClickAction()
	 */
	private final class FilterTableMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				if (doubleClickAction.isNotNull()) {
					doubleClickAction.get().actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, "doubleClick"));
				}
				doubleClick.accept(e);
			}
		}
	}

	private final class ScrollToSelected implements Consumer<List<Integer>> {

		@Override
		public void accept(List<Integer> selectedRowIndexes) {
			if (scrollToSelectedItem.get() && !selectedRowIndexes.isEmpty() && noRowVisible(selectedRowIndexes)) {
				scrollToCoordinate(selectedRowIndexes.get(0), getSelectedColumn(), centerOnScroll.get());
			}
		}

		private boolean noRowVisible(List<Integer> rows) {
			JViewport viewport = Utilities.parentOfType(JViewport.class, FilterTable.this);
			if (viewport != null) {
				return rows.stream().noneMatch(row -> rowVisible(viewport, row));
			}

			return false;
		}

		private boolean rowVisible(JViewport viewport, int row) {
			int topRow = rowAtPoint(viewport.getViewPosition());
			int visibleRows = viewport.getExtentSize().height / getRowHeight();

			return row >= topRow && row <= topRow + visibleRows;
		}
	}

	private final class MouseSortHandler extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!sortingEnabled.get() || e.getButton() != MouseEvent.BUTTON1 || e.isControlDown()) {
				return;
			}

			FilterTableColumnModel<C> columnModel = columnModel();
			int index = columnModel.getColumnIndexAtX(e.getX());
			if (index >= 0) {
				if (!getSelectionModel().isSelectionEmpty()) {
					setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
				}
				ColumnSortOrder<C> columnSortOrder = sortModel.columnSortOrder(columnModel.getColumn(index).identifier());
				if (e.isAltDown()) {
					sortModel.addSortOrder(columnSortOrder.identifier(), nextSortOrder(columnSortOrder.sortOrder()));
				}
				else {
					sortModel.setSortOrder(columnSortOrder.identifier(), nextSortOrder(columnSortOrder.sortOrder()));
				}
			}
		}
	}

	private final class ColumnDragMouseHandler extends MouseMotionAdapter {
		@Override
		public void mouseDragged(MouseEvent e) {
			scrollRectToVisible(new Rectangle(e.getX(), getVisibleRect().y, 1, 1));
		}
	}

	/**
	 * A builder for a {@link FilterTable}
	 * @param <R> the type representing rows
	 * @param <C> the type used to identify columns
	 */
	public interface Builder<R, C> extends ComponentBuilder<Void, FilterTable<R, C>, Builder<R, C>> {

		/**
		 * @param summaryValuesFactory the column summary values factory
		 * @return this builder instance
		 */
		Builder<R, C> summaryValuesFactory(SummaryValues.Factory<C> summaryValuesFactory);

		/**
		 * @param filterPanelFactory the table filter condition panel factory
		 * @return this builder instance
		 */
		Builder<R, C> filterPanelFactory(TableConditionPanel.Factory<C> filterPanelFactory);

		/**
		 * @param filterFieldFactory the column filter field factory
		 * @return this builder instance
		 * @see FilterTable#filters()
		 */
		Builder<R, C> filterFieldFactory(FieldFactory filterFieldFactory);

		/**
		 * The cell renderer for the given column, overrides {@link #cellRendererFactory(FilterTableCellRenderer.Factory)}.
		 * @param identifier the column identifier
		 * @param cellRenderer the cell renderer to use for the given column
		 * @param <T> the column type
		 * @return this builder instance
		 */
		<T> Builder<R, C> cellRenderer(C identifier, FilterTableCellRenderer<T> cellRenderer);

		/**
		 * Note that this factory is only used to create cell renderers for columns which do not already have a cell renderer
		 * and is overridden by any renderer set via {@link #cellRenderer(Object, FilterTableCellRenderer)}.
		 * @param cellRendererFactory the table cell renderer factory
		 * @return this builder instance
		 */
		Builder<R, C> cellRendererFactory(FilterTableCellRenderer.Factory<R, C> cellRendererFactory);

		/**
		 * the cell renderer for the given column, overrides {@link #cellEditorFactory(FilterTableCellEditor.Factory)}.
		 * @param identifier the column identifier
		 * @param cellEditor the cell editor to use for the given column
		 * @param <T> the column type
		 * @return this builder instance
		 */
		<T> Builder<R, C> cellEditor(C identifier, FilterTableCellEditor<T> cellEditor);

		/**
		 * Note that this factory is only used to create cell editors for columns which do not already have a cell editor
		 * and is overridden by any editor set via {@link #cellEditor(Object, FilterTableCellEditor)}.
		 * @param cellEditorFactory the table cell editor factory
		 * @return this builder instance
		 */
		Builder<R, C> cellEditorFactory(FilterTableCellEditor.Factory<C> cellEditorFactory);

		/**
		 * @param autoStartsEdit true if editing should start automatically
		 * @return this builder instance
		 */
		Builder<R, C> autoStartsEdit(boolean autoStartsEdit);

		/**
		 * @param centerOnScroll the center on scroll behavious
		 * @return this builder instance
		 */
		Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll);

		/**
		 * @param doubleClickAction the double click action
		 * @return this builder instance
		 */
		Builder<R, C> doubleClickAction(Action doubleClickAction);

		/**
		 * @param scrollToSelectedItem true if this table should scroll to the selected item
		 * @return this builder instance
		 */
		Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem);

		/**
		 * @param sortingEnabled true if sorting via clicking the header should be enbled
		 * @return this builder instance
		 */
		Builder<R, C> sortingEnabled(boolean sortingEnabled);

		/**
		 * @param selectionMode the table selection mode
		 * @return this builder instance
		 * @see JTable#setSelectionMode(int)
		 */
		Builder<R, C> selectionMode(int selectionMode);

		/**
		 * @param columnReorderingAllowed true if column reordering should be allowed
		 * @return this builder instance
		 */
		Builder<R, C> columnReorderingAllowed(boolean columnReorderingAllowed);

		/**
		 * @param columnResizingAllowed true if column resizing should be allowed
		 * @return this builder instance
		 */
		Builder<R, C> columnResizingAllowed(boolean columnResizingAllowed);

		/**
		 * @param autoResizeMode the table auto column resizing mode
		 * @return this builder instance
		 */
		Builder<R, C> autoResizeMode(int autoResizeMode);

		/**
		 * Only applicable to {@link FilterTableCellEditor}
		 * @param resizeRowToFitEditor true if the row should be resized to fit the editor
		 * @return this builder instance
		 */
		Builder<R, C> resizeRowToFitEditor(boolean resizeRowToFitEditor);

		/**
		 * @param filterView the initial filter condition view
		 * @return this builder instance
		 */
		Builder<R, C> filterView(ConditionView filterView);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder<R, C> keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);
	}

	/**
	 * Exports the table data to a String.
	 */
	public interface Export {

		/**
		 * @param delimiter the column delimiter, TAB by default
		 * @return this Export instance
		 */
		Export delimiter(char delimiter);

		/**
		 * @param header include a column header, default true
		 * @return this Export instance
		 */
		Export header(boolean header);

		/**
		 * @param hidden include hidden columns, default false
		 * @return this Export instance
		 */
		Export hidden(boolean hidden);

		/**
		 * @param selected include only selected rows, default false
		 * @return this Export instance
		 */
		Export selected(boolean selected);

		/**
		 * @return the table data exported to a String
		 */
		String get();
	}

	private static final class DefaultBuilder<R, C>
					extends AbstractComponentBuilder<Void, FilterTable<R, C>, Builder<R, C>>
					implements Builder<R, C> {

		private final FilterTableModel<R, C> tableModel;
		private final List<FilterTableColumn<C>> columns;
		private final ControlMap controlMap = controlMap(ControlKeys.class);
		private final Map<C, FilterTableCellRenderer<?>> cellRenderers = new HashMap<>();
		private final Map<C, FilterTableCellEditor<?>> cellEditors = new HashMap<>();

		private SummaryValues.Factory<C> summaryValuesFactory;
		private TableConditionPanel.Factory<C> filterPanelFactory = new DefaultFilterPanelFactory<>();
		private FieldFactory filterFieldFactory = new DefaultFilterFieldFactory();
		private FilterTableCellRenderer.Factory<R, C> cellRendererFactory;
		private FilterTableCellEditor.Factory<C> cellEditorFactory;
		private boolean autoStartsEdit = false;
		private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;
		private Action doubleClickAction;
		private boolean scrollToSelectedItem = true;
		private boolean sortingEnabled = true;
		private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		private boolean columnReorderingAllowed = ALLOW_COLUMN_REORDERING.get();
		private boolean columnResizingAllowed = true;
		private int autoResizeMode = AUTO_RESIZE_MODE.get();
		private boolean resizeRowToFitEditor = RESIZE_ROW_TO_FIT_EDITOR.get();
		private ConditionView filterView = ConditionView.HIDDEN;

		private DefaultBuilder(FilterTableModel<R, C> tableModel, List<FilterTableColumn<C>> columns) {
			this.tableModel = tableModel;
			this.columns = new ArrayList<>(validateIdentifiers(columns));
			this.cellRendererFactory = FilterTableCellRenderer.factory();
		}

		@Override
		public Builder<R, C> summaryValuesFactory(SummaryValues.Factory<C> summaryValuesFactory) {
			this.summaryValuesFactory = requireNonNull(summaryValuesFactory);
			return this;
		}

		@Override
		public Builder<R, C> filterPanelFactory(TableConditionPanel.Factory<C> filterPanelFactory) {
			this.filterPanelFactory = requireNonNull(filterPanelFactory);
			return this;
		}

		@Override
		public Builder<R, C> filterFieldFactory(FieldFactory filterFieldFactory) {
			this.filterFieldFactory = requireNonNull(filterFieldFactory);
			return this;
		}

		@Override
		public <T> Builder<R, C> cellRenderer(C identifier, FilterTableCellRenderer<T> cellRenderer) {
			this.cellRenderers.put(requireNonNull(identifier), requireNonNull(cellRenderer));
			return this;
		}

		@Override
		public Builder<R, C> cellRendererFactory(FilterTableCellRenderer.Factory<R, C> cellRendererFactory) {
			this.cellRendererFactory = requireNonNull(cellRendererFactory);
			return this;
		}

		@Override
		public <T> Builder<R, C> cellEditor(C identifier, FilterTableCellEditor<T> cellEditor) {
			this.cellEditors.put(requireNonNull(identifier), requireNonNull(cellEditor));
			return this;
		}

		@Override
		public Builder<R, C> cellEditorFactory(FilterTableCellEditor.Factory<C> cellEditorFactory) {
			this.cellEditorFactory = requireNonNull(cellEditorFactory);
			return this;
		}

		@Override
		public Builder<R, C> autoStartsEdit(boolean autoStartsEdit) {
			this.autoStartsEdit = autoStartsEdit;
			return this;
		}

		@Override
		public Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll) {
			this.centerOnScroll = requireNonNull(centerOnScroll);
			return this;
		}

		@Override
		public Builder<R, C> doubleClickAction(Action doubleClickAction) {
			this.doubleClickAction = requireNonNull(doubleClickAction);
			return this;
		}

		@Override
		public Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem) {
			this.scrollToSelectedItem = scrollToSelectedItem;
			return this;
		}

		@Override
		public Builder<R, C> sortingEnabled(boolean sortingEnabled) {
			this.sortingEnabled = sortingEnabled;
			return this;
		}

		@Override
		public Builder<R, C> selectionMode(int selectionMode) {
			this.selectionMode = selectionMode;
			return this;
		}

		@Override
		public Builder<R, C> columnReorderingAllowed(boolean columnReorderingAllowed) {
			this.columnReorderingAllowed = columnReorderingAllowed;
			return this;
		}

		@Override
		public Builder<R, C> columnResizingAllowed(boolean columnResizingAllowed) {
			this.columnResizingAllowed = columnResizingAllowed;
			return this;
		}

		@Override
		public Builder<R, C> autoResizeMode(int autoResizeMode) {
			this.autoResizeMode = autoResizeMode;
			return this;
		}

		@Override
		public Builder<R, C> resizeRowToFitEditor(boolean resizeRowToFitEditor) {
			this.resizeRowToFitEditor = resizeRowToFitEditor;
			return this;
		}

		@Override
		public Builder<R, C> filterView(ConditionView filterView) {
			this.filterView = requireNonNull(filterView);
			return this;
		}

		@Override
		public Builder<R, C> keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return this;
		}

		@Override
		protected FilterTable<R, C> createComponent() {
			return new FilterTable<>(this);
		}

		@Override
		protected ComponentValue<Void, FilterTable<R, C>> createComponentValue(FilterTable<R, C> component) {
			return new FilterTableComponentValue<>(component);
		}

		private Collection<FilterTableColumn<C>> validateIdentifiers(List<FilterTableColumn<C>> columns) {
			if (columns.stream()
							.map(new ColumnIdentifier<>())
							.distinct()
							.count() != columns.size()) {
				throw new IllegalArgumentException("Column identifiers are not unique");
			}

			return columns;
		}

		private static final class ColumnIdentifier<C> implements Function<FilterTableColumn<C>, C> {

			@Override
			public C apply(FilterTableColumn<C> column) {
				return column.identifier();
			}
		}
	}

	private final class DefaultSummaryValuesFactory implements SummaryValues.Factory<C> {

		@Override
		public <T extends Number> Optional<SummaryValues<T>> createSummaryValues(C identifier, Format format) {
			Class<?> columnClass = tableModel.getColumnClass(identifier);
			if (Number.class.isAssignableFrom(columnClass)) {
				return Optional.of(new DefaultSummaryValues<>(identifier, tableModel, format));
			}

			return Optional.empty();
		}
	}

	private static final class DefaultSummaryValues<T extends Number, C> implements SummaryValues<T> {

		private final C identifier;
		private final FilterTableModel<?, C> tableModel;
		private final Format format;
		private final Event<?> valuesChanged = Event.event();

		private DefaultSummaryValues(C identifier, FilterTableModel<?, C> tableModel, Format format) {
			this.identifier = requireNonNull(identifier);
			this.tableModel = requireNonNull(tableModel);
			this.format = requireNonNull(format);
			this.tableModel.items().visible().addListener(valuesChanged);
			this.tableModel.selection().indexes().addListener(valuesChanged);
		}

		@Override
		public String format(Object value) {
			return format.format(value);
		}

		@Override
		public Observer<?> valuesChanged() {
			return valuesChanged.observer();
		}

		@Override
		public Collection<T> values() {
			return subset() ? tableModel.selectedValues(identifier) : tableModel.values(identifier);
		}

		@Override
		public boolean subset() {
			TableSelection<?> selection = tableModel.selection();

			return selection.empty().not().get() &&
							selection.count() != tableModel.items().visible().count();
		}
	}

	private final class DefaultExport implements Export {

		private char delimiter = '\t';
		private boolean header = true;
		private boolean hidden = false;
		private boolean selected = false;

		@Override
		public Export delimiter(char delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		@Override
		public Export header(boolean header) {
			this.header = header;
			return this;
		}

		@Override
		public Export hidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		@Override
		public Export selected(boolean selected) {
			this.selected = selected;
			return this;
		}

		@Override
		public String get() {
			List<Integer> rows = selected ?
							tableModel.selection().indexes().get() :
							IntStream.range(0, tableModel.items().visible().count())
											.boxed()
											.collect(toList());

			List<FilterTableColumn<C>> columns = new ArrayList<>(columnModel().visible().columns());
			if (hidden) {
				columns.addAll(columnModel().hidden().columns());
			}

			List<List<String>> lines = new ArrayList<>();
			if (header) {
				lines.add(columns.stream()
								.map(column -> String.valueOf(column.getHeaderValue()))
								.collect(toList()));
			}
			lines.addAll(rows.stream()
							.map(row -> stringValues(row, columns))
							.collect(toList()));

			return lines.stream()
							.map(line -> join(String.valueOf(delimiter), line))
							.collect(joining(System.lineSeparator()));
		}

		private List<String> stringValues(int row, List<FilterTableColumn<C>> columns) {
			return columns.stream()
							.map(column -> tableModel.getStringAt(row, column.identifier()))
							.collect(toList());
		}
	}

	private class ResizeRowToFitEditor implements PropertyChangeListener {

		private int editedRow = -1;

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			TableCellEditor editor = (TableCellEditor) event.getNewValue();
			if (editor instanceof DefaultFilterTableCellEditor<?>) {
				DefaultFilterTableCellEditor<?> filterTableCellEditor = (DefaultFilterTableCellEditor<?>) editor;
				editedRow = filterTableCellEditor.editedRow;
				setRowHeight(editedRow, filterTableCellEditor.componentValue().component().getPreferredSize().height);
			}
			else if (event.getNewValue() == null && editedRow != -1) {
				setRowHeight(editedRow, getRowHeight());
				editedRow = -1;
			}
		}
	}

	private final class MoveResizeColumnKeyListener extends KeyAdapter {

		private final boolean columnResizingAllowed;
		private final boolean columnReorderingAllowed;

		private final KeyStroke moveLeft;
		private final KeyStroke moveRight;
		private final KeyStroke increaseSize;
		private final KeyStroke decreaseSize;

		private MoveResizeColumnKeyListener(boolean columnReorderingAllowed,
																				boolean columnResizingAllowed) {
			this.columnReorderingAllowed = columnReorderingAllowed;
			this.columnResizingAllowed = columnResizingAllowed;
			moveLeft = controlMap.keyStroke(MOVE_COLUMN_LEFT).get();
			moveRight = controlMap.keyStroke(MOVE_COLUMN_RIGHT).get();
			increaseSize = controlMap.keyStroke(INCREASE_COLUMN_SIZE).get();
			decreaseSize = controlMap.keyStroke(DECREASE_COLUMN_SIZE).get();
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (columnReorderingAllowed && move(e)) {
				moveSelectedColumn(e.getKeyCode() == moveLeft.getKeyCode());
				e.consume();
			}
			else if (columnResizingAllowed && resize(e)) {
				resizeSelectedColumn(e.getKeyCode() == increaseSize.getKeyCode());
				e.consume();
			}
		}

		private boolean move(KeyEvent e) {
			return (moveLeft != null && moveLeft.equals(getKeyStrokeForEvent(e))) ||
							(moveRight != null && moveRight.equals(getKeyStrokeForEvent(e)));
		}

		private boolean resize(KeyEvent e) {
			return (decreaseSize != null && decreaseSize.equals(getKeyStrokeForEvent(e))) ||
							(increaseSize != null && increaseSize.equals(getKeyStrokeForEvent(e)));
		}

		private void moveSelectedColumn(boolean left) {
			int selectedColumnIndex = getSelectedColumn();
			if (selectedColumnIndex != -1) {
				int columnCount = columnModel().getColumnCount();
				int newIndex;
				if (left) {
					if (selectedColumnIndex == 0) {
						newIndex = columnCount - 1;
					}
					else {
						newIndex = selectedColumnIndex - 1;
					}
				}
				else {
					if (selectedColumnIndex == columnCount - 1) {
						newIndex = 0;
					}
					else {
						newIndex = selectedColumnIndex + 1;
					}
				}
				moveColumn(selectedColumnIndex, newIndex);
				scrollRectToVisible(getCellRect(getSelectedRow(), newIndex, true));
			}
		}

		private void resizeSelectedColumn(boolean enlarge) {
			int selectedColumnIndex = getSelectedColumn();
			if (selectedColumnIndex != -1) {
				TableColumn column = columnModel().getColumn(selectedColumnIndex);
				tableHeader.setResizingColumn(column);
				column.setWidth(column.getWidth() + (enlarge ? COLUMN_RESIZE_AMOUNT : -COLUMN_RESIZE_AMOUNT));
			}
		}
	}

	static final class ColumnComparator implements Comparator<TableColumn> {

		private final Comparator<String> collator = Text.collator();

		@Override
		public int compare(TableColumn col1, TableColumn col2) {
			return collator.compare(String.valueOf(col1.getHeaderValue()), String.valueOf(col2.getHeaderValue()));
		}
	}

	private static final class FilterTableComponentValue<R, C> extends AbstractComponentValue<Void, FilterTable<R, C>> {

		private FilterTableComponentValue(FilterTable<R, C> table) {
			super(table);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class FilterTableHeader extends JTableHeader {

		private FilterTableHeader(TableColumnModel columnModel) {
			super(columnModel);
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			int index = columnModel.getColumnIndexAtX(event.getPoint().x);
			if (index != -1) {
				return ((FilterTableColumn<?>) columnModel.getColumn(index)).toolTipText().orElse(null);
			}

			return null;
		}
	}
}
