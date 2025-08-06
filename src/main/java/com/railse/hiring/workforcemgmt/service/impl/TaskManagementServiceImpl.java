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
        System.out.println("Found Task: " + task);
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        System.out.println("Received create request: " + createRequest);
        List<TaskManagement> createdTasks = new ArrayList<>();

        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            System.out.println("Processing item: " + item);

            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");

            System.out.println("Created Task entity: " + newTask);

            TaskManagement saved = taskRepository.save(newTask);
            System.out.println("Saved Task: " + saved);

            createdTasks.add(saved);
        }

       List<TaskManagementDto> dtos = taskMapper.modelListToDtoList(createdTasks);
//        List<TaskManagementDto> dtos = createdTasks.stream()
//                .map(this::toDto)
//                .collect(Collectors.toList());

        System.out.println("Mapped DTOs: (debug): " + dtos);
        return dtos;
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        System.out.println("Received update request: " + updateRequest);
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

            TaskManagement saved = taskRepository.save(task);
            System.out.println("Updated Task: " + saved);
            updatedTasks.add(saved);
        }

        return taskMapper.modelListToDtoList(updatedTasks);
    }

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        System.out.println("Received assign-by-reference request: " + request);
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(
                request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .toList();

            for (TaskManagement oldTask : tasksOfType) {
                oldTask.setStatus(TaskStatus.CANCELLED);
                TaskManagement cancelled = taskRepository.save(oldTask);
                System.out.println("Cancelled old task: " + cancelled);
            }

            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(request.getReferenceId());
            newTask.setReferenceType(request.getReferenceType());
            newTask.setTask(taskType);
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("Task reassigned.");

            TaskManagement saved = taskRepository.save(newTask);
            System.out.println("Assigned new task: " + saved);
        }

        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        System.out.println("Received fetch-by-date request: " + request);
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .filter(task -> {
                    Long deadline = task.getTaskDeadlineTime();
                    return deadline != null &&
                            deadline >= request.getStartDate() &&
                            deadline <= request.getEndDate();
                })
                .collect(Collectors.toList());

        System.out.println("Filtered Tasks: " + filteredTasks);
        return taskMapper.modelListToDtoList(filteredTasks);
    }

//    private TaskManagementDto toDto(TaskManagement task) {
//        TaskManagementDto dto = new TaskManagementDto();
//        dto.setId(task.getId());
//        dto.setReferenceId(task.getReferenceId());
//        dto.setReferenceType(task.getReferenceType());
//        dto.setTask(task.getTask());
//        dto.setDescription(task.getDescription());
//        dto.setStatus(task.getStatus());
//        dto.setAssigneeId(task.getAssigneeId());
//        dto.setTaskDeadlineTime(task.getTaskDeadlineTime());
//        dto.setPriority(task.getPriority());
//        return dto;
//    }
}
