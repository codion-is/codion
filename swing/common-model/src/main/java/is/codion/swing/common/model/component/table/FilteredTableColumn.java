/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import javax.swing.table.TableColumn;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableColumn} with a typed identifier.
 * For instances use factory method {@link #filteredTableColumn(int)} or {@link #filteredTableColumn(int, Object)}.
 * @param <C> the column identifier type
 * @see #filteredTableColumn(int, Object)
 */
public final class FilteredTableColumn<C> extends TableColumn {

  private FilteredTableColumn(int modelIndex, C identifier) {
    super(modelIndex);
    super.setIdentifier(requireNonNull(identifier));
  }

  @Override
  public C getIdentifier() {
    return (C) super.getIdentifier();
  }

  /**
   * @param identifier an identifier for this column
   * @throws IllegalStateException always
   */
  @Override
  public void setIdentifier(Object identifier) {
    throw new IllegalStateException("Can't change the identifier of a FilteredTableColumn");
  }

  /**
   * Instantiates a new index based {@link FilteredTableColumn}.
   * @param modelIndex the column model index and identifier
   * @return a new {@link FilteredTableColumn} instance
   */
  public static FilteredTableColumn<Integer> filteredTableColumn(int modelIndex) {
    return new FilteredTableColumn<>(modelIndex, modelIndex);
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
    return new FilteredTableColumn<>(modelIndex, identifier);
  }
}
