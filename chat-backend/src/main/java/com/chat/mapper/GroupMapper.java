package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.Group;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMapper extends BaseMapper<Group> {

    /** 查询用户加入的所有群 */
    @Select("SELECT g.* FROM t_group g " +
            "INNER JOIN t_group_member gm ON g.id = gm.group_id " +
            "WHERE gm.user_id = #{userId} " +
            "ORDER BY g.updated_at DESC")
    List<Group> findMyGroups(@Param("userId") Long userId);
}
