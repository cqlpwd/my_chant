package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.MomentLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 朋友圈点赞 Mapper
 */
public interface MomentLikeMapper extends BaseMapper<MomentLike> {

    @Select("SELECT user_id FROM t_moment_like WHERE moment_id = #{momentId}")
    List<Long> findLikeUserIds(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM t_moment_like WHERE moment_id = #{momentId}")
    int countByMomentId(@Param("momentId") Long momentId);
}
