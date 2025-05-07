package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.LinkedImageOps;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
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

import java.util.ArrayList;
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
}