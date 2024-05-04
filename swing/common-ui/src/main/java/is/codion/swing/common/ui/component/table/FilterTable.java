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
import is.codion.common.Separators;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValues;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
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
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.item.Item.item;
import static is.codion.common.model.table.TableSummaryModel.tableSummaryModel;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.itemComboBoxModel;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.itemComboBox;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static is.codion.swing.common.ui.component.table.FilterTable.FilterTableControl.*;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.component.table.FilterTableSortModel.nextSortOrder;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
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
 * For instances use the builder {@link #builder(FilterTableModel, ColumnFactory)}
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @see #builder(FilterTableModel, ColumnFactory)
 */
public final class FilterTable<R, C> extends JTable {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterTable.class, getBundle(FilterTable.class.getName()));

	/**
	 * Specifies the default table column resize mode for tables in the application<br>
	 * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
	 * Default value: {@link JTable#AUTO_RESIZE_OFF}
	 */
	public static final PropertyValue<Integer> AUTO_RESIZE_MODE =
					Configuration.integerValue(FilterTable.class.getName() + ".autoResizeMode", AUTO_RESIZE_OFF);

	/**
	 * Specifies whether columns can be rearranged in tables<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING =
					Configuration.booleanValue(FilterTable.class.getName() + ".allowColumnReordering", true);

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final KeyboardShortcuts<FilterTableControl> DEFAULT_KEYBOARD_SHORTCUTS =
					keyboardShortcuts(FilterTableControl.class);

	/**
	 * The controls.
	 */
	public enum FilterTableControl implements KeyboardShortcuts.Shortcut {
		/**
		 * Moves the selected column to the left.<br>
		 * Default key stroke: CTRL-SHIFT-LEFT
		 */
		MOVE_COLUMN_LEFT(keyStroke(VK_LEFT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Moves the selected column to the right.<br>
		 * Default key stroke: CTRL-SHIFT-RIGHT
		 */
		MOVE_COLUMN_RIGHT(keyStroke(VK_RIGHT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Decreases the size of the selected column.<br>
		 * Default key stroke: CTRL-SUBTRACT
		 */
		DECREASE_COLUMN_SIZE(keyStroke(VK_SUBTRACT, CTRL_DOWN_MASK)),
		/**
		 * Increases the size of the selected column.<br>
		 * Default key stroke: CTRL-ADD
		 */
		INCREASE_COLUMN_SIZE(keyStroke(VK_ADD, CTRL_DOWN_MASK)),
		/**
		 * Copy the selected cell contents to the clipboard.<br>
		 * Default key stroke: CTRL-ALT-C
		 */
		COPY_CELL(keyStroke(VK_C, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Toggles the sort on the selected column.<br>
		 * Default key stroke: ALT-DOWN ARROW
		 */
		TOGGLE_SORT_COLUMN(keyStroke(VK_DOWN, ALT_DOWN_MASK)),
		/**
		 * Toggles the sort on the selected column adding it to any already sorted columns.<br>
		 * Default key stroke: ALT-UP ARROW
		 */
		TOGGLE_SORT_COLUMN_ADD(keyStroke(VK_UP, ALT_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		FilterTableControl(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
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
	private static final String RESIZE_OFF = "resize_off";
	private static final String RESIZE_NEXT_COLUMN = "resize_next_column";
	private static final String RESIZE_SUBSEQUENT_COLUMNS = "resize_subsequent_columns";
	private static final String RESIZE_LAST_COLUMN = "resize_last_column";
	private static final String RESIZE_ALL_COLUMNS = "resize_all_columns";
	private static final int SEARCH_FIELD_MINIMUM_WIDTH = 100;
	private static final int COLUMN_RESIZE_AMOUNT = 10;

	private final FilterTableModel<R, C> tableModel;
	private final FilterTableSearchModel searchModel;
	private final FilterTableSortModel<R, C> sortModel;
	private final TableSummaryModel<C> summaryModel;

	private final ColumnConditionPanel.Factory<C> filterPanelFactory;
	private final Event<MouseEvent> doubleClickEvent = Event.event();
	private final Value<Action> doubleClickAction;
	private final State sortingEnabled;
	private final State scrollToSelectedItem;
	private final Value<CenterOnScroll> centerOnScroll;

	private FilterTableConditionPanel<C> filterPanel;
	private JTextField searchField;

	private FilterTable(DefaultBuilder<R, C> builder) {
		super(builder.tableModel, new DefaultFilterTableColumnModel<>(builder.columns),
						builder.tableModel.selectionModel());
		this.tableModel = builder.tableModel;
		this.sortModel = new DefaultFilterTableSortModel<>(tableModel.columns());
		this.searchModel = new DefaultFilterTableSearchModel<>(tableModel, columnModel());
		this.summaryModel = tableSummaryModel(builder.summaryValuesFactory == null ?
						new DefaultSummaryValuesFactory() : builder.summaryValuesFactory);
		this.filterPanelFactory = builder.filterPanelFactory;
		this.centerOnScroll = Value.nonNull(CenterOnScroll.NEITHER)
						.initialValue(builder.centerOnScroll)
						.build();
		this.doubleClickAction = Value.nullable(builder.doubleClickAction).build();
		this.scrollToSelectedItem = State.state(builder.scrollToSelectedItem);
		this.sortingEnabled = State.state(builder.sortingEnabled);
		autoStartsEdit(builder.autoStartsEdit);
		setSelectionMode(builder.selectionMode);
		setAutoResizeMode(builder.autoResizeMode);
		configureColumns(builder.cellRendererFactory);
		configureTableHeader(builder.columnReorderingAllowed, builder.columnResizingAllowed);
		bindEvents(builder.shortcuts);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(getTableHeader(), searchField, filterPanel);
		Utilities.updateUI(columnModel().hidden().stream()
						.flatMap(FilterTable::columnComponents)
						.collect(toList()));
	}

	@Override
	public FilterTableModel<R, C> getModel() {
		return (FilterTableModel<R, C>) super.getModel();
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
		List<R> selection = ((FilterTableModel<R, C>) dataModel).selectionModel().getSelectedItems();
		super.setModel(dataModel);
		if (!selection.isEmpty()) {
			((FilterTableModel<R, C>) dataModel).selectionModel().setSelectedItems(selection);
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
		if (!(selectionModel instanceof FilterTableSelectionModel)) {
			throw new IllegalArgumentException("FilterTable selection model must be a FilterTableSelectionModel instance");
		}
		super.setSelectionModel(selectionModel);
	}

	/**
	 * @return the filter panel
	 */
	public FilterTableConditionPanel<C> filterPanel() {
		if (filterPanel == null) {
			filterPanel = filterTableConditionPanel(tableModel.filterModel(), columnModel(), filterPanelFactory);
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
	 * @return the Value controlling the action to perform when a double click is performed on the table,
	 * null for no double click action
	 */
	public Value<Action> doubleClickAction() {
		return doubleClickAction;
	}

	/**
	 * @return the State controlling whether sorting via the table header is enabled
	 */
	public State sortingEnabled() {
		return sortingEnabled;
	}

	/**
	 * @return the State controlling whether the JTable instance scrolls automatically to the coordinate
	 * of the item selected in the underlying table model
	 */
	public State scrollToSelectedItem() {
		return scrollToSelectedItem;
	}

	/**
	 * @return the Value controlling the scrolling behaviour when scrolling to the selected row/column
	 */
	public Value<CenterOnScroll> centerOnScroll() {
		return centerOnScroll;
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		tableModel.selectionModel().setSelectionMode(selectionMode);
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
		ItemComboBoxModel<Integer> autoResizeComboBoxModel = createAutoResizeModeComboBoxModel();
		Dialogs.okCancelDialog(borderLayoutPanel()
										.centerComponent(itemComboBox(autoResizeComboBoxModel).build())
										.border(Borders.emptyBorder())
										.build())
						.owner(getParent())
						.title(MESSAGES.getString(AUTO_RESIZE))
						.onOk(() -> setAutoResizeMode(autoResizeComboBoxModel.selectedValue().get()))
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
	 * @param columnIdentifier the column identifier
	 */
	public void scrollToColumn(C columnIdentifier) {
		requireNonNull(columnIdentifier);
		JViewport viewport = Utilities.parentOfType(JViewport.class, this);
		if (viewport != null) {
			scrollToRowColumn(viewport, rowAtPoint(viewport.getViewPosition()),
							columnModel().getColumnIndex(columnIdentifier), CenterOnScroll.NEITHER);
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
			Utilities.setClipboard(getModel().getStringAt(selectedRow, column.getIdentifier()));
		}
	}

	/**
	 * Copies the table data as a TAB delimited string, with header, to the clipboard.
	 */
	public void copyToClipboard() {
		Utilities.setClipboard(export()
						.delimiter('\t')
						.get());
	}

	/**
	 * @return a {@link Export} instance for exporting the table model data
	 */
	public Export export() {
		return new DefaultExport();
	}

	/**
	 * Copies the table data as a TAB delimited string, with header, to the clipboard.
	 */
	public void copySelectedToClipboard() {
		Utilities.setClipboard(export()
						.delimiter('\t')
						.selected(true)
						.get());
	}

	/**
	 * @return a control for showing the column selection dialog
	 */
	public Control createSelectColumnsControl() {
		return Control.builder(this::selectColumns)
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
										.toArray(ToggleControl[]::new))
						.build();
	}

	/**
	 * @return a Control for resetting the columns to their original location and visibility
	 */
	public Control createResetColumnsControl() {
		return Control.builder(columnModel()::resetColumns)
						.name(MESSAGES.getString(RESET))
						.enabled(columnModel().locked().not())
						.description(MESSAGES.getString(RESET_COLUMNS_DESCRIPTION))
						.build();
	}

	/**
	 * @return a Control for selecting the auto-resize mode
	 */
	public Control createAutoResizeModeControl() {
		return Control.builder(this::selectAutoResizeMode)
						.name(MESSAGES.getString(AUTO_RESIZE) + "...")
						.enabled(columnModel().locked().not())
						.build();
	}

	/**
	 * @return a ToggleControl for toggling the table selection mode (single or multiple)
	 */
	public ToggleControl createSingleSelectionModeControl() {
		return ToggleControl.builder(tableModel.selectionModel().singleSelectionMode())
						.name(MESSAGES.getString(SINGLE_SELECTION_MODE))
						.build();
	}

	/**
	 * @return a Control for copying the contents of the selected cell
	 */
	public Control createCopyCellControl() {
		return Control.builder(this::copySelectedCell)
						.name(MESSAGES.getString("copy_cell"))
						.enabled(tableModel.selectionModel().selectionNotEmpty())
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
	public EventObserver<MouseEvent> doubleClickEvent() {
		return doubleClickEvent.observer();
	}

	/**
	 * Instantiates a new {@link SummaryValues} instance.
	 * @param columnIdentifier the column identifier
	 * @param tableModel the table model
	 * @param format the format
	 * @param <T> the column value type
	 * @param <C> the column identifier type
	 * @return a new {@link SummaryValues} instance
	 */
	public static <T extends Number, C> SummaryValues<T> summaryValues(C columnIdentifier,
																																		 FilterTableModel<?, C> tableModel, Format format) {
		return new DefaultSummaryValues<>(columnIdentifier, tableModel, format);
	}

	/**
	 * Instantiates a new {@link FilterTable.Builder} using the given model
	 * @param tableModel the table model
	 * @param columns the columns
	 * @param <R> the type representing rows
	 * @param <C> the type used to identify columns
	 * @return a new {@link FilterTable.Builder} instance
	 */
	public static <R, C> Builder<R, C> builder(FilterTableModel<R, C> tableModel,
																						 List<FilterTableColumn<C>> columns) {
		return new DefaultBuilder<>(tableModel, columns);
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
		Control nextResult = control(() -> selectSearchResult(false, true));
		Control selectNextResult = control(() -> selectSearchResult(true, true));
		Control previousResult = control(() -> selectSearchResult(false, false));
		Control selectPreviousResult = control(() -> selectSearchResult(true, false));
		Control requestTableFocus = control(this::requestFocusInWindow);

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
										.action(control(field::requestFocusInWindow))
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
		if (selectedColumn != -1) {
			C columnIdentifier = columnModel().getColumn(selectedColumn).getIdentifier();
			if (sortModel.isSortingEnabled(columnIdentifier)) {
				if (add) {
					sortModel.addSortOrder(columnIdentifier, nextSortOrder(sortModel.sortOrder(columnIdentifier)));
				}
				else {
					sortModel.setSortOrder(columnIdentifier, nextSortOrder(sortModel.sortOrder(columnIdentifier)));
				}
			}
		}
	}

	private Controls searchFieldPopupMenuControls() {
		return Controls.builder()
						.control(ToggleControl.builder(searchModel.caseSensitive())
										.name(MESSAGES.getString("case_sensitive_search")))
						.controls(ToggleControl.builder(searchModel.regularExpression())
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
		return ToggleControl.builder(columnModel().visible(column.getIdentifier()))
						.name(String.valueOf(column.getHeaderValue()))
						.description(column.toolTipText())
						.build();
	}

	private void configureColumns(FilterTableCellRendererFactory<C> cellRendererFactory) {
		columnModel().columns().stream()
						.filter(column -> column.getCellRenderer() == null)
						.forEach(column -> column.setCellRenderer(cellRendererFactory.tableCellRenderer(column)));
		columnModel().columns().stream()
						.filter(column -> column.getHeaderRenderer() == null)
						.forEach(column -> column.setHeaderRenderer(new FilterTableHeaderRenderer<>(this, column)));
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

	private ItemComboBoxModel<Integer> createAutoResizeModeComboBoxModel() {
		ItemComboBoxModel<Integer> autoResizeComboBoxModel = itemComboBoxModel(asList(
						item(AUTO_RESIZE_OFF, MESSAGES.getString(RESIZE_OFF)),
						item(AUTO_RESIZE_NEXT_COLUMN, MESSAGES.getString(RESIZE_NEXT_COLUMN)),
						item(AUTO_RESIZE_SUBSEQUENT_COLUMNS, MESSAGES.getString(RESIZE_SUBSEQUENT_COLUMNS)),
						item(AUTO_RESIZE_LAST_COLUMN, MESSAGES.getString(RESIZE_LAST_COLUMN)),
						item(AUTO_RESIZE_ALL_COLUMNS, MESSAGES.getString(RESIZE_ALL_COLUMNS))
		));
		autoResizeComboBoxModel.setSelectedItem(item(getAutoResizeMode()));

		return autoResizeComboBoxModel;
	}

	private void bindEvents(KeyboardShortcuts<FilterTableControl> shortcuts) {
		columnModel().columnHiddenEvent().addConsumer(this::onColumnHidden);
		tableModel.selectionModel().selectedIndexesEvent().addConsumer(new ScrollToSelected());
		tableModel.filterModel().conditionChangedEvent().addListener(getTableHeader()::repaint);
		searchModel.currentResult().addListener(this::repaint);
		sortModel.sortingChangedEvent().addListener(getTableHeader()::repaint);
		sortModel.sortingChangedEvent().addListener(() ->
						tableModel.comparator().set(sortModel.sorted() ? sortModel.comparator() : null));
		addMouseListener(new FilterTableMouseListener());
		addKeyListener(new MoveResizeColumnKeyListener(shortcuts));
		shortcuts.keyStroke(COPY_CELL).optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(createCopyCellControl())
										.enable(this));
		shortcuts.keyStroke(TOGGLE_SORT_COLUMN_ADD).optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(createToggleSortColumnAddControl())
										.enable(this));
		shortcuts.keyStroke(TOGGLE_SORT_COLUMN).optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.action(createToggleSortColumnControl())
										.enable(this));
	}

	private void onColumnHidden(C columnIdentifier) {
		//disable the filter model for the column to be hidden, to prevent confusion
		ColumnConditionModel<?, ?> filterModel = tableModel.filterModel().conditionModels().get(columnIdentifier);
		if (filterModel != null && !filterModel.locked().get()) {
			filterModel.enabled().set(false);
		}
	}

	private Control createToggleSortColumnAddControl() {
		return control(() -> toggleColumnSorting(getSelectedColumn(), true));
	}

	private Control createToggleSortColumnControl() {
		return control(() -> toggleColumnSorting(getSelectedColumn(), false));
	}

	private static Stream<JComponent> columnComponents(FilterTableColumn<?> column) {
		Collection<JComponent> components = new ArrayList<>(3);
		addIfComponent(components, column.getCellRenderer());
		addIfComponent(components, column.getCellEditor());
		addIfComponent(components, column.getHeaderRenderer());

		return components.stream();
	}

	private static void addIfComponent(Collection<JComponent> components, Object object) {
		if (object instanceof JComponent) {
			components.add((JComponent) object);
		}
	}

	/**
	 * A MouseListener for handling double click, which invokes the action returned by {@link #getDoubleClickAction()}
	 * with this table as the ActionEvent source as well as triggering the {@link #addDoubleClickListener(Consumer)} event.
	 * @see #getDoubleClickAction()
	 */
	private final class FilterTableMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				if (doubleClickAction.isNotNull()) {
					doubleClickAction.get().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleClick"));
				}
				doubleClickEvent.accept(e);
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
			if (!sortingEnabled.get() || e.getButton() != MouseEvent.BUTTON1 || e.isAltDown()) {
				return;
			}

			FilterTableColumnModel<C> columnModel = columnModel();
			int index = columnModel.getColumnIndexAtX(e.getX());
			if (index >= 0) {
				if (!getSelectionModel().isSelectionEmpty()) {
					setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
				}
				C columnIdentifier = columnModel.getColumn(index).getIdentifier();
				if (sortModel.isSortingEnabled(columnIdentifier)) {
					SortOrder nextSortOrder = nextSortOrder(sortModel.sortOrder(columnIdentifier));
					if (e.isControlDown()) {
						sortModel.addSortOrder(columnIdentifier, nextSortOrder);
					}
					else {
						sortModel.setSortOrder(columnIdentifier, nextSortOrder);
					}
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
		 * @param filterPanelFactory the column filter panel factory
		 * @return this builder instance
		 */
		Builder<R, C> filterPanelFactory(ColumnConditionPanel.Factory<C> filterPanelFactory);

		/**
		 * Note that this factory is only used to create cell renderers for columns which do not already have a cell renderer set.
		 * @param cellRendererFactory the table cell renderer factory
		 * @return this builder instance
		 */
		Builder<R, C> cellRendererFactory(FilterTableCellRendererFactory<C> cellRendererFactory);

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
		 * @param shortcuts provides this tables {@link KeyboardShortcuts} instance.
		 * @return this builder instance
		 */
		Builder<R, C> keyStrokes(Consumer<KeyboardShortcuts<FilterTableControl>> shortcuts);
	}

	/**
	 * Exports the table data to a String.
	 */
	public interface Export {

		/**
		 * @param delimiter the column delimiter (TAB by default)
		 * @return this Export instance
		 */
		Export delimiter(char delimiter);

		/**
		 * @param header include a column header
		 * @return this Export instance
		 */
		Export header(boolean header);

		/**
		 * @param hidden include hidden columns
		 * @return this Export instance
		 */
		Export hidden(boolean hidden);

		/**
		 * @param selected include only selected rows (default false)
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
		private final KeyboardShortcuts<FilterTableControl> shortcuts = DEFAULT_KEYBOARD_SHORTCUTS.copy();

		private SummaryValues.Factory<C> summaryValuesFactory;
		private ColumnConditionPanel.Factory<C> filterPanelFactory;
		private FilterTableCellRendererFactory<C> cellRendererFactory;
		private boolean autoStartsEdit = false;
		private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;
		private Action doubleClickAction;
		private boolean scrollToSelectedItem = true;
		private boolean sortingEnabled = true;
		private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		private boolean columnReorderingAllowed = ALLOW_COLUMN_REORDERING.get();
		private boolean columnResizingAllowed = true;
		private int autoResizeMode = AUTO_RESIZE_MODE.get();

		private DefaultBuilder(FilterTableModel<R, C> tableModel, List<FilterTableColumn<C>> columns) {
			this.tableModel = requireNonNull(tableModel);
			this.columns = new ArrayList<>(requireNonNull(columns));
			this.filterPanelFactory = new DefaultFilterPanelFactory<>();
			this.cellRendererFactory = new DefaultFilterTableCellRendererFactory<>(tableModel);
		}

		@Override
		public Builder<R, C> summaryValuesFactory(SummaryValues.Factory<C> summaryValuesFactory) {
			this.summaryValuesFactory = requireNonNull(summaryValuesFactory);
			return this;
		}

		@Override
		public Builder<R, C> filterPanelFactory(ColumnConditionPanel.Factory<C> filterPanelFactory) {
			this.filterPanelFactory = requireNonNull(filterPanelFactory);
			return this;
		}

		@Override
		public Builder<R, C> cellRendererFactory(FilterTableCellRendererFactory<C> cellRendererFactory) {
			this.cellRendererFactory = requireNonNull(cellRendererFactory);
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
		public Builder<R, C> keyStrokes(Consumer<KeyboardShortcuts<FilterTableControl>> shortcuts) {
			requireNonNull(shortcuts).accept(this.shortcuts);
			return this;
		}

		@Override
		protected FilterTable<R, C> createComponent() {
			return new FilterTable<>(this);
		}

		@Override
		protected ComponentValue<Void, FilterTable<R, C>> createComponentValue(FilterTable<R, C> component) {
			throw new UnsupportedOperationException("A ComponentValue can not be based on a FilterTable");
		}

		@Override
		protected void setInitialValue(FilterTable<R, C> component, Void initialValue) {}
	}

	private final class DefaultSummaryValuesFactory implements SummaryValues.Factory<C> {

		@Override
		public <T extends Number> Optional<SummaryValues<T>> createSummaryValues(C columnIdentifier, Format format) {
			Class<?> columnClass = tableModel.getColumnClass(columnIdentifier);
			if (Number.class.isAssignableFrom(columnClass)) {
				return Optional.of(new DefaultSummaryValues<>(columnIdentifier, tableModel, format));
			}

			return Optional.empty();
		}
	}

	private static final class DefaultSummaryValues<T extends Number, C> implements SummaryValues<T> {

		private final C columnIdentifier;
		private final FilterTableModel<?, C> tableModel;
		private final Format format;
		private final Event<?> changeEvent = Event.event();

		private DefaultSummaryValues(C columnIdentifier, FilterTableModel<?, C> tableModel, Format format) {
			this.columnIdentifier = requireNonNull(columnIdentifier);
			this.tableModel = requireNonNull(tableModel);
			this.format = requireNonNull(format);
			this.tableModel.dataChangedEvent().addListener(changeEvent);
			this.tableModel.selectionModel().selectionEvent().addListener(changeEvent);
		}

		@Override
		public String format(Object value) {
			return format.format(value);
		}

		@Override
		public EventObserver<?> changeEvent() {
			return changeEvent.observer();
		}

		@Override
		public Collection<T> values() {
			return subset() ? tableModel.selectedValues(columnIdentifier) : tableModel.values(columnIdentifier);
		}

		@Override
		public boolean subset() {
			FilterTableSelectionModel<?> tableSelectionModel = tableModel.selectionModel();

			return tableSelectionModel.selectionNotEmpty().get() &&
							tableSelectionModel.selectionCount() != tableModel.visibleCount();
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
							tableModel.selectionModel().getSelectedIndexes() :
							IntStream.range(0, getRowCount())
											.boxed()
											.collect(toList());

			List<FilterTableColumn<C>> columns = new ArrayList<>(columnModel().visible());
			if (hidden) {
				columns.addAll(columnModel().hidden());
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

			return new StringBuilder()
							.append(lines.stream().map(line -> join(String.valueOf(delimiter), line))
											.collect(joining(Separators.LINE_SEPARATOR)))
							.toString();
		}

		private List<String> stringValues(int row, List<FilterTableColumn<C>> columns) {
			return columns.stream()
							.map(column -> tableModel.getStringAt(row, column.getIdentifier()))
							.collect(toList());
		}
	}

	private final class MoveResizeColumnKeyListener extends KeyAdapter {

		private final KeyStroke moveLeft;
		private final KeyStroke moveRight;
		private final KeyStroke increaseSize;
		private final KeyStroke decreaseSize;

		private MoveResizeColumnKeyListener(KeyboardShortcuts<FilterTableControl> shortcuts) {
			moveLeft = shortcuts.keyStroke(MOVE_COLUMN_LEFT).get();
			moveRight = shortcuts.keyStroke(MOVE_COLUMN_RIGHT).get();
			increaseSize = shortcuts.keyStroke(INCREASE_COLUMN_SIZE).get();
			decreaseSize = shortcuts.keyStroke(DECREASE_COLUMN_SIZE).get();
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (move(e)) {
				moveSelectedColumn(e.getKeyCode() == moveLeft.getKeyCode());
				e.consume();
			}
			else if (resize(e)) {
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

	private static final class DefaultFilterPanelFactory<C> implements ColumnConditionPanel.Factory<C> {

		@Override
		public <T> Optional<ColumnConditionPanel<C, T>> createConditionPanel(ColumnConditionModel<C, T> filterModel) {
			return columnConditionPanel(filterModel);
		}
	}

	private static final class DefaultFilterTableCellRendererFactory<R, C> implements FilterTableCellRendererFactory<C> {

		private final FilterTableModel<R, C> tableModel;

		private DefaultFilterTableCellRendererFactory(FilterTableModel<R, C> tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public TableCellRenderer tableCellRenderer(FilterTableColumn<C> column) {
			return FilterTableCellRenderer.builder(tableModel, column.getIdentifier(),
							tableModel.getColumnClass(column.getIdentifier())).build();
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
				return ((FilterTableColumn<?>) columnModel.getColumn(index)).toolTipText();
			}

			return null;
		}
	}
}