/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.domain.DomainType.domainType;
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
    DatabaseDomain domain = new DatabaseDomain(domainType(schema.getName()), schema.getTables().values());

    return domain.entities().entityDefinitions().stream()
            .map(definition -> new DefinitionRow(domain, definition))
            .collect(toList());
  }

  private static List<TableColumn> createDefinitionColumns() {
    TableColumn domainColumn = new TableColumn(DefinitionTableModel.DOMAIN);
    domainColumn.setIdentifier(DefinitionTableModel.DOMAIN);
    domainColumn.setHeaderValue("Domain");
    TableColumn entityTypeColumn = new TableColumn(DefinitionTableModel.ENTITY);
    entityTypeColumn.setIdentifier(DefinitionTableModel.ENTITY);
    entityTypeColumn.setHeaderValue("Entity");

    return asList(domainColumn, entityTypeColumn);
  }

  private static final class DefinitionColumnValueProvider implements ColumnValueProvider<DefinitionRow, Integer> {

    @Override
    public Class<?> getColumnClass(Integer columnIdentifier) {
      switch (columnIdentifier) {
        case DefinitionTableModel.DOMAIN:
        case DefinitionTableModel.ENTITY:
          return String.class;
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }

    @Override
    public Object getValue(DefinitionRow row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case DefinitionTableModel.DOMAIN:
          return row.domain.type().name();
        case DefinitionTableModel.ENTITY:
          return row.definition.entityType().name();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }
}
