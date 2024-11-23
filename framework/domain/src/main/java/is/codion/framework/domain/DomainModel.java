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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.DefaultEntities;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serial;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link Domain} implementation. Extend to define a domain model.
 * @see #add(EntityDefinition...)
 * @see #add(ReportType, Report)
 * @see #add(ProcedureType, DatabaseProcedure)
 * @see #add(FunctionType, DatabaseFunction)
 */
public abstract class DomainModel implements Domain {

	private final DomainType domainType;
	private final DomainEntities entities;
	private final DomainReports reports = new DomainReports();
	private final DomainProcedures procedures = new DomainProcedures();
	private final DomainFunctions functions = new DomainFunctions();

	/**
	 * Instantiates a new DomainModel identified by the given {@link DomainType}.
	 * @param domainType the Domain model type to associate with this domain model
	 */
	protected DomainModel(DomainType domainType) {
		this.domainType = requireNonNull(domainType);
		this.entities = new DomainEntities(domainType);
	}

	@Override
	public final DomainType type() {
		return domainType;
	}

	@Override
	public final Entities entities() {
		return entities;
	}

	@Override
	public final Map<ReportType<?, ?, ?>, Report<?, ?, ?>> reports() {
		return unmodifiableMap(reports.reports);
	}

	@Override
	public final Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures() {
		return unmodifiableMap(procedures.procedures);
	}

	@Override
	public final Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions() {
		return unmodifiableMap(functions.functions);
	}

	@Override
	public final <T, R, P> Report<T, R, P> report(ReportType<T, R, P> reportType) {
		Report<T, R, P> report = reports.report(reportType);
		if (report == null) {
			throw new IllegalArgumentException("Undefined report: " + reportType);
		}

		return report;
	}

	@Override
	public final <C, T> DatabaseProcedure<C, T> procedure(ProcedureType<C, T> procedureType) {
		return procedures.procedure(procedureType);
	}

	@Override
	public final <C, T, R> DatabaseFunction<C, T, R> function(FunctionType<C, T, R> functionType) {
		return functions.function(functionType);
	}

	/**
	 * Adds a new {@link EntityDefinition} to this domain model.
	 * @param definitions the definitions to add
	 * @throws IllegalArgumentException in case a entityType has already been used to define an entity
	 * @throws IllegalArgumentException in case no attribute definitions are specified for an entity
	 * @throws IllegalArgumentException in case an entity definition is not associated with this domain
	 */
	protected final void add(EntityDefinition... definitions) {
		Arrays.stream(requireNonNull(definitions))
						.peek(this::validate)
						.forEach(entities::addEntityDefinition);
	}

	/**
	 * Adds a report to this domain model.
	 * @param reportType the report to add
	 * @param report the actual report to associate with the report type
	 * @param <T> the report type
	 * @param <R> the report result type
	 * @param <P> the report parameters type
	 * @throws RuntimeException in case loading the report failed
	 * @throws IllegalArgumentException in case the report has already been added
	 */
	protected final <T, R, P> void add(ReportType<T, R, P> reportType, Report<T, R, P> report) {
		reports.addReport(reportType, report);
	}

	/**
	 * Adds the given procedure to this domain
	 * @param procedureType the procedure type to identify the procedure
	 * @param procedure the procedure to add
	 * @param <C> the connection type
	 * @param <T> the argument type
	 * @throws IllegalArgumentException in case a procedure has already been associated with the given type
	 */
	protected final <C, T> void add(ProcedureType<C, T> procedureType, DatabaseProcedure<C, T> procedure) {
		procedures.addProcedure(procedureType, procedure);
	}

	/**
	 * Adds the given function to this domain
	 * @param functionType the function type to identify the function
	 * @param function the function to add
	 * @param <C> the connection type
	 * @param <T> the argument type
	 * @param <R> the result type
	 * @throws IllegalArgumentException in case a function has already been associated with the given type
	 */
	protected final <C, T, R> void add(FunctionType<C, T, R> functionType, DatabaseFunction<C, T, R> function) {
		functions.addFunction(functionType, function);
	}

	/**
	 * Specifies whether foreign keys are validated by asserting that the referenced entity has been defined.
	 * Disable this validation in cases where entities have circular references.
	 * @param validateForeignKeys true to enable foreign key validation, false to disable
	 */
	protected final void validateForeignKeys(boolean validateForeignKeys) {
		entities.validateForeignKeysInternal(validateForeignKeys);
	}

	/**
	 * Adds all entities, procedures, functions and reports from the given domain model.
	 * @param domain the domain model to copy from
	 * @see #addEntities(Domain)
	 * @see #addProcedures(Domain)
	 * @see #addFunctions(Domain)
	 * @see #addReports(Domain)
	 */
	protected final void add(Domain domain) {
		addEntities(domain);
		addProcedures(domain);
		addFunctions(domain);
		addReports(domain);
	}

	/**
	 * Adds all the entities from the given domain to this domain.
	 * Note that the entity type names must be unique.
	 * @param domain the domain model which entities to add
	 * @throws IllegalArgumentException in case a non-unique entity type name is encountered
	 * @see EntityType#name()
	 */
	protected final void addEntities(Domain domain) {
		requireNonNull(domain).entities().definitions().forEach(definition -> {
			if (!entities.contains(definition.entityType())) {
				entities.addEntityDefinition(definition);
			}
		});
	}

	/**
	 * Adds all the procedures from the given domain to this domain.
	 * @param domain the domain model which procedures to add
	 */
	protected final void addProcedures(Domain domain) {
		requireNonNull(domain).procedures().forEach((procedureType, procedure) -> {
			if (!procedures.procedures.containsKey(procedureType)) {
				procedures.procedures.put(procedureType, procedure);
			}
		});
	}

	/**
	 * Adds all the functions from the given domain to this domain.
	 * @param domain the domain model which functions to add
	 */
	protected final void addFunctions(Domain domain) {
		requireNonNull(domain).functions().forEach((functionType, function) -> {
			if (!functions.functions.containsKey(functionType)) {
				functions.functions.put(functionType, function);
			}
		});
	}

	/**
	 * Adds all the reports from the given domain to this domain.
	 * @param domain the domain model which reports to add
	 */
	protected final void addReports(Domain domain) {
		requireNonNull(domain).reports().forEach((reportType, report) -> {
			if (!reports.reports.containsKey(reportType)) {
				reports.reports.put(reportType, report);
			}
		});
	}

	private void validate(EntityDefinition definition) {
		if (!domainType.contains(requireNonNull(definition).entityType())) {
			throw new IllegalArgumentException("Entity type '" +
							definition.entityType() + "' is not part of domain: " + domainType);
		}
	}

	private static final class DomainEntities extends DefaultEntities {

		@Serial
		private static final long serialVersionUID = 1;

		private DomainEntities(DomainType domainType) {
			super(domainType);
		}

		private void addEntityDefinition(EntityDefinition definition) {
			super.add(definition);
		}

		private void validateForeignKeysInternal(boolean validateForeignKeys) {
			super.validateForeignKeys(validateForeignKeys);
		}
	}

	private static final class DomainProcedures {

		private final Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures = new HashMap<>();

		private void addProcedure(ProcedureType<?, ?> procedureType, DatabaseProcedure<?, ?> procedure) {
			if (procedures.containsKey(requireNonNull(procedureType))) {
				throw new IllegalArgumentException("Procedure already defined: " + procedureType);
			}

			procedures.put(procedureType, requireNonNull(procedure));
		}

		private <C, T> DatabaseProcedure<C, T> procedure(ProcedureType<C, T> procedureType) {
			DatabaseProcedure<C, T> operation = (DatabaseProcedure<C, T>) procedures.get(requireNonNull(procedureType));
			if (operation == null) {
				throw new IllegalArgumentException("Procedure not found: " + procedureType);
			}

			return operation;
		}
	}

	private static final class DomainFunctions {

		private final Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions = new HashMap<>();

		private void addFunction(FunctionType<?, ?, ?> functionType, DatabaseFunction<?, ?, ?> function) {
			if (functions.containsKey(requireNonNull(functionType))) {
				throw new IllegalArgumentException("Function already defined: " + functionType);
			}

			functions.put(functionType, requireNonNull(function));
		}

		private <C, T, R> DatabaseFunction<C, T, R> function(FunctionType<C, T, R> functionType) {
			DatabaseFunction<C, T, R> operation = (DatabaseFunction<C, T, R>) functions.get(requireNonNull(functionType));
			if (operation == null) {
				throw new IllegalArgumentException("Function not found: " + functionType);
			}

			return operation;
		}
	}

	private static final class DomainReports {

		private final Map<ReportType<?, ?, ?>, Report<?, ?, ?>> reports = new HashMap<>();

		private <T, R, P> void addReport(ReportType<T, R, P> reportType, Report<T, R, P> report) {
			if (reports.containsKey(requireNonNull(reportType))) {
				throw new IllegalArgumentException("Report has already been defined: " + reportType);
			}
			try {
				requireNonNull(report).load();
				reports.put(reportType, report);
			}
			catch (ReportException e) {
				throw new RuntimeException(e);
			}
		}

		private <T, R, P> Report<T, R, P> report(ReportType<T, R, P> reportType) {
			return (Report<T, R, P>) reports.get(requireNonNull(reportType));
		}
	}
}
