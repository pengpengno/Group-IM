//package com.github.im.common.reactor.mono
//
//import reactor.core.publisher.Mono
//import spock.lang.Specification
//import spock.lang.Subject
//
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//
//class MonoShareSpec extends Specification {
//
//    private static final Logger log = LoggerFactory.getLogger(MonoShareSpec)
//
//    def "测试 Mono.share 的行为"() {
//        given:
//        def share = Mono.just("test")
//                .doOnNext { log.info("share --- {}", it) }
//                .share()
//
//        def test = Mono.just("test")
//                .doOnNext { log.info("test --- {}", it) }
//
//        when:
//        log.info("test =======")
//        test.subscribe()
//        test.subscribe()
//
//        log.info("share =======")
//        share.subscribe()
//        share.subscribe()
//
//        then:
//        noExceptionThrown()
//    }
//
//    def "测试 Mono.publish 的行为"() {
//        given:
//        def publish = Mono.just("test")
//                .doOnNext { log.info("publish --- {}", it) }
//                .publish { mo -> mo.map { it + "add String" } }
//                .doOnNext { log.info("publish --- {}", it) }
//
//        def map = Mono.just("test")
//                .doOnNext { log.info("publish --- {}", it) }
//                .publish { mo -> mo.map { it + "add String" } }
//                .map { it + "add String" }
//
//        def test = Mono.just("test")
//                .doOnNext { log.info("test --- {}",
