import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.*;

public final class I18n {

	private static final String DEFAULT = "default";

	private final Pattern localeProperties = Pattern.compile(".*_[a-z]{2}_[A-Z]{2}.properties");
	private final Map<String, Set<Resource>> moduleResources = new LinkedHashMap<>();

	public I18n(Map<String, List<String>> modulePropertiesFiles) {
		modulePropertiesFiles.forEach((module, resourceFiles) -> {
			Set<Resource> resources = resourceFiles.stream()
							.map(this::cleanFileString)
							.map(file -> toResource(file, resourceFiles))
							.collect(toCollection(LinkedHashSet::new));
			moduleResources.put(module, resources);
		});
	}

	public String toAsciidoc() {
		StringBuilder builder = new StringBuilder();
		builder.append("= Internationalization (i18n)\n\n");
		builder.append("Overview of the available i18n properties files and their keys and values.\n\n");
		moduleResources.forEach((module, resources) -> {
			builder.append("== ").append(module).append("\n\n");
			resources.forEach(resource -> {
				builder.append("=== ").append(resource.owner).append(".java\n\n");
				builder.append("[source]\n");
				builder.append("----\n");
				resource.localeFiles.values().forEach(localeFile -> builder.append(removePrefix(localeFile)).append("\n"));
				builder.append("----\n");
				builder.append("[cols=\"");
				builder.append(IntStream.rangeClosed(0, resource.locales.size())
								.mapToObj(i -> "1")
								.collect(joining(",")));
				builder.append("\"]\n");
				builder.append("|===\n");
				builder.append("|key");
				resource.locales.forEach(locale -> builder.append("|" + locale));
				builder.append("\n\n");
				Properties defaultProperties = resource.localeProperties.get(I18n.DEFAULT);
				List<String> propertyNames = Collections.list(defaultProperties.propertyNames())
								.stream()
								.map(Objects::toString)
								.sorted(Comparator.naturalOrder())
								.collect(toList());
				for (String propertyName : propertyNames) {
					builder.append("|" + propertyName);
					resource.locales.forEach(locale -> {
						Properties localeProperties = resource.localeProperties.get(locale);
						String value = (String) localeProperties.get(propertyName);
						builder.append("|").append(value.replaceAll("\\|", "\\\\|"));
					});
					builder.append("\n");
				}
				builder.append("|===\n\n");
			});
		});

		return builder.toString().trim();
	}

	private String cleanFileString(String file) {
		file = removePrefix(file);
		if (localeProperties.matcher(file).matches()) {
			return file.substring(0, file.length() - 17);
		}

		return file.substring(0, file.length() - 11);
	}

	private static Resource toResource(String resourceOwner, List<String> propertiesFiles) {
		return new Resource(resourceOwner, propertiesFiles.stream()
						.filter(file -> isPropertiesFileFor(removePrefix(file), resourceOwner))
						.collect(toMap(I18n::parseLocale, Function.identity(), (s, s2) -> s, LinkedHashMap::new)));
	}

	private static boolean isPropertiesFileFor(String file, String resourceOwner) {
		return file.endsWith(resourceOwner + ".properties") || file.matches(Pattern.quote(resourceOwner) + "_[a-z]{2}_[A-Z]{2}.properties");
	}

	private static String parseLocale(String file) {
		int localeUnderscore = file.indexOf("_", file.lastIndexOf("/"));
		if (localeUnderscore != -1) {
			return file.substring(localeUnderscore + 1, file.lastIndexOf("."));
		}

		return DEFAULT;
	}

	private static String removePrefix(String file) {
		return file.substring(file.indexOf("src/main/resources/") + 19);
	}

	private static final class Resource {

		private final String owner;
		private final List<String> locales;
		private final Map<String, String> localeFiles;
		private final Map<String, Properties> localeProperties = new LinkedHashMap<>();

		private Resource(String owner, Map<String, String> localeFiles) {
			this.owner = owner;
			this.localeFiles = localeFiles;
			this.locales = unmodifiableList(new ArrayList<>(localeFiles.keySet()));
			this.locales.forEach(locale -> localeProperties.put(locale, createProperties(locale)));
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof Resource)) {
				return false;
			}
			Resource resource = (Resource) object;

			return Objects.equals(owner, resource.owner);
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner);
		}

		private Properties createProperties(String locale) {
			try (InputStream stream = Files.newInputStream(Paths.get(localeFiles.get(locale)))) {
				Properties properties = new Properties();
				properties.load(stream);

				return properties;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}