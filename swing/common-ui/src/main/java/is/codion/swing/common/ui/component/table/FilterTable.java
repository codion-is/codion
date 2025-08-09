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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

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
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSort.ColumnSortOrder;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ComponentFactory;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.ActionCommand;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Dimension;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;
import static is.codion.common.item.Item.item;
import static is.codion.common.model.summary.TableSummaryModel.tableSummaryModel;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.itemComboBox;
import static is.codion.swing.common.ui.component.table.FilterTable.ControlKeys.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.ActionEvent.ACTION_PERFORMED;
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
 * For instances use the builder {@link #builder()}
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @see #builder()
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
					integerValue(FilterTable.class.getName() + ".autoResizeMode", AUTO_RESIZE_OFF);

	/**
	 * Specifies whether columns can be rearranged
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> COLUMN_REORDERING =
					booleanValue(FilterTable.class.getName() + ".columnReordering", true);

	/**
	 * Specifies whether columns can be resized
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> COLUMN_RESIZING =
					booleanValue(FilterTable.class.getName() + ".columnResizing", true);

	/**
	 * Specifies whether the table resizes the row being edited to fit the editor component. Only applicable to {@link FilterTableCellEditor}.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> RESIZE_ROW_TO_FIT_EDITOR =
					booleanValue(FilterTable.class.getName() + ".resizeRowToFitEditor", true);

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
		 * Toggles the sort on the selected column from {@link SortOrder#ASCENDING} to {@link SortOrder#DESCENDING} to {@link SortOrder#UNSORTED}.<br>
		 * Default key stroke: ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_NEXT_SORT_ORDER = CommandControl.key("toggleNextSortOrder", keyStroke(VK_DOWN, ALT_DOWN_MASK));
		/**
		 * Toggles the sort on the selected column from {@link SortOrder#ASCENDING} to {@link SortOrder#UNSORTED} to {@link SortOrder#DESCENDING}.<br>
		 * Default key stroke: ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_PREVIOUS_SORT_ORDER = CommandControl.key("togglePreviousSortOrder", keyStroke(VK_UP, ALT_DOWN_MASK));
		/**
		 * Toggles the sort on the selected column from {@link SortOrder#ASCENDING} to {@link SortOrder#DESCENDING} to {@link SortOrder#UNSORTED}, adding it to any already sorted columns.<br>
		 * Default key stroke: SHIFT-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_NEXT_SORT_ORDER_ADD = CommandControl.key("toggleNextSortOrderAdd", keyStroke(VK_DOWN, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Toggles the sort on the selected column from {@link SortOrder#ASCENDING} to {@link SortOrder#UNSORTED} to {@link SortOrder#DESCENDING}, adding it to any already sorted columns.<br>
		 * Default key stroke: SHIFT-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> TOGGLE_PREVIOUS_SORT_ORDER_ADD = CommandControl.key("togglePreviousSortOrderAdd", keyStroke(VK_UP, SHIFT_DOWN_MASK | ALT_DOWN_MASK));

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
	private static final String SINGLE_SELECTION = "single_selection";
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
	private final TableSummaryModel<C> summaryModel;

	private final TableConditionPanel.Factory<C> filterPanelFactory;
	private final ComponentFactory filterComponentFactory;
	private final Event<MouseEvent> doubleClicked = Event.event();
	private final Value<Action> doubleClick;
	private final State sortable;
	private final State scrollToSelectedItem;
	private final Value<CenterOnScroll> centerOnScroll;
	private final boolean scrollToAddedItem;

	private final ControlMap controlMap;

	private @Nullable TableConditionPanel<C> filterPanel;
	private @Nullable JTextField searchField;

	private FilterTable(DefaultBuilder<R, C> builder) {
		super(builder.tableModel,
						new DefaultFilterTableColumnModel<>(builder.columns),
						builder.tableModel.selection());
		this.tableModel = builder.tableModel;
		this.searchModel = new DefaultFilterTableSearchModel<>(tableModel, columnModel());
		this.summaryModel = tableSummaryModel(builder.summaryValuesFactory == null ?
						new DefaultSummaryValuesFactory() : builder.summaryValuesFactory);
		this.filterPanelFactory = builder.filterPanelFactory;
		this.filterComponentFactory = builder.filterComponentFactory;
		this.centerOnScroll = Value.builder()
						.nonNull(CenterOnScroll.NEITHER)
						.value(builder.centerOnScroll)
						.build();
		this.doubleClick = Value.nullable(builder.doubleClick);
		this.scrollToSelectedItem = State.state(builder.scrollToSelectedItem);
		this.scrollToAddedItem = builder.scrollToAddedItem;
		this.sortable = State.state(builder.sortable);
		this.controlMap = builder.controlMap;
		this.controlMap.control(COPY_CELL).set(createCopyCellControl());
		this.controlMap.control(TOGGLE_PREVIOUS_SORT_ORDER).set(createToggleSortOrderControl(true));
		this.controlMap.control(TOGGLE_NEXT_SORT_ORDER).set(createToggleSortOrderControl(false));
		this.controlMap.control(TOGGLE_PREVIOUS_SORT_ORDER_ADD).set(createToggleSortOrderAddControl(true));
		this.controlMap.control(TOGGLE_NEXT_SORT_ORDER_ADD).set(createToggleSortOrderAddControl(false));
		CommandControl startEditing = Control.action(new StartEditing());
		builder.startEditKeyStrokes.forEach(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(startEditing)
						.enable(this));
		filters().view().set(builder.filterView);
		autoStartsEdit(builder.autoStartsEdit);
		setSurrendersFocusOnKeystroke(builder.surrendersFocusOnKeystroke);
		setSelectionMode(builder.selectionMode);
		setAutoResizeMode(builder.autoResizeMode);
		configureColumns(builder.cellRenderers, builder.cellRendererFactory, builder.cellEditors, builder.cellEditorFactory);
		configureTableHeader(builder.columnReordering, builder.columnResizing);
		bindEvents(builder.columnReordering, builder.columnResizing);
		if (builder.resizeRowToFitEditor) {
			addPropertyChangeListener(TABLE_CELL_EDITOR, new ResizeRowToFitEditor());
		}
		if (builder.cellSelection) {
			setCellSelectionEnabled(true);
		}
		if (builder.rowHeight > 0) {
			setRowHeight(builder.rowHeight);
		}
		if (builder.rowMargin > 0) {
			setRowMargin(builder.rowMargin);
		}
		if (builder.intercellSpacing != null) {
			setIntercellSpacing(builder.intercellSpacing);
		}
		if (builder.gridColor != null) {
			setGridColor(builder.gridColor);
		}
		if (builder.showGrid != null) {
			setShowGrid(builder.showGrid);
		}
		if (builder.showHorizontalLines != null) {
			setShowHorizontalLines(builder.showHorizontalLines);
		}
		if (builder.showVerticalLines != null) {
			setShowVerticalLines(builder.showVerticalLines);
		}
		if (builder.dragEnabled != null) {
			setDragEnabled(builder.dragEnabled);
		}
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(getTableHeader(), searchField, filterPanel);
		Utilities.updateUI(columnModel().hidden().columns().stream()
						.flatMap(FilterTable::columnComponents)
						.collect(toList()));
		updateCellEditorUI();
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
	public FilterTableSearchModel search() {
		return searchModel;
	}

	/**
	 * @return the summary model
	 */
	public TableSummaryModel<C> summaries() {
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
	 * <p>The {@link Action} is only triggered if enabled.
	 * <p>The {@link ActionEvent} propagated when this action is performed, contains the associated {@link MouseEvent} as source.
	 * {@snippet :
	 *   public void actionPerformed(ActionEvent event) {
	 *       MouseEvent mouseEvent = (MouseEvent) event.getSource();
	 *       Point location = mouseEvent.getLocationOnScreen();
	 *       // ...
	 *   }
	 *}
	 * @return the {@link Value} controlling the action to perform when a double click is performed on the table
	 */
	public Value<Action> doubleClick() {
		return doubleClick;
	}

	/**
	 * @return the {@link State} controlling whether sorting via the table header is enabled
	 */
	public State sortable() {
		return sortable;
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
		Dialogs.okCancel()
						.component(columnSelectionPanel)
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
		ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue = itemComboBox()
						.items(AUTO_RESIZE_MODES)
						.value(getAutoResizeMode())
						.buildValue();
		Dialogs.okCancel()
						.component(borderLayoutPanel()
										.center(componentValue.component())
										.border(Borders.emptyBorder()))
						.owner(getParent())
						.title(MESSAGES.getString(AUTO_RESIZE))
						.onOk(() -> setAutoResizeMode(componentValue.getOrThrow()))
						.show();
	}

	/**
	 * Returns true if the given cell is visible.
	 * @param row the row
	 * @param column the column
	 * @return true if this table is contained in a scrollpanel and the cell with the given coordinates is visible.
	 */
	public boolean cellVisible(int row, int column) {
		JViewport viewport = parentOfType(JViewport.class, this);

		return viewport != null && cellVisible(viewport, row, column);
	}

	/**
	 * Scrolls horizontally so that the column identified by the given identifier becomes visible.
	 * Has no effect if this table is not contained in a scrollpanel.
	 * @param identifier the column identifier
	 */
	public void scrollToColumn(C identifier) {
		requireNonNull(identifier);
		JViewport viewport = parentOfType(JViewport.class, this);
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
	public void scrollToRowColumn(int row, int column, CenterOnScroll centerOnScroll) {
		requireNonNull(centerOnScroll);
		JViewport viewport = parentOfType(JViewport.class, this);
		if (viewport != null) {
			scrollToRowColumn(viewport, row, column, centerOnScroll);
		}
	}

	/**
	 * Copies the contents of the selected cell to the clipboard.
	 */
	public void copySelectedCell() {
		int selectedRow = getSelectedRow();
		int selectedColumn = columnModel.getSelectionModel().getLeadSelectionIndex();
		if (selectedRow >= 0 && selectedColumn >= 0) {
			FilterTableColumn<C> column = columnModel().getColumn(selectedColumn);
			Utilities.setClipboard(model().values().string(selectedRow, column.identifier()));
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
						.caption(MESSAGES.getString(SELECT) + "...")
						.enabled(columnModel().locked().not())
						.description(MESSAGES.getString(SELECT_COLUMNS))
						.build();
	}

	/**
	 * @return Controls containing {@link ToggleControl}s for showing/hiding columns.
	 */
	public Controls createToggleColumnsControls() {
		return Controls.builder()
						.caption(MESSAGES.getString(SELECT))
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
						.caption(MESSAGES.getString(RESET))
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
						.caption(MESSAGES.getString(AUTO_RESIZE) + "...")
						.build();
	}

	/**
	 * @return Controls containing {@link ToggleControl}s for choosing the auto resize mode.
	 */
	public Controls createToggleAutoResizeModeControls() {
		return Controls.builder()
						.caption(MESSAGES.getString(AUTO_RESIZE))
						.controls(createAutoResizeModeControls())
						.build();
	}

	/**
	 * @return a ToggleControl controlling whether single selection mode is enabled
	 */
	public ToggleControl createSingleSelectionControl() {
		return Control.builder()
						.toggle(tableModel.selection().singleSelection())
						.caption(MESSAGES.getString(SINGLE_SELECTION))
						.build();
	}

	/**
	 * @return a Control for copying the contents of the selected cell
	 */
	public CommandControl createCopyCellControl() {
		return Control.builder()
						.command(this::copySelectedCell)
						.caption(MESSAGES.getString("copy_cell"))
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
	public Observer<MouseEvent> doubleClicked() {
		return doubleClicked.observer();
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
	 * @return a {@link Builder.ModelStep} instance
	 */
	public static Builder.ModelStep builder() {
		return DefaultBuilder.MODEL;
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

		return Components.stringField()
						.link(searchModel.searchString())
						.minimumWidth(SEARCH_FIELD_MINIMUM_WIDTH)
						.preferredWidth(SEARCH_FIELD_MINIMUM_WIDTH)
						.selectAllOnFocusGained(true)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.action(nextResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectNextResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DOWN)
										.action(nextResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DOWN)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectNextResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_UP)
										.action(previousResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_UP)
										.modifiers(SHIFT_DOWN_MASK)
										.action(selectPreviousResult))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ESCAPE)
										.action(requestTableFocus))
						.popupMenuControls(textField -> searchFieldPopupMenuControls())
						.hint(Messages.find() + "...")
						.onTextChanged(this::onSearchTextChanged)
						.onBuild(field -> KeyEvents.builder()
										.keyCode(VK_F)
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
			scrollToRowColumn(rowColumn.row(), rowColumn.column(), centerOnScroll.getOrThrow());
		});
	}

	private Optional<RowColumn> searchResult(boolean addToSelection, boolean next) {
		if (next) {
			return addToSelection ? searchModel.results().selectNext() : searchModel.results().next();
		}

		return addToSelection ? searchModel.results().selectPrevious() : searchModel.results().previous();
	}

	private void onSearchTextChanged(String searchText) {
		if (!searchText.isEmpty()) {
			searchModel.results().next();
		}
	}

	private void toggleColumnSort(int selectedColumn, boolean previous, boolean add) {
		if (sortable.is() && selectedColumn != -1) {
			C identifier = columnModel().getColumn(selectedColumn).identifier();
			if (!tableModel.sort().order(identifier).locked().is()) {
				toggleColumnSort(identifier, previous, add);
			}
		}
	}

	private void toggleColumnSort(C identifier, boolean previous, boolean add) {
		ColumnSortOrder<C> columnSortOrder = tableModel.sort().columns().get(identifier);
		if (add) {
			tableModel.sort().order(identifier).add(previous ? previous(columnSortOrder.sortOrder()) : next(columnSortOrder.sortOrder()));
		}
		else {
			tableModel.sort().order(identifier).set(previous ? previous(columnSortOrder.sortOrder()) : next(columnSortOrder.sortOrder()));
		}
	}

	private Controls searchFieldPopupMenuControls() {
		return Controls.builder()
						.control(Control.builder()
										.toggle(searchModel.caseSensitive())
										.caption(MESSAGES.getString("case_sensitive_search")))
						.control(Control.builder()
										.toggle(searchModel.regularExpression())
										.caption(MESSAGES.getString("regular_expression_search")))
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
						.caption(String.valueOf(column.getHeaderValue()))
						.description(column.toolTipText().orElse(null))
						.build();
	}

	private void configureColumns(Map<C, FilterTableCellRenderer<?>> cellRenderers,
																FilterTableCellRenderer.Factory<R, C> cellRendererFactory,
																Map<C, FilterTableCellEditor<?>> cellEditors,
																FilterTableCellEditor.@Nullable Factory<C> cellEditorFactory) {
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

	private List<ToggleControl> createAutoResizeModeControls() {
		List<ToggleControl> controls = new ArrayList<>();
		State.Group group = State.group();
		for (Item<Integer> resizeMode : AUTO_RESIZE_MODES) {
			State state = State.state(resizeMode.getOrThrow().equals(getAutoResizeMode()));
			group.add(state);
			state.addConsumer(enabled -> {
				if (enabled) {
					setAutoResizeMode(resizeMode.getOrThrow());
				}
			});
			controls.add(Control.builder()
							.toggle(state)
							.caption(resizeMode.caption())
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
		tableModel.items().visible().added().addConsumer(new ScrollToAdded());
		tableModel.filters().changed().addListener(getTableHeader()::repaint);
		searchModel.results().current().addListener(this::repaint);
		tableModel.sort().observer().addListener(getTableHeader()::repaint);
		addMouseListener(new FilterTableMouseListener());
		addKeyListener(new MoveResizeColumnKeyListener(columnReorderingAllowed, columnResizingAllowed));
		controlMap.keyEvent(COPY_CELL).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_PREVIOUS_SORT_ORDER_ADD).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_NEXT_SORT_ORDER_ADD).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_PREVIOUS_SORT_ORDER).ifPresent(keyEvent -> keyEvent.enable(this));
		controlMap.keyEvent(TOGGLE_NEXT_SORT_ORDER).ifPresent(keyEvent -> keyEvent.enable(this));
	}

	private void onColumnHidden(C columnIdentifier) {
		//disable the filter model for the column to be hidden, to prevent confusion
		tableModel.filters().optional(columnIdentifier)
						.ifPresent(condition -> condition.enabled().set(false));
	}

	private CommandControl createToggleSortOrderAddControl(boolean previous) {
		return command(() -> toggleColumnSort(columnModel.getSelectionModel().getLeadSelectionIndex(), previous, true));
	}

	private CommandControl createToggleSortOrderControl(boolean previous) {
		return command(() -> toggleColumnSort(columnModel.getSelectionModel().getLeadSelectionIndex(), previous, false));
	}

	private static Stream<JComponent> columnComponents(FilterTableColumn<?> column) {
		Collection<JComponent> components = new ArrayList<>(3);
		addIfComponent(components, column.getCellRenderer());
		addIfComponent(components, column.getHeaderRenderer());

		return components.stream();
	}

	private Map<C, ConditionPanel<?>> createFilterPanels() {
		Map<C, ConditionPanel<?>> conditionPanels = new HashMap<>();
		for (Map.Entry<C, ConditionModel<?>> entry : tableModel.filters().get().entrySet()) {
			ConditionModel<?> condition = entry.getValue();
			C identifier = entry.getKey();
			if (columnModel().contains(identifier) && filterComponentFactory.supportsType(condition.valueClass())) {
				conditionPanels.put(identifier, ColumnConditionPanel.builder()
								.model(condition)
								.componentFactory(filterComponentFactory)
								.tableColumn(columnModel().column(identifier))
								.build());
			}
		}

		return conditionPanels;
	}

	private void configureFilterConditionPanel(TableConditionPanel<C> filterConditionPanel) {
		filterConditionPanel.panels().forEach(this::configureFilterPanel);
	}

	private void configureFilterPanel(C identifier, ConditionPanel<?> filterPanel) {
		filterPanel.focusGained().ifPresent(focusGained ->
						focusGained.addListener(() -> scrollToColumn(identifier)));
	}

	private void updateCellEditorUI() {
		columnModel().columns().forEach(column -> {
			TableCellEditor columnCellEditor = column.getCellEditor();
			if (columnCellEditor instanceof DefaultFilterTableCellEditor) {
				((DefaultFilterTableCellEditor<?>) columnCellEditor).updateUI();
			}
			else if (columnCellEditor instanceof DefaultCellEditor) {
				((JComponent) ((DefaultCellEditor) columnCellEditor).getComponent()).updateUI();
			}
			else if (columnCellEditor instanceof JComponent) {
				((JComponent) columnCellEditor).updateUI();
			}
		});
	}

	private static void addIfComponent(Collection<JComponent> components, Object object) {
		if (object instanceof JComponent) {
			components.add((JComponent) object);
		}
	}

	private static SortOrder next(SortOrder sortOrder) {
		switch (sortOrder) {
			case UNSORTED:
				return SortOrder.ASCENDING;
			case ASCENDING:
				return SortOrder.DESCENDING;
			case DESCENDING:
				return SortOrder.UNSORTED;
			default:
				throw new IllegalStateException();
		}
	}

	private static SortOrder previous(SortOrder sortOrder) {
		switch (sortOrder) {
			case UNSORTED:
				return SortOrder.DESCENDING;
			case ASCENDING:
				return SortOrder.UNSORTED;
			case DESCENDING:
				return SortOrder.ASCENDING;
			default:
				throw new IllegalStateException();
		}
	}

	private final class FilterTableMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() == 2) {
				doubleClick.optional()
								.filter(Action::isEnabled)
								.ifPresent(action -> action.actionPerformed(new ActionEvent(event, ACTION_PERFORMED, "doubleClick")));
				doubleClicked.accept(event);
			}
		}
	}

	private final class ScrollToSelected implements Consumer<List<Integer>> {

		@Override
		public void accept(List<Integer> selectedRows) {
			JViewport viewport = parentOfType(JViewport.class, FilterTable.this);
			if (viewport != null && scrollToSelectedItem.is() && !selectedRows.isEmpty()) {
				int column = getSelectedColumn();
				if (noCellVisible(viewport, selectedRows, column)) {
					scrollToRowColumn(selectedRows.get(0), column, centerOnScroll.getOrThrow());
				}
			}
		}

		private boolean noCellVisible(JViewport viewport, List<Integer> rows, int column) {
			return rows.stream().noneMatch(row -> cellVisible(viewport, row, column));
		}

		private boolean cellVisible(JViewport viewport, int row, int column) {
			return viewport.getViewRect().contains(getCellRect(row, column, true));
		}
	}

	private final class ScrollToAdded implements Consumer<Collection<R>> {

		@Override
		public void accept(Collection<R> addedItems) {
			JViewport viewport = parentOfType(JViewport.class, FilterTable.this);
			if (viewport != null && scrollToAddedItem && !addedItems.isEmpty()) {
				Set<R> items = new HashSet<>(addedItems);
				List<R> visibleItems = tableModel.items().visible().get();
				for (int row = 0; row < visibleItems.size(); row++) {
					if (items.contains(visibleItems.get(row))) {
						scrollToAddedRow(viewport, row);
						return;
					}
				}
			}
		}

		private void scrollToAddedRow(JViewport viewport, int row) {
			Rectangle cellRectangle = getCellRect(row, 0, true);
			Rectangle viewRectangle = viewport.getViewRect();
			cellRectangle.setLocation(0, cellRectangle.y - viewRectangle.y);
			cellRectangle.height = viewRectangle.height;
			viewport.scrollRectToVisible(cellRectangle);
		}
	}

	private final class MouseSortHandler extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!sortable.is() || e.getButton() != MouseEvent.BUTTON1 || e.isControlDown()) {
				return;
			}

			FilterTableColumnModel<C> columnModel = columnModel();
			int index = columnModel.getColumnIndexAtX(e.getX());
			if (index >= 0) {
				C identifier = columnModel.getColumn(index).identifier();
				if (!tableModel.sort().order(identifier).locked().is()) {
					if (!getSelectionModel().isSelectionEmpty()) {
						setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
					}
					toggleColumnSort(identifier, e.isShiftDown(), e.isAltDown());
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

	private final class StartEditing implements ActionCommand {

		private static final String START_EDITING = "startEditing";

		@Override
		public void execute(ActionEvent actionEvent) throws Exception {
			Action startEditing = getActionMap().get(START_EDITING);
			if (startEditing != null) {
				startEditing.actionPerformed(actionEvent);
			}
		}
	}

	/**
	 * A builder for a {@link FilterTable}
	 * @param <R> the type representing rows
	 * @param <C> the type used to identify columns
	 */
	public interface Builder<R, C> extends ComponentBuilder<FilterTable<R, C>, Builder<R, C>> {

		/**
		 * Provides a {@link Builder} instance based on a given table model
		 */
		interface ModelStep {

			/**
			 * @param model the table model
			 * @return a {@link Builder} based on the given columns
			 */
			<R, C> ColumnsBuilder<R, C> model(FilterTableModel<R, C> model);
		}

		/**
		 * @param <R> the type representing rows
		 * @param <C> the type used to identify columns
		 */
		interface ColumnsBuilder<R, C> {

			/**
			 * @param columns the columns
			 * @return a {@link Builder} based on the given columns
			 * @throws IllegalArgumentException in case the column identifiers are not unique
			 */
			Builder<R, C> columns(List<FilterTableColumn<C>> columns);
		}

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
		 * @param componentFactory the column filter component factory
		 * @return this builder instance
		 * @see FilterTable#filters()
		 */
		Builder<R, C> filterComponentFactory(ComponentFactory componentFactory);

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
		 * Associates the given keyStroke with the action associated with the 'startEditing' key in the table action map.
		 * @param keyStroke a keyStroke for triggering the 'startEditing' action
		 * @return this builder instance
		 */
		Builder<R, C> startEditing(KeyStroke keyStroke);

		/**
		 * @param autoStartsEdit true if editing should start automatically
		 * @return this builder instance
		 */
		Builder<R, C> autoStartsEdit(boolean autoStartsEdit);

		/**
		 * @param surrendersFocusOnKeystroke true if the table should surrenders focus on keystroke
		 * @return this builder instance
		 * @see JTable#setSurrendersFocusOnKeystroke(boolean)
		 */
		Builder<R, C> surrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke);

		/**
		 * @param centerOnScroll the center on scroll behavious
		 * @return this builder instance
		 */
		Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll);

		/**
		 * @param doubleClick the action to perform on a double-click
		 * @return this builder instance
		 */
		Builder<R, C> doubleClick(Action doubleClick);

		/**
		 * @param scrollToSelectedItem true if this table should scroll to the selected item
		 * @return this builder instance
		 */
		Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem);

		/**
		 * <p>Specifies whether the table should scroll when items are added, so that the topmost added item appears at the top of the table view.
		 * <p>Default: false.
		 * @param scrollToAddedItem true if this table should scroll to the topmost added item
		 * @return this builder instance
		 */
		Builder<R, C> scrollToAddedItem(boolean scrollToAddedItem);

		/**
		 * @param sortable true if sorting via clicking the header should be enbled
		 * @return this builder instance
		 */
		Builder<R, C> sortable(boolean sortable);

		/**
		 * @param selectionMode the table selection mode
		 * @return this builder instance
		 * @see JTable#setSelectionMode(int)
		 */
		Builder<R, C> selectionMode(int selectionMode);

		/**
		 * @param cellSelection true if cell selection should be enabled
		 * @return this builder instance
		 * @see JTable#setCellSelectionEnabled(boolean)
		 */
		Builder<R, C> cellSelection(boolean cellSelection);

		/**
		 * @param columnReordering true if column reordering should be allowed
		 * @return this builder instance
		 */
		Builder<R, C> columnReordering(boolean columnReordering);

		/**
		 * @param columnResizing true if column resizing should be allowed
		 * @return this builder instance
		 */
		Builder<R, C> columnResizing(boolean columnResizing);

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

		/**
		 * @param rowHeight the row height
		 * @return this builder instance
		 * @see JTable#setRowHeight(int)
		 */
		Builder<R, C> rowHeight(int rowHeight);

		/**
		 * @param rowMargin the row margin
		 * @return this builder instance
		 * @see JTable#setRowMargin(int)
		 */
		Builder<R, C> rowMargin(int rowMargin);

		/**
		 * @param intercellSpacing the intercell spacing
		 * @return this builder instance
		 * @see JTable#setIntercellSpacing(Dimension)
		 */
		Builder<R, C> intercellSpacing(Dimension intercellSpacing);

		/**
		 * @param gridColor the grid color
		 * @return this builder instance
		 * @see JTable#setGridColor(Color)
		 */
		Builder<R, C> gridColor(Color gridColor);

		/**
		 * @param showGrid the show grid value
		 * @return this builder instance
		 * @see JTable#setShowGrid(boolean)
		 */
		Builder<R, C> showGrid(boolean showGrid);

		/**
		 * @param showHorizontalLines the show horizontal lines value
		 * @return this builder instance
		 * @see JTable#setShowHorizontalLines(boolean)
		 */
		Builder<R, C> showHorizontalLines(boolean showHorizontalLines);

		/**
		 * @param showVerticalLines the show vertical lines value
		 * @return this builder instance
		 * @see JTable#setShowVerticalLines(boolean)
		 */
		Builder<R, C> showVerticalLines(boolean showVerticalLines);

		/**
		 * @param dragEnabled the drag enabled value
		 * @return this builder instance
		 * @see JTable#setDragEnabled(boolean)
		 */
		Builder<R, C> dragEnabled(boolean dragEnabled);
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
		 * <p>Replaces newlines inside strings.
		 * <p>Note that strings are always trimmed so newlines at the
		 * beginning and end of strings are trimmed before replacement is performed.
		 * <p>Default replacement is a single whitespace (" ").
		 * <p>Set to null to keep newlines in place.
		 * @param replacement the string to use when replacing newlines
		 * @return this Export instance
		 */
		Export replaceNewline(String replacement);

		/**
		 * @return the table data exported to a String
		 */
		String get();
	}

	private static class DefaultModelStep implements Builder.ModelStep {

		@Override
		public <R, C> Builder.ColumnsBuilder<R, C> model(FilterTableModel<R, C> model) {
			return new DefaultColumnsBuilder<>(requireNonNull(model));
		}
	}

	private static class DefaultColumnsBuilder<R, C> implements Builder.ColumnsBuilder<R, C> {

		private final FilterTableModel<R, C> tableModel;

		private DefaultColumnsBuilder(FilterTableModel<R, C> tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public Builder<R, C> columns(List<FilterTableColumn<C>> columns) {
			return new DefaultBuilder<>(tableModel, requireNonNull(columns));
		}
	}

	private static final class DefaultBuilder<R, C>
					extends AbstractComponentBuilder<FilterTable<R, C>, Builder<R, C>>
					implements Builder<R, C> {

		private static final Builder.ModelStep MODEL = new DefaultModelStep();

		private final FilterTableModel<R, C> tableModel;
		private final List<FilterTableColumn<C>> columns;
		private final ControlMap controlMap = controlMap(ControlKeys.class);
		private final Map<C, FilterTableCellRenderer<?>> cellRenderers = new HashMap<>();
		private final Map<C, FilterTableCellEditor<?>> cellEditors = new HashMap<>();
		private final Collection<KeyStroke> startEditKeyStrokes = new ArrayList<>();

		private SummaryValues.@Nullable Factory<C> summaryValuesFactory;
		private TableConditionPanel.Factory<C> filterPanelFactory = new DefaultFilterPanelFactory<>();
		private ComponentFactory filterComponentFactory = new FilterComponentFactory();
		private FilterTableCellRenderer.Factory<R, C> cellRendererFactory;
		private FilterTableCellEditor.@Nullable Factory<C> cellEditorFactory;
		private boolean autoStartsEdit = false;
		private boolean surrendersFocusOnKeystroke = false;
		private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;
		private @Nullable Action doubleClick;
		private boolean scrollToSelectedItem = true;
		private boolean scrollToAddedItem = false;
		private boolean sortable = true;
		private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		private boolean cellSelection = false;
		private boolean columnReordering = COLUMN_REORDERING.getOrThrow();
		private boolean columnResizing = COLUMN_RESIZING.getOrThrow();
		private int autoResizeMode = AUTO_RESIZE_MODE.getOrThrow();
		private boolean resizeRowToFitEditor = RESIZE_ROW_TO_FIT_EDITOR.getOrThrow();
		private ConditionView filterView = ConditionView.HIDDEN;
		private int rowHeight = -1;
		private int rowMargin = -1;
		private @Nullable Dimension intercellSpacing;
		private @Nullable Color gridColor;
		private @Nullable Boolean showGrid;
		private @Nullable Boolean showHorizontalLines;
		private @Nullable Boolean showVerticalLines;
		private @Nullable Boolean dragEnabled;

		private DefaultBuilder(FilterTableModel<R, C> tableModel, List<FilterTableColumn<C>> columns) {
			this.tableModel = tableModel;
			this.columns = new ArrayList<>(validateColumns(columns));
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
		public Builder<R, C> filterComponentFactory(ComponentFactory componentFactory) {
			this.filterComponentFactory = requireNonNull(componentFactory);
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
		public Builder<R, C> startEditing(KeyStroke keyStroke) {
			this.startEditKeyStrokes.add(requireNonNull(keyStroke));
			return this;
		}

		@Override
		public Builder<R, C> autoStartsEdit(boolean autoStartsEdit) {
			this.autoStartsEdit = autoStartsEdit;
			return this;
		}

		@Override
		public Builder<R, C> surrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
			this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
			return this;
		}

		@Override
		public Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll) {
			this.centerOnScroll = requireNonNull(centerOnScroll);
			return this;
		}

		@Override
		public Builder<R, C> doubleClick(Action doubleClick) {
			this.doubleClick = requireNonNull(doubleClick);
			return this;
		}

		@Override
		public Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem) {
			this.scrollToSelectedItem = scrollToSelectedItem;
			return this;
		}

		@Override
		public Builder<R, C> scrollToAddedItem(boolean scrollToAddedItem) {
			this.scrollToAddedItem = scrollToAddedItem;
			return this;
		}

		@Override
		public Builder<R, C> sortable(boolean sortable) {
			this.sortable = sortable;
			return this;
		}

		@Override
		public Builder<R, C> selectionMode(int selectionMode) {
			this.selectionMode = selectionMode;
			return this;
		}

		@Override
		public Builder<R, C> cellSelection(boolean cellSelection) {
			this.cellSelection = cellSelection;
			return this;
		}

		@Override
		public Builder<R, C> columnReordering(boolean columnReordering) {
			this.columnReordering = columnReordering;
			return this;
		}

		@Override
		public Builder<R, C> columnResizing(boolean columnResizing) {
			this.columnResizing = columnResizing;
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
		public Builder<R, C> rowHeight(int rowHeight) {
			this.rowHeight = rowHeight;
			return this;
		}

		@Override
		public Builder<R, C> rowMargin(int rowMargin) {
			this.rowMargin = rowMargin;
			return this;
		}

		@Override
		public Builder<R, C> intercellSpacing(Dimension intercellSpacing) {
			this.intercellSpacing = intercellSpacing;
			return this;
		}

		@Override
		public Builder<R, C> gridColor(Color gridColor) {
			this.gridColor = gridColor;
			return this;
		}

		@Override
		public Builder<R, C> showGrid(boolean showGrid) {
			this.showGrid = showGrid;
			return this;
		}

		@Override
		public Builder<R, C> showHorizontalLines(boolean showHorizontalLines) {
			this.showHorizontalLines = showHorizontalLines;
			return this;
		}

		@Override
		public Builder<R, C> showVerticalLines(boolean showVerticalLines) {
			this.showVerticalLines = showVerticalLines;
			return this;
		}

		@Override
		public Builder<R, C> dragEnabled(boolean dragEnabled) {
			this.dragEnabled = dragEnabled;
			return this;
		}

		@Override
		protected FilterTable<R, C> createComponent() {
			return new FilterTable<>(this);
		}

		private Collection<FilterTableColumn<C>> validateColumns(List<FilterTableColumn<C>> columns) {
			if (columns.stream()
							.map(new ColumnIdentifier<>())
							.distinct()
							.count() != columns.size()) {
				throw new IllegalArgumentException("Column identifiers are not unique");
			}
			List<Integer> modelIndexes = columns.stream()
							.map(new ModelIndex())
							.sorted()
							.collect(toList());
			for (int i = 0; i < modelIndexes.size(); i++) {
				if (modelIndexes.get(i) != i) {
					throw new IllegalArgumentException("Column model indexes should start with zero, be unique and continuous");
				}
			}

			return columns;
		}

		private static final class ColumnIdentifier<C> implements Function<FilterTableColumn<C>, C> {

			@Override
			public C apply(FilterTableColumn<C> column) {
				return column.identifier();
			}
		}

		private static final class ModelIndex implements Function<FilterTableColumn<?>, Integer> {

			@Override
			public Integer apply(FilterTableColumn<?> column) {
				return column.getModelIndex();
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
			return subset() ? tableModel.values().selected(identifier) : tableModel.values().get(identifier);
		}

		@Override
		public boolean subset() {
			FilterListSelection<?> selection = tableModel.selection();

			return selection.empty().not().is() &&
							selection.count() != tableModel.items().visible().count();
		}
	}

	private final class DefaultExport implements Export {

		private char delimiter = '\t';
		private boolean header = true;
		private boolean hidden = false;
		private boolean selected = false;
		private String newlineReplacement = " ";

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
		public Export replaceNewline(String replacement) {
			this.newlineReplacement = replacement;
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
							.map(column -> tableModel.values().string(row, column.identifier()))
							.map(String::trim)
							.map(this::replaceNewlines)
							.collect(toList());
		}

		private String replaceNewlines(String string) {
			if (newlineReplacement != null) {
				return string.replace("\r\n", newlineReplacement).replace("\n", newlineReplacement).replace("\r", newlineReplacement);
			}

			return string;
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

		private final @Nullable KeyStroke moveLeft;
		private final @Nullable KeyStroke moveRight;
		private final @Nullable KeyStroke increaseSize;
		private final @Nullable KeyStroke decreaseSize;

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
			int selectedColumnIndex = columnModel.getSelectionModel().getLeadSelectionIndex();
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
			int selectedColumnIndex = columnModel.getSelectionModel().getLeadSelectionIndex();
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

	private static final class FilterTableHeader extends JTableHeader {

		private FilterTableHeader(TableColumnModel columnModel) {
			super(columnModel);
		}

		@Override
		public @Nullable String getToolTipText(MouseEvent event) {
			int index = columnModel.getColumnIndexAtX(event.getPoint().x);
			if (index != -1) {
				return ((FilterTableColumn<?>) columnModel.getColumn(index)).toolTipText().orElse(null);
			}

			return null;
		}
	}
}
