package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API response wrapper")
public class ResponseWrapper<T> {
    
    @Schema(description = "Success indicator")
    private Boolean success;
    
    @Schema(description = "Response message")
    private String message;
    
    @Schema(description = "Response data")
    private T data;
    
    @Schema(description = "Error code if failed")
    private String errorCode;
    
    @Schema(description = "Timestamp")
    private LocalDateTime timestamp;
    
    public static <T> ResponseWrapper<T> success(T data, String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static <T> ResponseWrapper<T> error(String message, String errorCode) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}