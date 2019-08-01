package qunar.tc.bistoury.ui.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import qunar.tc.bistoury.ui.dao.UserDao;
import qunar.tc.bistoury.ui.model.User;

/**
 * @author leix.xie
 * @date 2019/7/4 11:01
 * @describe
 */
@Repository
public class UserDaoImpl implements UserDao {

    private static final String SELECT_USER_BY_USER_CODE = "select user_code,password from bistoury_user where user_code=?";

    private static final String INSERT_USER = "insert into bistoury_user (user_code, password) VALUES (?,?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public User getUserByUserCode(final String userCode) {
        return this.jdbcTemplate.query(SELECT_USER_BY_USER_CODE, USER_EXTRACTOR, userCode);
    }

    @Override
    public int registerUser(User user) {
        return this.jdbcTemplate.update(INSERT_USER, user.getUserCode(), user.getPassword());
    }

    private static final ResultSetExtractor<User> USER_EXTRACTOR = resultSet -> {
        if (resultSet.next()) {
            String userCode = resultSet.getString("user_code");
            String password = resultSet.getString("password");
            return new User(userCode, password);
        }
        return null;
    };
}
