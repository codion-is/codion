/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

/**
 * The possible actions to take on a referential integrity error.
 */
public enum ReferentialIntegrityErrorHandling {
  /**
   * Display the error.
   */
  ERROR,
  /**
   * Display the dependencies causing the error.
   */
  DEPENDENCIES;

  /**
   * Specifies whether to display the error message or the dependent entities in case of a referential integrity error on delete<br>
   * Value type: {@link ReferentialIntegrityErrorHandling}<br>
   * Default value: {@link ReferentialIntegrityErrorHandling#ERROR}
   */
  public static final PropertyValue<ReferentialIntegrityErrorHandling> REFERENTIAL_INTEGRITY_ERROR_HANDLING = Configuration.value(
          "is.codion.swing.framework.ui.referentialIntegrityErrorHandling", ReferentialIntegrityErrorHandling.ERROR,
          ReferentialIntegrityErrorHandling::valueOf);
}
