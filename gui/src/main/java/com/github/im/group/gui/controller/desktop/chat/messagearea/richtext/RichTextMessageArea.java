package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.LinkedImageOps;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import com.gluonhq.charm.glisten.application.AppManager;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.*;
import org.reactfx.util.Either;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 富文本 消息 文本编辑框
 */
public class RichTextMessageArea extends GenericStyledArea<ParStyle, Either<String, MessageNode>, TextStyle> {
    private final static TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final static LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();


    @Getter
    private final static int fontSize = 12;

    @Getter
    private final static Font font = Font.font("Serif", fontSize);

    public RichTextMessageArea()
    {
        super(
            ParStyle.EMPTY,                                                 // default paragraph style
            (paragraph, style) -> paragraph.setStyle(style.toCss()),        // paragraph style setter
            TextStyle.EMPTY.updateFontSize(fontSize).updateFontFamily(font.getFamily()).updateTextColor(Color.BLACK),  // default segment style
            styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()),                            // segment operations
            seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter

        this.setWrapText(true);
        this.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC,
                                MessageNode.codec()),
                        TextStyle.CODEC));

        this.setParagraphGraphicFactory( new BulletFactory( this ) );  // and folded paragraph indicator
        this.setAutoHeight(true);

    }

    @Override
    public void setEditable(boolean value) {
        super.setEditable(value);
        this.heightProperty().addListener((obs, oldH, newH) -> {
            double contentHeight = this.computePrefHeight();
            double paddingTop = Math.max(0, (newH.doubleValue() - contentHeight) / 2);
            this.setPadding(new Insets(paddingTop, 0, paddingTop, 0));
        });
    }

    private static Node createNode(StyledSegment<Either<String, MessageNode>, TextStyle> seg,
                                   BiConsumer<? super TextExt, TextStyle> applyStyle ) {
        return seg.getSegment().unify(
                text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                MessageNode::createNode
        );
    }

    public void foldParagraphs( int startPar, int endPar ) {
        foldParagraphs( startPar, endPar, getAddFoldStyle() );
    }

    public void foldSelectedParagraphs() {
        foldSelectedParagraphs( getAddFoldStyle() );
    }

    public void foldText( int start, int end ) {
        fold( start, end, getAddFoldStyle() );
    }

    public void unfoldParagraphs( int startingFromPar ) {
        unfoldParagraphs( startingFromPar, getFoldStyleCheck(), getRemoveFoldStyle() );
    }

    public void unfoldText( int startingFromPos ) {
        startingFromPos = offsetToPosition( startingFromPos, Bias.Backward ).getMajor();
        unfoldParagraphs( startingFromPos, getFoldStyleCheck(), getRemoveFoldStyle() );
    }

    protected UnaryOperator<ParStyle> getAddFoldStyle() {
        return pstyle -> pstyle.updateFold( true );
    }

    protected UnaryOperator<ParStyle> getRemoveFoldStyle() {
        return pstyle -> pstyle.updateFold( false );
    }

    protected Predicate<ParStyle> getFoldStyleCheck() {
        return pstyle -> pstyle.isFolded();
    }


    /**
     * 设置背景色
     */
    public void bg(){
        var instance = AppManager.getInstance();
        Background background;
        BackgroundFill backgroundFill;
        if (instance != null){
            var fill = instance.getAppBar().getBackground().getFills().get(0).getFill();
            backgroundFill = new BackgroundFill(
                    fill, // 蓝色
                    new CornerRadii(15),  // 圆角半径
                    Insets.EMPTY
            );
        }else{
            backgroundFill = new BackgroundFill(
                    Color.BLUE, // 蓝色
                    new CornerRadii(15),  // 圆角半径
                    Insets.EMPTY
            );
        }

        background = new Background(backgroundFill);
        this.setBackground(background);
    }

    /**
     * 插入文件
     * @param node  消息节点
     */
    public void insertNode(MessageNode node) {
        if (node != null) {
            Platform.runLater(()-> {
                ReadOnlyStyledDocument<ParStyle, Either<String, MessageNode>, TextStyle> ros =
                        ReadOnlyStyledDocument.fromSegment(Either.right(node),
                                ParStyle.EMPTY, TextStyle.EMPTY, this.getSegOps());
                this.replaceSelection(ros);
            });


        }
    }


    /***
     * 根据富文本中的内容自动计算 其高度
     * @return 返回prefHeight
     */
    public double computePrefHeight(){
        final var textArea = this;

        Font font = RichTextMessageArea.getFont();
        double lineHeight = Toolkit.getToolkit()
                .getFontLoader()
                .getFontMetrics(font)
                .getLineHeight();

        double width = textArea.getWidth() > 0 ? textArea.getWidth() : 400;

        Text helper = new Text();
        helper.setFont(font);
        helper.setWrappingWidth(width);
        new Scene(new Group(helper)); // 必须放入 Scene 才能正确计算
        helper.applyCss();

        var doc = textArea.getDocument();
        List<Either<String, MessageNode>> segments = new ArrayList<>();
        doc.getParagraphs().forEach(par -> segments.addAll(par.getSegments()));
        if (segments.isEmpty()) {
            return 0;
        }
        if(segments.size() ==1){
            // 当段落为1 的时候 且为文件类型
            var stringMessageNodeEither = segments.get(0);
            var isFileNode = stringMessageNodeEither.isRight();
            if(isFileNode){
                var messageNode = stringMessageNodeEither.getRight();
                var fileType = messageNode.getType();
                if(fileType == Chat.MessageType.IMAGE){
                    return 100;
                }
            }
        }
        int wrapLines = 0;
        for (Paragraph<?, ?, ?> paragraph : textArea.getParagraphs()) {

            String text = paragraph.getText();
            if (text.isEmpty()) {
                wrapLines += 1;
                continue;
            }

            helper.setText(text);
            double paraHeight = helper.getLayoutBounds().getHeight();
            wrapLines += Math.max(1, (int) Math.ceil(paraHeight / lineHeight));
        }

        // 5. 设置最大高度限制（如不超过 300px）
        double maxHeight = 300;

        double totalHeight = wrapLines * lineHeight + 10; // padding 可调整
        return Math.min(totalHeight, maxHeight);
    }


    /**
     * 获取区域得大小
     * 最大宽度为 400  ，多出得就要开始换行展示
     * @return
     */
    public Tuple2<Double,Double> calculateAreaSize(){
        double prefHeight = computePrefHeight();
        return Tuples.of(400d,prefHeight);
    }


    /**
     * 根据给定的宽度计算出 区域 应有的高度
     * 1. 首先获取组件内容类型
     *   a) 文本类型  ： 获取其字体样式 ，根据 大小 以及宽度计算得出高度
     *   b) 文件、图像类型  ： 首先获取图像的原始高度 ， 文件图像的数量 ，然后根据 宽度与高度计算出 缩放比例
     *   ...
     * 2. 根据组件类型返回 自适应的高度
     *
     * @param width 指定的最大宽度
     * @return 组件自适应的 Height  max 值 为 400 ， min值为 字体大小
     */
    public Long calculateAreaHeight(Long width){
        // 默认最小高度
        double minHeight = fontSize + 6;
        double maxHeight = 400;
        double totalHeight = 0;

        Font font = RichTextMessageArea.getFont();
        double lineHeight = Toolkit.getToolkit()
                .getFontLoader()
                .getFontMetrics(font)
                .getLineHeight();

        Text helper = new Text();
        helper.setFont(font);
        helper.setWrappingWidth(width);
        new Scene(new Group(helper));
        helper.applyCss();

        var segments = collectMessageNodes();
        if (segments.isEmpty()) {
            return (long) minHeight;
        }

        for (Either<String, MessageNode> segment : segments) {
            if (segment.isLeft()) {
                // 普通文本
                String text = segment.getLeft();
                if (text == null || text.isEmpty()) {
                    totalHeight += lineHeight;
                    continue;
                }
                helper.setText(text);
                helper.applyCss();
                double textHeight = helper.getLayoutBounds().getHeight();
                totalHeight += Math.max(lineHeight, textHeight);
            } else {
                MessageNode node = segment.getRight();
                Chat.MessageType type = node.getType();
                switch (type) {
                    case IMAGE -> {
                        // 获取图像原始尺寸
                        if(node instanceof StreamImage streamImage){
                            var image = streamImage.getImage();
                            if (image != null) {
                                double origWidth = image.getWidth();
                                double origHeight = image.getHeight();
                                // 缩放比例计算
                                double scale = Math.min(1.0, width / origWidth);
                                totalHeight += origHeight * scale + 6; // 加些 margin
                            } else {
                                totalHeight += 100; // 默认图片高度
                            }
                        }

                    }
                    case FILE -> {
                        totalHeight += 40; // 默认文件消息高度
                    }
                    default -> {
                        totalHeight += 30; // 默认其他节点高度
                    }
                }
            }
        }

        totalHeight += 10; // padding
        totalHeight = Math.min(totalHeight, maxHeight);
        return (long) Math.max(totalHeight, minHeight);
    }


    /**
     * 获取组件中的消息体
     * 获取 MessageNode
     * @return 返回 MessageNode
     */
    public List<Either<String, MessageNode>> collectMessageNodes() {
        var doc = this.getDocument();
        List<Either<String, MessageNode>> segments = new ArrayList<>();
        doc.getParagraphs().forEach(par -> segments.addAll(par.getSegments()));
        if (segments.isEmpty()) {
            return Collections.emptyList();
        }
        return segments;
    }

}