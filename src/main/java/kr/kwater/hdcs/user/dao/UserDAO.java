package kr.kwater.hdcs.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.kwater.hdcs.user.dto.UserCreateDTO;
import kr.kwater.hdcs.user.dto.UserResponseDTO;
import kr.kwater.hdcs.user.vo.UserVO;

@Mapper
public interface UserDAO {

    @Select("SELECT user_id AS userId, " +
            "       \"password\" AS password, " +
            "       user_name AS userName, " +
            "       email AS email, " +
            "       phone_number AS phoneNumber, " +
            "       use_yn AS useYn, " +
            "       role_type AS roleType, " +
            "       created_at AS createdAt " +
            "FROM public.tb_user " +
            "WHERE user_id = #{userId} AND use_yn = 'Y'")
    UserVO findByUserId(String userId);

    @Select("SELECT user_id AS userId, " +
            "       user_name AS name, " +
            "       email AS email, " +
            "       role_type AS role, " +
            "       CASE WHEN use_yn = 'Y' THEN true ELSE false END AS active, " +
            "       TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS createdAt " +
            "FROM public.tb_user " +
            "ORDER BY created_at DESC")
    List<UserResponseDTO> findAllUsers();

    @Insert("INSERT INTO public.tb_user " +
            "  (user_id, \"password\", user_name, email, role_type, use_yn, created_at) " +
            "VALUES " +
            "  (#{userId}, #{password}, #{name}, #{email}, #{role}, 'Y', NOW())")
    void insertUser(UserCreateDTO dto);

    @Update("UPDATE public.tb_user " +
            "SET user_name = #{name}, email = #{email}, role_type = #{role} " +
            "WHERE user_id = #{userId}")
    void updateUserInfo(UserCreateDTO dto);

    @Update("UPDATE public.tb_user SET \"password\" = #{password} WHERE user_id = #{userId}")
    void updateUserPassword(@Param("userId") String userId, @Param("password") String password);

    @Delete("DELETE FROM public.tb_user WHERE user_id = #{userId}")
    void deleteByUserId(String userId);

    @Select("SELECT COUNT(*) FROM public.tb_user WHERE user_id = #{userId}")
    int countByUserId(String userId);
}
