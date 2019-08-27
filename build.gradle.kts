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

plugins {
    net.kautler.idea
    net.kautler.versions
    net.kautler.java
    net.kautler.jars
    net.kautler.antlr
    net.kautler.osgi
    net.kautler.tests
    net.kautler.codenarc
    net.kautler.pmd
    net.kautler.spotbugs
    net.kautler.java9
    net.kautler.javadoc
    net.kautler.readme
    net.kautler.publishing
}

defaultTasks("build")
tasks.register("generate")
