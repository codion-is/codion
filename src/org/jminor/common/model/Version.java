/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A simple version interface for semantic versioning (http://semver.org)
 */
public interface Version extends Comparable<Version> {

  /**
   * @return the major part of this version
   */
  int getMajor();

  /**
   * @return the minor part of this version
   */
  int getMinor();

  /**
   * @return the patch part of this version
   */
  int getPatch();

  /**
   * @return the metadata part of this version
   */
  String getMetadata();
}
