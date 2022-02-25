/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
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
   * @param connectionProvider the connection provider
   */
  EntityPopupMenu(final Entity entity, final EntityConnectionProvider connectionProvider) {
    requireNonNull(entity);
    populateEntityMenu(this, entity, connectionProvider, new HashSet<>());
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connectionProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   * @param visitedEntities used to prevent cyclical dependencies wreaking havoc
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity,
                                         final EntityConnectionProvider connectionProvider, final Set<Entity> visitedEntities) {
    Entities entities = connectionProvider.getEntities();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getPrimaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(entities.getDefinition(entity.getEntityType())
            .getForeignKeyProperties()), visitedEntities);
    populateValueMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getProperties()), entities);
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties) {
    Text.collate(primaryKeyProperties);
    for (final ColumnProperty<?> property : primaryKeyProperties) {
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

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity,
                                             final EntityConnectionProvider connectionProvider,
                                             final List<ForeignKeyProperty> fkProperties,
                                             final Set<Entity> visitedEntities) {
    try {
      if (!visitedEntities.contains(entity)) {
        visitedEntities.add(entity);
        Text.collate(fkProperties);
        EntityDefinition definition = connectionProvider.getEntities().getDefinition(entity.getEntityType());
        EntityValidator validator = definition.getValidator();
        for (final ForeignKeyProperty property : fkProperties) {
          ForeignKey foreignKey = property.getAttribute();
          boolean fkValueNull = entity.isForeignKeyNull(foreignKey);
          boolean isLoaded = entity.isLoaded(foreignKey);
          boolean valid = isValid(validator, entity, definition, property);
          boolean modified = entity.isModified(foreignKey);
          String toolTipText = getForeignKeyAttributeNames(foreignKey);
          if (!fkValueNull) {
            Entity referencedEntity;
            if (isLoaded) {
              referencedEntity = entity.getForeignKey(foreignKey);
            }
            else {
              referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedKey(foreignKey));
              entity.remove(foreignKey);
              entity.put(foreignKey, referencedEntity);
            }
            StringBuilder builder = new StringBuilder("[FK").append(isLoaded ? "] " : "+] ")
                    .append(property.getCaption()).append(": ").append(referencedEntity.toString());
            if (modified) {
              builder.append(getOriginalValue(entity, property));
            }
            JMenu foreignKeyMenu = new JMenu(builder.toString());
            setInvalidModified(foreignKeyMenu, valid, modified);
            foreignKeyMenu.setToolTipText(toolTipText);
            populateEntityMenu(foreignKeyMenu, referencedEntity, connectionProvider, visitedEntities);
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
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getForeignKeyAttributeNames(final ForeignKey foreignKey) {
    return foreignKey.getReferences().stream()
            .map(reference -> reference.getAttribute().getName())
            .collect(joining(", "));
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property<?>> properties,
                                        final Entities entities) {
    Text.collate(properties);
    final int maxValueLength = 20;
    EntityDefinition definition = entities.getDefinition(entity.getEntityType());
    EntityValidator validator = definition.getValidator();
    for (final Property<?> property : properties) {
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

  private static void setInvalidModified(final JMenuItem menuItem, final boolean valid, final boolean modified) {
    Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setBackground(Color.RED);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static String getOriginalValue(final Entity entity, final Property<?> property) {
    Object originalValue = entity.getOriginal(property.getAttribute());

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(final EntityValidator validator, final Entity entity, final EntityDefinition definition, final Property<?> property) {
    try {
      validator.validate(entity, definition, property);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }
}
