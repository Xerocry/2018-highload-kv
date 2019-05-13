/*
 * Copyright 2018 (c) Vadim Tsesko <incubos@yandex.com>
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

package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains utility methods for unit tests
 *
 * @author Vadim Tsesko <incubos@yandex.com>
 */
abstract class TestBase {
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();
    private static final int KEY_LENGTH = 16;
    private static final int VALUE_LENGTH = 1024;

    static int randomPort() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0), 1);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Can't discover a free port", e);
        }
    }

    @NotNull
    static String randomId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    @NotNull
    static byte[] randomKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for(int i = 0; i < KEY_LENGTH; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString().getBytes();
    }

    @NotNull
    static byte[] randomValue() {
        final byte[] result = new byte[VALUE_LENGTH];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }

    @NotNull
    static String endpoint(final int port) {
        return "http://localhost:" + port;
    }
}
