/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    addAllInts(data);
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

  public int indexOf(final int integer) {
    return super.indexOf(new Integer(integer));
  }

  public List toIntegerList() {
    return this;
  }

  public List<Integer> toSortedIntegerList() {
    final Integer[] integers = toIntegerArray();
    Arrays.sort(integers);

    return new ArrayList<Integer>(Arrays.asList(integers));
  }

  public Integer[] toIntegerArray() {
    return super.toArray(new Integer[size()]);
  }

  public int[] toIntArray() {
    final int[] ret = new int[size()];
    for (int i = 0; i < ret.length; i++)
      ret[i] = getIntAt(i);

    return ret;
  }

  public void addAllInts(final int[] integers) {
    for (int integer : integers)
      super.add(integer);
  }

  public boolean contains(final int integer) {
    return super.contains(new Integer(integer));
  }
}