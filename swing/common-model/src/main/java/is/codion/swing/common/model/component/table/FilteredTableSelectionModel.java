/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.table.SelectionModel;

import javax.swing.ListSelectionModel;

/**
 * A selection model for a {@link FilteredTableModel}.
 * @param <R> the type of rows
 */
public interface FilteredTableSelectionModel<R> extends ListSelectionModel, SelectionModel<R> {}
