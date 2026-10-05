/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.common.utilities.format.LocaleDateTimePattern;

import java.util.Locale;

/**
 * The {@link LocaleDateTimePattern} javadoc snippets, each the region of the same name.
 */
final class LocaleDateTimePatternSnippets {

  void usage() {
    LocaleDateTimePattern pattern = LocaleDateTimePattern.builder() // @start region=usage
            .delimiterDash()
            .yearFourDigits()
            .hoursMinutes()
            .build();

    Locale iceland = Locale.forLanguageTag("is-IS");
    Locale us = Locale.forLanguageTag("en-US");

    pattern.datePattern(iceland);     // "dd-MM-yyyy"
    pattern.datePattern(us);          // "MM-dd-yyyy"

    pattern.dateTimePattern(iceland); // "dd-MM-yyyy HH:mm"
    pattern.dateTimePattern(us);      // "MM-dd-yyyy HH:mm" // @end
  }
}
