package com.invitation.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawalRequest {

    @NotNull(message = "Số tiền rút không được để trống")
    @Min(value = 10000, message = "Số tiền rút tối thiểu là 10.000 VNĐ")
    private Long amount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String accountHolder;
}
