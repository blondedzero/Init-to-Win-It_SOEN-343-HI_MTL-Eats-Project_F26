package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {}
