package ru.rbs.test.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.rbs.test.task.enumeration.ServiceType;

public record TakeTicketRequest(@NotBlank(message = "Имя не должно быть пустым") @Size(max = 100) String clientName,
                                @NotNull(message = "Тип услуги не выбран") ServiceType service) {
}
