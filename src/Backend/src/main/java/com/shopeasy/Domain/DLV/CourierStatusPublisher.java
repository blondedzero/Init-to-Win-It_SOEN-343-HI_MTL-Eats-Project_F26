package com.shopeasy.Domain.DLV;

import com.shopeasy.Foundation.entities.Courier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
class CourierStatusPublisher {

    private final List<CourierStatusObserver> observers = new CopyOnWriteArrayList<>();

    void subscribe(CourierStatusObserver observer) {
        observers.add(observer);
    }

    void unsubscribe(CourierStatusObserver observer) {
        observers.remove(observer);
    }

    void notifyCourierIdle(Courier courier) {
        for (CourierStatusObserver observer : observers) {
            observer.onCourierIdle(courier);
        }
    }
}
