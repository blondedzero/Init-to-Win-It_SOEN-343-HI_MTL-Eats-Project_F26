package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierRepository extends JpaRepository<Courier, Long> {
    public List<Courier> findByStatusIn(List<CourierStatus> status);
    public List<Courier> findByStatusNotIn(List<CourierStatus> status);
}
