package com.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.entity.Moment;
import com.chat.entity.MomentComment;
import com.chat.entity.MomentLike;
import com.chat.entity.User;
import com.chat.mapper.MomentCommentMapper;
import com.chat.mapper.MomentLikeMapper;
import com.chat.mapper.MomentMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.MomentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 朋友圈服务实现
 */
@Service
public class MomentServiceImpl extends ServiceImpl<MomentMapper, Moment> implements MomentService {

    private final MomentMapper momentMapper;
    private final MomentLikeMapper likeMapper;
    private final MomentCommentMapper commentMapper;
    private final UserMapper userMapper;

    public MomentServiceImpl(MomentMapper momentMapper,
                             MomentLikeMapper likeMapper,
                             MomentCommentMapper commentMapper,
                             UserMapper userMapper) {
        this.momentMapper = momentMapper;
        this.likeMapper = likeMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Moment createMoment(Long userId, String content, String images, String location) {
        if ((content == null || content.trim().isEmpty()) && (images == null || images.trim().isEmpty())) {
            throw new RuntimeException("内容不能为空");
        }
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(content != null ? content.trim() : null);
        moment.setImages(images);
        moment.setLocation(location);
        momentMapper.insert(moment);
        return moment;
    }

    @Override
    public List<Map<String, Object>> getFriendMoments(Long userId) {
        List<Moment> moments = momentMapper.findFriendMoments(userId);

        // 批量获取发布者信息
        Set<Long> userIds = moments.stream().map(Moment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userMapper.selectById(uid);
            if (u != null) userMap.put(uid, u);
        }

        return moments.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("content", m.getContent());
            map.put("images", m.getImages());
            map.put("location", m.getLocation());
            map.put("createdAt", m.getCreatedAt());

            User publisher = userMap.get(m.getUserId());
            if (publisher != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", publisher.getId());
                userInfo.put("nickname", publisher.getNickname());
                userInfo.put("avatar", publisher.getAvatar());
                map.put("user", userInfo);
            }

            // 点赞信息
            List<Long> likeUserIds = likeMapper.findLikeUserIds(m.getId());
            map.put("likeCount", likeUserIds.size());
            map.put("liked", likeUserIds.contains(userId));
            map.put("likeUserIds", likeUserIds);

            // 评论数量
            int commentCount = commentMapper.countByMomentId(m.getId());
            map.put("commentCount", commentCount);

            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long momentId, Long userId) {
        LambdaQueryWrapper<MomentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId);
        MomentLike exist = likeMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            result.put("liked", false);
        } else {
            MomentLike like = new MomentLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            likeMapper.insert(like);
            result.put("liked", true);
        }
        result.put("likeCount", likeMapper.countByMomentId(momentId));
        return result;
    }

    @Override
    public List<Map<String, Object>> getLikes(Long momentId) {
        List<Long> userIds = likeMapper.findLikeUserIds(momentId);
        return userIds.stream().map(uid -> {
            User user = userMapper.selectById(uid);
            Map<String, Object> map = new HashMap<>();
            map.put("userId", uid);
            if (user != null) {
                map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> addComment(Long momentId, Long userId, String content, Long replyTo) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("评论内容不能为空");
        }

        MomentComment comment = new MomentComment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setReplyTo(replyTo);
        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId());
        result.put("content", comment.getContent());
        result.put("replyTo", comment.getReplyTo());
        result.put("createdAt", comment.getCreatedAt());
        if (user != null) {
            result.put("nickname", user.getNickname());
            result.put("avatar", user.getAvatar());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getComments(Long momentId) {
        List<MomentComment> comments = commentMapper.findByMomentId(momentId);
        Set<Long> userIds = comments.stream().map(MomentComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userMapper.selectById(uid);
            if (u != null) userMap.put(uid, u);
        }

        return comments.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("content", c.getContent());
            map.put("replyTo", c.getReplyTo());
            map.put("createdAt", c.getCreatedAt());

            User u = userMap.get(c.getUserId());
            if (u != null) {
                map.put("userId", u.getId());
                map.put("nickname", u.getNickname());
                map.put("avatar", u.getAvatar());
            }

            // 如果回复了某条评论，带上被回复者的昵称
            if (c.getReplyTo() != null) {
                for (MomentComment parent : comments) {
                    if (parent.getId().equals(c.getReplyTo())) {
                        User replyUser = userMap.get(parent.getUserId());
                        if (replyUser != null) {
                            map.put("replyToNickname", replyUser.getNickname());
                        }
                        break;
                    }
                }
            }

            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        MomentComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人的评论");
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteMoment(Long momentId, Long userId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new RuntimeException("动态不存在");
        }
        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人的动态");
        }
        // 删除关联的点赞和评论
        LambdaQueryWrapper<MomentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(MomentLike::getMomentId, momentId);
        likeMapper.delete(likeWrapper);

        LambdaQueryWrapper<MomentComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(MomentComment::getMomentId, momentId);
        commentMapper.delete(commentWrapper);

        momentMapper.deleteById(momentId);
    }
}
