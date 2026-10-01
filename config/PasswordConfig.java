package com.syncpoint.archive.config;

import com.syncpoint.archive.util.PasswordUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;


@Configuration
public class PasswordConfig {

    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    public static boolean isBCryptHash(String hash) {
        return hash != null && BCRYPT.matcher(hash).matches();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MigratingPasswordEncoder(new BCryptPasswordEncoder());
    }

    static class MigratingPasswordEncoder implements PasswordEncoder {

        private final PasswordEncoder bcrypt;

        MigratingPasswordEncoder(PasswordEncoder bcrypt) {
            this.bcrypt = bcrypt;
        }

        @Override
        public String encode(CharSequence rawPassword) {
            return bcrypt.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String storedHash) {
            if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
                return false;
            }
            if (isBCryptHash(storedHash)) {
                return bcrypt.matches(rawPassword, storedHash);
            }
            return matchesLegacy(rawPassword, storedHash);
        }

        @Override
        public boolean upgradeEncoding(String storedHash) {
            return !isBCryptHash(storedHash);
        }

        
        private boolean matchesLegacy(CharSequence rawPassword, String storedHash) {
            return PasswordUtil.verifyLegacyPassword(rawPassword.toString(), storedHash);
        }
    }
}
