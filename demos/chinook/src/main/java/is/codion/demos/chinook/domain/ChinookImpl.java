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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.domain;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.demos.chinook.domain.api.Chinook.Track.RaisePriceParameters;
import is.codion.demos.chinook.migration.MigrationManager;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.ColumnTemplate;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;

public final class ChinookImpl extends DomainModel {

	// tag::columnTemplates[]
	private static final ColumnTemplate<String> REQUIRED_SEARCHABLE =
					column -> column.define()
									.column()
									.nullable(false)
									.searchable(true);
	private static final ColumnTemplate<LocalDateTime> INSERT_TIME =
					column -> column.define()
									.column()
									.readOnly(true)
									.captionResource(Chinook.class.getName(), "insert_time");
	private static final ColumnTemplate<String> INSERT_USER =
					column -> column.define()
									.column()
									.readOnly(true)
									.captionResource(Chinook.class.getName(), "insert_user");
	// end::columnTemplates[]

	public ChinookImpl() {
		super(DOMAIN);
		add(artist(), album(), employee(), customer(), genre(), preferences(), mediaType(),
						track(), invoice(), invoiceLine(), playlist(), playlistTrack());
		add(Customer.REPORT, classPathReport(ChinookImpl.class, "customer_report.jasper"));
		add(Track.RAISE_PRICE, new RaisePrice());
		add(Invoice.UPDATE_TOTALS, new UpdateTotals());
		add(Playlist.RANDOM_PLAYLIST, new CreateRandomPlaylist(entities()));
	}

	@Override
	public void configure(Database database) throws DatabaseException {
		MigrationManager.migrate(database);
	}

	EntityDefinition artist() {
		return Artist.TYPE.define(
										Artist.ID.define()
														.primaryKey(),
										Artist.NAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120),
										Artist.NUMBER_OF_ALBUMS.define()
														.subquery("""
																		SELECT COUNT(*)
																		FROM chinook.album
																		WHERE album.artist_id = artist.id"""),
										Artist.NUMBER_OF_TRACKS.define()
														.subquery("""
																		SELECT count(*)
																		FROM chinook.track
																		JOIN chinook.album ON track.album_id = album.id
																		WHERE album.artist_id = artist.id"""))
						.keyGenerator(identity())
						.orderBy(ascending(Artist.NAME))
						.formatter(Artist.NAME)
						.build();
	}

	EntityDefinition album() {
		return Album.TYPE.define(
										Album.ID.define()
														.primaryKey(),
										Album.ARTIST_ID.define()
														.column()
														.nullable(false),
										Album.ARTIST_FK.define()
														.foreignKey()
														.attributes(Artist.NAME),
										// tag::columnTemplateUsage1[]
										Album.TITLE.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(160),
										// end::columnTemplateUsage1[]
										Album.COVER.define()
														.column()
														.format(new CoverFormatter()),
										Album.NUMBER_OF_TRACKS.define()
														.subquery("""
																		SELECT COUNT(*) FROM chinook.track
																		WHERE track.album_id = album.id"""),
										Album.TAGS.define()
														.column()
														.converter(Array.class, new TagsConverter(), ResultSet::getArray),
										Album.RATING.define()
														.subquery("""
																		SELECT AVG(rating) FROM chinook.track
																		WHERE track.album_id = album.id"""),
										// tag::columnTemplateUsage2[]
										Album.INSERT_TIME.define()
														.column(INSERT_TIME),
										Album.INSERT_USER.define()
														.column(INSERT_USER))
						// end::columnTemplateUsage2[]
						.keyGenerator(identity())
						.orderBy(ascending(Album.ARTIST_ID, Album.TITLE))
						.formatter(Album.TITLE)
						.build();
	}

	EntityDefinition employee() {
		return Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey(),
										Employee.LASTNAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Employee.FIRSTNAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Employee.TITLE.define()
														.column()
														.maximumLength(30),
										Employee.REPORTSTO.define()
														.column(),
										Employee.REPORTSTO_FK.define()
														.foreignKey()
														.attributes(Employee.FIRSTNAME, Employee.LASTNAME),
										Employee.BIRTHDATE.define()
														.column(),
										Employee.HIREDATE.define()
														.column()
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Employee.ADDRESS.define()
														.column()
														.maximumLength(70),
										Employee.CITY.define()
														.column()
														.maximumLength(40),
										Employee.STATE.define()
														.column()
														.maximumLength(40),
										Employee.COUNTRY.define()
														.column()
														.maximumLength(40),
										Employee.POSTALCODE.define()
														.column()
														.maximumLength(10),
										Employee.PHONE.define()
														.column()
														.maximumLength(24),
										Employee.FAX.define()
														.column()
														.maximumLength(24),
										Employee.EMAIL.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(60),
										Employee.INSERT_TIME.define()
														.column(INSERT_TIME),
										Employee.INSERT_USER.define()
														.column(INSERT_USER))
						.keyGenerator(identity())
						.validator(new EmailValidator(Employee.EMAIL))
						.orderBy(ascending(Employee.LASTNAME, Employee.FIRSTNAME))
						.formatter(EntityFormatter.builder()
										.value(Employee.LASTNAME)
										.text(", ")
										.value(Employee.FIRSTNAME)
										.build())
						.build();
	}

	EntityDefinition customer() {
		return Customer.TYPE.define(
										Customer.ID.define()
														.primaryKey(),
										Customer.LASTNAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Customer.FIRSTNAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(40),
										Customer.COMPANY.define()
														.column()
														.maximumLength(80),
										Customer.ADDRESS.define()
														.column()
														.maximumLength(70),
										Customer.CITY.define()
														.column()
														.maximumLength(40),
										Customer.STATE.define()
														.column()
														.maximumLength(40),
										Customer.COUNTRY.define()
														.column()
														.maximumLength(40),
										Customer.POSTALCODE.define()
														.column()
														.maximumLength(10),
										Customer.PHONE.define()
														.column()
														.maximumLength(24),
										Customer.FAX.define()
														.column()
														.maximumLength(24),
										Customer.EMAIL.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(60),
										Customer.SUPPORTREP_ID.define()
														.column(),
										Customer.SUPPORTREP_FK.define()
														.foreignKey()
														.attributes(Employee.FIRSTNAME, Employee.LASTNAME),
										Customer.INSERT_TIME.define()
														.column(INSERT_TIME),
										Customer.INSERT_USER.define()
														.column(INSERT_USER))
						.keyGenerator(identity())
						.validator(new EmailValidator(Customer.EMAIL))
						.orderBy(ascending(Customer.LASTNAME, Customer.FIRSTNAME))
						.formatter(new CustomerFormatter())
						.build();
	}

	EntityDefinition preferences() {
		return Preferences.TYPE.define(
										Preferences.CUSTOMER_ID.define()
														.primaryKey(),
										Preferences.CUSTOMER_FK.define()
														.foreignKey(),
										Preferences.PREFERRED_GENRE_ID.define()
														.column(),
										Preferences.PREFERRED_GENRE_FK.define()
														.foreignKey()
														.attributes(Genre.NAME),
										Preferences.NEWSLETTER_SUBSCRIBED.define()
														.column()
														.nullable(false)
														.defaultValue(false))
						.caption("Preferences")
						.build();
	}

	EntityDefinition genre() {
		return Genre.TYPE.define(
										Genre.ID.define()
														.primaryKey(),
										Genre.NAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120))
						.keyGenerator(identity())
						.orderBy(ascending(Genre.NAME))
						.formatter(Genre.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition mediaType() {
		return MediaType.TYPE.define(
										MediaType.ID.define()
														.primaryKey(),
										MediaType.NAME.define()
														.column()
														.nullable(false)
														.maximumLength(120))
						.keyGenerator(identity())
						.formatter(MediaType.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition track() {
		return Track.TYPE.define(
										Track.ID.define()
														.primaryKey()
														.expression("track.id"),
										Track.ALBUM_ID.define()
														.column()
														.nullable(false),
										// tag::referenceDepth2[]
										Track.ALBUM_FK.define()
														.foreignKey()
														.referenceDepth(2)
														.attributes(Album.ARTIST_FK, Album.TITLE),
										// end::referenceDepth2[]
										Track.ARTIST_NAME.define()
														.column()
														.expression("artist.name")
														.readOnly(true),
										Track.NAME.define()
														.column(REQUIRED_SEARCHABLE)
														.expression("track.name")
														.maximumLength(200),
										Track.GENRE_ID.define()
														.column(),
										Track.GENRE_FK.define()
														.foreignKey(),
										Track.COMPOSER.define()
														.column()
														.maximumLength(220),
										Track.MEDIATYPE_ID.define()
														.column()
														.nullable(false),
										Track.MEDIATYPE_FK.define()
														.foreignKey(),
										Track.MILLISECONDS.define()
														.column()
														.nullable(false)
														.format(NumberFormat.getIntegerInstance()),
										Track.BYTES.define()
														.column()
														.format(NumberFormat.getIntegerInstance()),
										Track.RATING.define()
														.column()
														.nullable(false)
														.defaultValue(5)
														.range(1, 10),
										Track.UNITPRICE.define()
														.column()
														.nullable(false)
														.minimum(0)
														.fractionDigits(2),
										Track.PLAY_COUNT.define()
														.column()
														.nullable(false)
														.defaultValue(0),
										Track.RANDOM.define()
														.column()
														.readOnly(true)
														.selected(false),
										Track.INSERT_TIME.define()
														.column(INSERT_TIME)
														.expression("track.insert_time"),
										Track.INSERT_USER.define()
														.column(INSERT_USER)
														.expression("track.insert_user"))
						.keyGenerator(identity())
						.selectQuery(EntitySelectQuery.builder()
										// Override the default FROM clause, joining
										// the ALBUM and ARTIST tables in order to
										// have the ARTIST.NAME column available
										.from("chinook.track " +
														"JOIN chinook.album ON track.album_id = album.id " +
														"JOIN chinook.artist ON album.artist_id = artist.id")
										.build())
						.orderBy(ascending(Track.NAME))
						// Implement a custom condition for specifying
						// tracks that are not in a given playlist
						.condition(Track.NOT_IN_PLAYLIST, (columns, values) -> """
										track.id NOT IN (
												SELECT track_id
												FROM chinook.playlisttrack
												WHERE playlist_id = ?
										)""")
						.formatter(Track.NAME)
						.build();
	}

	EntityDefinition invoice() {
		return Invoice.TYPE.define(
										Invoice.ID.define()
														.primaryKey(),
										Invoice.CUSTOMER_ID.define()
														.column()
														.nullable(false),
										Invoice.CUSTOMER_FK.define()
														.foreignKey()
														.attributes(Customer.FIRSTNAME, Customer.LASTNAME, Customer.EMAIL),
										Invoice.DATE.define()
														.column()
														.nullable(false)
														.defaultValue(Invoice.DATE_DEFAULT_VALUE)
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Invoice.BILLINGADDRESS.define()
														.column()
														.maximumLength(70),
										Invoice.BILLINGCITY.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGSTATE.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGCOUNTRY.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGPOSTALCODE.define()
														.column()
														.maximumLength(10),
										Invoice.TOTAL.define()
														.column()
														.fractionDigits(2)
														.nullable(false)
														.withDefault(true),
										Invoice.CALCULATED_TOTAL.define()
														.subquery("""
																		SELECT SUM(unitprice * quantity)
																		FROM chinook.invoiceline
																		WHERE invoice_id = invoice.id""")
														.fractionDigits(2),
										Invoice.INSERT_TIME.define()
														.column(INSERT_TIME),
										Invoice.INSERT_USER.define()
														.column(INSERT_USER))
						// tag::identity[]
						.keyGenerator(identity())
						// end::identity[]
						.orderBy(OrderBy.builder()
										.ascending(Invoice.CUSTOMER_ID)
										.descending(Invoice.DATE)
										.build())
						.formatter(Invoice.ID)
						.build();
	}

	EntityDefinition invoiceLine() {
		return InvoiceLine.TYPE.define(
										InvoiceLine.ID.define()
														.primaryKey(),
										InvoiceLine.INVOICE_ID.define()
														.column()
														.nullable(false),
										// tag::referenceDepth0[]
										InvoiceLine.INVOICE_FK.define()
														.foreignKey()
														.referenceDepth(0)
														.hidden(true),
										// end::referenceDepth0[]
										InvoiceLine.TRACK_ID.define()
														.column()
														.nullable(false),
										InvoiceLine.TRACK_FK.define()
														.foreignKey()
														.attributes(Track.NAME, Track.UNITPRICE),
										InvoiceLine.UNITPRICE.define()
														.column()
														.nullable(false),
										InvoiceLine.QUANTITY.define()
														.column()
														.nullable(false)
														.defaultValue(1),
										InvoiceLine.TOTAL.define()
														.derived(InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
														.value(new InvoiceLineTotal()),
										InvoiceLine.INSERT_TIME.define()
														.column(INSERT_TIME),
										InvoiceLine.INSERT_USER.define()
														.column(INSERT_USER))
						.keyGenerator(identity())
						.build();
	}

	EntityDefinition playlist() {
		return Playlist.TYPE.define(
										Playlist.ID.define()
														.primaryKey(),
										Playlist.NAME.define()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120))
						.keyGenerator(identity())
						.orderBy(ascending(Playlist.NAME))
						.formatter(Playlist.NAME)
						.build();
	}

	EntityDefinition playlistTrack() {
		return PlaylistTrack.TYPE.define(
										PlaylistTrack.ID.define()
														.primaryKey(),
										PlaylistTrack.PLAYLIST_ID.define()
														.column()
														.nullable(false),
										PlaylistTrack.PLAYLIST_FK.define()
														.foreignKey(),
										PlaylistTrack.ARTIST.define()
														.denormalized(PlaylistTrack.ALBUM, Album.ARTIST_FK),
										PlaylistTrack.TRACK_ID.define()
														.column()
														.nullable(false),
										PlaylistTrack.TRACK_FK.define()
														.foreignKey()
														.referenceDepth(3),
										PlaylistTrack.ALBUM.define()
														.denormalized(PlaylistTrack.TRACK_FK, Track.ALBUM_FK))
						.keyGenerator(identity())
						.formatter(EntityFormatter.builder()
										.value(PlaylistTrack.PLAYLIST_FK)
										.text(" - ")
										.value(PlaylistTrack.TRACK_FK)
										.build())
						.build();
	}

	// Converts between a SQL VARCHAR ARRAY and a List<String>.
	private static final class TagsConverter implements Converter<List<String>, Array> {

		private static final int ARRAY_VALUE_INDEX = 2;

		private final ResultPacker<String> packer = resultSet -> resultSet.getString(ARRAY_VALUE_INDEX);

		@Override
		public Array toColumnValue(List<String> value, Statement statement) throws SQLException {
			return value.isEmpty() ? null :
							statement.getConnection().createArrayOf("VARCHAR", value.toArray(new Object[0]));
		}

		@Override
		public List<String> fromColumnValue(Array columnValue) throws SQLException {
			try (ResultSet resultSet = columnValue.getResultSet()) {
				return packer.pack(resultSet);
			}
		}
	}

	private static final class UpdateTotals implements DatabaseProcedure<EntityConnection, Collection<Long>> {

		@Override
		public void execute(EntityConnection connection,
												Collection<Long> invoiceIds) {
			Collection<Entity> invoices =
							connection.select(where(Invoice.ID.in(invoiceIds))
											.forUpdate()
											.build());

			connection.update(invoices.stream()
							.map(UpdateTotals::updateTotal)
							.filter(Entity::modified)
							.toList());
		}

		private static Entity updateTotal(Entity invoice) {
			invoice.set(Invoice.TOTAL, invoice.optional(Invoice.CALCULATED_TOTAL).orElse(BigDecimal.ZERO));

			return invoice;
		}
	}

	private static final class CreateRandomPlaylist implements DatabaseFunction<EntityConnection, RandomPlaylistParameters, Entity> {

		private final Entities entities;

		private CreateRandomPlaylist(Entities entities) {
			this.entities = entities;
		}

		@Override
		public Entity execute(EntityConnection connection,
													RandomPlaylistParameters parameters) {
			List<Long> trackIds = randomTrackIds(connection, parameters.noOfTracks(), parameters.genres());

			return insertPlaylist(connection, parameters.playlistName(), trackIds);
		}

		private Entity insertPlaylist(EntityConnection connection, String playlistName,
																	List<Long> trackIds) {
			Entity playlist = connection.insertSelect(createPlaylist(playlistName));

			connection.insert(createPlaylistTracks(playlist.primaryKey().value(), trackIds));

			return playlist;
		}

		private Entity createPlaylist(String playlistName) {
			return entities.entity(Playlist.TYPE)
							.with(Playlist.NAME, playlistName)
							.build();
		}

		private List<Entity> createPlaylistTracks(Long playlistId, List<Long> trackIds) {
			return trackIds.stream()
							.map(trackId -> createPlaylistTrack(playlistId, trackId))
							.toList();
		}

		private Entity createPlaylistTrack(Long playlistId, Long trackId) {
			return entities.entity(PlaylistTrack.TYPE)
							.with(PlaylistTrack.PLAYLIST_ID, playlistId)
							.with(PlaylistTrack.TRACK_ID, trackId)
							.build();
		}

		private static List<Long> randomTrackIds(EntityConnection connection, int noOfTracks,
																						 Collection<Entity> genres) {
			return connection.select(Track.ID,
							where(Track.GENRE_FK.in(genres))
											.orderBy(ascending(Track.RANDOM))
											.limit(noOfTracks)
											.build());
		}
	}

	private static final class RaisePrice implements DatabaseFunction<EntityConnection, RaisePriceParameters, Collection<Entity>> {

		@Override
		public Collection<Entity> execute(EntityConnection entityConnection,
																			RaisePriceParameters parameters) {
			Select select = where(Track.ID.in(parameters.trackIds()))
							.forUpdate()
							.build();

			return entityConnection.updateSelect(entityConnection.select(select).stream()
							.map(track -> raisePrice(track, parameters.priceIncrease()))
							.toList());
		}

		private static Entity raisePrice(Entity track, BigDecimal priceIncrease) {
			track.set(Track.UNITPRICE, track.get(Track.UNITPRICE).add(priceIncrease));

			return track;
		}
	}
}