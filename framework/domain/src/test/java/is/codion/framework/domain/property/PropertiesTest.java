/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.DateFormats;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import static is.codion.framework.domain.entity.Entities.entityIdentity;
import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertiesTest {

  private static final Entity.Identity ENTITY_ID = entityIdentity("entityId");
  private static final Entity.Identity REFERENCED_ENTITY_ID = entityIdentity("referencedEntityId");

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(ENTITY_ID.integerAttribute("attribute"), "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferenceAttribute() {
    final Attribute<Entity> attribute = ENTITY_ID.entityAttribute("attribute");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute, "caption", REFERENCED_ENTITY_ID, columnProperty(attribute)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_ID.entityAttribute("attribute"), "caption", REFERENCED_ENTITY_ID, (ColumnProperty.Builder<?>) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityId() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_ID.entityAttribute("attribute"), "caption", null, columnProperty(ENTITY_ID.integerAttribute("col"))));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_ID.integerAttribute("attribute")).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_ID.doubleAttribute("attribute")).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_ID.localDateTimeAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).get().getMaximumFractionDigits());
  }

  @Test
  public void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).numberFormatGrouping(false));
  }

  @Test
  public void setMinimumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).minimumValue(5));
  }

  @Test
  public void setMaximumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.localDateAttribute("attribute")).maximumValue(5));
  }

  @Test
  public void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.stringAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  public void minimumMaximumValue() {
    final ColumnProperty.Builder<Double> builder = columnProperty(ENTITY_ID.doubleAttribute("attribute"));
    builder.minimumValue(5);
    assertThrows(IllegalArgumentException.class, () -> builder.maximumValue(4));
    builder.maximumValue(6);
    assertThrows(IllegalArgumentException.class, () -> builder.minimumValue(7));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty(ENTITY_ID.integerAttribute("attribute")).columnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(ENTITY_ID.integerAttribute("attribute")).columnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property<Integer> property = columnProperty(ENTITY_ID.integerAttribute("attribute")).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property<Integer> property = columnProperty(ENTITY_ID.integerAttribute("attribute")).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final ColumnProperty.Builder<Integer> columnProperty = columnProperty(ENTITY_ID.integerAttribute("attribute"));
    final ColumnProperty.Builder<Integer> columnProperty2 = columnProperty(ENTITY_ID.integerAttribute("attribute2"));
    final ForeignKeyProperty.Builder foreignKeyProperty =
            foreignKeyProperty(ENTITY_ID.entityAttribute("fkAttribute"), "fk", REFERENCED_ENTITY_ID,
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.nullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final ColumnProperty.Builder<Integer> updatableReferenceProperty = columnProperty(ENTITY_ID.integerAttribute("attribute"));
    final ColumnProperty.Builder<Integer> nonUpdatableReferenceProperty = columnProperty(ENTITY_ID.integerAttribute("attribute")).updatable(false);

    final ForeignKeyProperty.Builder updatableForeignKeyProperty = foreignKeyProperty(ENTITY_ID.entityAttribute(
            "fkProperty"), "test",
            REFERENCED_ENTITY_ID, updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty(ENTITY_ID.entityAttribute(
            "fkProperty"), "test",
            REFERENCED_ENTITY_ID, nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty(ENTITY_ID.entityAttribute("fkProperty"), "test", REFERENCED_ENTITY_ID,
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_ID.entityAttribute("id"), "caption", ENTITY_ID, (ColumnProperty.Builder<?>) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(ENTITY_ID.entityAttribute("id")
            , "caption", ENTITY_ID,
            Collections.emptyList()));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_ID.integerAttribute("test"), "caption", "select").readOnly(false));
  }

  @Test
  public void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_ID.integerAttribute("test"), "caption", "select").updatable(false));
  }

  @Test
  public void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_ID.integerAttribute("test"), "caption", "select").insertable(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_ID.stringAttribute("property")).maximumLength(-4));
  }

  @Test
  public void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_ID.integerAttribute("property")).searchProperty(true));
  }
}
