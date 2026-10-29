package com.shopeasy.Domain;

import com.shopeasy.Foundation.entities.SystemEvent;
import com.shopeasy.Foundation.repositories.SystemEventRepository;
import org.springframework.stereotype.Service;

@Service
public class EventManager {
    private final SystemEventRepository systemEventRepository;

    public EventManager(SystemEventRepository systemEventRepository) {
        this.systemEventRepository = systemEventRepository;
    }

    public Long logEvent(String eventDesc) {
        SystemEvent systemEvent = new SystemEvent(eventDesc);
        SystemEvent savedEvent = systemEventRepository.save(systemEvent);
        return savedEvent.getEventId();
    }

    public SystemEvent getSystemEvent(Long eventId) {
        return systemEventRepository.findById(eventId).orElse(null);
    }

}
