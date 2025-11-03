package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String contactName;

    @Email
    @Size(max = 320)
    private String email;

    @Size(max = 50)
    private String phone;

    @Min(0)
    private Integer leadTimeDays;

    @Size(max = 2048)
    private String address;

    @Size(max = 2048)
    private String notes;

    private Boolean isActive;
}
