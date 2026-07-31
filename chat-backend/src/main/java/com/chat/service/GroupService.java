package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.entity.Group;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;

import java.util.List;
import java.util.Map;

public interface GroupService extends IService<Group> {

    /** 创建群聊 */
    Group createGroup(Long ownerId, String name, List<Long> memberIds, String avatar);

    /** 查询用户加入的群列表 */
    List<Group> getMyGroups(Long userId);

    /** 获取群详情（含成员） */
    Map<String, Object> getGroupDetail(Long groupId, Long userId);

    /** 获取群成员 */
    List<Map<String, Object>> getGroupMembers(Long groupId);

    /** 发送群消息 */
    GroupMessage sendGroupMessage(Long groupId, Long senderId, String content, Integer messageType);

    /** 获取群聊历史消息 */
    List<Map<String, Object>> getGroupChatHistory(Long groupId);

    /** 邀请成员入群 */
    void addMembers(Long groupId, Long operatorId, List<Long> memberIds);

    /** 退出群聊 */
    void leaveGroup(Long groupId, Long userId);

    /** 解散群（仅群主） */
    void dismissGroup(Long groupId, Long userId);

    /** 修改群名称 */
    void updateGroupName(Long groupId, Long userId, String name);

    /** 修改群公告 */
    void updateAnnouncement(Long groupId, Long userId, String announcement);
}
