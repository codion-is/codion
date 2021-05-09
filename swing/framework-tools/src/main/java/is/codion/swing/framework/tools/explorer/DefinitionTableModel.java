/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static is.codion.framework.domain.DomainType.domainType;
import static java.util.Arrays.asList;

final class DefinitionTableModel extends AbstractFilteredTableModel<DefinitionRow, Integer> {

  static final int DOMAIN = 0;
  static final int ENTITY = 1;

  private final SchemaTableModel schemaTableModel;

  DefinitionTableModel(final SchemaTableModel schemaTableModel, final DefinitionSortModel sortModel) {
    super(new SwingFilteredTableColumnModel<>(createDefinitionColumns()), sortModel,
            asList(new DefaultColumnFilterModel<>(0, String.class, "%"),
                    new DefaultColumnFilterModel<>(1, String.class, "%")));
    this.schemaTableModel = schemaTableModel;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final DefinitionRow definition = getItemAt(rowIndex);
    switch (columnIndex) {
      case DOMAIN:
        return definition.domain.getDomainType().getName();
      case ENTITY:
        return definition.definition.getEntityType().getName();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIndex);
    }
  }

  @Override
  protected Collection<DefinitionRow> refreshItems() {
    final Collection<DefinitionRow> items = new ArrayList<>();
    schemaTableModel.getSelectionModel().getSelectedItems().forEach(schema -> items.addAll(createDomainDefinitions(schema)));

    return items;
  }

  private static Collection<DefinitionRow> createDomainDefinitions(final Schema schema) {
    final DatabaseDomain domain = new DatabaseDomain(domainType(schema.getName()), schema.getTables().values());

    return domain.getEntities().getDefinitions().stream().map(definition ->
            new DefinitionRow(domain, definition)).collect(Collectors.toList());
  }

  private static List<TableColumn> createDefinitionColumns() {
    final TableColumn domainColumn = new TableColumn(DefinitionTableModel.DOMAIN);
    domainColumn.setIdentifier(DefinitionTableModel.DOMAIN);
    domainColumn.setHeaderValue("Domain");
    final TableColumn entityTypeColumn = new TableColumn(DefinitionTableModel.ENTITY);
    entityTypeColumn.setIdentifier(DefinitionTableModel.ENTITY);
    entityTypeColumn.setHeaderValue("Entity");

    return asList(domainColumn, entityTypeColumn);
  }
}
