/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.TextUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.input.InputProviderPanel;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A static utility class concerned with UI related tasks.
 */
public final class EntityUiUtil {

  private static final int MAXIMUM_VALUE_LENGTH = 1000;

  private EntityUiUtil() {}

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field, used as a caption for the dialog as well
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see EntityDefinition#getSearchProperties()
   */
  public static Collection<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption) {
    return lookupEntities(entityId, connectionProvider, singleSelection, dialogParent, lookupCaption, lookupCaption);
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see EntityDefinition#getSearchProperties()
   */
  public static Collection<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption, final String dialogTitle) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(entityId, connectionProvider);
    if (singleSelection) {
      lookupModel.getMultipleSelectionEnabledValue().set(false);
    }
    final InputProviderPanel inputPanel = new InputProviderPanel(lookupCaption, new EntityLookupFieldInputProvider(lookupModel, null));
    UiUtil.displayInDialog(dialogParent, inputPanel, dialogTitle, true, inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return lookupModel.getSelectedEntities();
    }

    return emptyList();
  }

  /**
   * Displays a popup menu containing the values of the given entity
   * @param entity the entity which values to display
   * @param component the component on which to display the popup menu
   * @param location the popup menu location
   * @param connectionProvider the connection provider for populating the values
   */
  public static void showEntityMenu(final Entity entity, final JComponent component, final Point location,
                                    final EntityConnectionProvider connectionProvider) {
    if (entity != null) {
      final JPopupMenu popupMenu = new JPopupMenu();
      //we copy it because foreign key values get populated along the way
      final Entity copy = connectionProvider.getDomain().deepCopyEntity(entity);
      populateEntityMenu(popupMenu, copy, connectionProvider);
      popupMenu.show(component, location.x, location.y);
    }
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connectionProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity,
                                         final EntityConnectionProvider connectionProvider) {
    final Domain domain = connectionProvider.getDomain();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(domain.getDefinition(entity.getEntityId()).getPrimaryKeyProperties()));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(domain.getDefinition(entity.getEntityId())
            .getForeignKeyProperties()));
    populateValueMenu(rootMenu, entity, new ArrayList<>(domain.getDefinition(entity.getEntityId()).getProperties()), domain);
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<ColumnProperty> primaryKeyProperties) {
    TextUtil.collate(primaryKeyProperties);
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
                                             final List<ForeignKeyProperty> fkProperties) {
    try {
      TextUtil.collate(fkProperties);
      final Entity.Validator validator = connectionProvider.getDomain().getDefinition(entity.getEntityId()).getValidator();
      for (final ForeignKeyProperty property : fkProperties) {
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        final boolean isLoaded = entity.isLoaded(property.getPropertyId());
        final boolean valid = isValid(validator, entity, property);
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
          final StringBuilder builder = new StringBuilder("[FK").append(isLoaded ? "] " : "+] ").append(property.getCaption())
                  .append(": ").append(referencedEntity.toString());
          if (modified) {
            builder.append(getOriginalValue(entity, property));
          }
          final JMenu foreignKeyMenu = new JMenu(builder.toString());
          setInvalidModified(foreignKeyMenu, valid, modified);
          foreignKeyMenu.setToolTipText(toolTipText);
          populateEntityMenu(foreignKeyMenu, entity.getForeignKey(property.getPropertyId()), connectionProvider);
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
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getForeignKeyColumnNames(final ForeignKeyProperty foreignKeyProperty) {
    final List<String> columnNames = new LinkedList<>();
    foreignKeyProperty.getColumnProperties().forEach(property -> columnNames.add(property.getColumnName()));

    return String.join(", ", columnNames);
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties,
                                        final Domain domain) {
    TextUtil.collate(properties);
    final int maxValueLength = 20;
    final Entity.Validator validator = domain.getDefinition(entity.getEntityId()).getValidator();
    for (final Property property : properties) {
      final boolean valid = isValid(validator, entity, property);
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

  private static boolean isValid(final Entity.Validator validator, final Entity entity, final Property property) {
    try {
      validator.validate(entity, property);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }
}
