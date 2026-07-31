package com.keyfeed.keyfeedmonolithic.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingSpecTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src", "main", "java");

    private static final Pattern GET_MESSAGE_IN_LOG_CALL =
            Pattern.compile("log\\.(error|warn)\\s*\\([^;]*\\b(e|ex|t|\\w*(?:Exception|Error|Throwable))\\.getMessage\\(\\)");

    @Test
    @DisplayName("log.error/warn 호출에 getMessage() 대신 예외 객체를 마지막 인자로 전달한다")
    void 로그_호출에_예외_메시지_문자열을_전달하지_않음() throws IOException {
        assertThat(MAIN_SOURCE_ROOT).exists();

        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            List<Path> violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsGetMessageInLogCall)
                    .toList();

            assertThat(violations)
                    .as("log.error/warn에 getMessage()를 전달하면 스택트레이스가 유실됩니다. 예외 객체를 마지막 인자로 전달하세요: %s", violations)
                    .isEmpty();
        }
    }

    private boolean containsGetMessageInLogCall(Path path) {
        try {
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return GET_MESSAGE_IN_LOG_CALL.matcher(source).find();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
