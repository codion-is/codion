package org.jminor.framework.client.model;

import org.jminor.common.db.Criteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.combobox.PropertyComboBoxModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import javax.swing.ComboBoxModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for editing a Entity instance, providing property change events and combobox models
 */
public class EntityEditModel {

  protected static final Logger log = Util.getLogger(EntityEditModel.class);

  /**
   * Fired when the active entity has changed
   */
  public final Event evtEntityChanged = new Event();

  /**
   * An event fired when the underlying table has undergone changes,
   * such as insert, update or delete
   */
  public final Event evtEntitiesChanged;

  /**
   * Active when a non-null entity is active
   */
  public final State stEntityNotNull = new State();

  /**
   * The entity used for editing
   */
  protected final Entity entity;

  /**
   * The EntityDbProvider instance to use when populating combo boxes and such
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds the ComboBoxModels used by this EntityModel, those that implement Refreshable
   * are refreshed when refreshComboBoxModels() is called
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, ComboBoxModel> propertyComboBoxModels;

  /**
   * Holds events signaling property changes made to the active entity via the ui
   */
  private final Map<Property, Event> propertyValueSetEventMap = new HashMap<Property, Event>();

  /**
   * Holds events signaling property changes made to the active entity, via the model or ui
   */
  private final Map<Property, Event> propertyChangeEventMap = new HashMap<Property, Event>();

  public EntityEditModel(final String entityID, final EntityDbProvider dbProvider, final Event evtEntitiesChanged) {
    this.entity = new Entity(entityID);
    this.entity.setAs(getDefaultEntity());
    this.dbProvider = dbProvider;
    this.propertyComboBoxModels = new HashMap<Property, ComboBoxModel>(initializeEntityComboBoxModels());
    this.evtEntitiesChanged = evtEntitiesChanged;
    bindEvents();
  }

  public String getEntityID() {
    return entity.getEntityID();
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public State getEntityNotNullState() {
    return stEntityNotNull;
  }

  /**
   * @return true if the active entity is null
   * @see org.jminor.framework.domain.Entity#isNull()
   */
  public boolean isEntityNull() {
    return entity.isNull();
  }

  /**
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  public Entity getEntityCopy() {
    return entity.getCopy();
  }

  /**
   * @return the state which indicates the modified state of the active entity
   * @see org.jminor.framework.domain.Entity#getModifiedState()
   */
  public State getEntityModifiedState() {
    return entity.getModifiedState();
  }

  /**
   * @return true if the active entity has been modified
   * @see org.jminor.framework.domain.Entity#isModified()
   */
  public boolean isEntityModified() {
    return getEntityModifiedState().isActive();
  }

  public Event getEntityChangedEvent() {
    return evtEntityChanged;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of property <code>propertyID</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(String, Object)
   * @see #setValue(org.jminor.framework.domain.Property, Object)
   */
  public Event getPropertyValueSetEvent(final String propertyID) {
    return getPropertyValueSetEvent(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(String, Object)
   * @see #setValue(org.jminor.framework.domain.Property, Object)
   */
  public Event getPropertyValueSetEvent(final Property property) {
    if (!propertyValueSetEventMap.containsKey(property))
      propertyValueSetEventMap.put(property, new Event());

    return propertyValueSetEventMap.get(property);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of the property identified by <code>propertyID</code> changes
   */
  public Event getPropertyChangeEvent(final String propertyID) {
    return getPropertyChangeEvent(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> changes
   */
  public Event getPropertyChangeEvent(final Property property) {
    if (!propertyChangeEventMap.containsKey(property))
      propertyChangeEventMap.put(property, new Event());

    return propertyChangeEventMap.get(property);
  }

  /**
   * Returns true if the value of the given property is valid, the default implementation simply returns true
   * @param property the property
   * @return true if the value of the given property is valid
   */
  public boolean isValid(final Property property) {
    return property.isNullable() || !isValueNull(property.getPropertyID());
  }

  /**
   * Refreshes the Refreshable ComboBoxModels associated with this EntityModel
   * @see org.jminor.common.model.Refreshable
   */
  public void refreshComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values())
      if (comboBoxModel instanceof Refreshable)
        ((Refreshable) comboBoxModel).refresh();
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a PropertyComboBoxModel representing <code>property</code>
   */
  public PropertyComboBoxModel getPropertyComboBoxModel(final Property property) {
    return (PropertyComboBoxModel) propertyComboBoxModels.get(property);
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.evtEntitiesChanged is used
   * @param nullValue the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a PropertyComboBoxModel representing <code>property</code>, if no combo box model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  public PropertyComboBoxModel initializePropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                               final String nullValue) {
    PropertyComboBoxModel ret = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (ret == null) {
      setComboBoxModel(property, ret = createPropertyComboBoxModel(property,
              refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValue));
      ret.refresh();
    }

    return ret;
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.evtEntitiesChanged is used
   * @param nullValue the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a new PropertyComboBoxModel based on the given property
   */
  public PropertyComboBoxModel createPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                           final String nullValue) {
    return new PropertyComboBoxModel(getEntityID(), property, getDbProvider(), nullValue, refreshEvent);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the property identified by <code>propertyID</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @see #initializeEntityComboBoxModels()
   */
  public EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    final Property property = EntityRepository.getProperty(getEntityID(), propertyID);
    if (!(property instanceof Property.ForeignKeyProperty))
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");

    return getEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel associated with the <code>property</code>
   * @see #initializeEntityComboBoxModels()
   */
  public EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    return (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
  }

  public EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID) {
    final Property property = EntityRepository.getProperty(getEntityID(), propertyID);
    if (!(property instanceof Property.ForeignKeyProperty))
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");

    return initializeEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @see #initializeEntityComboBoxModels()
   */
  public EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    EntityComboBoxModel ret = getEntityComboBoxModel(foreignKeyProperty);
    if (ret == null)
      setComboBoxModel(foreignKeyProperty, ret = createEntityComboBoxModel(foreignKeyProperty));

    return ret;
  }

  /**
   * Sets the active entity, that is, the entity to be edited
   * @param entity the entity to set as active, if null then the default entity value is set as active
   * @see #evtEntityChanged
   */
  public final void setEntity(final Entity entity) {
    if (entity != null && this.entity.propertyValuesEqual(entity))
      return;

    this.entity.setAs(entity == null ? getDefaultEntity() : entity);
    stEntityNotNull.setActive(!this.entity.isNull());
    evtEntityChanged.fire();
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>,
   * basic type validation is performed.
   * @param propertyID the ID of the property to update
   * @param value the new value
   */
  public void setValue(final String propertyID, final Object value) {
    setValue(EntityRepository.getProperty(getEntityID(), propertyID), value);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   */
  public void setValue(final Property property, final Object value) {
    final Object oldValue = getValue(property);
    final Object newValue = doSetValue(property, value, true);

    if (!Util.equal(newValue, oldValue))
      notifyPropertyValueSet(new Property.Event(this, getEntityID(), property, newValue, oldValue, false, false));
  }

  /**
   * @param propertyID the property identifier
   * @return true if the value of the given property is null
   */
  public boolean isValueNull(final String propertyID) {
    return entity.isValueNull(propertyID);
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public Object getValue(final Property property) {
    return getValue(property.getPropertyID());
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public Object getValue(final String propertyID) {
    return entity.getValue(propertyID);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public Entity getEntityValue(final String propertyID) {
    return entity.getEntityValue(propertyID);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public Entity getEntityValue(final Property.ForeignKeyProperty foreignKeyProperty) {
    return getEntityValue(foreignKeyProperty.getPropertyID());
  }

  /**
   * @return a map of initialized EntityComboBoxModels associated with
   * their respective properties.
   * Use this method to provide combo box models with specific functionality.
   */
  protected Map<Property.ForeignKeyProperty, EntityComboBoxModel> initializeEntityComboBoxModels() {
    return initializeEntityComboBoxModels(new EntityComboBoxModel[0]);
  }

  /**
   * Returns a Map, mapping the provided EntityComboBoxModels to their respective properties according to the entityID.
   * This implementation maps the EntityComboBoxModel to the Property.ForeignKeyProperty with the same entityID.
   * If the underlying Entity references the same Entity via more than one foreign key, a RuntimeException is thrown.
   * @param comboBoxModels the EntityComboBoxModels to map to their respective properties
   * @return a Map of EntityComboBoxModels mapped to their respective properties
   */
  protected final Map<Property.ForeignKeyProperty, EntityComboBoxModel> initializeEntityComboBoxModels(final EntityComboBoxModel... comboBoxModels) {
    final Map<Property.ForeignKeyProperty, EntityComboBoxModel> ret =
            new HashMap<Property.ForeignKeyProperty, EntityComboBoxModel>();
    if (comboBoxModels == null || comboBoxModels.length == 0)
      return ret;

    for (final EntityComboBoxModel comboBoxModel : comboBoxModels) {
      final List<Property.ForeignKeyProperty> properties =
              EntityRepository.getForeignKeyProperties(getEntityID(), comboBoxModel.getEntityID());
      if (properties.size() > 1)
        throw new RuntimeException("Multiple possible properties found for EntityComboBoxModel: " + comboBoxModel);
      else if (properties.size() == 1)
        ret.put(properties.get(0), comboBoxModel);
      else
        throw new RuntimeException("Property not found for EntityComboBoxModel: " + comboBoxModel);
    }

    return ret;
  }

  /**
   * Creates a default EntityComboBoxModel for the given property, override to provide
   * specific EntityComboBoxModels (filtered for example) for properties.
   * This method is called when creating a EntitComboBoxModel for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted EntityComboBoxModel with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a EntityComboBoxModel
   * @return a EntityComboBoxModel for the given property
   * @see Configuration#DEFAULT_COMBO_BOX_NULL_VALUE_ITEM
   * @see org.jminor.framework.domain.Property#isNullable()
   */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    return new EntityComboBoxModel(foreignKeyProperty.referenceEntityID, getDbProvider(), false,
            foreignKeyProperty.isNullable() ?
                    (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null, true);
  }

  /**
   * Creates a EntityLookupModel for the given entityID
   * @param entityID the ID of the entity
   * @param additionalSearchCriteria an additional search criteria applied when performing the lookup
   * @param lookupProperties the properties involved in the lookup
   * @return a EntityLookupModel
   */
  public EntityLookupModel createEntityLookupModel(final String entityID, final Criteria additionalSearchCriteria,
                                                   final List<Property> lookupProperties) {
    return new EntityLookupModel(entityID, getDbProvider(), additionalSearchCriteria, lookupProperties);
  }

  /**
   * If this method is overridden then calling super.getDefaultValue() would be proper
   * @return the default entity for this EntitModel, it is set as active when no item is selected
   * @see #getDefaultValue(org.jminor.framework.domain.Property)
   */
  public Entity getDefaultEntity() {
    final Entity ret = new Entity(getEntityID());
    for (final Property property : EntityRepository.getDatabaseProperties(getEntityID()))
      if (!property.hasParentProperty() && !(property instanceof Property.DenormalizedProperty))//these are set via their respective parent properties
        ret.setValue(property, getDefaultValue(property), true);

    return ret;
  }

  /**
   * Returns the default value for the given property, used when initializing a new
   * default entity for this edit model. This does not apply to denormalized properties
   * (Property.DenormalizedProperty) nor properties that are wrapped in foreign key properties
   * (Property.ForeignKeyProperty)
   * If the default value of a property should be the last value used <code>persistValueOnClear</code>
   * should be overridden so that it returns <code>true</code> for that property.
   * @param property the property
   * @return the default value for the property
   * @see #persistValueOnClear(org.jminor.framework.domain.Property)
   */
  public Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property) : property.getDefaultValue();
  }

  /**
   * Sets the value in the underlying entity
   * @param property the property for which to set the value
   * @param value the value
   * @param validateType if true then type validation should be performed
   * @return the value that was just set
   */
  protected Object doSetValue(final Property property, final Object value, final boolean validateType) {
    entity.setValue(property.getPropertyID(), value, validateType);

    return value;
  }

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity for this EntityModel.
   * Override for selective reset of field values when the model is cleared.
   * For Property.ForeignKeyProperty values this method by default returns the value of the
   * property <code>Configuration.PERSIST_ENTITY_REFERENCE_VALUES</code>.
   * @param property the property
   * @return true if the given entity field value should be reset when the model is cleared
   * @see org.jminor.framework.Configuration#PERSIST_ENTITY_REFERENCE_VALUES
   */
  protected boolean persistValueOnClear(final Property property) {
    return property instanceof Property.ForeignKeyProperty
            && (Boolean) Configuration.getValue(Configuration.PERSIST_ENTITY_REFERENCE_VALUES);
  }

  /**
   * You must call super.bindEvents() in case you override this method
   */
  protected void bindEvents() {
    entity.addPropertyListener(new Property.Listener() {
      @Override
      protected void propertyChanged(final Property.Event event) {
        final Event propertyEvent = propertyChangeEventMap.get(event.getProperty());
        if (propertyEvent != null)
          propertyEvent.fire(event);
      }
    });
    if ((Boolean) Configuration.getValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      entity.addPropertyListener(new Property.Listener() {
        @Override
        protected void propertyChanged(final Property.Event event) {
          final String msg = getPropertyChangeDebugString(event);
          System.out.println(msg);
          log.trace(msg);
        }
      });
    }
  }

  /**
   * Sets the ComboBoxModel to be associated with the given property
   * @param property the property
   * @param model the ComboBoxModel
   * @throws RuntimeException in case the ComboBoxModel has already been set for this property
   */
  private void setComboBoxModel(final Property property, final ComboBoxModel model) {
    if (propertyComboBoxModels.containsKey(property))
      throw new RuntimeException("ComboBoxModel already associated with property: " + property);

    propertyComboBoxModels.put(property, model);
  }

  private void notifyPropertyValueSet(final Property.Event event) {
    if ((Boolean) Configuration.getValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      final String msg = getPropertyChangeDebugString(event);
      System.out.println(msg);
      log.trace(msg);
    }
    getPropertyValueSetEvent(event.getProperty()).fire(event);
  }

  private static String getPropertyChangeDebugString(final Property.Event event) {
    final StringBuilder ret = new StringBuilder();
    if (event.getSource() instanceof Entity)
      ret.append("[entity] ");
    else
      ret.append(event.isModelChange() ? "[model] " : "[ui] ");
    ret.append(event.getEntityID()).append(" : ").append(event.getProperty()).append(
            event.getProperty().hasParentProperty() ? " [fk]" : "").append("; ");
    if (!event.isInitialization()) {
      if (event.getOldValue() != null)
        ret.append(event.getOldValue().getClass().getSimpleName()).append(" ");
      ret.append(getValueString(event.getProperty(), event.getOldValue()));
    }
    if (!event.isInitialization())
      ret.append(" -> ");
    if (event.getNewValue() != null)
      ret.append(event.getNewValue().getClass().getSimpleName()).append(" ");
    ret.append(getValueString(event.getProperty(), event.getNewValue()));

    return ret.toString();
  }

  /**
   * @param property the property
   * @param value the value
   * @return a string representing the given property value for debug output
   */
  private static String getValueString(final Property property, final Object value) {
    final boolean valueIsNull = Entity.isValueNull(property.getPropertyType(), value);
    final StringBuilder ret = new StringBuilder("[").append(valueIsNull ? (value == null ? "null" : "null value") : value).append("]");
    if (value instanceof Entity)
      ret.append(" PK{").append(((Entity)value).getPrimaryKey()).append("}");

    return ret.toString();
  }
}