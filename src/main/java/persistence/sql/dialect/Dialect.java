package persistence.sql.dialect;

import persistence.model.meta.DataType;
import persistence.model.meta.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static java.sql.Types.*;

public abstract class Dialect {

    protected final String identifierQuote = "\"";

    protected final String valueQuote = "'";

    protected final Map<Class<?>, DataType> dataTypeRegistry = new HashMap<>();

    protected Dialect() {
        initiateDataTypeRegistry();
    }

    protected void initiateDataTypeRegistry() {
        registerDataType(INTEGER);
        registerDataType(VARCHAR);
        registerDataType(BIGINT);
    }

    protected void registerDataType(int typeCode) {
        DataType dataType = new DataType(
                typeCode,
                mapSqlCodeToJavaType(typeCode),
                mapSqlCodeToNamePattern(typeCode),
                mapSqlCodeToIsQuoteRequiredValue(typeCode)
        );
        dataTypeRegistry.put(mapSqlCodeToJavaType(typeCode), dataType);
    }

    protected Class<?> mapSqlCodeToJavaType(int typeCode) {
        return switch (typeCode) {
            case BIGINT -> Long.class;
            case INTEGER -> Integer.class;
            case VARCHAR -> String.class;
            default -> throw new IllegalArgumentException("UNKNOWN TYPE. sql code = " + typeCode);
        };
    }

    protected String mapSqlCodeToNamePattern(int typeCode) {
        return switch (typeCode) {
            case BIGINT -> "bigint";
            case INTEGER -> "int";
            case VARCHAR -> "varchar(%d)";
            default -> throw new IllegalArgumentException("UNKNOWN TYPE. sql code = " + typeCode);
        };
    }

    protected Boolean mapSqlCodeToIsQuoteRequiredValue(int typeCode) {
        return switch (typeCode) {
            case BIGINT, INTEGER -> false;
            case VARCHAR -> true;
            default -> throw new IllegalArgumentException("UNKNOWN TYPE. sql code = " + typeCode);
        };
    }

    public String getDataTypeFullName(Class<?> javaType, int length) {
        DataType dataType = dataTypeRegistry.get(javaType);
        if (dataType == null) {
            throw new NoSuchElementException("UNSUPPORTED JAVA TYPE : " + javaType.getSimpleName());
        }
        return dataType.getFullName(length);
    }

    public String getIdentifierQuote() {
        return identifierQuote;
    }

    public String getIdentifierQuoted(String identifier) {
        return identifierQuote + identifier + identifierQuote;
    }

    public String getIdentifiersQuoted(List<String> identifiers) {
        return identifiers.stream()
                .map(this::getIdentifierQuoted)
                .collect(Collectors.joining(", "));
    }

    public String getValueQuoted(Value value) {
        Object columnValue = value.getValue();
        if (columnValue == null) {
            return getNullPhrase(true);
        }
        DataType dataType = dataTypeRegistry.get(value.getType());
        if (dataType.isQuoteRequired()) {
            return valueQuote + columnValue + valueQuote;
        }
        return columnValue.toString();
    }

    public String getValuesQuoted(List<Value> values) {
        return values.stream()
                .map(this::getValueQuoted)
                .collect(Collectors.joining(", "));
    }

    public String getNullPhrase(Boolean isNull) {
        return isNull ? "NULL" : "NOT NULL";
    }

    public String getAutoGeneratedIdentityPhrase() {
        return "AUTO INCREMENT";
    };

    public String getCreateTablePhrase() {
        return "CREATE TABLE";
    }

    public String getDropTablePhrase() {
        return "DROP TABLE IF EXISTS";
    }

    public String buildPrimaryKeyPhrase(List<String> columnNames) {
        String quotedColumnNames = columnNames.stream()
                .map(this::getIdentifierQuoted)
                .collect(Collectors.joining(", "));

        return String.format("PRIMARY KEY (%s)", quotedColumnNames);
    }

    public Boolean shouldSpecifyNotNullOnIdentity() {
        return true;
    };
}
