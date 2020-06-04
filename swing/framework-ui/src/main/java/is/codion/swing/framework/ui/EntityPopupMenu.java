/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

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
    final Entities entities = connectionProvider.getEntities();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getPrimaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(entities.getDefinition(entity.getEntityType())
            .getForeignKeyProperties()), visitedEntities);
    populateValueMenu(rootMenu, entity, new ArrayList<>(entities.getDefinition(entity.getEntityType()).getProperties()), entities);
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties) {
    Text.collate(primaryKeyProperties);
    for (final ColumnProperty<?> property : primaryKeyProperties) {
      final boolean modified = entity.isModified(property.getAttribute());
      final StringBuilder builder = new StringBuilder("[PK] ")
              .append(property.getAttribute()).append(": ").append(entity.getAsString(property.getAttribute()));
      if (modified) {
        builder.append(getOriginalValue(entity, property));
      }
      final JMenuItem menuItem = new JMenuItem(builder.toString());
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
        final EntityDefinition definition = connectionProvider.getEntities().getDefinition(entity.getEntityType());
        final EntityValidator validator = definition.getValidator();
        for (final ForeignKeyProperty property : fkProperties) {
          final Attribute<Entity> attribute = property.getAttribute();
          final boolean fkValueNull = entity.isForeignKeyNull(attribute);
          final boolean isLoaded = entity.isLoaded(attribute);
          final boolean valid = isValid(validator, entity, definition, property);
          final boolean modified = entity.isModified(attribute);
          final String toolTipText = getForeignKeyAttributeNames(property);
          if (!fkValueNull) {
            final Entity referencedEntity;
            if (isLoaded) {
              referencedEntity = entity.getForeignKey(attribute);
            }
            else {
              referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedKey(attribute));
              entity.remove(attribute);
              entity.put(attribute, referencedEntity);
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

  private static String getForeignKeyAttributeNames(final ForeignKeyProperty foreignKeyProperty) {
    return foreignKeyProperty.getColumnAttributes().stream().map(Attribute::getName).collect(joining(", "));
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property<?>> properties,
                                        final Entities entities) {
    Text.collate(properties);
    final int maxValueLength = 20;
    final EntityDefinition definition = entities.getDefinition(entity.getEntityType());
    final EntityValidator validator = definition.getValidator();
    for (final Property<?> property : properties) {
      final boolean valid = isValid(validator, entity, definition, property);
      final boolean modified = entity.isModified(property.getAttribute());
      final boolean isForeignKeyProperty = property instanceof ColumnProperty
              && ((ColumnProperty<?>) property).isForeignKeyProperty();
      if (!isForeignKeyProperty && !(property instanceof ForeignKeyProperty)) {
        final String prefix = "[" + property.getAttribute().getTypeClass().getSimpleName().substring(0, 1)
                + (property instanceof DerivedProperty ? "*" : "")
                + (property instanceof DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isNull(property.getAttribute()) ? "<null>" : entity.getAsString(property.getAttribute());
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
    final Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setBackground(Color.RED);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static String getOriginalValue(final Entity entity, final Property<?> property) {
    final Object originalValue = entity.getOriginal(property.getAttribute());

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(final EntityValidator validator, final Entity entity, final EntityDefinition definition, final Property<?> property) {
    try {
      validator.validate(entity, definition, property);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }
}
