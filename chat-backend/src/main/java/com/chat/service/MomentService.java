package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.entity.Moment;

import java.util.List;
import java.util.Map;

/**
 * 朋友圈服务接口
 */
public interface MomentService extends IService<Moment> {

    /**
     * 发布动态
     */
    Moment createMoment(Long userId, String content, String images, String location);

    /**
     * 获取好友动态列表（含点赞、评论信息）
     */
    List<Map<String, Object>> getFriendMoments(Long userId);

    /**
     * 点赞 / 取消点赞
     */
    Map<String, Object> toggleLike(Long momentId, Long userId);

    /**
     * 获取动态的点赞用户列表
     */
    List<Map<String, Object>> getLikes(Long momentId);

    /**
     * 发表评论
     */
    Map<String, Object> addComment(Long momentId, Long userId, String content, Long replyTo);

    /**
     * 获取动态的评论列表
     */
    List<Map<String, Object>> getComments(Long momentId);

    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 删除动态
     */
    void deleteMoment(Long momentId, Long userId);
}
