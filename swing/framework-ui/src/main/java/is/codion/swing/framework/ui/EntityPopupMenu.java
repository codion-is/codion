/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.attribute.Condition.key;
import static is.codion.swing.common.ui.Utilities.setClipboard;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A popup menu for inspecting the values of a single entity instance.
 */
final class EntityPopupMenu extends JPopupMenu {

  private static final int MAXIMUM_VALUE_LENGTH = 42;

  /**
   * Note that this has a side effect, as it populates the full foreign key graph of the given entity, so use a copy.
   * @param entity the entity
   * @param connection the connection
   */
  EntityPopupMenu(Entity entity, EntityConnection connection) {
    requireNonNull(entity);
    requireNonNull(connection);
    populateEntityMenu(this, entity, connection, new HashSet<>());
  }

  /**
   * Populates the given root menu with the values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connection the connection to use when selecting foreign key references
   * @param visitedKeys used to prevent cyclical dependencies wreaking havoc
   */
  private static void populateEntityMenu(JComponent rootMenu, Entity entity, EntityConnection connection, Set<Entity.Key> visitedKeys) {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entity.definition().primaryKey().columnDefinitions()));
    populateForeignKeyMenu(rootMenu, entity, connection, new ArrayList<>(entity.definition().foreignKeys().definitions()), visitedKeys);
    populateValueMenu(rootMenu, entity, new ArrayList<>(entity.definition().attributes().definitions()));
  }

  private static void populatePrimaryKeyMenu(JComponent rootMenu, Entity entity, List<ColumnDefinition<?>> primaryKeyColumns) {
    Text.collate(primaryKeyColumns);
    for (ColumnDefinition<?> primaryKeyColumn : primaryKeyColumns) {
      JMenuItem menuItem = new JMenuItem(new StringBuilder("[PK] ")
              .append(primaryKeyColumn.attribute())
              .append(" [").append(primaryKeyColumn.attribute().type().valueClass().getSimpleName()).append("]: ")
              .append(createValueString(entity, primaryKeyColumn)).toString());
      menuItem.addActionListener(Control.control(() -> setClipboard(entity.toString(primaryKeyColumn.attribute()))));
      setInvalidModified(menuItem, true, entity.isModified(primaryKeyColumn.attribute()));
      menuItem.setToolTipText(primaryKeyColumn.attribute().name());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(JComponent rootMenu, Entity entity, EntityConnection connection,
                                             List<ForeignKeyDefinition> fkDefinitions, Set<Entity.Key> visitedKeys) {
    if (!visitedKeys.contains(entity.primaryKey())) {
      visitedKeys.add(entity.primaryKey());
      Text.collate(fkDefinitions);
      EntityDefinition definition = entity.definition();
      EntityValidator validator = definition.validator();
      for (ForeignKeyDefinition fkDefinition : fkDefinitions) {
        ForeignKey foreignKey = fkDefinition.attribute();
        StringBuilder captionBuilder = new StringBuilder("[FK] ").append(fkDefinition.caption()).append(": ");
        Entity.Key referencedKey = entity.referencedKey(foreignKey);
        if (referencedKey == null) {
          JMenuItem menuItem = new JMenuItem(captionBuilder.append(createValueString(entity, fkDefinition)).toString());
          setInvalidModified(menuItem, valid(validator, entity, foreignKey), entity.isModified(foreignKey));
          menuItem.setToolTipText(foreignKeyAttributeNames(foreignKey));
          rootMenu.add(menuItem);
        }
        else if (!visitedKeys.contains(referencedKey)) {
          Entity referencedEntity = selectEntity(referencedKey, connection);
          entity = entity.isImmutable() ? entity.deepCopy() : entity;
          entity.put(foreignKey, referencedEntity);
          JMenu foreignKeyMenu = new JMenu(captionBuilder.append(createValueString(entity, fkDefinition)).toString());
          setInvalidModified(foreignKeyMenu, valid(validator, entity, foreignKey), entity.isModified(foreignKey));
          foreignKeyMenu.setToolTipText(foreignKeyAttributeNames(foreignKey));
          populateEntityMenu(foreignKeyMenu, referencedEntity, connection, visitedKeys);
          rootMenu.add(foreignKeyMenu);
        }
      }
    }
  }

  private static String foreignKeyAttributeNames(ForeignKey foreignKey) {
    return foreignKey.references().stream()
            .map(reference -> reference.column().toString())
            .collect(joining(", "));
  }

  private static void populateValueMenu(JComponent rootMenu, Entity entity, List<AttributeDefinition<?>> attributeDefinitions) {
    Text.collate(attributeDefinitions);
    EntityDefinition definition = entity.definition();
    EntityValidator validator = definition.validator();
    for (AttributeDefinition<?> attributeDefinition : attributeDefinitions) {
      boolean primaryKeyColumn = attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).primaryKeyColumn();
      if (!primaryKeyColumn && !(attributeDefinition instanceof ForeignKeyDefinition)) {
        JMenuItem menuItem = new JMenuItem(new StringBuilder(attributeDefinition.toString())
                .append(" [").append(attributeDefinition.attribute().type().valueClass().getSimpleName())
                .append(attributeDefinition.derived() ? "*" : "").append("]: ")
                .append(createValueString(entity, attributeDefinition)).toString());
        menuItem.addActionListener(Control.control(() -> setClipboard(entity.toString(attributeDefinition.attribute()))));
        setInvalidModified(menuItem, valid(validator, entity, attributeDefinition.attribute()), entity.isModified(attributeDefinition.attribute()));
        menuItem.setToolTipText(attributeDefinition.attribute().toString());
        rootMenu.add(menuItem);
      }
    }
  }

  private static String createValueString(Entity entity, AttributeDefinition<?> attributeDefinition) {
    StringBuilder builder = new StringBuilder();
    if (entity.isModified(attributeDefinition.attribute())) {
      builder.append(createValueString(entity.original(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition));
      builder.append(" → ");
    }
    builder.append(createValueString(entity.get(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition));

    return builder.toString();
  }

  private static String createValueString(Object value, AttributeDefinition<Object> attributeDefinition) {
    String valueAsString = value == null ? "<null>" : attributeDefinition.toString(value);
    if (valueAsString.length() > MAXIMUM_VALUE_LENGTH) {
      valueAsString = valueAsString.substring(0, MAXIMUM_VALUE_LENGTH) + "...";
    }

    return valueAsString;
  }

  private static void setInvalidModified(JMenuItem menuItem, boolean valid, boolean modified) {
    Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setForeground(Color.RED);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static boolean valid(EntityValidator validator, Entity entity, Attribute<?> attribute) {
    try {
      validator.validate(entity, attribute);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  private static Entity selectEntity(Entity.Key primaryKey, EntityConnection connection) {
    try {
      return connection.selectSingle(where(key(primaryKey))
              .fetchDepth(0)
              .build());
    }
    catch (RecordNotFoundException e) {
      return ProxyBuilder.builder(Entity.class)
              .delegate(Entity.entity(primaryKey))
              .method("toString", parameters -> primaryKey.toString() + " <RECORD NOT FOUND>")
              .build();
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
