/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

/**
 * The possible actions to take on a referential integrity error.
 */
public enum ReferentialIntegrityErrorHandling {
  /**
   * Display the error.
   */
  DISPLAY_ERROR,
  /**
   * Display the dependencies causing the error.
   */
  DISPLAY_DEPENDENCIES;

  /**
   * Specifies whether to display the error message or the dependent entities in case of a referential integrity error on delete<br>
   * Value type: {@link ReferentialIntegrityErrorHandling}<br>
   * Default value: {@link ReferentialIntegrityErrorHandling#DISPLAY_ERROR}
   */
  public static final PropertyValue<ReferentialIntegrityErrorHandling> REFERENTIAL_INTEGRITY_ERROR_HANDLING =
          Configuration.enumValue("is.codion.swing.framework.ui.referentialIntegrityErrorHandling",
                  ReferentialIntegrityErrorHandling.class, ReferentialIntegrityErrorHandling.DISPLAY_ERROR);
}
