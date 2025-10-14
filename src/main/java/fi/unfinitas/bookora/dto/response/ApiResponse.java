package fi.unfinitas.bookora.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper for consistent response format.
 *
 * @param <T> the type of data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;

    private String message;

    private T data;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Create a success response with data.
     */
    public static <T> ApiResponse<T> success(final String message, final T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a success response without data.
     */
    public static <T> ApiResponse<T> success(final String message) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(final String message) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with data (e.g., validation errors).
     */
    public static <T> ApiResponse<T> error(final String message, final T data) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a fail response (client error).
     */
    public static <T> ApiResponse<T> fail(final String message) {
        return ApiResponse.<T>builder()
                .status("FAIL")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a fail response with data.
     */
    public static <T> ApiResponse<T> fail(final String message, final T data) {
        return ApiResponse.<T>builder()
                .status("FAIL")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
