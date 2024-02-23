package pro.julleon.sandbox.services;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;

/**
 * Класс JdbcUserDetailService представляет собой реализацию интерфейса UserDetailsService,
 * использующую JDBC для извлечения информации о пользователе из базы данных PostgreSQL.
 * Класс также наследуется от MappingSqlQuery<UserDetails>, обеспечивая маппинг результата SQL-запроса
 * на объект UserDetails.
 */
public class JdbcUserDetailService extends MappingSqlQuery<UserDetails> implements UserDetailsService {

    /**
     * Конструктор JdbcUserDetailService принимает DataSource и вызывает конструктор родительского класса
     * с SQL-запросом для извлечения данных о пользователе, пароле и его ролях.
     * Также указываются параметры запроса и выполняется его компиляция.
     */
    public JdbcUserDetailService(DataSource ds) {
        // Вызов конструктора родительского класса с SQL-запросом
        super(ds, """
                      select
                         t_u.c_username,
                         t_up.c_password,
                         array_agg(t_ua.c_authority) as c_authorities
                      from
                         t_user as t_u
                         left join t_user_password as t_up on t_up.id_user = t_u.id
                         left join t_user_authority as t_ua on t_ua.id_user = t_u.id
                      where
                         t_u.c_username = :username
                      group by
                         t_u.id, t_up.id;
                """);
        // Указание параметра SQL-запроса
        this.declareParameter(new SqlParameter("username", Types.VARCHAR));
        // Компиляция запроса
        this.compile();
    }

    /**
     * Метод mapRow переопределен для маппинга каждой строки результата SQL-запроса на объект UserDetails.
     * Создается объект User, содержащий имя пользователя, его пароль и роли.
     */
    @Override
    protected UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
        // Построение объекта User из данных результата запроса
        return User.builder()
                .username(rs.getString("c_username"))
                .password(rs.getString("c_password"))
                .authorities((String[]) rs.getArray("c_authorities").getArray())
                .build();
    }

    /**
     * Метод loadUserByUsername переопределен для получения UserDetails по имени пользователя.
     * Используется findObjectByNamedParam для выполнения запроса с параметром username.
     * В случае отсутствия результата выбрасывается исключение UsernameNotFoundException.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Использование findObjectByNamedParam для выполнения запроса с параметром
        return Optional
                .ofNullable(this.findObjectByNamedParam(Map.of("username", username)))
                // В случае отсутствия результата выбрасывается исключение
                .orElseThrow(() -> new UsernameNotFoundException("Username %s not found".formatted(username)));
    }
}
