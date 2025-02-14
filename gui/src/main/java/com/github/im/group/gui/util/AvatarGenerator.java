package com.github.im.group.gui.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/***
 * AvatarGenerator.java
 * used to generate avatar image
 * input  text   return image .
 */
public class AvatarGenerator {

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
        gc.setFont(new Font("Arial", size / 2)); // 字体大小
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 绘制文字
        gc.fillText(displayText, size / 2, size / 2);

        // 返回生成的图像
        return canvas.snapshot(null, null);
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

    public static Image generateCircleAvatar(String name, double size) {
        return generateCircleAvatar(name, size, null);
    }

    public static Image generateSquareAvatarWithRoundedCorners(String name, double size) {
        return generateSquareAvatarWithRoundedCorners(name, size, null);
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
}
