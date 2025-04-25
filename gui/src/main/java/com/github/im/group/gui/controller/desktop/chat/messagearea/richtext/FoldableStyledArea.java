package com.github.im.group.gui.controller.desktop.chat.messagearea.richtext;

import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.LinkedImage;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.LinkedImageOps;
import com.github.im.group.gui.controller.desktop.chat.messagearea.richtext.image.StreamImage;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.*;
import org.reactfx.util.Either;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class FoldableStyledArea extends GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> {
    private final static TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final static LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();

    public FoldableStyledArea()
    {
        super(
            ParStyle.EMPTY,                                                 // default paragraph style
            (paragraph, style) -> paragraph.setStyle(style.toCss()),        // paragraph style setter
            TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),  // default segment style
            styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()),                            // segment operations
            seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter

        this.setWrapText(true);
        this.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC,
                                LinkedImage.codec()),
                        TextStyle.CODEC));
        this.setParagraphGraphicFactory( new BulletFactory( this ) );  // and folded paragraph indicator
    }

    private static Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
                                   BiConsumer<? super TextExt, TextStyle> applyStyle ) {
        return seg.getSegment().unify(
                text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                LinkedImage::createNode
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
     * Action listener which inserts a new image at the current caret position.
     */
    public void insertImage( Image image) {
        if (image != null) {
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros =
                    ReadOnlyStyledDocument.fromSegment(Either.right(new StreamImage(image)),
                            ParStyle.EMPTY, TextStyle.EMPTY, this.getSegOps());
            this.replaceSelection(ros);
        }
    }
}