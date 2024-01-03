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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilteredTableCellRenderer} implementation.
 * @param <R> the row type
 * @param <C> the column identifier type
 */
final class DefaultFilteredTableCellRenderer<R, C> extends DefaultTableCellRenderer implements FilteredTableCellRenderer {

  private final Settings<C> settings;
  private final FilteredTableModel<R, C> tableModel;
  private final C columnIdentifier;
  private final boolean toolTipData;
  private final boolean columnShadingEnabled;
  private final boolean alternateRowColoring;
  private final Function<Object, Object> displayValueProvider;
  private final CellColorProvider<C> cellColorProvider;

  /**
   * @param builder the builder
   * @param settings the UI settings for the renderer
   */
  DefaultFilteredTableCellRenderer(DefaultFilteredTableCellRendererBuilder<R, C> builder, Settings<C> settings) {
    this.tableModel = requireNonNull(builder).tableModel;
    this.settings = requireNonNull(settings);
    this.settings.updateColors();
    this.columnIdentifier = builder.columnIdentifier;
    this.toolTipData = builder.toolTipData;
    this.columnShadingEnabled = builder.columnShadingEnabled;
    this.alternateRowColoring = builder.alternateRowColoring;
    this.displayValueProvider = builder.displayValueProvider;
    this.cellColorProvider = builder.cellColorProvider;
    setHorizontalAlignment(builder.horizontalAlignment);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (settings != null) {
      settings.updateColors();
    }
  }

  @Override
  public boolean columnShadingEnabled() {
    return columnShadingEnabled;
  }

  @Override
  public boolean alternateRowColoring() {
    return alternateRowColoring;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setForeground(settings.foregroundColor(cellColorProvider.foregroundColor(row, columnIdentifier, value, isSelected)));
    setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, columnShadingEnabled, isSelected,
            cellColorProvider.backgroundColor(row, columnIdentifier, value, isSelected)));
    setBorder(hasFocus || isSearchResult(tableModel, row, column) ? settings.focusedCellBorder() : settings.defaultCellBorder());
    if (toolTipData) {
      setToolTipText(value == null ? "" : value.toString());
    }

    return this;
  }

  /**
   * @param value the value to set
   */
  @Override
  protected void setValue(Object value) {
    super.setValue(displayValueProvider.apply(value));
  }

  /**
   * @return the Settings instance
   */
  Settings<C> settings() {
    return settings;
  }

  private static boolean isSearchResult(FilteredTableModel<?, ?> tableModel, int row, int column) {
    return tableModel.searchModel().currentResult().equals(row, column);
  }

  /**
   * A default {@link FilteredTableCellRenderer} implementation for Boolean values
   * @param <R> the row type
   * @param <C> the column identifier type
   */
  public static final class BooleanRenderer<R, C> extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, FilteredTableCellRenderer {

    private final Settings<C> settings;
    private final FilteredTableModel<R, C> tableModel;
    private final C columnIdentifier;
    private final boolean columnShadingEnabled;
    private final boolean alternateRowColoring;
    private final CellColorProvider<C> cellColorProvider;

    /**
     * @param builder the builder
     * @param settings the UI settings for the renderer
     */
    BooleanRenderer(DefaultFilteredTableCellRendererBuilder<R, C> builder, Settings<C> settings) {
      super(new NullableToggleButtonModel());
      this.tableModel = requireNonNull(builder).tableModel;
      this.settings = requireNonNull(settings);
      this.settings.updateColors();
      this.columnIdentifier = requireNonNull(builder.columnIdentifier);
      this.columnShadingEnabled = builder.columnShadingEnabled;
      this.alternateRowColoring = builder.alternateRowColoring;
      this.cellColorProvider = builder.cellColorProvider;
      setHorizontalAlignment(builder.horizontalAlignment);
      setBorderPainted(true);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      if (settings != null) {
        settings.updateColors();
      }
    }

    @Override
    public boolean columnShadingEnabled() {
      return columnShadingEnabled;
    }

    @Override
    public boolean alternateRowColoring() {
      return alternateRowColoring;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(settings.foregroundColor(cellColorProvider.foregroundColor(row, columnIdentifier, value, isSelected)));
      setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, columnShadingEnabled, isSelected,
              cellColorProvider.backgroundColor(row, columnIdentifier, value, isSelected)));
      setBorder(hasFocus || isSearchResult(tableModel, row, column) ? settings.focusedCellBorder() : settings.defaultCellBorder());

      return this;
    }
  }
}
