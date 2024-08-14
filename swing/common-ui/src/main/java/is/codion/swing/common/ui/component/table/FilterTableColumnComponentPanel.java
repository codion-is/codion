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

import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A panel that synchronizes child component sizes and positions to table columns.
 * For instances use the {@link #filterTableColumnComponentPanel(FilterTableColumnModel, Map)} factory method.
 * @param <C> the type identifying the table columns
 * @see #filterTableColumnComponentPanel(FilterTableColumnModel, Map)
 */
public final class FilterTableColumnComponentPanel<C> extends JPanel {

	private final FilterTableColumnModel<C> columnModel;
	private final Collection<FilterTableColumn<C>> columns;
	private final Box.Filler scrollBarFiller;
	private final JPanel basePanel;
	private final Map<C, JComponent> components;
	private final Map<C, JPanel> nullComponents = new HashMap<>(0);

	private FilterTableColumnComponentPanel(FilterTableColumnModel<C> columnModel, Map<C, ? extends JComponent> components) {
		this.columnModel = requireNonNull(columnModel);
		this.columns = columnModel.columns();
		requireNonNull(components).forEach((identifier, component) -> {
			if (!columnModel.containsColumn(identifier)) {
				throw new IllegalArgumentException("Column " + identifier + " is not part of column model");
			}
		});
		this.components = Collections.unmodifiableMap(components);
		this.basePanel = new JPanel(FlexibleGridLayout.builder()
						.rows(1)
						.build());
		Dimension fillerSize = new Dimension(UIManager.getInt("ScrollBar.width"), 0);
		this.scrollBarFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
		setLayout(new BorderLayout());
		add(basePanel, BorderLayout.WEST);
		bindColumnAndComponentSizes();
		resetPanel();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(scrollBarFiller, basePanel);
		if (components != null) {
			Utilities.updateUI(components.values());
		}
		if (nullComponents != null) {
			Utilities.updateUI(nullComponents.values());
		}
	}

	/**
	 * @return the column components mapped their respective column identifiers
	 */
	public Map<C, JComponent> components() {
		return components;
	}

	/**
	 * Instantiates a new {@link FilterTableColumnComponentPanel}.
	 * @param columnModel the column model
	 * @param columnComponents the column components mapped to their respective column
	 * @param <C> the column identifier type
	 * @return a new {@link FilterTableColumnComponentPanel}
	 */
	public static <C> FilterTableColumnComponentPanel<C> filterTableColumnComponentPanel(FilterTableColumnModel<C> columnModel,
																																											 Map<C, ? extends JComponent> columnComponents) {
		return new FilterTableColumnComponentPanel<>(columnModel, columnComponents);
	}

	private void resetPanel() {
		Component childFocusOwner = childFocusOwner();
		if (childFocusOwner != null) {
			basePanel.requestFocusInWindow();
		}
		basePanel.removeAll();
		columnModel.visible().stream()
						.map(this::columnComponent)
						.forEach(basePanel::add);
		basePanel.add(scrollBarFiller);
		syncPanelWidths();
		repaint();
		if (childFocusOwner != null && childFocusOwner.isShowing()) {
			childFocusOwner.requestFocusInWindow();
		}
	}

	private Component childFocusOwner() {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (Utilities.parentOfType(FilterTableColumnComponentPanel.class, focusOwner) == this) {
			return focusOwner;
		}

		return null;
	}

	private void bindColumnAndComponentSizes() {
		columnModel.addColumnModelListener(new SyncColumnModelListener());
		for (FilterTableColumn<C> column : columns) {
			JComponent component = columnComponent(column);
			component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
			column.addPropertyChangeListener(new SyncListener(component, column));
		}
	}

	private void syncPanelWidths() {
		for (FilterTableColumn<C> column : columns) {
			syncPanelWidth(columnComponent(column), column);
		}
	}

	private JComponent columnComponent(FilterTableColumn<C> column) {
		return components.getOrDefault(column.identifier(),
						nullComponents.computeIfAbsent(column.identifier(), c -> new JPanel()));
	}

	private static void syncPanelWidth(JComponent component, TableColumn column) {
		component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
		component.revalidate();
	}

	private static final class SyncListener implements PropertyChangeListener {

		private final JComponent component;
		private final TableColumn column;

		private SyncListener(JComponent component, TableColumn column) {
			this.component = component;
			this.column = column;
		}

		@Override
		public void propertyChange(PropertyChangeEvent changeEvent) {
			if ("width".equals(changeEvent.getPropertyName())) {
				syncPanelWidth(component, column);
			}
		}
	}

	private final class SyncColumnModelListener implements TableColumnModelListener {
		@Override
		public void columnAdded(TableColumnModelEvent e) {
			resetPanel();
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
			resetPanel();
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			if (e.getFromIndex() != e.getToIndex()) {
				resetPanel();
			}
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {/*Not required*/}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {/*Not required*/}
	}
}
