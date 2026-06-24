package com.github.im.server.model

import jakarta.persistence.Column
import jakarta.persistence.Lob
import spock.lang.Specification

class MessageEntitySpec extends Specification {

    def "content column keeps text capacity for long structured payloads"() {
        given:
        def contentField = Message.getDeclaredField("content")
        def lobAnnotation = contentField.getAnnotation(Lob)
        def columnAnnotation = contentField.getAnnotation(Column)

        expect:
        // Meeting summaries and future rich system payloads are serialized as
        // JSON, so the message body must not silently regress to varchar(255).
        lobAnnotation != null
        columnAnnotation != null
        columnAnnotation.columnDefinition().toUpperCase().contains("TEXT")
    }
}
