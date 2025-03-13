package com.patrol.domain.facility.service;


import com.patrol.api.facility.dto.FacilitiesResponse;

import java.util.List;

public interface FacilityService {
  List<FacilitiesResponse> findAll();

  List<FacilitiesResponse> getFacilitiesWithinRadius(
          double latitude,
          double longitude,
          double radius
  );


}
