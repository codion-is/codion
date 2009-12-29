/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Array class for primitive integer handling
 */
public class IntArray extends ArrayList<Integer> implements Serializable {

  private static final long serialVersionUID = 1;

  /** Constructs a new IntArray. */
  public IntArray() {}

  public IntArray(final int initialCapacity) {
    super(initialCapacity);
  }

  public IntArray(final int[] data) {
    super(data.length);
    addIntegers(data);
  }

  public void addInt(final int integer) {
    super.add(integer);
  }

  public Integer getIntegerAt(final int index) {
    return super.get(index);
  }

  public int getIntAt(final int index) {
    return super.get(index);
  }

  public int[] toIntArray() {
    final int[] integers = new int[size()];
    for (int i = 0; i < integers.length; i++)
      integers[i] = getIntAt(i);

    return integers;
  }

  public void addIntegers(final int[] integers) {
    for (int integer : integers)
      super.add(integer);
  }
}