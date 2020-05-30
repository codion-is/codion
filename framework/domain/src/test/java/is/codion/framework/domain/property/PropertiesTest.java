/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.DateFormats;

import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertiesTest {

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(Attributes.attribute("attribute", Integer.class), "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferenceAttribute() {
    final EntityAttribute attribute = Attributes.entityAttribute("attribute");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute, "caption", "referencedEntityId", columnProperty(attribute)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("attribute"), "caption", "referencedEntityId", (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("attribute"), "caption", null, columnProperty(Attributes.attribute("col", Integer.class))));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", Integer.class)).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", Double.class)).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDateTime.class)).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).maximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).get().getMaximumFractionDigits());
  }

  @Test
  public void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).numberFormatGrouping(false));
  }

  @Test
  public void setMinimumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).minimumValue(5));
  }

  @Test
  public void setMaximumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class)).maximumValue(5));
  }

  @Test
  public void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", String.class)).maximumFractionDigits(5));
  }

  @Test
  public void minimumMaximumValue() {
    final ColumnProperty.Builder<Double> builder = columnProperty(Attributes.attribute("attribute", Double.class));
    builder.minimumValue(5);
    assertThrows(IllegalArgumentException.class, () -> builder.maximumValue(4));
    builder.maximumValue(6);
    assertThrows(IllegalArgumentException.class, () -> builder.minimumValue(7));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty(Attributes.attribute("attribute", Integer.class)).columnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(Attributes.attribute("attribute", Integer.class)).columnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = columnProperty(Attributes.attribute("attribute", Integer.class)).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = columnProperty(Attributes.attribute("attribute", Integer.class)).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void setEntityIdAlreadySet() {
    final Property.Builder property = columnProperty(Attributes.attribute("attribute", Integer.class)).entityId("entityId");
    assertThrows(IllegalStateException.class, () -> property.entityId("test"));
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final ColumnProperty.Builder columnProperty = columnProperty(Attributes.attribute("attribute", Integer.class));
    final ColumnProperty.Builder columnProperty2 = columnProperty(Attributes.attribute("attribute2", Integer.class));
    final ForeignKeyProperty.Builder foreignKeyProperty =
            foreignKeyProperty(Attributes.entityAttribute("fkAttribute"), "fk", "referenceEntityID",
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.nullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final ColumnProperty.Builder updatableReferenceProperty = columnProperty(Attributes.attribute("attribute", Integer.class));
    final ColumnProperty.Builder nonUpdatableReferenceProperty = columnProperty(Attributes.attribute("attribute", Integer.class)).updatable(false);

    final ForeignKeyProperty.Builder updatableForeignKeyProperty = foreignKeyProperty(Attributes.entityAttribute("fkProperty"), "test",
            "referencedEntityID", updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty(Attributes.entityAttribute("fkProperty"), "test",
            "referencedEntityID", nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty(Attributes.entityAttribute("fkProperty"), "test", "referencedEntityID",
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("id"), "caption", "entityId", (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(Attributes.entityAttribute("id"), "caption", "entityId",
            Collections.emptyList()));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class), "caption", "select").readOnly(false));
  }

  @Test
  public void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class), "caption", "select").updatable(false));
  }

  @Test
  public void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class), "caption", "select").insertable(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("property", String.class)).maximumLength(-4));
  }

  @Test
  public void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("property", Integer.class)).searchProperty(true));
  }
}
