package com.example.tintuc.service;

import com.example.tintuc.entity.ActivityLog;
import com.example.tintuc.entity.ActivityType;
import com.example.tintuc.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void record(String actor, ActivityType action, String subject) {
        ActivityLog activity = new ActivityLog();
        activity.setActor(actor);
        activity.setAction(action);
        activity.setSubject(subject);
        activityLogRepository.save(activity);
    }
}
