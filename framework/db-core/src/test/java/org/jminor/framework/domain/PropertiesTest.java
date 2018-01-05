/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;

import org.junit.Test;

import java.sql.Types;
import java.text.NumberFormat;

import static org.junit.Assert.*;

public final class PropertiesTest {

  @Test(expected = IllegalArgumentException.class)
  public void derivedPropertyWithoutLinkedProperties() {
    Properties.derivedProperty("propertyId", Types.INTEGER, "caption", linkedValues -> null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNonUniqueReferencePropertyId() {
    final String propertyId = "propertyId";
    Properties.foreignKeyProperty(propertyId, "caption", "referencedEntityId", Properties.columnProperty(propertyId));
  }

  @Test(expected = NullPointerException.class)
  public void foreignKeyPropertyWithoutReferenceProperty() {
    Properties.foreignKeyProperty("propertyId", "caption", "referencedEntityId", (Property.ColumnProperty) null);
  }

  @Test(expected = NullPointerException.class)
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    Properties.foreignKeyProperty("propertyId", "caption", null, Properties.columnProperty("col"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void intPropertyWithDateFormat() {
    Properties.columnProperty("propertyId", Types.INTEGER).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doublePropertyWithDateFormat() {
    Properties.columnProperty("propertyId", Types.DOUBLE).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void datePropertyWithNumberFormat() {
    Properties.columnProperty("propertyId", Types.DATE).setFormat(NumberFormat.getIntegerInstance());
  }

  @Test(expected = IllegalArgumentException.class)
  public void timestampPropertyWithNumberFormat() {
    Properties.columnProperty("propertyId", Types.TIMESTAMP).setFormat(NumberFormat.getIntegerInstance());
  }

  @Test(expected = IllegalStateException.class)
  public void setMaximumFractionDigitsNotNumerical() {
    Properties.columnProperty("propertyId", Types.DATE).setMaximumFractionDigits(5);
  }

  @Test(expected = IllegalStateException.class)
  public void getMaximumFractionDigitsNotNumerical() {
    Properties.columnProperty("propertyId", Types.DATE).getMaximumFractionDigits();
  }

  @Test(expected = IllegalStateException.class)
  public void setUserNumberFormatGroupingNotNumerical() {
    Properties.columnProperty("propertyId", Types.DATE).setUseNumberFormatGrouping(false);
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", Properties.columnProperty("propertyId").setColumnName("hello").getColumnName());
  }

  @Test(expected = NullPointerException.class)
  public void setColumnNameNull() {
    Properties.columnProperty("propertyId").setColumnName(null);
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = Properties.columnProperty("propertyId").setDescription(description);
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = Properties.columnProperty("propertyId").setMnemonic(mnemonic);
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test(expected = IllegalStateException.class)
  public void setEntityIDAlreadySet() {
    final Property property = Properties.columnProperty("propertyId").setEntityID("entityId");
    property.setEntityID("test");
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final Property.ColumnProperty columnProperty = Properties.columnProperty("propertyId");
    final Property.ColumnProperty columnProperty2 = Properties.columnProperty("propertyId2");
    final Property.ForeignKeyProperty foreignKeyProperty= Properties.foreignKeyProperty("fkPropertyID", "fk", "referenceEntityID",
            new Property.ColumnProperty[] {columnProperty, columnProperty2});
    foreignKeyProperty.setNullable(false);
    assertFalse(columnProperty.isNullable());
    assertFalse(columnProperty2.isNullable());
    assertFalse(foreignKeyProperty.isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final Property.ColumnProperty updatableReferenceProperty = Properties.columnProperty("propertyId");
    final Property.ColumnProperty nonUpdatableReferenceProperty = Properties.columnProperty("propertyId").setUpdatable(false);

    final Property.ForeignKeyProperty updatableForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", nonUpdatableReferenceProperty);

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty = Properties.foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", new Property.ColumnProperty[] {updatableReferenceProperty, nonUpdatableReferenceProperty});
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test(expected = NullPointerException.class)
  public void foreignKeyPropertyNullProperty() {
    Properties.foreignKeyProperty("id", "caption", "entityId", (Property.ColumnProperty) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyNoProperties() {
    Properties.foreignKeyProperty("id", "caption", "entityId", new Property.ColumnProperty[0]);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void subqueryPropertySetReadOnlyFalse() {
    Properties.subqueryProperty("test", Types.INTEGER, "caption", "select").setReadOnly(false);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void derivedPropertySetReadOnlyFalse() {
    Properties.derivedProperty("test", Types.INTEGER, "caption", linkedValues ->
            null, "linked").setReadOnly(false);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void denormalizedViewPropertySetReadOnlyFalse() {
    final Property.ColumnProperty columnProperty = Properties.columnProperty("property", Types.INTEGER);
    final Property.ForeignKeyProperty foreignKeyProperty = Properties.foreignKeyProperty("foreignId", "caption",
            "entityId", columnProperty);
    Properties.denormalizedViewProperty("test", "foreignId", columnProperty, "caption").setReadOnly(false);
  }
}
