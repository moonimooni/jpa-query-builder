package persistence.sql.dml;

import persistence.model.EntityColumn;
import persistence.model.EntityFactory;
import persistence.model.EntityTable;
import persistence.model.meta.Value;
import persistence.sql.dialect.Dialect;
import persistence.sql.dml.clause.FindOption;
import persistence.sql.dml.clause.WhereClause;

import java.util.List;

public class DmlQueryBuilder {
    private final Dialect dialect;

    public DmlQueryBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    public String getInsertQuery(Object entityObject) {
        String INSERT_FORMAT = "INSERT INTO %s (%s) VALUES (%s)";

        EntityTable table = EntityFactory.createPopulatedSchema(entityObject);

        List<EntityColumn> columns = table.getInsertableColumns(table);
        List<String> columnNames = columns.stream()
                .map(EntityColumn::getName)
                .toList();
        List<Object> values = columns.stream()
                .map(EntityColumn::getValue)
                .map(Value::getValue)
                .toList();

        String columnNamesJoined = dialect.getIdentifiersQuoted(columnNames);
        String valueNames = dialect.getValuesQuoted(values);

        String tableName = table.getName();

        return String.format(INSERT_FORMAT, dialect.getIdentifierQuoted(tableName), columnNamesJoined, valueNames);
    }

    public String getSelectQuery(Class<?> entityClass, FindOption findOption) {
        String SELECT_ALL = "*";
        String SELECT_FORMAT = "SELECT %s FROM %s";

        EntityTable table = EntityFactory.createEmptySchema(entityClass);

        List<String> selectingColumnNames = findOption.getSelectingColumns().stream()
                .map(EntityColumn::getName)
                .toList();

        String selectingColumnNamesJoined = selectingColumnNames.isEmpty()
                ? SELECT_ALL
                : dialect.getIdentifiersQuoted(selectingColumnNames);

        String query = String.format(
                SELECT_FORMAT,
                selectingColumnNamesJoined,
                dialect.getIdentifierQuoted(table.getName())
        );
        if (!findOption.getWhere().isEmpty()) {
            return query + " " + findOption.joinWhereClauses(dialect);
        }
        return query;
    }

    public String getDeleteQuery(Class<?> entityClass, WhereClause where) {
        String DELETE_FORMAT = "DELETE FROM %s";

        return null;

//        EntityTable table = EntityFactory.createEmptySchema(entityClass);
//        String tableName = table.getName();
//
//
    }
}
