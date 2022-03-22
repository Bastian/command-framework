/*
 * Copyright 2019-2022 Björn Kautler
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

package net.kautler.command.api.restriction.javacord.slash

import jakarta.inject.Inject
import net.kautler.command.api.CommandContext
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.interaction.SlashCommandInteraction
import org.jboss.weld.junit4.WeldInitiator
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject

class PrivateMessageJavacordSlashTest extends Specification {
    @Rule
    WeldInitiator weld = WeldInitiator
            .from(PrivateMessageJavacordSlash)
            .inject(this)
            .build()

    @Inject
    @Subject
    PrivateMessageJavacordSlash privateMessageJavacord

    CommandContext<SlashCommandInteraction> commandContext = Stub {
        it.message >> Stub(SlashCommandInteraction) {
            it.channel >> Optional.of(Stub(TextChannel))
        }
    }

    def 'private message "#privateMessage" should #be allowed'() {
        given:
            commandContext.message.channel.get().asPrivateChannel() >> (privateMessage ? commandContext.message.channel : Optional.empty())

        expect:
            privateMessageJavacord.allowCommand(commandContext) == allowed

        where:
            privateMessage || allowed | be
            true           || true    | 'be'
            false          || false   | 'not be'
    }
}
