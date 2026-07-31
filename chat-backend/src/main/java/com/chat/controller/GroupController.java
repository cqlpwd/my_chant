package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.entity.Group;
import com.chat.entity.GroupMessage;
import com.chat.entity.User;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/group")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;
    private final GroupMemberMapper groupMemberMapper;

    public GroupController(GroupService groupService,
                           SimpMessagingTemplate messagingTemplate,
                           UserMapper userMapper,
                           GroupMemberMapper groupMemberMapper) {
        this.groupService = groupService;
        this.messagingTemplate = messagingTemplate;
        this.userMapper = userMapper;
        this.groupMemberMapper = groupMemberMapper;
    }

    /** 创建群聊 */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createGroup(Authentication authentication,
                                                          @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<Number> memberIdsRaw = (List<Number>) body.get("memberIds");
        List<Long> memberIds = new ArrayList<>();
        if (memberIdsRaw != null) {
            for (Number n : memberIdsRaw) {
                memberIds.add(n.longValue());
            }
        }

        try {
            Group group = groupService.createGroup(userId, name, memberIds, null);

            // 通知所有被邀请的成员
            Map<String, Object> notify = new HashMap<>();
            notify.put("type", "GROUP_CREATED");
            notify.put("groupId", group.getId());
            notify.put("groupName", group.getName());
            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/chat", notify);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("groupId", group.getId());
            result.put("name", group.getName());
            return ApiResponse.success("创建成功", result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取我的群聊列表 */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getMyGroups(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Group> groups = groupService.getMyGroups(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Group g : groups) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("avatar", g.getAvatar());
            item.put("ownerId", g.getOwnerId());
            item.put("memberCount", g.getMemberCount());

            // 获取成员头像（最多 9 个，用于前端拼接微信群聊头像）
            List<Map<String, Object>> members = groupMemberMapper.findMembersByGroupId(g.getId());
            List<String> memberAvatars = new ArrayList<>();
            for (Map<String, Object> m : members) {
                String avatar = (String) m.get("avatar");
                if (avatar != null && !avatar.isEmpty()) {
                    memberAvatars.add(avatar);
                    if (memberAvatars.size() >= 9) break;
                }
            }
            item.put("memberAvatars", memberAvatars);

            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /** 获取群详情 */
    @GetMapping("/{groupId}")
    public ApiResponse<Map<String, Object>> getGroupDetail(Authentication authentication,
                                                             @PathVariable Long groupId) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            Map<String, Object> detail = groupService.getGroupDetail(groupId, userId);
            return ApiResponse.success(detail);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取群聊历史消息 */
    @GetMapping("/{groupId}/history")
    public ApiResponse<List<Map<String, Object>>> getGroupChatHistory(Authentication authentication,
                                                                        @PathVariable Long groupId) {
        Long userId = (Long) authentication.getPrincipal();
        if (!groupMemberMapper.isMember(groupId, userId)) {
            return ApiResponse.error(403, "你不是该群成员");
        }
        List<Map<String, Object>> messages = groupService.getGroupChatHistory(groupId);
        return ApiResponse.success(messages);
    }

    /** 发送群消息（HTTP 保底） */
    @PostMapping("/{groupId}/send")
    public ApiResponse<Map<String, Object>> sendGroupMessage(Authentication authentication,
                                                               @PathVariable Long groupId,
                                                               @RequestBody Map<String, Object> body) {
        Long senderId = (Long) authentication.getPrincipal();
        String content = (String) body.get("content");
        Integer messageType = body.containsKey("messageType")
                ? Integer.valueOf(body.get("messageType").toString()) : 0;

        try {
            GroupMessage msg = groupService.sendGroupMessage(groupId, senderId, content, messageType);
            User sender = userMapper.selectById(senderId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "GROUP_CHAT");
            payload.put("id", msg.getId());
            payload.put("groupId", groupId);
            payload.put("senderId", senderId);
            payload.put("senderNickname", sender != null ? sender.getNickname() : "");
            payload.put("senderAvatar", sender != null ? sender.getAvatar() : "");
            payload.put("content", msg.getContent());
            payload.put("messageType", msg.getMessageType());
            payload.put("createdAt", msg.getCreatedAt() != null
                    ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 推送给群内所有成员（除了发送者本人通过回执收到）
            List<Long> memberIds = groupMemberMapper.findMemberIdsByGroupId(groupId);
            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/chat", payload);
            }

            return ApiResponse.success("发送成功", payload);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 邀请成员入群 */
    @PostMapping("/{groupId}/members")
    public ApiResponse<Void> addMembers(Authentication authentication,
                                         @PathVariable Long groupId,
                                         @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("memberIds");
        List<Long> memberIds = new ArrayList<>();
        if (rawIds != null) {
            for (Number n : rawIds) {
                memberIds.add(n.longValue());
            }
        }

        try {
            groupService.addMembers(groupId, userId, memberIds);

            // 通知新成员
            Group group = groupService.getById(groupId);
            Map<String, Object> notify = new HashMap<>();
            notify.put("type", "GROUP_ADDED");
            notify.put("groupId", groupId);
            notify.put("groupName", group != null ? group.getName() : "");
            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/chat", notify);
            }

            return ApiResponse.success("邀请成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 退出群聊 */
    @PostMapping("/{groupId}/leave")
    public ApiResponse<Void> leaveGroup(Authentication authentication,
                                          @PathVariable Long groupId) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            groupService.leaveGroup(groupId, userId);
            return ApiResponse.success("已退出群聊", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 解散群聊 */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dismissGroup(Authentication authentication,
                                            @PathVariable Long groupId) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            groupService.dismissGroup(groupId, userId);
            return ApiResponse.success("群聊已解散", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 修改群名称 */
    @PutMapping("/{groupId}/name")
    public ApiResponse<Void> updateGroupName(Authentication authentication,
                                               @PathVariable Long groupId,
                                               @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            groupService.updateGroupName(groupId, userId, body.get("name"));
            return ApiResponse.success("修改成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 修改群公告 */
    @PutMapping("/{groupId}/announcement")
    public ApiResponse<Void> updateAnnouncement(Authentication authentication,
                                                  @PathVariable Long groupId,
                                                  @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            groupService.updateAnnouncement(groupId, userId, body.get("announcement"));
            return ApiResponse.success("修改成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
