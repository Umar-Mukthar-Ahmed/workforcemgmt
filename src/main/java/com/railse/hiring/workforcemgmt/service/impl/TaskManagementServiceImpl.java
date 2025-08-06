package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;

    public TaskManagementServiceImpl(TaskRepository taskRepository,
                                     ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();

        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");

            createdTasks.add(taskRepository.save(newTask));
        }

        return taskMapper.modelListToDtoList(createdTasks);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();

        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }

            updatedTasks.add(taskRepository.save(task));
        }

        return taskMapper.modelListToDtoList(updatedTasks);
    }

    //  Bug 1: Reassigning cancels old task and creates new task
    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<TaskManagement> existingTasks = taskRepository
                .findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (TaskManagement oldTask : existingTasks) {
            // Cancel the old task
            oldTask.setStatus(TaskStatus.CANCELLED);
            oldTask.setDescription("Task reassigned.");
            taskRepository.save(oldTask);

            // Create a new task with same type but new assignee
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(oldTask.getReferenceId());
            newTask.setReferenceType(oldTask.getReferenceType());
            newTask.setTask(oldTask.getTask());
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("Task reassigned.");
            newTask.setTaskDeadlineTime(oldTask.getTaskDeadlineTime());
            newTask.setPriority(oldTask.getPriority());

            taskRepository.save(newTask);
        }

        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .filter(task -> {
                    Long deadline = task.getTaskDeadlineTime();
                    if (deadline == null) return false;

                    // Smart logic:
                    boolean withinRange = deadline >= request.getStartDate() && deadline <= request.getEndDate();
                    boolean beforeRangeButStillOpen = deadline < request.getStartDate() && task.getStatus() != TaskStatus.COMPLETED;

                    return withinRange || beforeRangeButStillOpen;
                })
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

}

