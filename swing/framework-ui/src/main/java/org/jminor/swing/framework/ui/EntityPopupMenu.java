/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Text;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.entity.EntityValidator;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A popup menu for inspecting the values of a single entity instance.
 */
final class EntityPopupMenu extends JPopupMenu {

  private static final int MAXIMUM_VALUE_LENGTH = 1000;

  /**
   * Note that this has a side-effect, as it populates the full foreign key graph of the given entity,
   * so use a copy.
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
    final Domain domain = connectionProvider.getDomain();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(domain.getDefinition(entity.getEntityId()).getPrimaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(domain.getDefinition(entity.getEntityId())
            .getForeignKeyProperties()), visitedEntities);
    populateValueMenu(rootMenu, entity, new ArrayList<>(domain.getDefinition(entity.getEntityId()).getProperties()), domain);
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<ColumnProperty> primaryKeyProperties) {
    Text.collate(primaryKeyProperties);
    for (final ColumnProperty property : primaryKeyProperties) {
      final boolean modified = entity.isModified(property);
      final StringBuilder builder = new StringBuilder("[PK] ").append(property.getPropertyId()).append(": ").append(entity.getAsString(property));
      if (modified) {
        builder.append(getOriginalValue(entity, property));
      }
      final JMenuItem menuItem = new JMenuItem(builder.toString());
      setInvalidModified(menuItem, true, modified);
      menuItem.setToolTipText(property.getPropertyId());
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
        final EntityDefinition definition = connectionProvider.getDomain().getDefinition(entity.getEntityId());
        final EntityValidator validator = definition.getValidator();
        for (final ForeignKeyProperty property : fkProperties) {
          final boolean fkValueNull = entity.isForeignKeyNull(property);
          final boolean isLoaded = entity.isLoaded(property.getPropertyId());
          final boolean valid = isValid(validator, entity, definition, property);
          final boolean modified = entity.isModified(property);
          final String toolTipText = getForeignKeyColumnNames(property);
          if (!fkValueNull) {
            final Entity referencedEntity;
            if (isLoaded) {
              referencedEntity = entity.getForeignKey(property.getPropertyId());
            }
            else {
              referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedKey(property));
              entity.remove(property);
              entity.put(property, referencedEntity);
            }
            final StringBuilder builder = new StringBuilder("[FK").append(isLoaded ? "] " : "+] ")
                    .append(property.getCaption()).append(": ").append(referencedEntity.toString());
            if (modified) {
              builder.append(getOriginalValue(entity, property));
            }
            final JMenu foreignKeyMenu = new JMenu(builder.toString());
            setInvalidModified(foreignKeyMenu, valid, modified);
            foreignKeyMenu.setToolTipText(toolTipText);
            populateEntityMenu(foreignKeyMenu, referencedEntity, connectionProvider, visitedEntities);
            rootMenu.add(foreignKeyMenu);
          }
          else {
            final StringBuilder builder = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
            if (modified) {
              builder.append(getOriginalValue(entity, property));
            }
            final JMenuItem menuItem = new JMenuItem(builder.toString());
            setInvalidModified(menuItem, valid, modified);
            menuItem.setToolTipText(toolTipText);
            rootMenu.add(menuItem);
          }
        }
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getForeignKeyColumnNames(final ForeignKeyProperty foreignKeyProperty) {
    return foreignKeyProperty.getColumnProperties().stream()
            .map(ColumnProperty::getColumnName).collect(joining(", "));
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties,
                                        final Domain domain) {
    Text.collate(properties);
    final int maxValueLength = 20;
    final EntityDefinition definition = domain.getDefinition(entity.getEntityId());
    final EntityValidator validator = definition.getValidator();
    for (final Property property : properties) {
      final boolean valid = isValid(validator, entity, definition, property);
      final boolean modified = entity.isModified(property);
      final boolean isForeignKeyProperty = property instanceof ColumnProperty
              && ((ColumnProperty) property).isForeignKeyProperty();
      if (!isForeignKeyProperty && !(property instanceof ForeignKeyProperty)) {
        final String prefix = "[" + property.getTypeClass().getSimpleName().substring(0, 1)
                + (property instanceof DerivedProperty ? "*" : "")
                + (property instanceof DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isNull(property) ? "<null>" : entity.getAsString(property);
        final boolean longValue = value != null && value.length() > maxValueLength;
        final StringBuilder builder = new StringBuilder(prefix).append(property).append(": ");
        if (longValue) {
          builder.append(value, 0, maxValueLength).append("...");
        }
        else {
          builder.append(value);
        }
        if (modified) {
          builder.append(getOriginalValue(entity, property));
        }
        final JMenuItem menuItem = new JMenuItem(builder.toString());
        setInvalidModified(menuItem, valid, modified);
        final StringBuilder toolTipBuilder = new StringBuilder();
        if (property instanceof ColumnProperty) {
          toolTipBuilder.append(property.getPropertyId());
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
    final Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setBackground(Color.RED);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static String getOriginalValue(final Entity entity, final Property property) {
    final Object originalValue = entity.getOriginal(property);

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(final EntityValidator validator, final Entity entity, final EntityDefinition definition, final Property property) {
    try {
      validator.validate(entity, definition, property);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }
}
