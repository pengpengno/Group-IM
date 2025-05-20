package com.github.im.group.gui.util;

import com.gluonhq.charm.glisten.control.Avatar;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.Getter;

/***
 * AvatarGenerator.java
 * used to generate avatar image
 * input  text   return image .
 */
public class AvatarGenerator {

    // 头像的缓存处理， 每次根据name 生成一次 ，优先从缓存中读取，不存在啧创建
    private static final java.util.Map<String, Image> avatarCache = new java.util.HashMap<>();
    private static final int CACHE_SIZE = 100;


    public static Image generateCircleAvatarCache(String name, double size, Color color) {
        if (avatarCache.containsKey(name)) {
            return avatarCache.get(name);
        } else {
            Image avatar = generateCircleAvatar(name, size, color);
            avatarCache.put(name, avatar);
            if (avatarCache.size() > CACHE_SIZE) {
                // 移除最旧的缓存项
                String oldestKey = avatarCache.keySet().iterator().next();
                avatarCache.remove(oldestKey);
            }
            return avatar;
        }

    }

        // 圆形头像
    public static Image generateCircleAvatar(String name, double size, Color color) {
        // 解析名字
        String displayText = getAvatarText(name);

        // 创建画布
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, size, size); // 清除画布，确保背景透明

        Paint backgroundColor = color;

        // 设置背景颜色
        if (backgroundColor == null) {
            backgroundColor = getRandomColor();
        }

        // 绘制圆形背景
        gc.setFill(backgroundColor);
        gc.fillOval(0, 0, size, size); // 绘制圆形背景

        // 设置文字样式
        gc.setFill(Color.WHITE); // 文字颜色
        Font arial = Font.font("Arial", FontWeight.BOLD, size);

        gc.setFont(arial); // 字体大小
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 绘制文字
        gc.fillText(displayText, size / 2, size / 2);

        // 返回生成的图像
        return canvas.snapshot(null, null);
    }

    private static Image generateSquareAvatarWithRoundedCornersCache(String name, double size, Color color) {
        if (avatarCache.containsKey(name)) {
            return avatarCache.get(name);
        } else {
            Image avatar = generateSquareAvatarWithRoundedCorners(name, size, color);
            avatarCache.put(name, avatar);
            if (avatarCache.size() > CACHE_SIZE) {
                // 移除最旧的缓存项
                String oldestKey = avatarCache.keySet().iterator().next();
                avatarCache.remove(oldestKey);
            }
            return avatar;
        }
    }
    // 正方形圆角头像

    public static Image generateSquareAvatarWithRoundedCorners(String name, double size, Color color) {
        // 解析名字
        String displayText = getAvatarText(name);

        // 创建画布
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, size, size); // 清除画布，确保背景透明

        Paint backgroundColor = color;

        // 设置背景颜色
        if (backgroundColor == null) {
            backgroundColor = getRandomColor();
        }

        // 绘制正方形圆角背景
        double cornerRadius = size / 5; // 设置圆角半径
        gc.setFill(backgroundColor);
        gc.fillRoundRect(0, 0, size, size, cornerRadius, cornerRadius); // 绘制圆角矩形背景

        // 设置文字样式
        gc.setFill(Color.WHITE); // 文字颜色
        gc.setFont(new Font("Arial", size / 2)); // 字体大小
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 绘制文字
        gc.fillText(displayText, size / 2, size / 2);

        // 返回生成的图像
        return canvas.snapshot(null, null);
    }

    /**
     * 生成圆形头像的方法
     * 此方法重载了另一个具有更多参数的generateCircleAvatar方法，提供了一个简化版本的接口
     * 使用此方法时，可以选择不提供第三个参数，此时将使用默认设置生成头像
     * 主要用途是创建具有指定名称和大小的圆形头像，适用于需要简化头像生成过程的场景
     *
     * @param name 头像显示的名称，通常用于标识用户或角色
     * @param size 头像的大小，表示直径长度
     * @return 返回生成的圆形头像对象
     */
    public static Image generateCircleAvatar(String name, AvatarSize size) {
        return generateCircleAvatarCache(name, size.getSize(), null);
    }
    public static Image generateCircleAvatar(String name, double size) {
        return generateCircleAvatarCache(name, size, null);
    }


    /**
     * 生成头像
     * @param name
     * @param size
     * @return
     */
    public static Avatar getAvatar(String name, double size) {
        var image = generateSquareAvatarWithRoundedCornersCache(name, size, null);
        return new Avatar(size, image);

    }

    /**
     * 生成一个带有圆角的正方形头像
     * 此方法重载了另一个具有更多参数的方法，提供了一个简化版本的接口
     * 使用场景例如：当需要根据用户名称快速生成一个固定样式的头像时
     *
     * @param name 用户名，将显示在头像上
     * @param size 头像的大小，单位为像素
     * @return 返回一个带有圆角的正方形头像对象
     */
    public static Image generateSquareAvatarWithRoundedCorners(String name, double size) {
        return generateSquareAvatarWithRoundedCornersCache(name, size, null);
    }

    private static String getAvatarText(String name) {
        if (name == null || name.isEmpty()) {
            return "?"; // 默认文本
        }
        if (name.length() <= 2) {
            return name;
        }

        // 中文名: 取前两个字符
        if (name.matches("[\\u4e00-\\u9fa5]+")) {
            if (name.length() == 3){
                // 三个字 得  取名
                return name.substring(1, 3);
            }
            return name.substring(0, 2);
        }

        // 英文名: 取首字母和第二个字母
        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    private static Paint getRandomColor() {
        // 随机背景色
        Color[] colors = {
                Color.web("#F44336"), Color.web("#E91E63"), Color.web("#9C27B0"),
                Color.web("#673AB7"), Color.web("#3F51B5"), Color.web("#2196F3"),
                Color.web("#03A9F4"), Color.web("#00BCD4"), Color.web("#009688"),
                Color.web("#4CAF50"), Color.web("#8BC34A"), Color.web("#CDDC39"),
                Color.web("#FFEB3B"), Color.web("#FFC107"), Color.web("#FF9800"),
                Color.web("#FF5722"), Color.web("#795548"), Color.web("#607D8B")
        };

        return colors[(int) (Math.random() * colors.length)];
    }

    public enum AvatarType {
        CIRCLE,
        SQUARE
    }

    @Getter
    public enum AvatarSize {
        SMALL(20),
        MEDIUM(50),
        LARGE(70);

        private final int size;

        AvatarSize(int size) {
            this.size = size;
        }

    }
}
