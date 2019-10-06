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

package net.kautler.test.protocol.testproperties

import static java.nio.charset.StandardCharsets.UTF_8

class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        new URLConnection(url) {
            @Override
            InputStream getInputStream() throws IOException {
                if (url.path == 'IOException') {
                    throw new IOException()
                }
                def properties = URLDecoder.decode(url.path, UTF_8.name())
                new ByteArrayInputStream(properties.bytes)
            }

            @Override
            void connect() {
            }
        }
    }
}
