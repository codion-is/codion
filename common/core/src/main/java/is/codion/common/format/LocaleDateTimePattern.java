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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.format;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a locale sensitive numerical date format pattern.
 * Note that the time part is 24 hour based and is not locale sensitive.
 * <p>
 * Orders the year and month parts according to locale,
 * with two-digit month and day parts and two or four digit year.
 * {@snippet :
 * LocaleDateTimePattern pattern = LocaleDateTimePattern.builder()
 *     .delimiterDash()
 *     .yearFourDigits()
 *     .hoursMinutes()
 *     .build();
 *
 * Locale iceland = new Locale("is", "IS");
 * Locale us = new Locale("en", "US");
 *
 * pattern.datePattern(iceland);    // "dd-MM-yyyy"
 * pattern.datePattern(us);         // "MM-dd-yyyy"
 *
 * pattern.dateTimePattern(iceland);// "dd-MM-yyyy HH:mm"
 * pattern.dateTimePattern(us)     ;// "MM-dd-yyyy HH:mm"
 *}
 * @see #builder()
 */
public final class LocaleDateTimePattern implements Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private static final String FOUR_DIGIT_YEAR = "yyyy";
	private static final String TWO_DIGIT_YEAR = "yy";
	private static final String TWO_DIGIT_MONTH = "MM";
	private static final String TWO_DIGIT_DAY = "dd";
	private static final String HOURS_MINUTES = "HH:mm";
	private static final String HOURS_MINUTES_SECONDS = "HH:mm:ss";
	private static final String HOURS_MINUTES_SECONDS_MILLISECONDS = "HH:mm:ss.SSS";

	private final String delimiter;
	private final boolean fourDigitYear;
	private final @Nullable String timePattern;

	private LocaleDateTimePattern(Builder builder) {
		this.delimiter = requireNonNull(builder.delimiter, "delimiter must be specified");
		this.fourDigitYear = builder.fourDigitYear;
		this.timePattern = builder.timePattern;
	}

	/**
	 * @return the time part of this format, an empty Optional if none is available
	 */
	public Optional<String> timePattern() {
		return Optional.ofNullable(timePattern);
	}

	/**
	 * @return the date part of this format using the default Locale
	 */
	public String datePattern() {
		return datePattern(Locale.getDefault());
	}

	/**
	 * @return the date and time (if available) parts of this format using the default Locale
	 */
	public String dateTimePattern() {
		return dateTimePattern(Locale.getDefault());
	}

	/**
	 * @param locale the locale
	 * @return the date part of this format
	 */
	public String datePattern(Locale locale) {
		return datePattern(locale, delimiter, fourDigitYear);
	}

	/**
	 * @param locale the locale
	 * @return the date and time (if available) parts of this format
	 */
	public String dateTimePattern(Locale locale) {
		return dateTimePattern(locale, delimiter, fourDigitYear, timePattern);
	}

	/**
	 * @return a new {@link DateTimeFormatter} instance based on this pattern
	 */
	public DateTimeFormatter createFormatter() {
		return DateTimeFormatter.ofPattern(dateTimePattern());
	}

	/**
	 * @return a new Builder for a {@link LocaleDateTimePattern}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A Builder for {@link LocaleDateTimePattern}.
	 */
	public static final class Builder {

		private String delimiter = ".";
		private boolean fourDigitYear = true;
		private @Nullable String timePattern;

		private Builder() {}

		/**
		 * @param delimiter the date delimiter
		 * @return this Builder instance
		 */
		public Builder delimiter(String delimiter) {
			this.delimiter = requireNonNull(delimiter);
			return this;
		}

		/**
		 * @return this Builder instance
		 */
		public Builder delimiterDash() {
			return delimiter("-");
		}

		/**
		 * @return this Builder instance
		 */
		public Builder delimiterDot() {
			return delimiter(".");
		}

		/**
		 * @return this Builder instance
		 */
		public Builder delimiterSlash() {
			return delimiter("/");
		}

		/**
		 * @return this Builder instance
		 */
		public Builder yearTwoDigits() {
			this.fourDigitYear = false;
			return this;
		}

		/**
		 * @return this Builder instance
		 */
		public Builder yearFourDigits() {
			this.fourDigitYear = true;
			return this;
		}

		/**
		 * @return this Builder instance
		 */
		public Builder hoursMinutes() {
			this.timePattern = HOURS_MINUTES;
			return this;
		}

		/**
		 * @return this Builder instance
		 */
		public Builder hoursMinutesSeconds() {
			this.timePattern = HOURS_MINUTES_SECONDS;
			return this;
		}

		/**
		 * @return this Builder instance
		 */
		public Builder hoursMinutesSecondsMilliseconds() {
			this.timePattern = HOURS_MINUTES_SECONDS_MILLISECONDS;
			return this;
		}

		/**
		 * @return a new {@link LocaleDateTimePattern} based on this builder.
		 */
		public LocaleDateTimePattern build() {
			return new LocaleDateTimePattern(this);
		}
	}

	private static String datePattern(Locale locale, String delimiter, boolean fourDigitYear) {
		return dateTimePattern(locale, delimiter, fourDigitYear, null);
	}

	private static String dateTimePattern(Locale locale, String delimiter, boolean fourDigitYear,
																				@Nullable String timePattern) {
		requireNonNull(locale);
		String datePattern = DateTimeFormatterBuilder.
						getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale).toLowerCase(locale);
		List<String> pattern = new ArrayList<>(Arrays.asList(null, null, null));
		pattern.set(indexOf(datePattern, Element.YEAR), fourDigitYear ? FOUR_DIGIT_YEAR : TWO_DIGIT_YEAR);
		pattern.set(indexOf(datePattern, Element.MONTH), TWO_DIGIT_MONTH);
		pattern.set(indexOf(datePattern, Element.DAY), TWO_DIGIT_DAY);
		StringBuilder builder = new StringBuilder(String.join(delimiter, pattern));
		if (timePattern != null) {
			builder.append(" ").append(timePattern);
		}

		return builder.toString();
	}

	private static int indexOf(String pattern, Element element) {
		return Stream.of(pattern.indexOf('y'), pattern.indexOf('m'), pattern.indexOf('d'))
						.sorted()
						.collect(Collectors.toList())
						.indexOf(pattern.indexOf(element.character()));
	}

	private enum Element {
		YEAR {
			@Override
			char character() {
				return 'y';
			}
		},
		MONTH {
			@Override
			char character() {
				return 'm';
			}
		},
		DAY {
			@Override
			char character() {
				return 'd';
			}
		};

		abstract char character();
	}
}
