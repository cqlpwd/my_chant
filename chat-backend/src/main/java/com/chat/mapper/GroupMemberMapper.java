package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.GroupMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /** 查询群的所有成员ID */
    @Select("SELECT user_id FROM t_group_member WHERE group_id = #{groupId}")
    List<Long> findMemberIdsByGroupId(@Param("groupId") Long groupId);

    /** 查询某个群的成员信息（带用户表 JOIN） */
    @Select("SELECT gm.*, u.nickname, u.avatar FROM t_group_member gm " +
            "LEFT JOIN t_user u ON gm.user_id = u.id " +
            "WHERE gm.group_id = #{groupId} " +
            "ORDER BY gm.role DESC, gm.joined_at ASC")
    List<java.util.Map<String, Object>> findMembersByGroupId(@Param("groupId") Long groupId);

    /** 检查用户是否在群中 */
    @Select("SELECT COUNT(*) > 0 FROM t_group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
