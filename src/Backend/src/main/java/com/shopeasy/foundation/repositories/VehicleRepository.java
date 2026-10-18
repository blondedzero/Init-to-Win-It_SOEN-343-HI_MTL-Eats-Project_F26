package com.shopeasy.foundation.repositories;

import com.shopeasy.foundation.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {}
