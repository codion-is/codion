/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.framework.tools.metadata.Schema;

import java.util.List;
import java.util.stream.Collectors;

import static is.codion.framework.domain.DomainType.domainType;
import static java.util.Arrays.asList;

final class DefinitionTableModel extends AbstractFilteredTableModel<DefinitionRow, Integer> {

  private final SchemaTableModel schemaTableModel;

  DefinitionTableModel(final SchemaTableModel schemaTableModel) {
    super(new DefinitionSortModel(), asList(new DefaultColumnConditionModel<>(0, String.class, "%"),
            new DefaultColumnConditionModel<>(1, String.class, "%")));
    this.schemaTableModel = schemaTableModel;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final DefinitionRow definition = getItemAt(rowIndex);
    switch (columnIndex) {
      case 0:
        return definition.domain.getDomainType().getName();
      case 1:
        return definition.definition.getEntityType().getName();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIndex);
    }
  }

  @Override
  protected void refreshModel() {
    clear();
    schemaTableModel.getSelectionModel().getSelectedItems().forEach(schema ->
            addItemsSorted(createDomainDefinitions(schema)));
  }

  private static List<DefinitionRow> createDomainDefinitions(final Schema schema) {
    final DatabaseDomain domain = new DatabaseDomain(domainType(schema.getName()), schema.getTables().values());

    return domain.getEntities().getDefinitions().stream().map(definition ->
            new DefinitionRow(domain, definition)).collect(Collectors.toList());
  }
}
