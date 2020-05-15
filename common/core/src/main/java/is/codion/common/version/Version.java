/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

import java.io.Serializable;

/**
 * A version.
 */
public interface Version extends Comparable<Version>, Serializable {

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
