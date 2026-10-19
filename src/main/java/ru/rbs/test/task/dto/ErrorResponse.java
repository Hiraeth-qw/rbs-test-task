package ru.rbs.test.task.dto;

import java.util.List;

public record ErrorResponse(Integer status, String message, List<String> details) {
}
