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

import org.jspecify.annotations.Nullable;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableColumn} with a typed identifier.
 * For instances use {@link #builder()}.
 * Note that the identifier is used as a default header value.
 * @param <C> the column identifier type
 * @see #builder()
 */
public final class FilterTableColumn<C> extends TableColumn {

	private final @Nullable String toolTipText;

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
	 * Sets both minimum and maximum widths to the given fixed width.
	 * @param fixedWidth the fixed column width
	 */
	public void setFixedWidth(int fixedWidth) {
		setMinWidth(fixedWidth);
		setMaxWidth(fixedWidth);
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
	 * @return a {@link Builder.IdentifierStep}
	 */
	public static Builder.IdentifierStep builder() {
		return DefaultBuilder.IDENTIFIER;
	}

	/**
	 * A builder for {@link FilterTableColumn} instances.
	 * @param <C> the column identifier type
	 */
	public interface Builder<C> {

		/**
		 * Provides a {@link Builder} or {@link ModelIndexStep}
		 */
		interface IdentifierStep {

			/**
			 * Instantiates a new {@link ModelIndexStep}.
			 * @param <C> the column identifier type
			 * @param identifier the column identifier
			 * @return a new {@link ModelIndexStep} instance
			 * @throws NullPointerException in case {@code identifier} is null
			 */
			<C> ModelIndexStep<C> identifier(C identifier);

			/**
			 * @param modelIndex the model index, also used as identifier
			 * @return a {@link Builder}
			 */
			Builder<Integer> modelIndex(int modelIndex);
		}

		/**
		 * Provides a {@link Builder}
		 * @param <C> the column identifier type
		 */
		interface ModelIndexStep<C> {

			/**
			 * Instantiates a new {@link FilterTableColumn.Builder}.
			 * @param modelIndex the column model index
			 * @return a new {@link FilterTableColumn} instance
			 */
			FilterTableColumn.Builder<C> modelIndex(int modelIndex);
		}

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
		Builder<C> headerValue(@Nullable Object headerValue);

		/**
		 * @param headerRenderer the header renderer
		 * @return this builder instance
		 */
		Builder<C> headerRenderer(@Nullable TableCellRenderer headerRenderer);

		/**
		 * @param toolTipText the column tool tip text
		 * @return this builder instance
		 */
		Builder<C> toolTipText(@Nullable String toolTipText);

		/**
		 * @param cellEditor the cell editor
		 * @return this builder instance
		 */
		Builder<C> cellEditor(@Nullable TableCellEditor cellEditor);

		/**
		 * @param cellRenderer the cell renderer
		 * @return this builder instance
		 */
		Builder<C> cellRenderer(@Nullable TableCellRenderer cellRenderer);

		/**
		 * @return the column identifier this builder is based on
		 */
		C identifier();

		/**
		 * @return a new {@link FilterTableColumn} based on this builder
		 */
		FilterTableColumn<C> build();
	}

	private static final class DefaultIdentifierStep implements Builder.IdentifierStep {

		@Override
		public <C> Builder.ModelIndexStep<C> identifier(C identifier) {
			return new DefaultModelIndexStep<>(requireNonNull(identifier));
		}

		@Override
		public Builder<Integer> modelIndex(int modelIndex) {
			return new DefaultBuilder<>(Integer.valueOf(modelIndex), modelIndex);
		}
	}

	private static final class DefaultModelIndexStep<C> implements Builder.ModelIndexStep<C> {

		private final C identifier;

		private DefaultModelIndexStep(C identifier) {
			this.identifier = identifier;
		}

		@Override
		public Builder<C> modelIndex(int modelIndex) {
			return new DefaultBuilder<>(identifier, modelIndex);
		}
	}

	private static final class DefaultBuilder<C> implements Builder<C> {

		private static final IdentifierStep IDENTIFIER = new DefaultIdentifierStep();

		private final C identifier;
		private final int modelIndex;

		private int preferredWidth;
		private int maxWidth;
		private int minWidth;
		private int width;
		private boolean resizable = true;
		private @Nullable Object headerValue;
		private @Nullable String toolTipText;
		private @Nullable TableCellRenderer headerRenderer;
		private @Nullable TableCellEditor cellEditor;
		private @Nullable TableCellRenderer cellRenderer;

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
		public Builder<C> headerValue(@Nullable Object headerValue) {
			this.headerValue = headerValue;
			return this;
		}

		@Override
		public Builder<C> headerRenderer(@Nullable TableCellRenderer headerRenderer) {
			this.headerRenderer = headerRenderer;
			return this;
		}

		@Override
		public Builder<C> toolTipText(@Nullable String toolTipText) {
			this.toolTipText = toolTipText;
			return this;
		}

		@Override
		public Builder<C> cellEditor(@Nullable TableCellEditor cellEditor) {
			this.cellEditor = cellEditor;
			return this;
		}

		@Override
		public Builder<C> cellRenderer(@Nullable TableCellRenderer cellRenderer) {
			this.cellRenderer = cellRenderer;
			return this;
		}

		@Override
		public C identifier() {
			return identifier;
		}

		@Override
		public FilterTableColumn<C> build() {
			return new FilterTableColumn<>(this);
		}
	}
}
