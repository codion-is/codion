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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * This is an internal class not for general usage.
 */
public interface MethodTracer {

	MethodTracer NO_OP = new NoOpMethodTracer();

	void enter(String method);

	void enter(String method, @Nullable Object argument);

	void enter(String method, @Nullable Object... arguments);

	@Nullable MethodTrace exit(String method);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage);

	boolean isEnabled();

	void setEnabled(boolean enabled);

	List<MethodTrace> entries();

	static MethodTracer methodTracer(int maxSize) {
		return methodTracer(maxSize, new ArgumentFormatter() {});
	}

	static MethodTracer methodTracer(int maxSize, ArgumentFormatter formatter) {
		return new DefaultMethodTracer(maxSize, formatter);
	}

	interface Traceable {

		MethodTracer tracer();

		void tracer(MethodTracer tracer);
	}

	interface ArgumentFormatter {

		String BRACKET_OPEN = "[";
		String BRACKET_CLOSE = "]";

		/**
		 * @param methodName the method name
		 * @param argument the argument to format
		 * @return the formatted argument
		 */
		default String format(String methodName, @Nullable Object argument) {
			if ("prepareStatement".equals(methodName)) {
				return (String) argument;
			}

			return format(argument);
		}

		/**
		 * @param argument the argument to format
		 * @return the formatted argument
		 */
		default String format(@Nullable Object argument) {
			if (argument == null) {
				return "null";
			}
			if (argument instanceof String) {
				return "'" + argument + "'";
			}
			if (argument instanceof Entity) {
				return entityToString((Entity) argument);
			}
			else if (argument instanceof Entity.Key) {
				return entityKeyToString((Entity.Key) argument);
			}
			if (argument instanceof List) {
				return format((List<?>) argument);
			}
			if (argument instanceof Collection) {
				return format((Collection<?>) argument);
			}
			if (argument instanceof byte[]) {
				return "byte[" + ((byte[]) argument).length + "]";
			}
			if (argument.getClass().isArray()) {
				return format((Object[]) argument);
			}

			return argument.toString();
		}

		/**
		 * @param argument the argument to format
		 * @return the formatted argument
		 */
		default String format(List<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}
			if (arguments.size() == 1) {
				return format(arguments.get(0));
			}

			return arguments.stream()
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		/**
		 * @param argument the argument to format
		 * @return the formatted argument
		 */
		default String format(Collection<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}

			return arguments.stream()
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		/**
		 * @param argument the argument to format
		 * @return the formatted argument
		 */
		default String format(Object[] arguments) {
			if (arguments.length == 0) {
				return "";
			}
			if (arguments.length == 1) {
				return format(arguments[0]);
			}

			return stream(arguments)
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private static String entityToString(Entity entity) {
			StringBuilder builder = new StringBuilder(entity.type().name()).append(" {");
			for (ColumnDefinition<?> columnDefinition : entity.definition().columns().definitions()) {
				boolean modified = entity.modified(columnDefinition.attribute());
				if (columnDefinition.primaryKey() || modified) {
					StringBuilder valueString = new StringBuilder();
					if (modified) {
						valueString.append(entity.original(columnDefinition.attribute())).append("->");
					}
					valueString.append(entity.string(columnDefinition.attribute()));
					builder.append(columnDefinition.attribute()).append(":").append(valueString).append(",");
				}
			}
			builder.deleteCharAt(builder.length() - 1);

			return builder.append("}").toString();
		}

		private static String entityKeyToString(Entity.Key key) {
			return key.type() + " {" + key + "}";
		}
	}
}
