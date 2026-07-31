package com.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.entity.Group;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;
import com.chat.entity.User;
import com.chat.mapper.GroupMapper;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.GroupMessageMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;

    public GroupServiceImpl(GroupMapper groupMapper,
                            GroupMemberMapper groupMemberMapper,
                            GroupMessageMapper groupMessageMapper,
                            UserMapper userMapper) {
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Group createGroup(Long ownerId, String name, List<Long> memberIds, String avatar) {
        Group group = new Group();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setAvatar(avatar != null ? avatar : "/uploads/default-group.svg");
        group.setMemberCount(memberIds.size() + 1);
        group.setCreatedAt(java.time.LocalDateTime.now());
        group.setUpdatedAt(java.time.LocalDateTime.now());
        groupMapper.insert(group);

        // 群主加入（role=2）
        GroupMember owner = new GroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(ownerId);
        owner.setRole(2);
        owner.setJoinedAt(java.time.LocalDateTime.now());
        groupMemberMapper.insert(owner);

        // 成员加入（role=0）
        for (Long memberId : memberIds) {
            if (!memberId.equals(ownerId)) {
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setUserId(memberId);
                member.setRole(0);
                member.setJoinedAt(java.time.LocalDateTime.now());
                groupMemberMapper.insert(member);
            }
        }

        return group;
    }

    @Override
    public List<Group> getMyGroups(Long userId) {
        return groupMapper.findMyGroups(userId);
    }

    @Override
    public Map<String, Object> getGroupDetail(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }
        boolean isMember = groupMemberMapper.isMember(groupId, userId);
        if (!isMember) {
            throw new RuntimeException("你不是该群成员");
        }

        List<Map<String, Object>> members = groupMemberMapper.findMembersByGroupId(groupId);
        User owner = userMapper.selectById(group.getOwnerId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", group.getId());
        result.put("name", group.getName());
        result.put("avatar", group.getAvatar());
        result.put("ownerId", group.getOwnerId());
        result.put("ownerNickname", owner != null ? owner.getNickname() : "");
        result.put("announcement", group.getAnnouncement());
        result.put("memberCount", group.getMemberCount());
        result.put("members", members);
        result.put("createdAt", group.getCreatedAt() != null
                ? group.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        return result;
    }

    @Override
    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        return groupMemberMapper.findMembersByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupMessage sendGroupMessage(Long groupId, Long senderId, String content, Integer messageType) {
        boolean isMember = groupMemberMapper.isMember(groupId, senderId);
        if (!isMember) {
            throw new RuntimeException("你不是该群成员，无法发送消息");
        }

        GroupMessage msg = new GroupMessage();
        msg.setGroupId(groupId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : 0);
        msg.setCreatedAt(java.time.LocalDateTime.now());
        groupMessageMapper.insert(msg);

        // 更新群组更新时间
        Group group = groupMapper.selectById(groupId);
        if (group != null) {
            group.setUpdatedAt(java.time.LocalDateTime.now());
            groupMapper.updateById(group);
        }

        return msg;
    }

    @Override
    public List<Map<String, Object>> getGroupChatHistory(Long groupId) {
        List<Map<String, Object>> messages = groupMessageMapper.getGroupChatHistory(groupId);
        Collections.reverse(messages);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> msg : messages) {
            if (msg.get("created_at") != null) {
                msg.put("createdAt", msg.get("created_at"));
                msg.remove("created_at");
            }
            if (msg.get("sender_nickname") != null) {
                msg.put("senderNickname", msg.get("sender_nickname"));
                msg.remove("sender_nickname");
            }
            if (msg.get("sender_avatar") != null) {
                msg.put("senderAvatar", msg.get("sender_avatar"));
                msg.remove("sender_avatar");
            }
            if (msg.get("group_id") != null) {
                msg.put("groupId", msg.get("group_id"));
                msg.remove("group_id");
            }
            if (msg.get("sender_id") != null) {
                msg.put("senderId", msg.get("sender_id"));
                msg.remove("sender_id");
            }
            if (msg.get("message_type") != null) {
                msg.put("messageType", msg.get("message_type"));
                msg.remove("message_type");
            }
        }
        return messages;
    }

    @Override
    @Transactional
    public void addMembers(Long groupId, Long operatorId, List<Long> memberIds) {
        boolean isMember = groupMemberMapper.isMember(groupId, operatorId);
        if (!isMember) {
            throw new RuntimeException("你不是该群成员");
        }

        for (Long memberId : memberIds) {
            if (!groupMemberMapper.isMember(groupId, memberId)) {
                GroupMember gm = new GroupMember();
                gm.setGroupId(groupId);
                gm.setUserId(memberId);
                gm.setRole(0);
                gm.setJoinedAt(java.time.LocalDateTime.now());
                groupMemberMapper.insert(gm);
            }
        }

        // 更新成员数
        List<Long> allMembers = groupMemberMapper.findMemberIdsByGroupId(groupId);
        Group group = groupMapper.selectById(groupId);
        if (group != null) {
            group.setMemberCount(allMembers.size());
            groupMapper.updateById(group);
        }
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }

        // 群主不能直接退出，需先转让或解散
        if (group.getOwnerId().equals(userId)) {
            throw new RuntimeException("群主不能退出，请先转让群主或解散群聊");
        }

        deleteMemberAndUpdate(groupId, userId, group);
    }

    @Override
    @Transactional
    public void dismissGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主才能解散群");
        }

        groupMessageMapper.delete(new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, groupId));
        groupMemberMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId));
        groupMapper.deleteById(groupId);
    }

    @Override
    public void updateGroupName(Long groupId, Long userId, String name) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主才能修改群名称");
        }
        group.setName(name);
        groupMapper.updateById(group);
    }

    @Override
    public void updateAnnouncement(Long groupId, Long userId, String announcement) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }
        boolean isMember = groupMemberMapper.isMember(groupId, userId);
        if (!isMember) {
            throw new RuntimeException("你不是该群成员");
        }
        group.setAnnouncement(announcement);
        groupMapper.updateById(group);
    }

    private void deleteMemberAndUpdate(Long groupId, Long userId, Group group) {
        groupMemberMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));

        List<Long> remainMembers = groupMemberMapper.findMemberIdsByGroupId(groupId);
        group.setMemberCount(remainMembers.size());
        groupMapper.updateById(group);
    }
}
