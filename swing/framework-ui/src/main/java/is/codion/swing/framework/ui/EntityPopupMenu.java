/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
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

  private static final int MAXIMUM_VALUE_LENGTH = 1000;

  /**
   * Note that this has a side effect, as it populates the full foreign key graph of the given entity, so use a copy.
   * @param entity the entity
   * @param connection the connection
   */
  EntityPopupMenu(Entity entity, EntityConnection connection) {
    requireNonNull(entity);
    populateEntityMenu(this, selectEntity(entity.getPrimaryKey(), connection), connection, new HashSet<>());
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connection the connection to use when selecting foreign key references
   * @param visitedEntities used to prevent cyclical dependencies wreaking havoc
   */
  private static void populateEntityMenu(JComponent rootMenu, Entity entity, EntityConnection connection, Set<Entity> visitedEntities) {
    Entities entities = connection.getEntities();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getPrimaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connection, new ArrayList<>(entities.getDefinition(entity.getEntityType())
            .getForeignKeyProperties()), visitedEntities);
    populateValueMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getProperties()), entities);
  }

  private static void populatePrimaryKeyMenu(JComponent rootMenu, Entity entity, List<ColumnProperty<?>> primaryKeyProperties) {
    Text.collate(primaryKeyProperties);
    for (ColumnProperty<?> property : primaryKeyProperties) {
      boolean modified = entity.isModified(property.getAttribute());
      String value = entity.toString(property.getAttribute());
      StringBuilder builder = new StringBuilder("[PK] ")
              .append(property.getAttribute()).append(": ").append(value);
      if (modified) {
        builder.append(getOriginalValue(entity, property));
      }
      JMenuItem menuItem = new JMenuItem(builder.toString());
      menuItem.addActionListener(Control.control(() -> setClipboard(value)));
      setInvalidModified(menuItem, true, modified);
      menuItem.setToolTipText(property.getAttribute().getName());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(JComponent rootMenu, Entity entity, EntityConnection connection,
                                             List<ForeignKeyProperty> fkProperties, Set<Entity> visitedEntities) {
    if (!visitedEntities.contains(entity)) {
      visitedEntities.add(entity);
      Text.collate(fkProperties);
      EntityDefinition definition = connection.getEntities().getDefinition(entity.getEntityType());
      EntityValidator validator = definition.getValidator();
      for (ForeignKeyProperty property : fkProperties) {
        ForeignKey foreignKey = property.getAttribute();
        boolean fkValueNull = entity.isForeignKeyNull(foreignKey);
        boolean valid = isValid(validator, entity, definition, property);
        boolean modified = entity.isModified(foreignKey);
        String toolTipText = getForeignKeyAttributeNames(foreignKey);
        if (!fkValueNull) {
          Entity referencedEntity = selectEntity(entity.getReferencedKey(foreignKey), connection);
          StringBuilder builder = new StringBuilder("[FK").append("] ")
                  .append(property.getCaption()).append(": ").append(referencedEntity.toString());
          if (modified) {
            builder.append(getOriginalValue(entity, property));
          }
          JMenu foreignKeyMenu = new JMenu(builder.toString());
          setInvalidModified(foreignKeyMenu, valid, modified);
          foreignKeyMenu.setToolTipText(toolTipText);
          populateEntityMenu(foreignKeyMenu, referencedEntity, connection, visitedEntities);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          StringBuilder builder = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
          if (modified) {
            builder.append(getOriginalValue(entity, property));
          }
          JMenuItem menuItem = new JMenuItem(builder.toString());
          setInvalidModified(menuItem, valid, modified);
          menuItem.setToolTipText(toolTipText);
          rootMenu.add(menuItem);
        }
      }
    }
  }

  private static String getForeignKeyAttributeNames(ForeignKey foreignKey) {
    return foreignKey.getReferences().stream()
            .map(reference -> reference.getAttribute().getName())
            .collect(joining(", "));
  }

  private static void populateValueMenu(JComponent rootMenu, Entity entity, List<Property<?>> properties,
                                        Entities entities) {
    Text.collate(properties);
    final int maxValueLength = 20;
    EntityDefinition definition = entities.getDefinition(entity.getEntityType());
    EntityValidator validator = definition.getValidator();
    for (Property<?> property : properties) {
      boolean valid = isValid(validator, entity, definition, property);
      boolean modified = entity.isModified(property.getAttribute());
      boolean isForeignKeyProperty = property instanceof ColumnProperty
              && definition.isForeignKeyAttribute(property.getAttribute());
      if (!isForeignKeyProperty && !(property instanceof ForeignKeyProperty)) {
        String prefix = "[" + property.getAttribute().getTypeClass().getSimpleName().charAt(0)
                + (property instanceof DerivedProperty ? "*" : "")
                + (property instanceof DenormalizedProperty ? "+" : "") + "] ";
        String value = entity.isNull(property.getAttribute()) ? "<null>" : entity.toString(property.getAttribute());
        boolean longValue = value != null && value.length() > maxValueLength;
        StringBuilder builder = new StringBuilder(prefix).append(property).append(": ");
        if (longValue) {
          builder.append(value, 0, maxValueLength).append("...");
        }
        else {
          builder.append(value);
        }
        if (modified) {
          builder.append(getOriginalValue(entity, property));
        }
        JMenuItem menuItem = new JMenuItem(builder.toString());
        menuItem.addActionListener(Control.control(() -> setClipboard(value)));
        setInvalidModified(menuItem, valid, modified);
        StringBuilder toolTipBuilder = new StringBuilder();
        if (property instanceof ColumnProperty) {
          toolTipBuilder.append(property.getAttribute());
        }
        if (longValue) {
          if (value.length() > MAXIMUM_VALUE_LENGTH) {
            toolTipBuilder.append(value, 0, MAXIMUM_VALUE_LENGTH);
          }
          else {
            toolTipBuilder.append(value);
          }
        }
        menuItem.setToolTipText(toolTipBuilder.toString());
        rootMenu.add(menuItem);
      }
    }
  }

  private static void setInvalidModified(JMenuItem menuItem, boolean valid, boolean modified) {
    Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setBackground(Color.RED);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static String getOriginalValue(Entity entity, Property<?> property) {
    Object originalValue = entity.getOriginal(property.getAttribute());

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(EntityValidator validator, Entity entity, EntityDefinition definition, Property<?> property) {
    try {
      validator.validate(entity, definition, property);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  private static Entity selectEntity(Key primaryKey, EntityConnection connection) {
    try {
      return connection.selectSingle(Conditions.condition(primaryKey)
              .toSelectCondition().fetchDepth(0));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
