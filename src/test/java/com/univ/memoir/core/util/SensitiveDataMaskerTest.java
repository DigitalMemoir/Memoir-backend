package com.univ.memoir.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataMasker Unit Tests")
class SensitiveDataMaskerTest {

    @Test
    @DisplayName("should return null when input is null")
    void shouldReturnNullWhenInputIsNull() {
        // when
        String result = SensitiveDataMasker.mask(null);

        // then
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("should handle empty or whitespace strings")
    void shouldHandleEmptyOrWhitespaceStrings(String input) {
        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("should mask JWT Bearer token")
    void shouldMaskJwtBearerToken() {
        // given
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("Bearer ***");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    @DisplayName("should mask multiple Bearer tokens")
    void shouldMaskMultipleBearerTokens() {
        // given
        String input = "Token1: Bearer abc.def.ghi Token2: Bearer xyz.uvw.rst";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).isEqualTo("Token1: Bearer *** Token2: Bearer ***");
    }

    @Test
    @DisplayName("should mask JSON apiKey field")
    void shouldMaskJsonApiKeyField() {
        // given
        String input = "{\"apiKey\": \"sk-1234567890abcdef\", \"name\": \"test\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"apiKey\": \"***\"");
        assertThat(result).doesNotContain("sk-1234567890abcdef");
    }

    @Test
    @DisplayName("should mask JSON apiKey field case-insensitive")
    void shouldMaskJsonApiKeyFieldCaseInsensitive() {
        // given
        String input = "{\"ApiKey\": \"secret123\", \"APIKEY\": \"secret456\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"ApiKey\": \"***\"");
        assertThat(result).contains("\"APIKEY\": \"***\"");
    }

    @Test
    @DisplayName("should mask JSON authorization field")
    void shouldMaskJsonAuthorizationField() {
        // given
        String input = "{\"authorization\": \"Basic dXNlcjpwYXNz\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"authorization\": \"***\"");
        assertThat(result).doesNotContain("Basic dXNlcjpwYXNz");
    }

    @Test
    @DisplayName("should mask JSON accessToken field")
    void shouldMaskJsonAccessTokenField() {
        // given
        String input = "{\"accessToken\": \"at_1234567890\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"accessToken\": \"***\"");
        assertThat(result).doesNotContain("at_1234567890");
    }

    @Test
    @DisplayName("should mask JSON refreshToken field")
    void shouldMaskJsonRefreshTokenField() {
        // given
        String input = "{\"refreshToken\": \"rt_abcdefghijk\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"refreshToken\": \"***\"");
        assertThat(result).doesNotContain("rt_abcdefghijk");
    }

    @Test
    @DisplayName("should mask JSON email field")
    void shouldMaskJsonEmailField() {
        // given
        String input = "{\"email\": \"user@example.com\", \"name\": \"John\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"email\": \"***\"");
        assertThat(result).doesNotContain("user@example.com");
    }

    @Test
    @DisplayName("should mask password parameter with equals sign")
    void shouldMaskPasswordParameterWithEquals() {
        // given
        String input = "username=john&password=secret123&rememberMe=true";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("password=***");
        assertThat(result).doesNotContain("secret123");
        assertThat(result).contains("username=john");
    }

    @Test
    @DisplayName("should mask password parameter case-insensitive")
    void shouldMaskPasswordParameterCaseInsensitive() {
        // given
        String input = "PASSWORD=secret&Password=test";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("PASSWORD=***");
        assertThat(result).contains("Password=***");
    }

    @Test
    @DisplayName("should mask apiKey parameter with equals sign")
    void shouldMaskApiKeyParameterWithEquals() {
        // given
        String input = "apiKey=sk-1234567890&endpoint=api.example.com";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("apiKey=***");
        assertThat(result).doesNotContain("sk-1234567890");
    }

    @Test
    @DisplayName("should mask authorization parameter")
    void shouldMaskAuthorizationParameter() {
        // given
        String input = "authorization=Bearer token123";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("authorization=***");
        assertThat(result).doesNotContain("token123");
    }

    @Test
    @DisplayName("should mask accessToken parameter")
    void shouldMaskAccessTokenParameter() {
        // given
        String input = "accessToken=at_xyz789&userId=123";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("accessToken=***");
        assertThat(result).doesNotContain("at_xyz789");
    }

    @Test
    @DisplayName("should mask refreshToken parameter")
    void shouldMaskRefreshTokenParameter() {
        // given
        String input = "refreshToken=rt_abc456";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("refreshToken=***");
        assertThat(result).doesNotContain("rt_abc456");
    }

    @Test
    @DisplayName("should mask secret parameter")
    void shouldMaskSecretParameter() {
        // given
        String input = "secret=mySecretValue&public=publicData";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("secret=***");
        assertThat(result).doesNotContain("mySecretValue");
        assertThat(result).contains("public=publicData");
    }

    @Test
    @DisplayName("should mask email parameter")
    void shouldMaskEmailParameter() {
        // given
        String input = "email=user@example.com&name=John";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("email=***");
        assertThat(result).doesNotContain("user@example.com");
    }

    @Test
    @DisplayName("should mask multiple sensitive fields in complex JSON")
    void shouldMaskMultipleSensitiveFieldsInComplexJson() {
        // given
        String input = "{\"user\":{\"email\":\"admin@test.com\",\"password\":\"pass123\"},\"auth\":{\"accessToken\":\"at_abc\",\"refreshToken\":\"rt_xyz\"}}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"email\":\"***\"");
        assertThat(result).contains("password=***");
        assertThat(result).contains("\"accessToken\":\"***\"");
        assertThat(result).contains("\"refreshToken\":\"***\"");
        assertThat(result).doesNotContain("admin@test.com");
        assertThat(result).doesNotContain("pass123");
        assertThat(result).doesNotContain("at_abc");
        assertThat(result).doesNotContain("rt_xyz");
    }

    @Test
    @DisplayName("should preserve non-sensitive data")
    void shouldPreserveNonSensitiveData() {
        // given
        String input = "username=john&age=30&city=Seoul";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("should handle mixed Bearer token and JSON fields")
    void shouldHandleMixedBearerTokenAndJsonFields() {
        // given
        String input = "Authorization: Bearer abc.def.ghi {\"apiKey\":\"secret\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("Bearer ***");
        assertThat(result).contains("\"apiKey\":\"***\"");
    }

    @Test
    @DisplayName("should handle special characters in sensitive values")
    void shouldHandleSpecialCharactersInSensitiveValues() {
        // given
        String input = "password=p@ss!w0rd#123&email=test+user@example.co.kr";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("password=***");
        assertThat(result).contains("email=***");
        assertThat(result).doesNotContain("p@ss!w0rd#123");
        assertThat(result).doesNotContain("test+user@example.co.kr");
    }

    @Test
    @DisplayName("should handle JSON with spaces")
    void shouldHandleJsonWithSpaces() {
        // given
        String input = "{\"apiKey\" : \"secret123\", \"authorization\" : \"Bearer token\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("\"apiKey\" : \"***\"");
        assertThat(result).contains("\"authorization\" : \"***\"");
    }

    @Test
    @DisplayName("should handle URL query parameters")
    void shouldHandleUrlQueryParameters() {
        // given
        String input = "https://api.example.com/user?apiKey=sk-123&password=secret&name=john";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("apiKey=***");
        assertThat(result).contains("password=***");
        assertThat(result).contains("name=john");
        assertThat(result).doesNotContain("sk-123");
        assertThat(result).doesNotContain("secret");
    }

    @Test
    @DisplayName("should handle long JWT token correctly")
    void shouldHandleLongJwtTokenCorrectly() {
        // given
        String input = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjk5OTk5OTk5LCJleHAiOjE3MDAwMDAwMDB9.very-long-signature-here";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).isEqualTo("Bearer ***");
    }

    @Test
    @DisplayName("should not mask fields that are not sensitive")
    void shouldNotMaskFieldsThatAreNotSensitive() {
        // given
        String input = "{\"username\":\"john\",\"role\":\"admin\",\"createdAt\":\"2024-01-01\"}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("should handle malformed JSON gracefully")
    void shouldHandleMalformedJsonGracefully() {
        // given
        String input = "{\"password\":\"secret\", malformed json";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("password=***");
        assertThat(result).doesNotContain("secret");
    }

    @Test
    @DisplayName("should handle single quotes in parameter values")
    void shouldHandleSingleQuotesInParameterValues() {
        // given
        String input = "password='secret123'&apiKey='sk-abc'";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("password=***");
        assertThat(result).contains("apiKey=***");
    }

    @Test
    @DisplayName("should handle colon separator in parameters")
    void shouldHandleColonSeparatorInParameters() {
        // given
        String input = "{password: secret123, apiKey: sk-abc}";

        // when
        String result = SensitiveDataMasker.mask(input);

        // then
        assertThat(result).contains("password: ***");
        assertThat(result).contains("apiKey: ***");
    }
}