/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.framework.tools.metadata.Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

final class DefinitionTableModel extends DefaultFilteredTableModel<DefinitionRow, Integer> {

  static final int DOMAIN = 0;
  static final int ENTITY = 1;

  private final SchemaTableModel schemaTableModel;

  DefinitionTableModel(SchemaTableModel schemaTableModel) {
    super(createDefinitionColumns(), new DefinitionColumnValueProvider(),
            asList(new DefaultColumnFilterModel<>(0, String.class, '%'),
                    new DefaultColumnFilterModel<>(1, String.class, '%')));
    this.schemaTableModel = schemaTableModel;
  }

  @Override
  protected Collection<DefinitionRow> refreshItems() {
    Collection<DefinitionRow> items = new ArrayList<>();
    schemaTableModel.selectionModel().getSelectedItems().forEach(schema -> items.addAll(createDomainDefinitions(schema)));

    return items;
  }

  private static Collection<DefinitionRow> createDomainDefinitions(Schema schema) {
    DatabaseDomain domain = new DatabaseDomain(domainType(schema.name()), schema.tables().values());

    return domain.entities().definitions().stream()
            .map(definition -> new DefinitionRow(domain, definition))
            .collect(toList());
  }

  private static List<FilteredTableColumn<Integer>> createDefinitionColumns() {
    FilteredTableColumn<Integer> domainColumn = filteredTableColumn(DefinitionTableModel.DOMAIN, DefinitionTableModel.DOMAIN);
    domainColumn.setHeaderValue("Domain");
    FilteredTableColumn<Integer> entityTypeColumn = filteredTableColumn(DefinitionTableModel.ENTITY, DefinitionTableModel.ENTITY);
    entityTypeColumn.setHeaderValue("Entity");

    return asList(domainColumn, entityTypeColumn);
  }

  private static final class DefinitionColumnValueProvider implements ColumnValueProvider<DefinitionRow, Integer> {

    @Override
    public Class<?> columnClass(Integer columnIdentifier) {
      switch (columnIdentifier) {
        case DefinitionTableModel.DOMAIN:
        case DefinitionTableModel.ENTITY:
          return String.class;
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }

    @Override
    public Object value(DefinitionRow row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case DefinitionTableModel.DOMAIN:
          return row.domain.type().name();
        case DefinitionTableModel.ENTITY:
          return row.definition.type().name();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }
}
