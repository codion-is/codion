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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.model.component.list.SwingFilterListModel;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.renderer.RowColors;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * <p>A {@link JList} based on a {@link SwingFilterListModel}
 * <p>The items are rendered with alternate row coloring, their background and foreground colors blended with the
 * selection colors when selected, as {@link is.codion.swing.common.ui.component.table.FilterTable} renders its rows,
 * unless a cell renderer is specified, see {@link Builder#cellRenderer(ListCellRenderer)}.
 * @param <T> the item type
 */
public final class FilterList<T> extends JList<T> {

	/**
	 * Specifies whether alternate row coloring is enabled by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see Builder#alternateRowColoring(boolean)
	 */
	public static final PropertyValue<Boolean> ALTERNATE_ROW_COLORING =
					booleanValue(FilterList.class.getName() + ".alternateRowColoring", true);

	/**
	 * Specifies whether selected rows use the selection colors the look and feel sets on the list, such as FlatLaf's
	 * inactive ones while the list is not focused, instead of the default ones of the look and feel.
	 * Selection colors set on the list explicitly are used regardless.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> INACTIVE_SELECTION =
					booleanValue(FilterList.class.getName() + ".inactiveSelection", false);

	/**
	 * Specifies whether alternating row backgrounds are painted below the items to fill the viewport.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see Builder#rowsFillViewport(boolean)
	 */
	public static final PropertyValue<Boolean> ROWS_FILL_VIEWPORT =
					booleanValue(FilterList.class.getName() + ".rowsFillViewport", false);

	private final @Nullable Function<T, String> formatter;
	private final boolean rowsFillViewport;

	FilterList(AbstractFilterListBuilder<?, T, ?> builder) {
		super(builder.listModel);
		this.formatter = builder.formatter;
		this.rowsFillViewport = builder.rowsFillViewport;
		super.setSelectionModel(builder.listModel.selection());
		if (builder.cellRenderer == null) {
			setCellRenderer(new DefaultCellRenderer<>(builder));
		}
		else {
			setCellRenderer(builder.cellRenderer);
		}
		builder.listModel.selection().indexes().addConsumer(new ScrollToSelected());
	}

	/**
	 * @return the list model
	 */
	public SwingFilterListModel<T> model() {
		return getModel();
	}

	@Override
	public SwingFilterListModel<T> getModel() {
		return (SwingFilterListModel<T>) super.getModel();
	}

	@Override
	public void setModel(ListModel<T> model) {
		throw new IllegalStateException("ListModel has already been set");
	}

	@Override
	public void setSelectionModel(ListSelectionModel selectionModel) {
		throw new IllegalStateException("Selection model has already been set");
	}

	/**
	 * Matches the formatted items in case a formatter is specified, see {@link Builder#formatter(Function)}.
	 * @param prefix the prefix to match
	 * @param startIndex the index to start the search from
	 * @param bias the search direction
	 * @return the index of the next item matching the prefix, -1 if none is found
	 * @throws IllegalArgumentException in case prefix is null or startIndex is out of bounds
	 */
	@Override
	public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
		if (formatter == null) {
			return super.getNextMatch(prefix, startIndex, bias);
		}
		ListModel<T> model = getModel();
		int max = model.getSize();
		if (prefix == null || startIndex < 0 || startIndex >= max) {
			throw new IllegalArgumentException();
		}
		String upperCasePrefix = prefix.toUpperCase();
		int increment = bias == Position.Bias.Forward ? 1 : -1;
		int index = startIndex;
		do {
			T item = model.getElementAt(index);
			if (item != null && formatter.apply(item).toUpperCase().startsWith(upperCasePrefix)) {
				return index;
			}
			index = (index + increment + max) % max;
		}
		while (index != startIndex);

		return -1;
	}

	/**
	 * @return a {@link Builder.ModelStep} instance
	 */
	public static Builder.ModelStep builder() {
		return DefaultFilterListBuilderFactory.MODEL;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (rowsFillViewport && getLayoutOrientation() == VERTICAL && getCellRenderer() instanceof DefaultCellRenderer) {
			fillViewportRows(graphics, (DefaultCellRenderer<T>) getCellRenderer());
		}
	}

	/**
	 * Paints the remaining rows, below the items, filling the viewport
	 * @param graphics the graphics
	 * @param renderer the cell renderer
	 */
	private void fillViewportRows(Graphics graphics, DefaultCellRenderer<T> renderer) {
		int size = getModel().getSize();
		Rectangle last = size == 0 ? null : getCellBounds(size - 1, size - 1);
		int rowHeight = getFixedCellHeight() > 0 ? getFixedCellHeight() : last == null ? renderer.rowHeight(this) : last.height;
		int y = last == null ? getInsets().top : last.y + last.height;
		for (int row = size; y < getHeight(); row++, y += rowHeight) {
			graphics.setColor(renderer.rowColors.background(getBackground(), row));
			graphics.fillRect(0, y, getWidth(), rowHeight);
		}
	}

	private final class ScrollToSelected implements Consumer<List<Integer>> {

		@Override
		public void accept(List<Integer> selectedIndexes) {
			Ancestor.ofType(JViewport.class).of(FilterList.this).optional().ifPresent(viewport -> {
				if (!selectedIndexes.isEmpty()) {
					ensureIndexIsVisible(selectedIndexes.get(0));
				}
			});
		}
	}

	private static final class DefaultCellRenderer<T> extends DefaultListCellRenderer {

		private final RowColors rowColors;
		private final @Nullable Function<T, String> formatter;
		private final @Nullable Function<T, @Nullable Color> background;
		private final @Nullable Function<T, @Nullable Color> foreground;
		private final @Nullable Function<T, @Nullable String> toolTip;

		private DefaultCellRenderer(AbstractFilterListBuilder<?, T, ?> builder) {
			this.rowColors = new RowColors("List", builder.alternateRowColoring, INACTIVE_SELECTION.getOrThrow());
			this.formatter = builder.formatter;
			this.background = builder.background;
			this.foreground = builder.foreground;
			this.toolTip = builder.toolTip;
		}

		@Override
		public void updateUI() {
			super.updateUI();
			//null when called during construction
			if (rowColors != null) {
				rowColors.update();
			}
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, @Nullable Object value, int index,
																									boolean selected, boolean cellHasFocus) {
			T item = (T) value;
			super.getListCellRendererComponent(list, item == null || formatter == null ? value : formatter.apply(item),
							index, selected, cellHasFocus);
			if (!dropTarget(list, index)) {
				setBackground(rowColors.background(list.getBackground(), index,
								item == null || background == null ? null : background.apply(item), selected, list.getSelectionBackground()));
				setForeground(rowColors.foreground(item == null || foreground == null || selected ? null : foreground.apply(item),
								selected, list.getSelectionForeground()));
			}
			setToolTipText(item == null || toolTip == null ? null : toolTip.apply(item));

			return this;
		}

		// the height of a row of text, a space, since an empty label has none
		private int rowHeight(JList<?> list) {
			return super.getListCellRendererComponent(list, " ", 0, false, false).getPreferredSize().height;
		}

		// the drop target colors, set by the default renderer
		private static boolean dropTarget(JList<?> list, int index) {
			JList.DropLocation dropLocation = list.getDropLocation();

			return dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index;
		}
	}

	/**
	 * Builds a {@link FilterList} instance.
	 * @param <V> the component value type
	 * @param <T> the value type
	 * @param <B> the builder type
	 * @see #builder()
	 */
	public interface Builder<V, T, B extends Builder<V, T, B>> extends ComponentValueBuilder<FilterList<T>, V, B> {

		/**
		 * @param visibleRowCount the visible row count
		 * @return this builder instance
		 * @see JList#setVisibleRowCount(int)
		 */
		B visibleRowCount(int visibleRowCount);

		/**
		 * @param layoutOrientation the list layout orientation
		 * @return thi builder instance
		 * @see JList#setLayoutOrientation(int)
		 */
		B layoutOrientation(int layoutOrientation);

		/**
		 * @param fixedCellHeight the fixed cell height
		 * @return this builder instance
		 * @see JList#setFixedCellHeight(int)
		 */
		B fixedCellHeight(int fixedCellHeight);

		/**
		 * @param fixedCellWidth the fixed cell width
		 * @return this builder instance
		 * @see JList#setFixedCellWidth(int)
		 */
		B fixedCellWidth(int fixedCellWidth);

		/**
		 * Formats the items for display, and for matching the prefix typed to navigate the list, which a cell renderer
		 * displays as it sees fit, see {@link #cellRenderer(ListCellRenderer)}. {@link Object#toString()} by default.
		 * @param formatter the formatter
		 * @return this builder instance
		 * @see FilterList#getNextMatch(String, int, Position.Bias)
		 */
		B formatter(Function<T, String> formatter);

		/**
		 * Ignored in case a cell renderer is specified, see {@link #cellRenderer(ListCellRenderer)}.
		 * @param background provides the background of each item, null for the default one, shaded on alternate rows
		 * and blended with the selection background when selected
		 * @return this builder instance
		 */
		B background(Function<T, @Nullable Color> background);

		/**
		 * Ignored in case a cell renderer is specified, see {@link #cellRenderer(ListCellRenderer)}.
		 * @param foreground provides the foreground of each item, null for the default one, selected items using the
		 * selection foreground
		 * @return this builder instance
		 */
		B foreground(Function<T, @Nullable Color> foreground);

		/**
		 * Ignored in case a cell renderer is specified, see {@link #cellRenderer(ListCellRenderer)}.
		 * @param toolTip provides the tool tip of each item, null for none
		 * @return this builder instance
		 */
		B toolTip(Function<T, @Nullable String> toolTip);

		/**
		 * Ignored in case a cell renderer is specified, see {@link #cellRenderer(ListCellRenderer)}.
		 * @param alternateRowColoring true if alternate row coloring should be enabled
		 * @return this builder instance
		 * @see #ALTERNATE_ROW_COLORING
		 */
		B alternateRowColoring(boolean alternateRowColoring);

		/**
		 * Ignored in case a cell renderer is specified, see {@link #cellRenderer(ListCellRenderer)},
		 * or the layout orientation is not {@link JList#VERTICAL}.
		 * @param rowsFillViewport true if alternating row backgrounds should be painted below the items to fill the viewport
		 * @return this builder instance
		 * @see #ROWS_FILL_VIEWPORT
		 */
		B rowsFillViewport(boolean rowsFillViewport);

		/**
		 * Replaces the default cell renderer, along with its colors.
		 * @param cellRenderer the cell renderer, null for the default one
		 * @return this builder instance
		 * @see JList#setCellRenderer(ListCellRenderer)
		 */
		B cellRenderer(@Nullable ListCellRenderer<? super T> cellRenderer);

		/**
		 * @param dragEnabled the drag enabled value
		 * @return this builder instance
		 * @see JList#setDragEnabled(boolean)
		 */
		B dragEnabled(boolean dragEnabled);

		/**
		 * @param dropMode the drop mode
		 * @return this builder instance
		 * @see JList#setDropMode(DropMode)
		 */
		B dropMode(DropMode dropMode);

		/**
		 * @param listSelectionListener the list selection listener
		 * @return this builder instance
		 * @see JList#addListSelectionListener(ListSelectionListener)
		 */
		B listSelectionListener(ListSelectionListener listSelectionListener);

		/**
		 * Builds a JList, where the value is represented by the list items.
		 * @param <T> the value type
		 */
		interface Items<T> extends Builder<List<T>, T, Items<T>> {

			/**
			 * @param selectionMode the list selection model
			 * @return this builder instance
			 * @see JList#setSelectionMode(int)
			 */
			Items<T> selectionMode(int selectionMode);

			/**
			 * Default false.
			 * @param nullable if true then null is used instead of an empty list
			 * @return this builder instance
			 */
			Items<T> nullable(boolean nullable);
		}

		/**
		 * Builds a multi-selection JList, where the value is represented by the selected items.
		 * @param <T> the value type
		 */
		interface SelectedItems<T> extends Builder<List<T>, T, SelectedItems<T>> {

			/**
			 * Default false.
			 * @param nullable if true then null is used instead of an empty list
			 * @return this builder instance
			 */
			SelectedItems<T> nullable(boolean nullable);
		}

		/**
		 * Builds a single-selection JList, where the value is represented by the selected item.
		 * @param <T> the value type
		 */
		interface SelectedItem<T> extends Builder<T, T, SelectedItem<T>> {}

		/**
		 * Provides a {@link Factory}
		 */
		interface ModelStep {

			/**
			 * @param listModel the list model
			 * @param <T> the list item type
			 * @return a {@link Factory}
			 */
			<T> Factory<T> model(SwingFilterListModel<T> listModel);
		}

		/**
		 * A factory for list builders, depending on what the component value should represent.
		 */
		interface Factory<T> {

			/**
			 * A JList builder, where the value is represented by the list items.
			 * @return a JList builder
			 */
			Items<T> items();

			/**
			 * A multi selection JList builder, where the value is represented by the selected items.
			 * @return a JList builder
			 */
			SelectedItems<T> selectedItems();

			/**
			 * A single-selection JList builder, where the value is represented by the selected item.
			 * @return a JList builder
			 */
			SelectedItem<T> selectedItem();
		}
	}
}
