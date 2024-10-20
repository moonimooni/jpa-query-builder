package persistence.sql.dml;

import database.H2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import persistence.model.exception.ColumnInvalidException;
import persistence.model.exception.ColumnNotFoundException;
import persistence.sql.dialect.DialectFactory;
import persistence.fixture.PersonWithTransientAnnotation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DmlQueryBuilderTest {
    DmlQueryBuilder queryBuilder;

    @BeforeEach
    void setup() {
        queryBuilder = new DmlQueryBuilder(DialectFactory.create(H2.class));
    }

    @Nested
    @DisplayName("Update 쿼리 생성 테스트")
    class UpdateQueryTests {
        @Test
        @DisplayName("엔티티 오브젝트의 PK를 기준으로 업데이트 쿼리를 생성한다.")
        void succeedToCreateQuery() {
            String expectedQuery = "UPDATE \"users\" " +
                    "SET \"id\" = 1, \"nick_name\" = '홍길동2', \"old\" = 30, \"email\" = 'test@test.com' " +
                    "WHERE (\"id\" = 1);";

            PersonWithTransientAnnotation user = new PersonWithTransientAnnotation(
                    1L, "홍길동2", 30, "test@test.com", 1
            );
            String resultQuery = queryBuilder.buildUpdateQuery(user);

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("엔티티 오브젝트에 PK가 없으면 실패한다.")
        void failToCreateQuery() {
            PersonWithTransientAnnotation user = new PersonWithTransientAnnotation(
                    "홍길동2", 30, "test@test.com", 1
            );

            assertThrows(ColumnInvalidException.class, () -> {
                queryBuilder.buildDeleteQuery(user);
            });
        }
    }

    @Nested
    @DisplayName("Insert 쿼리 생성 테스트")
    class InsertQueryTests {
        @Test
        @DisplayName("모든 필드의 값이 채워진 객체의 insert문을 생성한다.")
        void testCreateInsertQueryWithAllColumnsSet() {
            String expectedQuery = "INSERT INTO \"users\" " +
                    "(\"id\", \"nick_name\", \"old\", \"email\") " +
                    "VALUES (1, '홍길동', 20, 'test@test.com');";

            PersonWithTransientAnnotation user = new PersonWithTransientAnnotation(
                    1L, "홍길동", 20, "test@test.com", 1
            );
            String resultQuery = queryBuilder.buildInsertQuery(user);

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("id가 없는 객체의 insert문을 생성한다.")
        void testCreateInsertQueryWithoutId() {
            String expectedQuery = "INSERT INTO \"users\" " +
                    "(\"nick_name\", \"old\", \"email\") " +
                    "VALUES ('홍길동', 20, 'test@test.com');";

            PersonWithTransientAnnotation user = new PersonWithTransientAnnotation(
                    "홍길동", 20, "test@test.com", 1
            );
            String resultQuery = queryBuilder.buildInsertQuery(user);

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("null이 포함된 객체의 insert문을 생성한다.")
        void testCreateInsertQueryWithNull() {
            String expectedQuery = "INSERT INTO \"users\" " +
                    "(\"nick_name\", \"old\", \"email\") " +
                    "VALUES (NULL, NULL, 'test@test.com');";

            PersonWithTransientAnnotation user = new PersonWithTransientAnnotation("test@test.com");
            String resultQuery = queryBuilder.buildInsertQuery(user);

            assertEquals(expectedQuery, resultQuery);
        }
    }

    @Nested
    @DisplayName("Select 쿼리 생성 테스트")
    class SelectQueryTests {
        @Test
        @DisplayName("조건절 없이 테이블의 모든 레코드를 조회한다.")
        void testCreateSelectQueryWithoutClauses() {
            String expectedQuery = "SELECT * FROM \"users\";";

            String resultQuery = queryBuilder.buildSelectQuery(PersonWithTransientAnnotation.class);

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("조건절로 테이블의 모든 레코드를 조회한다.")
        void testCreateSelectQueryWithClauses() {
            // AND로 구분된 clause끼리는 순서보장 안되어도 상관 없음
            String expectedQuery1 = "SELECT * FROM \"users\" " +
                    "WHERE (\"email\" = 'test@test.com' AND \"nick_name\" = '홍길동') " +
                    "OR (\"id\" = 1);";
            String expectedQuery2 = "SELECT * FROM \"users\" " +
                    "WHERE (\"nick_name\" = '홍길동' AND \"email\" = 'test@test.com') " +
                    "OR (\"id\" = 1);";


            String resultQuery = queryBuilder.buildSelectQuery(
                    PersonWithTransientAnnotation.class,
                    List.of(
                            Map.of("email", "test@test.com", "nick_name", "홍길동"),
                            Map.of("id", 1L)
                    )
            );

            assertTrue(resultQuery.equals(expectedQuery1) || resultQuery.equals(expectedQuery2));
        }

        @Test
        @DisplayName("pk로 테이블의 특정 필드를 조회한다.")
        void testCreateSelectSpecificColumnsQueryWithClauses() {
            String expectedQuery = "SELECT \"email\", \"nick_name\", \"id\" FROM \"users\" " +
                    "WHERE (\"id\" = 1);";

            String resultQuery = queryBuilder.buildSelectQuery(
                    PersonWithTransientAnnotation.class,
                    List.of("email", "nick_name", "id"),
                    List.of(new LinkedHashMap<>(Map.of("id", 1L)))
            );

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("존재하지 않는 컬럼에 대한 비교절을 제시하면 에러를 내뱉는다.")
        void testThrowForUnknownClauses() {
            assertThrows(ColumnNotFoundException.class, () -> {
                queryBuilder.buildSelectQuery(
                        PersonWithTransientAnnotation.class,
                        List.of(new LinkedHashMap<>(Map.of("hobby", "잠자기")))
                );
            });
        }

        @Test
        @DisplayName("존재하지 않는 컬럼을 SELECT하려 한다면 에러를 내뱉는다.")
        void testThrowForUnknownSelections() {
            assertThrows(ColumnNotFoundException.class, () -> {
                queryBuilder.buildSelectQuery(
                        PersonWithTransientAnnotation.class,
                        List.of("id", "hobby"),
                        List.of(new LinkedHashMap<>(Map.of("id", 1L)))
                );
            });
        }
    }

    @Nested
    @DisplayName("Delete 쿼리 생성 테스트")
    class DeleteQueryTests {

        @Test
        @DisplayName("엔티티 객체가 주어지면 PK를 찾아 레코드를 삭제한다.")
        void succeedToDeleteByObject() {
            String expectedQuery = "DELETE FROM \"users\" WHERE (\"id\" = 1);";

            PersonWithTransientAnnotation person = new PersonWithTransientAnnotation(
                    1L, "홍길동", 20, "test@test.com", 1
            );
            String resultQuery = queryBuilder.buildDeleteQuery(person);

            assertEquals(expectedQuery, resultQuery);
        }

        @Test
        @DisplayName("PK가 없는 객체가 주어지면 에러를 내뱉는다.")
        void failToDeleteForEmptyPK() {
            PersonWithTransientAnnotation person = new PersonWithTransientAnnotation("test@test.com");

            assertThrows(ColumnInvalidException.class, () -> {
                queryBuilder.buildDeleteQuery(person);
            });
        }
    }
}
