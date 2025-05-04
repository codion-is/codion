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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableColumn} with a typed identifier.
 * For instances use factory method {@link #filterTableColumn(int)} or {@link #filterTableColumn(Object, int)}
 * or builder methods {@link #builder(int)}, {@link #builder(Enum)}  {@link #builder(Object, int)}.
 * Note that the identifier is used as a default header value.
 * @param <C> the column identifier type
 * @see #filterTableColumn(int)
 * @see #filterTableColumn(Object, int)
 * @see #builder(int)
 * @see #builder(Enum)
 * @see #builder(Object, int)
 */
public final class FilterTableColumn<C> extends TableColumn {

	private final String toolTipText;

	private FilterTableColumn(DefaultBuilder<C> builder) {
		super(builder.modelIndex);
		super.setIdentifier(builder.identifier);
		this.toolTipText = builder.toolTipText;
		if (builder.preferredWidth != 0) {
			setPreferredWidth(builder.preferredWidth);
		}
		if (builder.maxWidth != 0) {
			setMaxWidth(builder.maxWidth);
		}
		if (builder.minWidth != 0) {
			setMinWidth(builder.minWidth);
		}
		if (builder.width != 0) {
			setWidth(builder.width);
		}
		if (builder.headerValue != null) {
			setHeaderValue(builder.headerValue);
		}
		if (builder.headerRenderer != null) {
			setHeaderRenderer(builder.headerRenderer);
		}
		if (builder.cellEditor != null) {
			setCellEditor(builder.cellEditor);
		}
		if (builder.cellRenderer != null) {
			setCellRenderer(builder.cellRenderer);
		}
		setResizable(builder.resizable);
	}

	@Override
	public C getIdentifier() {
		return (C) super.getIdentifier();
	}

	/**
	 * @param identifier an identifier for this column
	 * @throws UnsupportedOperationException changing the identifier is not supported
	 */
	@Override
	public void setIdentifier(Object identifier) {
		throw new UnsupportedOperationException("Changing the identifier of a FilterTableColumn is not supported");
	}

	/**
	 * @param modelIndex the new modelIndex
	 * @throws UnsupportedOperationException changing the modelIndex is not supported
	 */
	@Override
	public void setModelIndex(int modelIndex) {
		throw new UnsupportedOperationException("Changing the model index of a FilterTableColumn is not supported");
	}

	/**
	 * @return the column identifier
	 */
	public C identifier() {
		return getIdentifier();
	}

	/**
	 * @return the tool tip text to display for this column, an empty Optional in case of no tool tip
	 */
	public Optional<String> toolTipText() {
		return Optional.ofNullable(toolTipText);
	}

	@Override
	public String toString() {
		return identifier().toString();
	}

	/**
	 * Instantiates a new index based {@link FilterTableColumn}.
	 * @param modelIndex the column model index, also used as identifier
	 * @return a new {@link FilterTableColumn} instance
	 */
	public static FilterTableColumn<Integer> filterTableColumn(int modelIndex) {
		return builder(modelIndex, modelIndex).build();
	}

	/**
	 * Instantiates a new {@link FilterTableColumn}.
	 * @param <C> the column identifier type
	 * @param identifier the column identifier
	 * @param modelIndex the column model index
	 * @return a new {@link FilterTableColumn} instance
	 * @throws NullPointerException in case {@code identifier} is null
	 */
	public static <C> FilterTableColumn<C> filterTableColumn(C identifier, int modelIndex) {
		return builder(identifier, modelIndex).build();
	}

	/**
	 * Instantiates a new enum based {@link FilterTableColumn.Builder}.
	 * The enum ordinal position is used as the column model index
	 * @param <C> the column identifier type
	 * @param identifier the column identifier
	 * @return a new {@link FilterTableColumn.Builder} instance
	 * @see Enum#ordinal()
	 */
	public static <C extends Enum<C>> FilterTableColumn.Builder<C> builder(C identifier) {
		return builder(identifier, identifier.ordinal());
	}

	/**
	 * Instantiates a new index based {@link FilterTableColumn.Builder}.
	 * @param modelIndex the column model index, also used as identifier
	 * @return a new {@link FilterTableColumn.Builder} instance
	 */
	public static FilterTableColumn.Builder<Integer> builder(int modelIndex) {
		return builder(modelIndex, modelIndex);
	}

	/**
	 * Instantiates a new {@link FilterTableColumn.Builder}.
	 * @param <C> the column identifier type
	 * @param identifier the column identifier
	 * @param modelIndex the column model index
	 * @return a new {@link FilterTableColumn} instance
	 * @throws NullPointerException in case {@code identifier} is null
	 */
	public static <C> FilterTableColumn.Builder<C> builder(C identifier, int modelIndex) {
		return new DefaultBuilder<>(identifier, modelIndex);
	}

	/**
	 * A builder for {@link FilterTableColumn} instances.
	 * @param <C> the column identifier type
	 */
	public interface Builder<C> {

		/**
		 * Sets both the minimum and maximum widths.
		 * @param fixedWidth the fixed width
		 * @return this builder instance
		 * @see #minWidth(int)
		 * @see #maxWidth(int)
		 */
		Builder<C> fixedWidth(int fixedWidth);

		/**
		 * @param preferredWidth the preferred column width
		 * @return this builder instance
		 */
		Builder<C> preferredWidth(int preferredWidth);

		/**
		 * @param maxWidth the maximum column width
		 * @return this builder instance
		 */
		Builder<C> maxWidth(int maxWidth);

		/**
		 * @param minWidth the minimum column width
		 * @return this builder instance
		 */
		Builder<C> minWidth(int minWidth);

		/**
		 * @param width the column width
		 * @return this builder instance
		 */
		Builder<C> width(int width);

		/**
		 * @param resizable true if the column should be resizable
		 * @return this builder instance
		 */
		Builder<C> resizable(boolean resizable);

		/**
		 * @param headerValue the header value
		 * @return this builder instance
		 */
		Builder<C> headerValue(Object headerValue);

		/**
		 * @param headerRenderer the header renderer
		 * @return this builder instance
		 */
		Builder<C> headerRenderer(TableCellRenderer headerRenderer);

		/**
		 * @param toolTipText the column tool tip text
		 * @return this builder instance
		 */
		Builder<C> toolTipText(String toolTipText);

		/**
		 * @param cellEditor the cell editor
		 * @return this builder instance
		 */
		Builder<C> cellEditor(TableCellEditor cellEditor);

		/**
		 * @param cellRenderer the cell renderer
		 * @return this builder instance
		 */
		Builder<C> cellRenderer(TableCellRenderer cellRenderer);

		/**
		 * @return a new {@link FilterTableColumn} based on this builder
		 */
		FilterTableColumn<C> build();
	}

	private static final class DefaultBuilder<C> implements Builder<C> {

		private final C identifier;
		private final int modelIndex;

		private int fixedWidth;
		private int preferredWidth;
		private int maxWidth;
		private int minWidth;
		private int width;
		private boolean resizable = true;
		private Object headerValue;
		private String toolTipText;
		private TableCellRenderer headerRenderer;
		private TableCellEditor cellEditor;
		private TableCellRenderer cellRenderer;

		private DefaultBuilder(C identifier, int modelIndex) {
			if (modelIndex < 0) {
				throw new IllegalArgumentException("Model index must be positive: " + modelIndex);
			}
			this.identifier = requireNonNull(identifier);
			this.modelIndex = modelIndex;
			this.headerValue = identifier;
		}

		@Override
		public Builder<C> fixedWidth(int fixedWidth) {
			minWidth(fixedWidth);
			maxWidth(fixedWidth);
			return this;
		}

		@Override
		public Builder<C> preferredWidth(int preferredWidth) {
			this.preferredWidth = preferredWidth;
			return this;
		}

		@Override
		public Builder<C> maxWidth(int maxWidth) {
			this.maxWidth = maxWidth;
			return this;
		}

		@Override
		public Builder<C> minWidth(int minWidth) {
			this.minWidth = minWidth;
			return this;
		}

		@Override
		public Builder<C> width(int width) {
			this.width = width;
			return this;
		}

		@Override
		public Builder<C> resizable(boolean resizable) {
			this.resizable = resizable;
			return this;
		}

		@Override
		public Builder<C> headerValue(Object headerValue) {
			this.headerValue = requireNonNull(headerValue);
			return this;
		}

		@Override
		public Builder<C> headerRenderer(TableCellRenderer headerRenderer) {
			this.headerRenderer = requireNonNull(headerRenderer);
			return this;
		}

		@Override
		public Builder<C> toolTipText(String toolTipText) {
			this.toolTipText = toolTipText;
			return this;
		}

		@Override
		public Builder<C> cellEditor(TableCellEditor cellEditor) {
			this.cellEditor = cellEditor;
			return this;
		}

		@Override
		public Builder<C> cellRenderer(TableCellRenderer cellRenderer) {
			this.cellRenderer = cellRenderer;
			return this;
		}

		@Override
		public FilterTableColumn<C> build() {
			return new FilterTableColumn<>(this);
		}
	}
}
