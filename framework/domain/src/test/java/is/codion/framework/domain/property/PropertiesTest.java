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

  private static final Identity ENTITY_ID = Identity.identity("entityId");
  private static final Identity REFERENCED_ENTITY_ID = Identity.identity("referencedEntityId");

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID), "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferenceAttribute() {
    final EntityAttribute attribute = Attributes.entityAttribute("attribute", ENTITY_ID);
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute, "caption", REFERENCED_ENTITY_ID, columnProperty(attribute)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("attribute", ENTITY_ID), "caption", REFERENCED_ENTITY_ID, (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("attribute", ENTITY_ID), "caption", null, columnProperty(Attributes.attribute("col", Integer.class, ENTITY_ID))));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", Double.class, ENTITY_ID)).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDateTime.class, ENTITY_ID)).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).maximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).get().getMaximumFractionDigits());
  }

  @Test
  public void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).numberFormatGrouping(false));
  }

  @Test
  public void setMinimumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).minimumValue(5));
  }

  @Test
  public void setMaximumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", LocalDate.class, ENTITY_ID)).maximumValue(5));
  }

  @Test
  public void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("attribute", String.class, ENTITY_ID)).maximumFractionDigits(5));
  }

  @Test
  public void minimumMaximumValue() {
    final ColumnProperty.Builder<Double> builder = columnProperty(Attributes.attribute("attribute", Double.class, ENTITY_ID));
    builder.minimumValue(5);
    assertThrows(IllegalArgumentException.class, () -> builder.maximumValue(4));
    builder.maximumValue(6);
    assertThrows(IllegalArgumentException.class, () -> builder.minimumValue(7));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).columnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).columnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property property = columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property property = columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final ColumnProperty.Builder columnProperty = columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID));
    final ColumnProperty.Builder columnProperty2 = columnProperty(Attributes.attribute("attribute2", Integer.class, ENTITY_ID));
    final ForeignKeyProperty.Builder foreignKeyProperty =
            foreignKeyProperty(Attributes.entityAttribute("fkAttribute", ENTITY_ID), "fk", REFERENCED_ENTITY_ID,
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.nullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final ColumnProperty.Builder updatableReferenceProperty = columnProperty(Attributes.attribute("attribute",
            Integer.class, ENTITY_ID));
    final ColumnProperty.Builder nonUpdatableReferenceProperty = columnProperty(Attributes.attribute("attribute", Integer.class, ENTITY_ID)).updatable(false);

    final ForeignKeyProperty.Builder updatableForeignKeyProperty = foreignKeyProperty(Attributes.entityAttribute(
            "fkProperty", ENTITY_ID), "test",
            REFERENCED_ENTITY_ID, updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty(Attributes.entityAttribute(
            "fkProperty", ENTITY_ID), "test",
            REFERENCED_ENTITY_ID, nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty(Attributes.entityAttribute("fkProperty", ENTITY_ID), "test", REFERENCED_ENTITY_ID,
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(Attributes.entityAttribute("id", ENTITY_ID), "caption", ENTITY_ID, (ColumnProperty.Builder) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(Attributes.entityAttribute("id", ENTITY_ID)
            , "caption", ENTITY_ID,
            Collections.emptyList()));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class, ENTITY_ID), "caption", "select").readOnly(false));
  }

  @Test
  public void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class, ENTITY_ID), "caption", "select").updatable(false));
  }

  @Test
  public void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(Attributes.attribute("test", Integer.class, ENTITY_ID), "caption", "select").insertable(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(Attributes.attribute("property", String.class, ENTITY_ID)).maximumLength(-4));
  }

  @Test
  public void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(Attributes.attribute("property", Integer.class, ENTITY_ID)).searchProperty(true));
  }
}
