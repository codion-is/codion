/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;

import org.junit.Test;

import java.sql.Types;
import java.text.NumberFormat;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PropertiesTest {

  @Test(expected = IllegalArgumentException.class)
  public void derivedPropertyWithoutLinkedProperties() {
    Properties.derivedProperty("propertyID", Types.INTEGER, "caption", new Property.DerivedProperty.Provider() {
      @Override
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

  @Test(expected = IllegalArgumentException.class)
  public void intPropertyWithDateFormat() {
    Properties.columnProperty("propertyID", Types.INTEGER).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doublePropertyWithDateFormat() {
    Properties.columnProperty("propertyID", Types.DOUBLE).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void datePropertyWithNumberFormat() {
    Properties.columnProperty("propertyID", Types.DATE).setFormat(NumberFormat.getIntegerInstance());
  }

  @Test(expected = IllegalArgumentException.class)
  public void timestampPropertyWithNumberFormat() {
    Properties.columnProperty("propertyID", Types.TIMESTAMP).setFormat(NumberFormat.getIntegerInstance());
  }
  
  @Test
  public void foreignKeyPropertyNullable() {
    final Property.ColumnProperty columnProperty = Properties.columnProperty("propertyID");
    final Property.ColumnProperty columnProperty2 = Properties.columnProperty("propertyID2");
    final Property.ForeignKeyProperty foreignKeyProperty= Properties.foreignKeyProperty("fkPropertyID", "fk", "referenceEntityID",
            new Property.ColumnProperty[] {columnProperty, columnProperty2}, new String[] {"", ""});
    foreignKeyProperty.setNullable(false);
    assertFalse(columnProperty.isNullable());
    assertFalse(columnProperty2.isNullable());
    assertFalse(foreignKeyProperty.isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final Property.ColumnProperty updatableReferenceProperty = Properties.columnProperty("propertyID");
    final Property.ColumnProperty nonUpdatableReferenceProperty = Properties.columnProperty("propertyID").setUpdatable(false);

    final Property.ForeignKeyProperty updatableForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", nonUpdatableReferenceProperty);

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", new Property.ColumnProperty[] {updatableReferenceProperty, nonUpdatableReferenceProperty},
            new String[] {"test", "testing"});
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }
}
