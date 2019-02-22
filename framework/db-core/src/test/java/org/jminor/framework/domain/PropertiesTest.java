/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.text.NumberFormat;

import static org.jminor.framework.domain.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertiesTest {

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty("propertyId", Types.INTEGER, "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferencePropertyId() {
    final String propertyId = "propertyId";
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(propertyId, "caption", "referencedEntityId", columnProperty(propertyId)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("propertyId", "caption", "referencedEntityId", (Property.ColumnProperty) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("propertyId", "caption", null, columnProperty("col")));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.INTEGER).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.DOUBLE).setFormat(DateFormats.getDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.DATE).setFormat(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.TIMESTAMP).setFormat(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty("propertyId", Types.DATE).setMaximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty("propertyId", Types.DATE).getMaximumFractionDigits());
  }

  @Test
  public void setUserNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty("propertyId", Types.DATE).setUseNumberFormatGrouping(false));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty("propertyId").setColumnName("hello").getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty("propertyId").setColumnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = columnProperty("propertyId").setDescription(description);
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = columnProperty("propertyId").setMnemonic(mnemonic);
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void setEntityIDAlreadySet() {
    final Property property = columnProperty("propertyId").setEntityID("entityId");
    assertThrows(IllegalStateException.class, () -> property.setEntityID("test"));
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final Property.ColumnProperty columnProperty = columnProperty("propertyId");
    final Property.ColumnProperty columnProperty2 = columnProperty("propertyId2");
    final Property.ForeignKeyProperty foreignKeyProperty = foreignKeyProperty("fkPropertyID", "fk", "referenceEntityID",
            new Property.ColumnProperty[] {columnProperty, columnProperty2});
    foreignKeyProperty.setNullable(false);
    assertFalse(columnProperty.isNullable());
    assertFalse(columnProperty2.isNullable());
    assertFalse(foreignKeyProperty.isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final Property.ColumnProperty updatableReferenceProperty = columnProperty("propertyId");
    final Property.ColumnProperty nonUpdatableReferenceProperty = columnProperty("propertyId").setUpdatable(false);

    final Property.ForeignKeyProperty updatableForeignKeyProperty = foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", nonUpdatableReferenceProperty);

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty = foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", new Property.ColumnProperty[] {updatableReferenceProperty, nonUpdatableReferenceProperty});
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("id", "caption", "entityId", (Property.ColumnProperty) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty("id", "caption", "entityId", new Property.ColumnProperty[0]));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty("test", Types.INTEGER, "caption", "select").setReadOnly(false));
  }

  @Test
  public void derivedPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty("test", Types.INTEGER, "caption", linkedValues ->
            null, "linked").setReadOnly(false));
  }

  @Test
  public void denormalizedViewPropertySetReadOnlyFalse() {
    final Property.ColumnProperty columnProperty = columnProperty("property", Types.INTEGER);
    foreignKeyProperty("foreignId", "caption","entityId", columnProperty);
    assertThrows(UnsupportedOperationException.class, () -> denormalizedViewProperty("test", "foreignId", columnProperty, "caption").setReadOnly(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("property", Types.VARCHAR).setMaxLength(-4));
  }
}
