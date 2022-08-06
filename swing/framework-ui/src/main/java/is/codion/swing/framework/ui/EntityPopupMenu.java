/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
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
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connection the connection to use when selecting foreign key references
   * @param visitedEntities used to prevent cyclical dependencies wreaking havoc
   */
  private static void populateEntityMenu(JComponent rootMenu, Entity entity, EntityConnection connection, Set<Entity> visitedEntities) {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entity.entityDefinition().primaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connection, new ArrayList<>(entity.entityDefinition()
            .foreignKeyProperties()), visitedEntities);
    populateValueMenu(rootMenu, entity, new ArrayList<>(entity.entityDefinition().properties()));
  }

  private static void populatePrimaryKeyMenu(JComponent rootMenu, Entity entity, List<ColumnProperty<?>> primaryKeyProperties) {
    Text.collate(primaryKeyProperties);
    for (ColumnProperty<?> property : primaryKeyProperties) {
      JMenuItem menuItem = new JMenuItem(new StringBuilder("[PK] ")
              .append(property.attribute())
              .append(" [").append(property.attribute().valueClass().getSimpleName()).append("]: ")
              .append(createValueString(entity, property)).toString());
      menuItem.addActionListener(Control.control(() -> setClipboard(entity.toString(property.attribute()))));
      setInvalidModified(menuItem, true, entity.isModified(property.attribute()));
      menuItem.setToolTipText(property.attribute().name());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(JComponent rootMenu, Entity entity, EntityConnection connection,
                                             List<ForeignKeyProperty> fkProperties, Set<Entity> visitedEntities) {
    if (!visitedEntities.contains(entity)) {
      visitedEntities.add(entity);
      Text.collate(fkProperties);
      EntityDefinition definition = entity.entityDefinition();
      EntityValidator validator = definition.validator();
      for (ForeignKeyProperty property : fkProperties) {
        ForeignKey foreignKey = property.attribute();
        StringBuilder captionBuilder = new StringBuilder("[FK] ").append(property.caption()).append(": ");
        if (entity.isForeignKeyNull(foreignKey)) {
          JMenuItem menuItem = new JMenuItem(captionBuilder.append(createValueString(entity, property)).toString());
          setInvalidModified(menuItem, isValid(validator, entity, foreignKey), entity.isModified(foreignKey));
          menuItem.setToolTipText(getForeignKeyAttributeNames(foreignKey));
          rootMenu.add(menuItem);
        }
        else {
          Entity referencedEntity = selectEntity(entity.referencedKey(foreignKey), connection);
          entity.put(foreignKey, referencedEntity);
          JMenu foreignKeyMenu = new JMenu(captionBuilder.append(createValueString(entity, property)).toString());
          setInvalidModified(foreignKeyMenu, isValid(validator, entity, foreignKey), entity.isModified(foreignKey));
          foreignKeyMenu.setToolTipText(getForeignKeyAttributeNames(foreignKey));
          populateEntityMenu(foreignKeyMenu, referencedEntity, connection, visitedEntities);
          rootMenu.add(foreignKeyMenu);
        }
      }
    }
  }

  private static String getForeignKeyAttributeNames(ForeignKey foreignKey) {
    return foreignKey.references().stream()
            .map(reference -> reference.attribute().toString())
            .collect(joining(", "));
  }

  private static void populateValueMenu(JComponent rootMenu, Entity entity, List<Property<?>> properties) {
    Text.collate(properties);
    EntityDefinition definition = entity.entityDefinition();
    EntityValidator validator = definition.validator();
    for (Property<?> property : properties) {
      boolean isPrimaryKeyProperty = property instanceof ColumnProperty && ((ColumnProperty<?>) property).primaryKeyColumn();
      if (!isPrimaryKeyProperty && !(property instanceof ForeignKeyProperty)) {
        JMenuItem menuItem = new JMenuItem(new StringBuilder(property.toString())
                .append(" [").append(property.attribute().valueClass().getSimpleName())
                .append(property instanceof DerivedProperty ? "*" : "").append("]: ")
                .append(createValueString(entity, property)).toString());
        menuItem.addActionListener(Control.control(() -> setClipboard(entity.toString(property.attribute()))));
        setInvalidModified(menuItem, isValid(validator, entity, property.attribute()), entity.isModified(property.attribute()));
        menuItem.setToolTipText(property.attribute().toString());
        rootMenu.add(menuItem);
      }
    }
  }

  private static String createValueString(Entity entity, Property<?> property) {
    StringBuilder builder = new StringBuilder();
    if (entity.isModified(property.attribute())) {
      builder.append(createValueString(entity.getOriginal(property.attribute()), (Property<Object>) property));
      builder.append(" \u2192 ");
    }
    builder.append(createValueString(entity.get(property.attribute()), (Property<Object>) property));

    return builder.toString();
  }

  private static String createValueString(Object value, Property<Object> property) {
    String valueAsString = value == null ? "<null>" : property.toString(value);
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

  private static boolean isValid(EntityValidator validator, Entity entity, Attribute<?> attribute) {
    try {
      validator.validate(entity, attribute);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  private static Entity selectEntity(Key primaryKey, EntityConnection connection) {
    try {
      return connection.selectSingle(Conditions.condition(primaryKey)
              .selectBuilder()
              .fetchDepth(0)
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
