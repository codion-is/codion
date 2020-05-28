/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.DateFormats;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertiesTest {

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(attribute("propertyId"), Types.INTEGER, "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferencePropertyId() {
    final Attribute<ForeignKeyValue> propertyId = attribute("propertyId");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(propertyId, "caption", "referencedEntityId", columnProperty(propertyId, Types.INTEGER)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(attribute("propertyId"), "caption", "referencedEntityId", (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(attribute("propertyId"), "caption", null, columnProperty(attribute("col"), Types.INTEGER)));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(attribute("propertyId"), Types.INTEGER).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(attribute("propertyId"), Types.DOUBLE).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(attribute("propertyId"), Types.TIMESTAMP).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).maximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).get().getMaximumFractionDigits());
  }

  @Test
  public void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).numberFormatGrouping(false));
  }

  @Test
  public void setMinimumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).minimumValue(5));
  }

  @Test
  public void setMaximumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.DATE).maximumValue(5));
  }

  @Test
  public void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("propertyId"), Types.VARCHAR).maximumFractionDigits(5));
  }

  @Test
  public void minimumMaximumValue() {
    final ColumnProperty.Builder builder = columnProperty(attribute("propertyId"), Types.DOUBLE);
    builder.minimumValue(5);
    assertThrows(IllegalArgumentException.class, () -> builder.maximumValue(4));
    builder.maximumValue(6);
    assertThrows(IllegalArgumentException.class, () -> builder.minimumValue(7));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty(attribute("propertyId"), Types.INTEGER).columnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(attribute("propertyId"), Types.INTEGER).columnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = columnProperty(attribute("propertyId"), Types.INTEGER).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = columnProperty(attribute("propertyId"), Types.INTEGER).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void setEntityIdAlreadySet() {
    final Property.Builder property = columnProperty(attribute("propertyId"), Types.INTEGER).entityId("entityId");
    assertThrows(IllegalStateException.class, () -> property.entityId("test"));
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final ColumnProperty.Builder columnProperty = columnProperty(attribute("propertyId"), Types.INTEGER);
    final ColumnProperty.Builder columnProperty2 = columnProperty(attribute("propertyId2"), Types.INTEGER);
    final ForeignKeyProperty.Builder foreignKeyProperty =
            foreignKeyProperty(attribute("fkPropertyID"), "fk", "referenceEntityID",
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.nullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final ColumnProperty.Builder updatableReferenceProperty = columnProperty(attribute("propertyId"), Types.INTEGER);
    final ColumnProperty.Builder nonUpdatableReferenceProperty = columnProperty(attribute("propertyId"), Types.INTEGER).updatable(false);

    final ForeignKeyProperty.Builder updatableForeignKeyProperty = foreignKeyProperty(attribute("fkProperty"), "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty(attribute("fkProperty"), "test",
            "referencedEntityID", nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty(attribute("fkProperty"), "test", "referencedEntityID",
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(attribute("id"), "caption", "entityId", (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute("id"), "caption", "entityId",
            Collections.emptyList()));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(attribute("test"), Types.INTEGER, "caption", "select").readOnly(false));
  }

  @Test
  public void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(attribute("test"), Types.INTEGER, "caption", "select").updatable(false));
  }

  @Test
  public void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(attribute("test"), Types.INTEGER, "caption", "select").insertable(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(attribute("property"), Types.VARCHAR).maximumLength(-4));
  }

  @Test
  public void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(attribute("property"), Types.INTEGER).searchProperty(true));
  }
}
