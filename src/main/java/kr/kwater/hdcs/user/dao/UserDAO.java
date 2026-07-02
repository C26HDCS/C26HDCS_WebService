package kr.kwater.hdcs.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    // 리스트 조회 쿼리
    @Select("SELECT user_id AS userId, " +
            "       user_name AS userName, " +
            "       email AS email, " +
            "       phone_number AS phoneNumber, " +
            "       use_yn AS useYn, " +
            "       role_type AS roleType, " +
            "       created_at AS createdAt " +
            "FROM public.tb_user " +
            "ORDER BY created_at DESC")
    List<UserVO> findAllUsers();
}