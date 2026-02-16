package com.campus.trade.mapper;

import com.campus.trade.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Insert("INSERT INTO t_user(username, password_hash, phone, role, status) VALUES(#{username}, #{passwordHash}, #{phone}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

    @Update("UPDATE t_user SET last_login_ip = #{ip}, last_login_at = NOW() WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("ip") String ip);

    @Select("SELECT avatar_url FROM t_user WHERE avatar_url IS NOT NULL AND avatar_url <> ''")
    java.util.List<String> listAllAvatarUrls();

    @Select("SELECT id, username, phone, role, avatar_url AS avatarUrl, real_name AS realName, student_no AS studentNo, id_verified AS idVerified, rating_score AS ratingScore, rating_count AS ratingCount, status, last_login_ip AS lastLoginIp, last_login_at AS lastLoginAt, created_at AS createdAt, updated_at AS updatedAt FROM t_user ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    java.util.List<User> listUsers(@Param("limit") int limit, @Param("offset") int offset);
}
