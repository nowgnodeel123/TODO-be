package com.nowgnodeel.todobe.todo.dto;

import com.nowgnodeel.todobe.todo.common.IsDone;

import java.time.LocalDateTime;

public record TodoCreateRequestDto(
        String title,
        LocalDateTime start,
        LocalDateTime end,
        IsDone isDone
) {
}
