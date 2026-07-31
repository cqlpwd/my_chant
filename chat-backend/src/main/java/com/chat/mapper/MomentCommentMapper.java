package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.MomentComment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 朋友圈评论 Mapper
 */
public interface MomentCommentMapper extends BaseMapper<MomentComment> {

    @Select("SELECT * FROM t_moment_comment WHERE moment_id = #{momentId} ORDER BY created_at ASC")
    List<MomentComment> findByMomentId(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM t_moment_comment WHERE moment_id = #{momentId}")
    int countByMomentId(@Param("momentId") Long momentId);
}
