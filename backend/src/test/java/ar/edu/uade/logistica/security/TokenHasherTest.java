package ar.edu.uade.logistica.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {
    private final TokenHasher hasher = new TokenHasher();

    @Test
    void producesStableSha256WithoutPersistingTheToken() {
        assertThat(hasher.sha256("token-secreto"))
                .isEqualTo("28946736960203f1dd24ad8f1ffc4ca11f01fd5213cf456929ccc3cbe7df74f0")
                .doesNotContain("token-secreto");
    }
}

