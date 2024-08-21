package playkosmos.utils;

import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[!@#$%^&*])(?=.*\\d).{8,}$");

    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    public static CompletableFuture<ValidationResult> validateUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (username == null || username.length() <= 3) {
                return new ValidationResult(false, "Username must be at least 3 characters long.\n");
            }
            return new ValidationResult(true, "Username is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validateEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                return new ValidationResult(false, "Email is not valid.\n");
            }
            return new ValidationResult(true, "Email is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validatePhoneNumber(String phoneNumber) {
        return CompletableFuture.supplyAsync(() -> {
            if (phoneNumber == null || !PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
                return new ValidationResult(false, "Phone number is not valid.\n");
            }
            return new ValidationResult(true, "Phone number is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validateDateOfBirth(LocalDate DOB) {
        return CompletableFuture.supplyAsync(() -> {
            if (DOB == null || Period.between(DOB, LocalDate.now()).getYears() < 16) {
                return new ValidationResult(false, "You must be at least 16 years old.\n");
            }
            return new ValidationResult(true, "Date of Birth is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validatePassword(String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
                return new ValidationResult(false, "Password must be at least 8 characters long, contain an uppercase letter, a special character, and a digit.\n");
            }
            return new ValidationResult(true, "Password is valid.");
        });
    }
}
