package com.railse.hiring.workforcemgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentDto {
    private Long taskId;
    private String comment;
    private Long timestamp;
    private String createdBy;
}
