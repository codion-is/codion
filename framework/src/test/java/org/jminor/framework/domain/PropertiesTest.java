/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;

import org.junit.Test;

import java.sql.Types;
import java.text.NumberFormat;
import java.util.Map;

import static org.junit.Assert.*;

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

  @Test(expected = IllegalStateException.class)
  public void setMaximumFractionDigitsNotNumerical() {
    Properties.columnProperty("propertyID", Types.DATE).setMaximumFractionDigits(5);
  }

  @Test(expected = IllegalStateException.class)
  public void getMaximumFractionDigitsNotNumerical() {
    Properties.columnProperty("propertyID", Types.DATE).getMaximumFractionDigits();
  }

  @Test(expected = IllegalStateException.class)
  public void setUserNumberFormatGroupingNotNumerical() {
    Properties.columnProperty("propertyID", Types.DATE).setUseNumberFormatGrouping(false);
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", Properties.columnProperty("propertyID").setColumnName("hello").getColumnName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setColumnNameNull() {
    Properties.columnProperty("propertyID").setColumnName(null);
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = Properties.columnProperty("propertyID").setDescription(description);
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = Properties.columnProperty("propertyID").setMnemonic(mnemonic);
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test(expected = IllegalStateException.class)
  public void setEntityIDAlreadySet() {
    final Property property = Properties.columnProperty("propertyID").setEntityID("entityID");
    property.setEntityID("test");
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

  @Test
  public void foreignKeyPropertyCompositeKey() {
    final Property.ColumnProperty columnProperty1 = Properties.columnProperty("fk1");
    final Property.ColumnProperty columnProperty2 = Properties.columnProperty("fk2");
    final Property.ForeignKeyProperty foreignKeyProperty = Properties.foreignKeyProperty("propertyID", "caption",
            "referencedEntityID", new Property.ColumnProperty[] {columnProperty1, columnProperty2}, new String[] {"id1", "id2"});
    assertEquals("id1", foreignKeyProperty.getReferencedPropertyID(columnProperty1));
    assertEquals("id2", foreignKeyProperty.getReferencedPropertyID(columnProperty2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNullProperty() {
    Properties.foreignKeyProperty("id", "caption", "entityID", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNoProperties() {
    Properties.foreignKeyProperty("id", "caption", "entityID", new Property.ColumnProperty[0], new String[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyCountMismatch() {
    final Property.ColumnProperty columnProperty1 = Properties.columnProperty("fk1");
    final Property.ColumnProperty columnProperty2 = Properties.columnProperty("fk2");
    Properties.foreignKeyProperty("propertyID", "caption", "referencedEntityID",
            new Property.ColumnProperty[] {columnProperty1, columnProperty2}, new String[] {"id1"});
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNullProperties() {
    final Property.ColumnProperty columnProperty1 = Properties.columnProperty("fk1");
    final Property.ColumnProperty columnProperty2 = Properties.columnProperty("fk2");
    Properties.foreignKeyProperty("propertyID", "caption", "referencedEntityID",
            new Property.ColumnProperty[] {columnProperty1, columnProperty2}, null);
  }
}
