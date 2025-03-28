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

final class DefaultLocaleDateTimePattern implements LocaleDateTimePattern, Serializable {

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

	private DefaultLocaleDateTimePattern(DefaultBuilder builder) {
		this.delimiter = requireNonNull(builder.delimiter, "delimiter must be specified");
		this.fourDigitYear = builder.fourDigitYear;
		this.timePattern = builder.timePattern;
	}

	@Override
	public Optional<String> timePattern() {
		return Optional.ofNullable(timePattern);
	}

	@Override
	public String datePattern() {
		return datePattern(Locale.getDefault());
	}

	@Override
	public String dateTimePattern() {
		return dateTimePattern(Locale.getDefault());
	}

	@Override
	public String datePattern(Locale locale) {
		return datePattern(locale, delimiter, fourDigitYear);
	}

	@Override
	public String dateTimePattern(Locale locale) {
		return dateTimePattern(locale, delimiter, fourDigitYear, timePattern);
	}

	@Override
	public DateTimeFormatter createFormatter() {
		return DateTimeFormatter.ofPattern(dateTimePattern());
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

	static final class DefaultBuilder implements Builder {

		private String delimiter = ".";
		private boolean fourDigitYear = true;
		private @Nullable String timePattern;

		@Override
		public Builder delimiter(String delimiter) {
			this.delimiter = requireNonNull(delimiter);
			return this;
		}

		@Override
		public Builder delimiterDash() {
			return delimiter("-");
		}

		@Override
		public Builder delimiterDot() {
			return delimiter(".");
		}

		@Override
		public Builder delimiterSlash() {
			return delimiter("/");
		}

		@Override
		public Builder yearTwoDigits() {
			this.fourDigitYear = false;
			return this;
		}

		@Override
		public Builder yearFourDigits() {
			this.fourDigitYear = true;
			return this;
		}

		@Override
		public Builder hoursMinutes() {
			this.timePattern = HOURS_MINUTES;
			return this;
		}

		@Override
		public Builder hoursMinutesSeconds() {
			this.timePattern = HOURS_MINUTES_SECONDS;
			return this;
		}

		@Override
		public Builder hoursMinutesSecondsMilliseconds() {
			this.timePattern = HOURS_MINUTES_SECONDS_MILLISECONDS;
			return this;
		}

		@Override
		public LocaleDateTimePattern build() {
			return new DefaultLocaleDateTimePattern(this);
		}
	}
}
