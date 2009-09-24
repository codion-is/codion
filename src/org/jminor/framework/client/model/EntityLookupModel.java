/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntityLookupModel {

  public final Event evtSelectedEntitiesChanged = new Event();
  public final Event evtSearchStringChanged = new Event();

  /**
   * The ID of the entity this lookup model is based on
   */
  private final String entityID;

  /**
   * The properties to use when doing the lookup
   */
  private final List<Property> lookupProperties;

  /**
   * The selected entities
   */
  private final List<Entity> selectedEntities = new ArrayList<Entity>();

  /**
   * The EntityDbProvider instance used by this EntityLookupModel
   */
  private final EntityDbProvider dbProvider;

  private Criteria additionalLookupCriteria;
  private String searchString;
  private boolean multipleSelectionAllowed;
  private boolean caseSensitive;
  private boolean wildcardPrefix;
  private boolean wildcardPostfix;
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
  private String multiValueSeperator = ",";

  public EntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final List<Property> lookupProperties) {
    this(entityID, dbProvider, null, lookupProperties);
  }

  public EntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final Criteria additionalLookupCriteria,
                           final List<Property> lookupProperties) {
    this(entityID, dbProvider, additionalLookupCriteria, false, lookupProperties);
  }

  public EntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final Criteria additionalLookupCriteria,
                           final boolean caseSensitive, final List<Property> lookupProperties) {
    this(entityID, dbProvider, additionalLookupCriteria, caseSensitive, true, true, lookupProperties);
  }

  public EntityLookupModel(final String entityID, final EntityDbProvider dbProvider, final Criteria additionalLookupCriteria,
                           final boolean caseSensitive, final boolean wildcardPrefix, final boolean wildcardPostfix,
                           final List<Property> lookupProperties) {
    if (dbProvider == null)
      throw new IllegalArgumentException("EntityLookupModel requires a non-null EntityDbProvider instance");
    if (entityID == null)
      throw new IllegalArgumentException("EntityLookupModel requires a non-null entityID");
    if (lookupProperties == null)
      throw new IllegalArgumentException("EntityLookupModel requires non-null lookupProperties");
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
    this.additionalLookupCriteria = additionalLookupCriteria;
    this.caseSensitive = caseSensitive;
    this.wildcardPrefix = wildcardPrefix;
    this.wildcardPostfix = wildcardPostfix;
  }

  public String getEntityID() {
    return entityID;
  }

  public boolean isMultipleSelectionAllowed() {
    return multipleSelectionAllowed;
  }

  public void setMultipleSelectionAllowed(final boolean multipleSelectionAllowed) {
    this.multipleSelectionAllowed = multipleSelectionAllowed;
  }

  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(Arrays.asList(entity));
  }

  public void setSelectedEntities(final List<Entity> entities) {
    if (entities != null && entities.size() > 1 && !isMultipleSelectionAllowed())
      throw new IllegalArgumentException("This EntityLookupModel does not allow the selection of multiple entities");

    this.selectedEntities.clear();
    if (entities != null)
      this.selectedEntities.addAll(entities);
    refreshSearchText();
    evtSelectedEntitiesChanged.fire();
  }

  public List<Entity> getSelectedEntities() {
    return selectedEntities;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  public void setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
  }

  public boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  public void setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
  }

  /**
   * @return the wildcard
   */
  public String getWildcard() {
    return wildcard;
  }

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   */
  public void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  public String getMultiValueSeperator() {
    return multiValueSeperator;
  }

  public void setMultiValueSeperator(final String multiValueSeperator) {
    this.multiValueSeperator = multiValueSeperator;
    refreshSearchText();
  }

  public void setAdditionalLookupCriteria(final Criteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
  }

  public void refreshSearchText() {
    setSearchString(getSelectedEntities().size() == 0 ? "" : toString(getSelectedEntities()));
  }

  public void setSearchString(final String searchString) {
    this.searchString = searchString == null ? "" : searchString;
    evtSearchStringChanged.fire();
  }

  public String getSearchString() {
    return this.searchString;
  }

  public boolean searchStringRepresentsSelected() {
    final String selectedAsString = toString(getSelectedEntities());
    return (getSelectedEntities().size() == 0 && getSearchString().length() == 0)
            || getSelectedEntities().size() > 0 && selectedAsString.equals(getSearchString());
  }

  public EntityCriteria getEntityCriteria() {
    if (getSearchString().equals(getWildcard()))
      return new EntityCriteria(getEntityID());

    final CriteriaSet baseCriteria = new CriteriaSet(CriteriaSet.Conjunction.OR);
    final String[] lookupTexts = isMultipleSelectionAllowed() ? getSearchString().split(getMultiValueSeperator()) : new String[] {getSearchString()};
    for (final Property lookupProperty : lookupProperties) {
      for (final String lookupText : lookupTexts) {
        final String modifiedLookupText = (isWildcardPrefix() ? getWildcard() : "") + lookupText
                + (isWildcardPostfix() ? getWildcard() : "");
        baseCriteria.addCriteria(new PropertyCriteria(lookupProperty, SearchType.LIKE, modifiedLookupText).setCaseSensitive(isCaseSensitive()));
      }
    }

    return new EntityCriteria(getEntityID(), additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet(CriteriaSet.Conjunction.AND, additionalLookupCriteria, baseCriteria));
  }

  public List<Entity> performQuery() throws UserException {
    try {
      return dbProvider.getEntityDb().selectMany(getEntityCriteria());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private String toString(final List<Entity> entityList) {
    final StringBuilder ret = new StringBuilder();
    for (int i = 0; i < entityList.size(); i++) {
      ret.append(entityList.get(i).toString());
      if (i < entityList.size()-1)
        ret.append(getMultiValueSeperator());
    }

    return ret.toString();
  }
}
