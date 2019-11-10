/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

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
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("propertyId", "caption", "referencedEntityId", (PropertyDefinition.ColumnPropertyDefinition) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("propertyId", "caption", null, columnProperty("col")));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.INTEGER).setFormat(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("propertyId", Types.DOUBLE).setFormat(new SimpleDateFormat(DateFormats.COMPACT)));
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
    assertThrows(IllegalStateException.class, () -> columnProperty("propertyId", Types.DATE).get().getMaximumFractionDigits());
  }

  @Test
  public void setUserNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty("propertyId", Types.DATE).setUseNumberFormatGrouping(false));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty("propertyId").setColumnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty("propertyId").setColumnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = columnProperty("propertyId").setDescription(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = columnProperty("propertyId").setMnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void setEntityIdAlreadySet() {
    final PropertyDefinition property = columnProperty("propertyId").setEntityId("entityId");
    assertThrows(IllegalStateException.class, () -> property.setEntityId("test"));
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final PropertyDefinition.ColumnPropertyDefinition columnProperty = columnProperty("propertyId");
    final PropertyDefinition.ColumnPropertyDefinition columnProperty2 = columnProperty("propertyId2");
    final PropertyDefinition.ForeignKeyPropertyDefinition foreignKeyProperty =
            foreignKeyProperty("fkPropertyID", "fk", "referenceEntityID",
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.setNullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final PropertyDefinition.ColumnPropertyDefinition updatableReferenceProperty = columnProperty("propertyId");
    final PropertyDefinition.ColumnPropertyDefinition nonUpdatableReferenceProperty = columnProperty("propertyId").setUpdatable(false);

    final PropertyDefinition.ForeignKeyPropertyDefinition updatableForeignKeyProperty = foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty("fkProperty", "test",
            "referencedEntityID", nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final Property.ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty("fkProperty", "test", "referencedEntityID",
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty("id", "caption", "entityId", (PropertyDefinition.ColumnPropertyDefinition) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty("id", "caption", "entityId",
            Collections.emptyList()));
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
    final PropertyDefinition.ColumnPropertyDefinition columnProperty = columnProperty("property", Types.INTEGER);
    foreignKeyProperty("foreignId", "caption","entityId", columnProperty);
    assertThrows(UnsupportedOperationException.class, () -> denormalizedViewProperty("test", "foreignId", columnProperty.get(), "caption").setReadOnly(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty("property", Types.VARCHAR).setMaxLength(-4));
  }
}
