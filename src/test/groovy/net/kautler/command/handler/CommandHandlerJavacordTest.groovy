/*
 * Copyright 2019 Björn Kautler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kautler.command.handler

import net.kautler.command.Internal
import net.kautler.command.LoggerProducer
import net.kautler.command.api.Command
import net.kautler.command.api.event.javacord.CommandNotAllowedEventJavacord
import net.kautler.command.api.event.javacord.CommandNotFoundEventJavacord
import net.kautler.command.api.prefix.PrefixProvider
import net.kautler.command.api.restriction.Restriction
import net.kautler.command.api.restriction.RestrictionChainElement
import net.kautler.test.ContextualInstanceCategory
import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.Message
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.listener.message.MessageCreateListener
import org.javacord.api.util.concurrent.ThreadPool
import org.javacord.core.DiscordApiImpl
import org.jboss.weld.junit.MockBean
import org.jboss.weld.junit4.WeldInitiator
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject
import spock.util.mop.Use

import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.ObservesAsync
import javax.enterprise.inject.Instance
import javax.enterprise.util.AnnotationLiteral
import javax.enterprise.util.TypeLiteral
import javax.inject.Inject
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

import static java.lang.Thread.currentThread
import static java.util.Arrays.asList
import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.SECONDS
import static org.apache.logging.log4j.Level.ERROR
import static org.apache.logging.log4j.Level.INFO
import static org.apache.logging.log4j.test.appender.ListAppender.getListAppender

class CommandHandlerJavacordTest extends Specification {
    DiscordApi discordApi = Mock()

    DiscordApi discordApiInCollection1 = Stub()

    DiscordApi discordApiInCollection2 = Stub()

    Restriction<Object> restriction = Stub {
        allowCommand(_) >> false
    }

    Command<Object> command = Stub {
        it.aliases >> ['test']
        it.restrictionChain >> new RestrictionChainElement(Restriction)
    }

    PrefixProvider<Object> defaultPrefixProvider = Stub {
        getCommandPrefix(_) >> '!'
    }

    TestEventReceiver testEventReceiverDelegate = Mock()

    @Rule
    WeldInitiator weld = WeldInitiator
            .from(
                    CommandHandlerJavacord,
                    LoggerProducer,
                    TestEventReceiver
            )
            .addBeans(
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .types(DiscordApi)
                            .creating(discordApi)
                            .build(),
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .types(new TypeLiteral<Collection<DiscordApi>>() { }.type)
                            .creating(asList(discordApiInCollection1, discordApiInCollection2))
                            .build(),
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .types(new TypeLiteral<Restriction<Object>>() { }.type)
                            .creating(restriction)
                            .build(),
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .types(new TypeLiteral<Command<Object>>() { }.type)
                            .creating(command)
                            .build(),
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .qualifiers(new AnnotationLiteral<Internal>() { })
                            .types(new TypeLiteral<PrefixProvider<Object>>() { }.type)
                            .creating(defaultPrefixProvider)
                            .build(),
                    MockBean.builder()
                            .scope(ApplicationScoped)
                            .qualifiers(new AnnotationLiteral<Internal>() { })
                            .types(TestEventReceiver)
                            .creating(testEventReceiverDelegate)
                            .build()
            )
            .inject(this)
            .build()

    @Inject
    @Subject
    CommandHandlerJavacord commandHandlerJavacord

    @Inject
    Instance<DiscordApi> discordApiInstance

    @Inject
    Instance<Collection<DiscordApi>> discordApiCollectionInstance

    Message message = Stub()

    MessageCreateEvent messageCreateEvent = Stub {
        it.message >> message
    }

    def 'an injector method for available restrictions should exist and forward to the common base class'() {
        given:
            CommandHandlerJavacord commandHandlerJavacord = Spy(useObjenesis: true)
            Instance<Restriction<? super Message>> availableRestrictions = Stub()

        when:
            def restrictionsInjectors = CommandHandlerJavacord
                    .declaredMethods
                    .findAll {
                        it.getAnnotation(Inject) &&
                                it.genericParameterTypes ==
                                [new TypeLiteral<Instance<Restriction<? super Message>>>() { }.type] as Type[]
                    }
                    .each { it.accessible = true }

        then:
            restrictionsInjectors.size() == 1

        when:
            restrictionsInjectors[0].invoke(commandHandlerJavacord, availableRestrictions)

        then:
            1 * commandHandlerJavacord.doSetAvailableRestrictions(availableRestrictions) >> { }
    }

    def 'an injector method for commands should exist and forward to the common base class'() {
        given:
            CommandHandlerJavacord commandHandlerJavacord = Spy(useObjenesis: true)
            Instance<Command<? super Message>> commands = Stub()

        when:
            def commandsInjectors = CommandHandlerJavacord
                    .declaredMethods
                    .findAll {
                        it.getAnnotation(Inject) &&
                                it.genericParameterTypes ==
                                [new TypeLiteral<Instance<Command<? super Message>>>() { }.type] as Type[]
                    }
                    .each { it.accessible = true }

        then:
            commandsInjectors.size() == 1

        when:
            commandsInjectors[0].invoke(commandHandlerJavacord, commands)

        then:
            1 * commandHandlerJavacord.doSetCommands(commands) >> { }
    }

    def 'an injector method for custom prefix provider should exist and forward to the common base class'() {
        given:
            CommandHandlerJavacord commandHandlerJavacord = Spy(useObjenesis: true)
            Instance<PrefixProvider<? super Message>> customPrefixProvider = Stub()

        when:
            def prefixProvidersInjectors = CommandHandlerJavacord
                    .declaredMethods
                    .findAll {
                        it.getAnnotation(Inject) &&
                                it.genericParameterTypes ==
                                [new TypeLiteral<Instance<PrefixProvider<? super Message>>>() { }.type] as Type[]
                    }
                    .each { it.accessible = true }

        then:
            prefixProvidersInjectors.size() == 1

        when:
            prefixProvidersInjectors[0].invoke(commandHandlerJavacord, customPrefixProvider)

        then:
            1 * commandHandlerJavacord.doSetCustomPrefixProvider(customPrefixProvider) >> { }
    }

    @Use(ContextualInstanceCategory)
    def 'post construct method should register message create listener and forward to the common base class'() {
        given:
            def commandHandlerJavacord = Spy(commandHandlerJavacord.ci())

        when:
            CommandHandlerJavacord
                    .declaredMethods
                    .findAll { it.getAnnotation(PostConstruct) }
                    .each {
                        it.accessible = true
                        it.invoke(commandHandlerJavacord)
                    }

        then:
            1 * discordApi.addMessageCreateListener(_) >> { MessageCreateListener listener ->
                listener.onMessageCreate(messageCreateEvent)
            }
            1 * commandHandlerJavacord.doHandleMessage(message, message.content) >> { }
            0 * commandHandlerJavacord.doHandleMessage(*_)
    }

    @Use(ContextualInstanceCategory)
    def 'injected discord apis should be logged properly [discordApisUnsatisfied: #discordApisUnsatisfied, discordApiCollectionsUnsatisfied: #discordApiCollectionsUnsatisfied]'() {
        given:
            commandHandlerJavacord.ci().with {
                it.discordApis = Spy(discordApiInstance)
                it.discordApis.unsatisfied >> discordApisUnsatisfied

                it.discordApiCollections = Spy(discordApiCollectionInstance)
                it.discordApiCollections.unsatisfied >> discordApiCollectionsUnsatisfied
            }

        and:
            // clear the appender here additionally
            // to get rid of log messages from container startup
            def testAppender = getListAppender('Test Appender')
            testAppender.clear()

        when:
            CommandHandlerJavacord
                    .declaredMethods
                    .findAll { it.getAnnotation(PostConstruct) }
                    .each {
                        it.accessible = true
                        it.invoke(commandHandlerJavacord.ci())
                    }

        then:
            testAppender
                    .events
                    .findAll { it.level == INFO }
                    .any { it.message.formattedMessage == expectedMessage }

        where:
            discordApisUnsatisfied | discordApiCollectionsUnsatisfied || expectedMessage
            true                   | true                             || 'No DiscordApi or Collection<DiscordApi> injected, JavacordCommandHandler will not be used.'
            true                   | false                            || 'Collection<DiscordApi> injected, JavacordCommandHandler will be used.'
            false                  | true                             || 'DiscordApi injected, JavacordCommandHandler will be used.'
            false                  | false                            || 'DiscordApi and Collection<DiscordApi> injected, JavacordCommandHandler will be used.'
    }

    @Use(ContextualInstanceCategory)
    def 'command not allowed event should be fired on restricted command'() {
        given:
            message.content >> '!test'
            def countDownLatch = new CountDownLatch(1)

        when:
            commandHandlerJavacord.ci().handleMessage(messageCreateEvent)
            countDownLatch.await(5, SECONDS)

        then:
            with(testEventReceiverDelegate) {
                1 * handleCommandNotAllowedEvent {
                    it.message == this.message
                    it.prefix == '!'
                    it.usedAlias == this.command.aliases[0]
                } >> { countDownLatch.countDown() }
                0 * _
            }
    }

    @Use(ContextualInstanceCategory)
    def 'message with correct prefix but wrong trigger should fire command not found event'() {
        given:
            message.content >> '!nocommand'
            def countDownLatch = new CountDownLatch(1)

        when:
            commandHandlerJavacord.ci().handleMessage(messageCreateEvent)
            countDownLatch.await(5, SECONDS)

        then:
            with(testEventReceiverDelegate) {
                1 * handleCommandNotFoundEvent {
                    it.message == this.message
                    it.prefix == '!'
                    it.usedAlias == 'nocommand'
                } >> { countDownLatch.countDown() }
                0 * _
            }
    }

    def 'execute async should use executor service of Javacord'() {
        given:
            ThreadPool threadPool = Mock()
            message.api >> Stub(DiscordApi) {
                it.threadPool >> threadPool
            }

        when:
            commandHandlerJavacord.executeAsync(message) { }

        then:
            1 * threadPool.executorService >> Stub(ExecutorService)
    }

    def 'asynchronous command execution should happen asynchronously'() {
        given:
            def discordApi = new DiscordApiImpl(null, null, null, null, false)
            message.api >> discordApi
            def threadFuture = new CompletableFuture()

        when:
            commandHandlerJavacord.executeAsync(message) {
                threadFuture.complete(currentThread())
            }

        then:
            threadFuture.get(5, SECONDS) != currentThread()

        cleanup:
            discordApi?.disconnect()
    }

    def 'asynchronous command execution should not log an error if none happened'() {
        given:
            def discordApi = new DiscordApiImpl(null, null, null, null, false)
            message.api >> discordApi

        when:
            commandHandlerJavacord.executeAsync(message) { }

        and:
            discordApi.disconnect()
            discordApi.threadPool.executorService.awaitTermination(Long.MAX_VALUE, DAYS)

        then:
            getListAppender('Test Appender')
                    .events
                    .findAll { it.level == ERROR }
                    .empty

        cleanup:
            discordApi?.disconnect()
    }

    def 'exception during asynchronous command execution should be logged properly'() {
        given:
            def discordApi = new DiscordApiImpl(null, null, null, null, false)
            message.api >> discordApi
            def exception = new Exception()

        when:
            commandHandlerJavacord.executeAsync(message) { throw exception }

        and:
            discordApi.disconnect()
            discordApi.threadPool.executorService.awaitTermination(Long.MAX_VALUE, DAYS)

        then:
            getListAppender('Test Appender')
                    .events
                    .findAll { it.level == ERROR }
                    .any {
                        (it.message.formattedMessage == 'Exception while executing command asynchronously') &&
                                ((it.thrown == exception) || (it.thrown.cause == exception))
                    }

        cleanup:
            discordApi?.disconnect()
    }

    @ApplicationScoped
    static class TestEventReceiver {
        @Inject
        @Internal
        TestEventReceiver delegate

        void handleCommandNotAllowedEvent(@ObservesAsync CommandNotAllowedEventJavacord commandNotAllowedEvent) {
            delegate.handleCommandNotAllowedEvent(commandNotAllowedEvent)
        }

        void handleCommandNotFoundEvent(@ObservesAsync CommandNotFoundEventJavacord commandNotFoundEvent) {
            delegate.handleCommandNotFoundEvent(commandNotFoundEvent)
        }
    }
}
