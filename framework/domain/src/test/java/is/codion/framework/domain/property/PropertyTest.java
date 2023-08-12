/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Serializer;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.domain.property.Property.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertyTest {

  private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
  private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType", PropertyTest.class.getName());
  private static final EntityType ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

  @Test
  void derivedProperties() {
    Attribute<Integer> derived = ENTITY_TYPE.integerAttribute("derived");
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(derived, "caption", linkedValues -> null));
    Attribute<Integer> source = ENTITY_TYPE.integerColumn("source");
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty(derived,
            "caption", linkedValues -> null, source)
            .nullable(false));
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty(derived,
            "caption", linkedValues -> null, source)
            .defaultValue(10));
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty(derived,
            "caption", linkedValues -> null, source)
            .maximumLength(10));
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty(derived,
            "caption", linkedValues -> null, source)
            .minimumValue(10));
    assertThrows(UnsupportedOperationException.class, () -> derivedProperty(derived,
            "caption", linkedValues -> null, source)
            .valueRange(10, 20));
  }

  @Test
  void foreignKeyPropertyNonUniqueReferencedAttribute() {
    Column<Integer> attribute = ENTITY_TYPE.integerColumn("attr");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, attribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities() {
    Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
    Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, attribute2, integerAttribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities2() {
    Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
    Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, integerAttribute, attribute2));
  }

  @Test
  void foreignKeyPropertyAttributeFromOtherEntity() {
    Column<Integer> attribute1 = ENTITY_TYPE2.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2));
  }

  @Test
  void foreignKeyPropertyDuplicateAttribute() {
    Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2, attribute1, attribute2));
  }

  @Test
  void foreignKeyWithoutReferencedAttribute() {
    Column<Integer> attribute = ENTITY_TYPE.integerColumn("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, null));
  }

  @Test
  void foreignKeyWithoutReference() {
    Column<Integer> attribute = ENTITY_TYPE.integerColumn("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", null, attribute));
  }

  @Test
  void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.doubleColumn("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void nonTemporalPropertyWithFormatPatter() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("attribute")).dateTimePattern("dd-MM-yy"));
  }

  @Test
  void nonDecimalWithRoundingMode() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("attribute")).decimalRoundingMode(RoundingMode.CEILING));
  }

  @Test
  void datePropertyWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateColumn("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateTimeColumn("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateColumn("attribute")).maximumFractionDigits(5));
  }

  @Test
  void maximumFractionDigitsNotNumerical() {
    assertEquals(-1, columnProperty(ENTITY_TYPE.localDateColumn("attribute")).build().maximumFractionDigits());
  }

  @Test
  void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateColumn("attribute")).numberFormatGrouping(false));
  }

  @Test
  void setRangeNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateColumn("attribute")).valueRange(5, 6));
  }

  @Test
  void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.stringColumn("attribute")).maximumFractionDigits(5));
  }

  @Test
  void minimumMaximumValue() {
    ColumnProperty.Builder<Double, ?> builder = columnProperty(ENTITY_TYPE.doubleColumn("attribute"));
    assertThrows(IllegalArgumentException.class, () -> builder.valueRange(5, 4));
  }

  @Test
  void setColumnName() {
    assertEquals("hello", ((ColumnProperty<?>) columnProperty(ENTITY_TYPE.integerColumn("attribute")).columnName("hello").build()).columnName());
  }

  @Test
  void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("attribute")).columnName(null));
  }

  @Test
  void description() {
    final String description = "Here is a description";
    Property<Integer> property = columnProperty(ENTITY_TYPE.integerColumn("attribute")).description(description).build();
    assertEquals(description, property.description());
  }

  @Test
  void mnemonic() {
    final Character mnemonic = 'M';
    Property<Integer> property = columnProperty(ENTITY_TYPE.integerColumn("attribute")).mnemonic(mnemonic).build();
    assertEquals(mnemonic, property.mnemonic());
  }

  @Test
  void subqueryProperties() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerColumn("test"), "select").readOnly(true));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerColumn("test"), "select").readOnly(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerColumn("test"), "select").updatable(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerColumn("test"), "select").insertable(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerColumn("test"), "select").columnExpression("expression"));
  }

  @Test
  void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.stringColumn("property")).maximumLength(-4));
  }

  @Test
  void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("property")).searchProperty(true));
  }

  @Test
  void i18n() throws IOException, ClassNotFoundException {
    Property<Integer> property =
            columnProperty(ENTITY_TYPE.integerColumn("i18n"))
                    .captionResourceKey("test").build();

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", property.caption());

    property = Serializer.deserialize(Serializer.serialize(property));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", property.caption());

    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE2.integerColumn("i18n"))
            .captionResourceKey("key"));

    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerColumn("i18n"))
            .captionResourceKey("invalid_key"));
  }

  @Test
  void itemProperty() {
    List<Item<Integer>> itemsDuplicate = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(1));
    assertThrows(IllegalArgumentException.class, () -> Property.itemProperty(ENTITY_TYPE.integerColumn("item"), itemsDuplicate));

    List<Item<Integer>> items = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(3));
    ItemProperty<Integer> property = (ItemProperty<Integer>) Property.itemProperty(ENTITY_TYPE.integerColumn("item"), items).build();
    assertFalse(property.isValid(4));
    assertThrows(IllegalArgumentException.class, () -> property.item(4));
    assertTrue(property.isValid(null));
    assertTrue(property.isValid(2));
    assertNotNull(property.item(null));
    assertNotNull(property.item(1));
  }
}
