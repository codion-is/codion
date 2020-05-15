/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.Configuration;
import dev.codion.common.value.PropertyValue;

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
          "dev.codion.swing.framework.ui.referentialIntegrityErrorHandling", ReferentialIntegrityErrorHandling.ERROR,
          ReferentialIntegrityErrorHandling::valueOf);
}
