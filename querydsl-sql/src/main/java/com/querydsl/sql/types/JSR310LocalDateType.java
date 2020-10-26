package com.querydsl.sql.types;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.jetbrains.annotations.Nullable;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/**
 * JSR310LocalDateType maps {@linkplain java.time.LocalDate}
 * to {@linkplain java.sql.Date} on the JDBC level
 *
 */
@IgnoreJRERequirement //conditionally included
public class JSR310LocalDateType extends AbstractJSR310DateTimeType<LocalDate> {

    public JSR310LocalDateType() {
        super(Types.DATE);
    }

    public JSR310LocalDateType(int type) {
        super(type);
    }

    @Override
    public String getLiteral(LocalDate value) {
        return dateFormatter.format(value);
    }

    @Override
    public Class<LocalDate> getReturnedClass() {
        return LocalDate.class;
    }

    @Nullable
    @Override
    public LocalDate getValue(ResultSet rs, int startIndex) throws SQLException {
        Date date = rs.getDate(startIndex, utc());
        return date != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                ZoneOffset.UTC).toLocalDate() : null;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, LocalDate value) throws SQLException {
        Instant i = value.atStartOfDay(ZoneOffset.UTC).toInstant();
        st.setDate(startIndex, new Date(i.toEpochMilli()), utc());
    }
}
