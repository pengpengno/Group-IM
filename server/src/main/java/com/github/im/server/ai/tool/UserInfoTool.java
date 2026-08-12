package com.github.im.server.ai.tool;

import com.github.im.dto.user.UserBasicInfo;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Legacy data adapter used by MCP handlers. */
@Component
public class UserInfoTool {
    private final UserService userService;

    public UserInfoTool(UserService userService) {
        this.userService = userService;
    }

    /** Returns directory-safe fields only: user id and username. */
    public String getUserInfo(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) return "请提供用户 ID、用户名或邮箱，例如：/user 张三";
        Optional<UserBasicInfo> exactMatch = findExactUser(normalized);
        if (exactMatch.isPresent()) return formatBasicInfo(exactMatch.get());

        Page<UserInfo> fuzzyMatches = userService.findUserByQueryStrings(normalized);
        if (fuzzyMatches.isEmpty()) return "没有找到匹配的用户：" + normalized;
        List<UserInfo> users = fuzzyMatches.getContent().stream().limit(5).toList();
        StringBuilder builder = new StringBuilder("找到以下用户：");
        for (int index = 0; index < users.size(); index++) {
            UserInfo user = users.get(index);
            builder.append(System.lineSeparator()).append(index + 1)
                    .append(". ID=").append(user.getUserId())
                    .append("，用户名=").append(safe(user.getUsername()));
        }
        if (fuzzyMatches.getTotalElements() > users.size()) {
            builder.append(System.lineSeparator()).append("结果较多，请提供更精确的用户名、邮箱或用户 ID。");
        }
        return builder.toString();
    }

    /** Sensitive data. Only the approval-protected MCP handler may invoke this. */
    public String getUserContactInfo(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) return "请提供要查询的用户 ID、用户名或邮箱，例如：/contact 张三";
        Optional<UserBasicInfo> exactMatch = findExactUser(normalized);
        if (exactMatch.isPresent()) {
            UserBasicInfo user = exactMatch.get();
            return "联系方式：" + System.lineSeparator()
                    + "用户ID：" + user.getUserId() + System.lineSeparator()
                    + "用户名：" + safe(user.getUsername()) + System.lineSeparator()
                    + "邮箱：" + safe(user.getEmail()) + System.lineSeparator()
                    + "手机号：" + safe(user.getPhoneNumber());
        }
        Page<UserInfo> fuzzyMatches = userService.findUserByQueryStrings(normalized);
        if (fuzzyMatches.isEmpty()) return "没有找到匹配的用户：" + normalized;
        UserInfo first = fuzzyMatches.getContent().get(0);
        return "找到候选用户：" + System.lineSeparator()
                + "用户ID：" + first.getUserId() + System.lineSeparator()
                + "用户名：" + safe(first.getUsername()) + System.lineSeparator()
                + "邮箱：" + safe(first.getEmail()) + System.lineSeparator()
                + "如需更精确结果，请提供完整用户名、邮箱或用户 ID。";
    }

    private Optional<UserBasicInfo> findExactUser(String query) {
        if (query.chars().allMatch(Character::isDigit)) return userService.findUserByUserId(Long.parseLong(query));
        return userService.findUserByUsername(query);
    }

    private String formatBasicInfo(UserBasicInfo user) {
        return "用户基础资料：" + System.lineSeparator()
                + "用户ID：" + user.getUserId() + System.lineSeparator()
                + "用户名：" + safe(user.getUsername());
    }

    private String safe(String value) { return value == null || value.isBlank() ? "未填写" : value; }
    private String normalize(String query) { return query == null ? "" : query.trim(); }
}
