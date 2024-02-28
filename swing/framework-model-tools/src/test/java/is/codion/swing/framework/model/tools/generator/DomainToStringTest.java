/*
 * Copyright (c) 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainToStringTest {

  @Test
  void underscoreToCamelCase() {
    assertEquals("", DomainToString.underscoreToCamelCase(""));
    assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("noOfSpeakers"));
    assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("no_of_speakers"));
    assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("No_OF_speakeRS"));
    assertEquals("helloWorld", DomainToString.underscoreToCamelCase("hello_World"));
    assertEquals("", DomainToString.underscoreToCamelCase("_"));
    assertEquals("aB", DomainToString.underscoreToCamelCase("a_b"));
    assertEquals("aB", DomainToString.underscoreToCamelCase("a_b_"));
    assertEquals("aBC", DomainToString.underscoreToCamelCase("a_b_c"));
    assertEquals("aBaC", DomainToString.underscoreToCamelCase("a_ba_c"));
    assertEquals("a", DomainToString.underscoreToCamelCase("a__"));
    assertEquals("a", DomainToString.underscoreToCamelCase("__a"));
    assertEquals("a", DomainToString.underscoreToCamelCase("__A"));
  }
}
