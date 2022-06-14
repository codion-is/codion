/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MapperTest {

  @Test
  void test() {
    class Person {
      final String name;
      final Integer age;

      private Person(String name, Integer age) {
        this.name = name;
        this.age = age;
      }

      public Integer getAge() {
        return age;
      }
    }

    List<Person> persons = Arrays.asList(
            new Person("One", 20),
            new Person("Two", 20),
            new Person("Three", 22),
            new Person("Four", 20),
            new Person("Five", 22),
            new Person("Six", 30)
    );

    Map<Integer, List<Person>> mapped = Mapper.map(persons, Person::getAge);

    List<Person> twenties = mapped.get(20);
    assertEquals(3, twenties.size());
    List<Person> twentytwos = mapped.get(22);
    assertEquals(2, twentytwos.size());
    assertEquals(1, mapped.get(30).size());

    assertEquals("One", twenties.get(0).name);
    assertEquals("Two", twenties.get(1).name);
    assertEquals("Four", twenties.get(2).name);

    assertEquals("Three", twentytwos.get(0).name);
    assertEquals("Five", twentytwos.get(1).name);
  }
}
