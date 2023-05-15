/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.Comparator;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableColumn} with a typed identifier.
 * For instances use factory method {@link #filteredTableColumn(int)} or {@link #filteredTableColumn(int, Object)}
 * or builder methods {@link #builder(int)} or {@link #builder(int, Object)}.
 * Note that the identifier is used as a default header value.
 * @param <C> the column identifier type
 * @see #filteredTableColumn(int)
 * @see #filteredTableColumn(int, Object)
 * @see #builder(int)
 * @see #builder(int, Object)
 */
public final class FilteredTableColumn<C> extends TableColumn {

  /**
   * A Comparator for comparing {@link Comparable} instances.
   */
  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;

  /**
   * A Comparator for comparing Objects according to their toString() value.
   */
  private static final Comparator<?> STRING_COMPARATOR = Comparator.comparing(Object::toString);

  private final Class<?> columnClass;
  private final Comparator<?> comparator;

  private FilteredTableColumn(DefaultBuilder<C> builder) {
    super(builder.modelIndex);
    super.setIdentifier(builder.identifier);
    this.columnClass = builder.columnClass;
    this.comparator = builder.comparator == null ? defaultComparator(this.columnClass) : builder.comparator;
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
   * @throws IllegalStateException always, changing the identifier is not supported
   */
  @Override
  public void setIdentifier(Object identifier) {
    throw new IllegalStateException("Can't change the identifier of a FilteredTableColumn");
  }

  /**
   * @return the column class
   */
  public Class<?> getColumnClass() {
    return columnClass;
  }

  /**
   * @return the column comparator
   */
  public Comparator<?> getComparator() {
    return comparator;
  }

  /**
   * Instantiates a new index based {@link FilteredTableColumn}.
   * @param modelIndex the column model index and identifier
   * @return a new {@link FilteredTableColumn} instance
   */
  public static FilteredTableColumn<Integer> filteredTableColumn(int modelIndex) {
    return builder(modelIndex, modelIndex).build();
  }

  /**
   * Instantiates a new {@link FilteredTableColumn}.
   * @param modelIndex the column model index
   * @param identifier the column identifier
   * @param <C> the column identifier type
   * @return a new {@link FilteredTableColumn} instance
   * @throws NullPointerException in case {@code identifier} is null
   */
  public static <C> FilteredTableColumn<C> filteredTableColumn(int modelIndex, C identifier) {
    return builder(modelIndex, identifier).build();
  }

  /**
   * Instantiates a new index based {@link FilteredTableColumn.Builder}.
   * @param modelIndex the column model index and identifier
   * @return a new {@link FilteredTableColumn.Builder} instance
   */
  public static FilteredTableColumn.Builder<Integer> builder(int modelIndex) {
    return builder(modelIndex, modelIndex);
  }

  /**
   * Instantiates a new {@link FilteredTableColumn.Builder}.
   * @param modelIndex the column model index
   * @param identifier the column identifier
   * @param <C> the column identifier type
   * @return a new {@link FilteredTableColumn} instance
   * @throws NullPointerException in case {@code identifier} is null
   */
  public static <C> FilteredTableColumn.Builder<C> builder(int modelIndex, C identifier) {
    return new DefaultBuilder<>(modelIndex, identifier);
  }

  private static Comparator<?> defaultComparator(Class<?> columnClass) {
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return STRING_COMPARATOR;
  }

  /**
   * A builder for {@link FilteredTableColumn} instances.
   * @param <C> the column identifier type
   */
  public interface Builder<C> {

    /**
     * @param columnClass the column class
     * @return this builder instance
     */
    Builder<C> columnClass(Class<?> columnClass);

    /**
     * @param comparator the column comparator
     * @return this builder instance
     */
    Builder<C> comparator(Comparator<?> comparator);

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
     * @return a new {@link FilteredTableColumn} based on this builder
     */
    FilteredTableColumn<C> build();
  }

  private static final class DefaultBuilder<C> implements Builder<C> {

    private final int modelIndex;
    private final C identifier;

    private Class<?> columnClass = Object.class;
    private Comparator<?> comparator;
    private int preferredWidth;
    private int maxWidth;
    private int minWidth;
    private int width;
    private boolean resizable = true;
    private Object headerValue;
    private TableCellRenderer headerRenderer;
    private TableCellEditor cellEditor;
    private TableCellRenderer cellRenderer;

    private DefaultBuilder(int modelIndex, C identifier) {
      if (modelIndex < 0) {
        throw new IllegalArgumentException("Model index must be positive: " + modelIndex);
      }
      this.modelIndex = modelIndex;
      this.identifier = requireNonNull(identifier);
      this.headerValue = identifier;
    }

    @Override
    public Builder<C> columnClass(Class<?> columnClass) {
      this.columnClass = requireNonNull(columnClass);
      return this;
    }

    @Override
    public Builder<C> comparator(Comparator<?> comparator) {
      this.comparator = requireNonNull(comparator);
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
    public FilteredTableColumn<C> build() {
      return new FilteredTableColumn<>(this);
    }
  }
}
