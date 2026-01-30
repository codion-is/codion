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
package is.codion.tools.generator.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Post-processing utilities for JavaPoet-generated source code.
 * JavaPoet doesn't provide fine-grained control over certain formatting aspects,
 * so we use post-processing to achieve the desired output structure.
 */
final class PostProcessing {

	private static final String PUBLIC_INTERFACE = "public interface ";
	private static final String STATIC_ENTITY_DEFINITION = "static EntityDefinition ";

	private PostProcessing() {}

	/**
	 * Removes blank lines between field declarations in generated interfaces.
	 * JavaPoet does not provide control over field spacing, so we use post-processing
	 * to achieve compact field declarations without empty lines between them.
	 * @param sourceString the source code to process
	 * @return the processed source code with blank lines removed between Column/ForeignKey fields
	 */
	static String removeInterfaceLineBreaks(String sourceString) {
		String[] lines = sourceString.split("\n");
		for (int i = 1; i < lines.length - 1; i++) {
			String line = lines[i];
			if (line != null && line.trim().isEmpty() && betweenColumnsOrForeignKeys(lines, i)) {
				lines[i] = null;
			}
		}

		return Arrays.stream(lines)
						.filter(Objects::nonNull)
						.collect(joining("\n"));
	}

	/**
	 * Restructures combined output so that each interface appears immediately before
	 * its corresponding EntityDefinition method. JavaPoet doesn't support interleaving
	 * methods and nested types in custom order, so we use post-processing.
	 * @param sourceString the source code to process
	 * @param entityNames list of (interfaceName, methodName) pairs in desired order
	 * @return the restructured source code with interface-method pairs
	 */
	static String interleaveInterfacesAndMethods(String sourceString, List<EntityNames> entityNames) {
		String[] lines = sourceString.split("\n");
		int preambleEnd = findPreambleEnd(lines);
		if (preambleEnd == -1) {
			return sourceString;
		}

		Map<String, List<String>> interfaces = new LinkedHashMap<>();
		Map<String, List<String>> methods = new LinkedHashMap<>();

		int i = preambleEnd + 1;
		while (i < lines.length) {
			String trimmed = lines[i].trim();
			if (trimmed.startsWith(PUBLIC_INTERFACE)) {
				String interfaceName = interfaceName(trimmed);
				if (interfaceName != null) {
					List<String> block = extractBlock(lines, i);
					interfaces.put(interfaceName, block);
					i += block.size();
					if (i < lines.length && lines[i].trim().isEmpty()) {
						i++;
					}
					continue;
				}
			}
			else if (trimmed.startsWith(STATIC_ENTITY_DEFINITION)) {
				String methodName = methodName(trimmed);
				if (methodName != null) {
					List<String> block = extractBlock(lines, i);
					methods.put(methodName, block);
					i += block.size();
					if (i < lines.length && lines[i].trim().isEmpty()) {
						i++;
					}
					continue;
				}
			}
			i++;
		}

		StringBuilder result = new StringBuilder();
		for (int j = 0; j <= preambleEnd; j++) {
			result.append(lines[j]).append("\n");
		}

		for (EntityNames names : entityNames) {
			List<String> interfaceBlock = interfaces.get(names.interfaceName());
			List<String> methodBlock = methods.get(names.methodName());
			if (interfaceBlock != null) {
				result.append("\n");
				interfaceBlock.forEach(blockLine -> result.append(blockLine).append("\n"));
			}
			if (methodBlock != null) {
				result.append("\n");
				methodBlock.forEach(blockLine -> result.append(blockLine).append("\n"));
			}
		}

		return result.append("}").toString();
	}

	/**
	 * Holds the interface name and method name pair for an entity.
	 */
	static final class EntityNames {

		private final String interfaceName;
		private final String methodName;

		/**
		 * @param interfaceName the interface name (e.g., "Address")
		 * @param methodName the method name (e.g., "address")
		 */
		EntityNames(String interfaceName, String methodName) {
			this.interfaceName = interfaceName;
			this.methodName = methodName;
		}

		/**
		 * @return the interface name
		 */
		String interfaceName() {
			return interfaceName;
		}

		/**
		 * @return the method name
		 */
		String methodName() {
			return methodName;
		}
	}

	private static int findPreambleEnd(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			String trimmed = lines[i].trim();
			if ("}".equals(trimmed) && i > 0) {
				String prevLine = lines[i - 1].trim();
				if (prevLine.contains("add(") || prevLine.endsWith(");")) {
					return i;
				}
			}
		}

		return -1;
	}

	private static String interfaceName(String line) {
		int start = line.indexOf("interface ") + "interface ".length();
		int end = line.indexOf(" ", start);
		if (end == -1) {
			end = line.indexOf("{", start);
		}
		if (start > 0 && end > start) {
			return line.substring(start, end).trim();
		}

		return null;
	}

	private static String methodName(String line) {
		int start = line.indexOf("EntityDefinition ") + "EntityDefinition ".length();
		int end = line.indexOf("(", start);
		if (start > 0 && end > start) {
			return line.substring(start, end).trim();
		}

		return null;
	}

	private static List<String> extractBlock(String[] lines, int startIndex) {
		List<String> block = new ArrayList<>();
		int braceDepth = 0;
		boolean foundOpenBrace = false;
		for (int i = startIndex; i < lines.length; i++) {
			String line = lines[i];
			block.add(line);
			for (char c : line.toCharArray()) {
				if (c == '{') {
					braceDepth++;
					foundOpenBrace = true;
				}
				else if (c == '}') {
					braceDepth--;
				}
			}
			if (foundOpenBrace && braceDepth == 0) {
				break;
			}
		}

		return block;
	}

	private static boolean betweenColumnsOrForeignKeys(String[] lines, int lineIndex) {
		return betweenLinesStartingWith(lines, lineIndex, "Column")
						|| betweenLinesStartingWith(lines, lineIndex, "ForeignKey");
	}

	private static boolean betweenLinesStartingWith(String[] lines, int lineIndex, String prefix) {
		String previousLine = lines[lineIndex - 1];
		String nextLine = lines[lineIndex + 1];

		return lineStartsWith(previousLine, prefix) && lineStartsWith(nextLine, prefix);
	}

	private static boolean lineStartsWith(String line, String prefix) {
		return line != null && line.trim().startsWith(prefix);
	}
}
