/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.CellColorProvider;

import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilteredTableCellRenderer.Builder} implementation.
 * @param <R> the row type
 * @param <C> the column identifier type
 */
public class DefaultFilteredTableCellRendererBuilder<R, C> implements FilteredTableCellRenderer.Builder<R, C> {

  final FilteredTableModel<R, C> tableModel;
  final C columnIdentifier;

  private final Class<?> columnClass;
  private final boolean useBooleanRenderer;

  int horizontalAlignment;
  boolean toolTipData;
  boolean columnShadingEnabled = true;
  boolean alternateRowColoring = FilteredTableCellRenderer.ALTERNATE_ROW_COLORING.get();
  int leftPadding = FilteredTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
  int rightPadding = FilteredTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
  Function<Object, Object> displayValueProvider = new DefaultDisplayValueProvider();
  CellColorProvider<C> cellColorProvider = new DefaultCellColorProvider<>();

  /**
   * Instantiates a new builder
   * @param tableModel the table model
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   */
  protected DefaultFilteredTableCellRendererBuilder(FilteredTableModel<R, C> tableModel, C columnIdentifier, Class<?> columnClass) {
    this(tableModel, columnIdentifier, columnClass, Boolean.class.equals(requireNonNull(columnClass)));
  }

  /**
   * Instantiates a new builder
   * @param tableModel the table model
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @param useBooleanRenderer true if the boolean renderer should be used
   */
  protected DefaultFilteredTableCellRendererBuilder(FilteredTableModel<R, C> tableModel, C columnIdentifier, Class<?> columnClass, boolean useBooleanRenderer) {
    this.tableModel = requireNonNull(tableModel);
    this.columnIdentifier = requireNonNull(columnIdentifier);
    this.columnClass = requireNonNull(columnClass);
    this.useBooleanRenderer = useBooleanRenderer;
    this.horizontalAlignment = defaultHorizontalAlignment();
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> toolTipData(boolean toolTipData) {
    this.toolTipData = toolTipData;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> columnShadingEnabled(boolean columnShadingEnabled) {
    this.columnShadingEnabled = columnShadingEnabled;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> alternateRowColoring(boolean alternateRowColoring) {
    this.alternateRowColoring = alternateRowColoring;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> leftPadding(int leftPadding) {
    this.leftPadding = leftPadding;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> rightPadding(int rightPadding) {
    this.rightPadding = rightPadding;
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> displayValueProvider(Function<Object, Object> displayValueProvider) {
    this.displayValueProvider = requireNonNull(displayValueProvider);
    return this;
  }

  @Override
  public final FilteredTableCellRenderer.Builder<R, C> cellColorProvider(CellColorProvider<C> cellColorProvider) {
    this.cellColorProvider = requireNonNull(cellColorProvider);
    return this;
  }

  @Override
  public final FilteredTableCellRenderer build() {
    return useBooleanRenderer ?
            new DefaultFilteredTableCellRenderer.BooleanRenderer<>(this, settings(leftPadding, rightPadding, alternateRowColoring)) :
            new DefaultFilteredTableCellRenderer<>(this, settings(leftPadding, rightPadding, alternateRowColoring));
  }

  /**
   * @param leftPadding the left padding
   * @param rightPadding the right padding
   * @param alternateRowColoring true if alternate row coloring is enabled
   * @return the {@link FilteredTableCellRenderer.Settings} instance for this renderer
   */
  protected FilteredTableCellRenderer.Settings<C> settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
    return new FilteredTableCellRenderer.Settings<>(leftPadding, rightPadding, alternateRowColoring);
  }

  private int defaultHorizontalAlignment() {
    if (useBooleanRenderer) {
      return FilteredTableCellRenderer.BOOLEAN_HORIZONTAL_ALIGNMENT.get();
    }
    if (Number.class.isAssignableFrom(columnClass)) {
      return FilteredTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.get();
    }
    if (Temporal.class.isAssignableFrom(columnClass)) {
      return FilteredTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.get();
    }

    return FilteredTableCellRenderer.HORIZONTAL_ALIGNMENT.get();
  }

  private static final class DefaultDisplayValueProvider implements Function<Object, Object> {
    @Override
    public Object apply(Object value) {
      return Objects.toString(value);
    }
  }

  private static final class DefaultCellColorProvider<R> implements CellColorProvider<R> {}
}
