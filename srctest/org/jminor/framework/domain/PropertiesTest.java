/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.Test;

import java.sql.Types;
import java.util.Map;

public final class PropertiesTest {

  @Test(expected = IllegalArgumentException.class)
  public void derivedPropertyWithoutLinkedProperties() {
    Properties.derivedProperty("propertyID", Types.INTEGER, "caption", new Property.DerivedProperty.Provider() {
      public Object getValue(final Map<String, Object> linkedValues) {
        return null;
      }
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNonUniqueReferencePropertyID() {
    final String propertyID = "propertyID";
    Properties.foreignKeyProperty(propertyID, "caption", "referencedEntityID", Properties.columnProperty(propertyID));
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyWithoutReferenceProperty() {
    Properties.foreignKeyProperty("propertyID", "caption", "referencedEntityID", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyWithoutReferenceEntityID() {
    Properties.foreignKeyProperty("propertyID", "caption", null, Properties.columnProperty("col"));
  }
}
