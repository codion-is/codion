/*
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Serializer;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public final class AttributeDefinitionTest {

  private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
  private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType", AttributeDefinitionTest.class.getName());
  private static final EntityType ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

  @Test
  void derivedAttribute() {
    Attribute<Integer> derived = ENTITY_TYPE.integerAttribute("derived");
    assertThrows(IllegalArgumentException.class, () -> derived.define()
            .derived(linkedValues -> null));
    Attribute<Integer> source = ENTITY_TYPE.integerColumn("source");
    assertThrows(UnsupportedOperationException.class, () -> derived.define()
            .derived(linkedValues -> null, source)
            .nullable(false));
    assertThrows(UnsupportedOperationException.class, () -> derived.define()
            .derived(linkedValues -> null, source)
            .defaultValue(10));
    assertThrows(UnsupportedOperationException.class, () -> derived.define()
            .derived(linkedValues -> null, source)
            .maximumLength(10));
    assertThrows(UnsupportedOperationException.class, () -> derived.define()
            .derived(linkedValues -> null, source)
            .minimumValue(10));
    assertThrows(UnsupportedOperationException.class, () -> derived.define()
            .derived(linkedValues -> null, source)
            .valueRange(10, 20));
  }

  @Test
  void foreignKeyNonUniqueReferencedAttribute() {
    Column<Integer> attribute = ENTITY_TYPE.integerColumn("attr");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, attribute));
  }

  @Test
  void foreignKeyDifferentReferenceEntities() {
    Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
    Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, attribute2, integerAttribute));
  }

  @Test
  void foreignKeyDifferentReferenceEntities2() {
    Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
    Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, integerAttribute, attribute2));
  }

  @Test
  void foreignKeyAttributeFromOtherEntity() {
    Column<Integer> attribute1 = ENTITY_TYPE2.integerColumn("attribute1");
    Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2));
  }

  @Test
  void foreignKeyDuplicateAttribute() {
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
  void intColumnWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("attribute").define().column()
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void doubleColumnWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.doubleColumn("attribute").define().column()
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void nonTemporalColumnWithFormatPatter() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").define().column().dateTimePattern("dd-MM-yy"));
  }

  @Test
  void nonDecimalWithRoundingMode() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").define().column().decimalRoundingMode(RoundingMode.CEILING));
  }

  @Test
  void dateColumnWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").define().column().format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void timestampColumnWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateTimeColumn("attribute").define().column().format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").define().column().maximumFractionDigits(5));
  }

  @Test
  void maximumFractionDigitsNotNumerical() {
    assertEquals(-1, ENTITY_TYPE.localDateColumn("attribute").define().column().build().maximumFractionDigits());
  }

  @Test
  void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").define().column().numberFormatGrouping(false));
  }

  @Test
  void setRangeNonNumerical() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").define().column().valueRange(5, 6));
  }

  @Test
  void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.stringColumn("attribute").define().column().maximumFractionDigits(5));
  }

  @Test
  void characterMaximumLength() {
    assertEquals(1, ENTITY_TYPE.characterColumn("attribute").define().column().build().maximumLength());
  }

  @Test
  void minimumMaximumValue() {
    ColumnDefinition.Builder<Double, ?> builder = ENTITY_TYPE.doubleColumn("attribute").define().column();
    assertThrows(IllegalArgumentException.class, () -> builder.valueRange(5, 4));
  }

  @Test
  void setColumnName() {
    assertEquals("hello", ((ColumnDefinition<?>) ENTITY_TYPE.integerColumn("attribute").define().column().name("hello").build()).name());
  }

  @Test
  void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.integerColumn("attribute").define().column().name(null));
  }

  @Test
  void description() {
    final String description = "Here is a description";
    AttributeDefinition<Integer> attributeDefinition = ENTITY_TYPE.integerColumn("attribute").define().column().description(description).build();
    assertEquals(description, attributeDefinition.description());
  }

  @Test
  void mnemonic() {
    final char mnemonic = 'M';
    AttributeDefinition<Integer> attributeDefinition = ENTITY_TYPE.integerColumn("attribute").define().column().mnemonic(mnemonic).build();
    assertEquals(mnemonic, attributeDefinition.mnemonic());
    attributeDefinition = ENTITY_TYPE.integerColumn("attribute").define().column().build();
    assertEquals(0, attributeDefinition.mnemonic());
  }

  @Test
  void subqueryColumns() {
    assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").define().subquery("select").readOnly(true));
    assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").define().subquery("select").readOnly(false));
    assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").define().subquery("select").updatable(false));
    assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").define().subquery("select").insertable(false));
    assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").define().subquery("select").expression("expression"));
  }

  @Test
  void stringColumnNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.stringColumn("attribute").define().column().maximumLength(-4));
  }

  @Test
  void searchColumnNonVarchar() {
    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").define().column().searchColumn(true));
  }

  @Test
  void i18n() throws IOException, ClassNotFoundException {
    AttributeDefinition<Integer> attributeDefinition =
            ENTITY_TYPE.integerColumn("i18n").define().column()
                    .captionResourceKey("test").build();

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", attributeDefinition.caption());

    attributeDefinition = Serializer.deserialize(Serializer.serialize(attributeDefinition));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", attributeDefinition.caption());

    assertThrows(IllegalStateException.class, () -> ENTITY_TYPE2.integerColumn("i18n").define().column()
            .captionResourceKey("key"));

    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("i18n").define().column()
            .captionResourceKey("invalid_key"));
  }

  @Test
  void itemAttribute() {
    List<Item<Integer>> itemsDuplicate = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(1));
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("item").define().column().items(itemsDuplicate));

    List<Item<Integer>> items = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(3));
    AttributeDefinition<Integer> attribute = ENTITY_TYPE.integerAttribute("item").define().attribute().items(items).build();
    assertFalse(attribute.validItem(4));
    assertTrue(attribute.validItem(null));
    assertTrue(attribute.validItem(2));
  }
}
